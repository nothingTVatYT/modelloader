package net.nothingtv.gdx.modelloader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.model.NodeAnimation;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.DebugDrawer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalShadowLight;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneSkybox;
import net.mgsx.gltf.scene3d.shaders.PBRDepthShader;
import net.mgsx.gltf.scene3d.shaders.PBRDepthShaderProvider;
import net.mgsx.gltf.scene3d.shaders.PBRShaderConfig;
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;
import net.nothingtv.gdx.tools.BaseMaterials;
import net.nothingtv.gdx.tools.BaseModels;
import net.nothingtv.gdx.tools.Debug;
import net.nothingtv.gdx.tools.JModelViewer;

public class ModelLoader extends ScreenAdapter {
	private final boolean useIBL = true;
	private final PerspectiveCamera camera;
	private final Cubemap diffuseCubemap;
	private final Cubemap environmentCubemap;
	private final Cubemap specularCubemap;
	private final Texture brdfLUT;
	private SceneSkybox skybox;
	private final DirectionalShadowLight shadowLight;
	private Vector3 modelPosition;
	private final ModelBatch modelBatch;
	private final ModelBatch shadowBatch;
	private final Array<RenderableProvider> renderInstances;
	private final Stage stage;
	private final Skin skin;
	private final Label fpsLabel;
	private final FirstPersonCameraController cameraController;
	private final Environment environment;
	private final GLProfiler glProfiler;
	private final StringBuilder statistics;
	private boolean isFullscreen;
	private boolean useModelViewer;
	private int width;
	private int height;
	private boolean renderShadows = true;
	private JModelViewer modelViewer;
	private final AnimatedModelInstance npc;
	private final Vector3 npcLocation = new Vector3();
	private final Vector3 cameraTarget = new Vector3();
	private DebugDrawer debugDrawer;

	public ModelLoader () {
		statistics = new StringBuilder();
		width = Gdx.graphics.getWidth();
		height = Gdx.graphics.getHeight();
		isFullscreen = Gdx.graphics.isFullscreen();
		if (Gdx.graphics.isFullscreen()) {
			width = Math.round(width * 0.75f);
			height = Math.round(height * 0.75f);
			isFullscreen = true;
		}

		Bullet.init();
		debugDrawer = new DebugDrawer();
		Debug debug = new Debug(debugDrawer);

		PBRShaderConfig config = PBRShaderProvider.createDefaultConfig();
		config.numBones = 60;
		config.numBoneWeights = 8;
		config.numDirectionalLights = 1;
		config.numPointLights = 0;
		modelBatch = new ModelBatch(new PBRShaderProvider(config));

		PBRDepthShader.Config depthConfig = PBRShaderProvider.createDefaultDepthConfig();
		depthConfig.numBones = 60;
		depthConfig.numBoneWeights = 8;
		shadowBatch = new ModelBatch(new PBRDepthShaderProvider(depthConfig));

		glProfiler = new GLProfiler(Gdx.graphics);
		glProfiler.enable();

		stage = new Stage();
		skin = new Skin(Gdx.files.internal("data/uiskin.json"));
		fpsLabel = new Label(getStatistics(), skin);
		stage.addActor(fpsLabel);

		renderInstances = new Array<>();

		modelPosition = new Vector3(5, 0.5f, 5);

		SceneAsset sceneAsset = new GLBLoader().load(Gdx.files.internal("models/Walking.glb"));
		npc = new AnimatedModelInstance(sceneAsset.scene.model, "");
		System.out.printf("Animations found: %d%n", npc.animations.size);
		String animationId = npc.animations.first().id;
		npc.animationController.setAnimation(animationId, -1);
		npc.transform.setTranslation(modelPosition);
		renderInstances.add(npc);

		System.out.printf("first node in modelInstance.nodes: %s%n", npc.nodes.first().id);
		for (NodeAnimation na : npc.animations.first().nodeAnimations) {
			if (na.translation != null && !na.translation.isEmpty()) {
				System.out.printf("NodeAnimation with a translation: %s%n", na.node.id);
			}
		}

		if (useModelViewer) {
			modelViewer = new JModelViewer();
			modelViewer.setVisible(true);
			modelViewer.showModel(sceneAsset.scene.model);
		}

		camera = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		float d = 500f;
		camera.near = d / 1000f;
		camera.far = d * 4;

		// setup light
		float lightIntensity = 1;
		environment = new Environment();
		shadowLight = new DirectionalShadowLight(2048, 2048, 100,  100, 1, 1000);
		shadowLight.set(lightIntensity, lightIntensity, lightIntensity, new Vector3(-0.4f, -0.4f, -0.4f));
		environment.add(shadowLight);
		environment.shadowMap = shadowLight;

		if (useIBL) {
			// setup quick IBL (image based lighting)
			IBLBuilder iblBuilder = IBLBuilder.createOutdoor(shadowLight);
			environmentCubemap = iblBuilder.buildEnvMap(1024);
			diffuseCubemap = iblBuilder.buildIrradianceMap(256);
			specularCubemap = iblBuilder.buildRadianceMap(10);
			iblBuilder.dispose();

			// This texture is provided by the library, no need to have it in your assets.
			brdfLUT = new Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png"));

			environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT));
			environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap));
			environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap));
		}

		// setup skybox
		//skybox = new SceneSkybox(environmentCubemap);
		//sceneManager.setSkyBox(skybox);

		Model floor = BaseModels.createBox(100, 1, 100, BaseMaterials.debugMaterial());
		renderInstances.add(new ModelInstance(floor));

		npc.transform.setTranslation(modelPosition);

		Vector3 camPosition = new Vector3(modelPosition).add(0, 2, -4);
		camera.position.set(camPosition);
		camera.lookAt(modelPosition);
		camera.up.set(Vector3.Y);
		camera.update(true);
		cameraController = new FirstPersonCameraController(camera);
		cameraController.autoUpdate = true;
		Gdx.input.setInputProcessor(cameraController);

		/*
		ModelInstance instance = new ModelInstance(BaseModels.createBox(1, 1, 1, BaseMaterials.whiteColorPBR()));
		Vector3 boxLoc = new Vector3(modelPosition);
		boxLoc.add(3, 0.5f, 0);
		instance.transform.setTranslation(boxLoc);
		renderInstances.add(instance);

		 */

		ModelInstance sphereInstance = new ModelInstance(BaseModels.createSphere(0.1f, BaseMaterials.colorPBR(Color.FIREBRICK)));
		sphereInstance.transform.setTranslation(modelPosition);
		renderInstances.add(sphereInstance);

		shadowLight.setCenter(modelPosition);
	}

	private String getStatistics() {
		statistics.setLength(0);
		int m = renderInstances != null ? renderInstances.size : 0;
		statistics.append("FPS: ").append(Gdx.graphics.getFramesPerSecond()).append("\nMeshes: ").append(m)
				.append("\nGL Calls: ").append(glProfiler.getCalls())
				.append("\nDraw Calls: ").append(glProfiler.getDrawCalls()).append("\nVertices: ").append(glProfiler.getVertexCount().total)
				.append("\nShader switches: ").append(glProfiler.getShaderSwitches());
		return statistics.toString();
	}

	@Override
	public void render(float deltaTime) {
		cameraController.update(deltaTime);

		if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE)) {
			dispose();
			Gdx.app.exit();
		}
		if (Gdx.input.isKeyJustPressed(Input.Keys.F10)) {
			if (isFullscreen) {
				Gdx.graphics.setWindowedMode(width, height);
			} else {
				Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
			}
			isFullscreen = !isFullscreen;
		}
		if (Gdx.input.isKeyJustPressed(Input.Keys.F9)) {
			renderShadows = !renderShadows;
		}

		// render
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		npc.update(deltaTime);

		// smoothly keep the npc on the floor
		npc.transform.getTranslation(npcLocation);
		npcLocation.y = MathUtils.lerp(npcLocation.y, 0.5f, deltaTime);
		npc.transform.setTranslation(npcLocation);
		if (npcLocation.x > 48 || npcLocation.z > 48 || npcLocation.x < -48 || npcLocation.z < -48) {
			npc.transform.rotate(Vector3.Y, 90 * deltaTime);
		}

		if (camera.position.dst2(npcLocation) > 10) {
			cameraTarget.set(npcLocation).add(0, 3, 0);
			camera.position.lerp(cameraTarget, deltaTime * 0.1f);
			camera.lookAt(npcLocation);
			camera.up.set(0, 1, 0);
			camera.update();
		}

		shadowLight.begin();
		shadowBatch.begin(shadowLight.getCamera());
		if (renderShadows) {
			shadowBatch.render(renderInstances);
		}
		shadowBatch.end();
		shadowLight.end();

		modelBatch.begin(camera);
		modelBatch.render(renderInstances, environment);
		modelBatch.end();

		debugDrawer.begin(camera);
		Debug.instance.drawDebugs();
		debugDrawer.end();

		fpsLabel.setText(getStatistics());
		stage.act(Gdx.graphics.getDeltaTime());
		stage.draw();
		glProfiler.reset();
	}

	@Override
	public void resize(int width, int height) {
		camera.viewportWidth = width;
		camera.viewportHeight = height;
		camera.update(true);
	}

	@Override
	public void dispose() {
		if (modelViewer != null)
			modelViewer.dispose();
		environmentCubemap.dispose();
		diffuseCubemap.dispose();
		specularCubemap.dispose();
		brdfLUT.dispose();
		if (skybox != null)
			skybox.dispose();
		glProfiler.disable();
	}
}
