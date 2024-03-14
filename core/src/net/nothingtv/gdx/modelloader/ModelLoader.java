package net.nothingtv.gdx.modelloader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.SphereShapeBuilder;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
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
import net.nothingtv.gdx.terrain.SimpleTerrain;
import net.nothingtv.gdx.testprojects.BaseMaterials;
import net.nothingtv.gdx.testprojects.BaseModels;
import net.nothingtv.gdx.tools.ModelOptimizer;

public class ModelLoader extends ScreenAdapter {
	private final boolean useIBL = true;
	private final boolean useMarkers = true;
	private PerspectiveCamera camera;
	private Cubemap diffuseCubemap;
	private Cubemap environmentCubemap;
	private Cubemap specularCubemap;
	private Texture brdfLUT;
	private SceneSkybox skybox;
	private DirectionalShadowLight shadowLight;
	private Vector3 modelPosition;
	private SimpleTerrain terrain;
	private ModelBatch modelBatch;
	private ModelBatch shadowBatch;
	private Array<RenderableProvider> renderInstances;
	private Stage stage;
	private Skin skin;
	private Label fpsLabel;
	private AnimationController animationController;
	private FirstPersonCameraController cameraController;
	private Environment environment;
	private GLProfiler glProfiler;
	private final StringBuilder statistics;
	private boolean isFullscreen;
	private int width;
	private int height;
	private boolean renderShadows = true;
	private boolean useRay = true;
	private final Vector3 down = new Vector3(0, -1, 0);

	public ModelLoader () {
		statistics = new StringBuilder();
		/*
		for (Graphics.DisplayMode mode : Gdx.graphics.getDisplayModes())
			System.out.println("Display mode: " + mode);
		System.out.println("Current display mode: " + Gdx.graphics.getDisplayMode());

		 */
		width = Gdx.graphics.getWidth();
		height = Gdx.graphics.getHeight();
		isFullscreen = Gdx.graphics.isFullscreen();
		if (Gdx.graphics.isFullscreen()) {
			width = Math.round(width * 0.75f);
			height = Math.round(height * 0.75f);
			isFullscreen = true;
		}

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

		modelPosition = new Vector3(100, 40, 100);

		SceneAsset sceneAsset = new GLBLoader().load(Gdx.files.internal("assets/models/Walking.glb"));
		ModelInstance modelInstance = new ModelInstance(sceneAsset.scene.model);
		animationController = new AnimationController(modelInstance);
		animationController.setAnimation("mixamo.com", -1);
		modelInstance.transform.setTranslation(modelPosition);
		renderInstances.add(modelInstance);

		camera = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		float d = 500f;
		camera.near = d / 1000f;
		camera.far = d * 4;

		// setup light
		float lightIntensity = 1;
		environment = new Environment();
		shadowLight = new DirectionalShadowLight(2048, 2048, 10,  10, 1, 1000);
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

		createTerrain();

		modelPosition.y = 33;
		snapToTerrain(modelPosition);
		modelInstance.transform.setTranslation(modelPosition);

		Vector3 camPosition = new Vector3(modelPosition).add(0, 2, -4);
		camera.position.set(camPosition);
		camera.lookAt(modelPosition);
		camera.up.set(Vector3.Y);
		camera.update(true);
		cameraController = new FirstPersonCameraController(camera);
		cameraController.autoUpdate = true;
		Gdx.input.setInputProcessor(cameraController);

		ModelInstance instance = new ModelInstance(createBoxModel(1, 1, 1, null));
		Vector3 boxLoc = new Vector3(modelPosition);
		boxLoc.add(3, 0, 0);
		boxLoc.y = getHeightAt(boxLoc) + 0.5f;
		instance.transform.setTranslation(boxLoc);
		renderInstances.add(instance);

		if (useMarkers) {
			ModelOptimizer optimizer = new ModelOptimizer();
			//Model markerModel = createSphereModel(0.15f, BaseMaterials.debugMaterial());
			Model markerModel = createBoxModel(0.15f, 0.15f, 0.15f, BaseMaterials.debugMaterial());
			System.out.printf("player position is %s%n", modelPosition);
			BaseModels.dumpModel(markerModel, "Marker");
			Vector3 loc = new Vector3();
			for (int z = 0; z < 30; z++)
				for (int x = 0; x < 30; x++) {
					loc.set(modelPosition);
					loc.add((x-15) * 1f, 0, (z-15) * 1f);
					snapToTerrain(loc);
					ModelInstance inst = new ModelInstance(markerModel);
					inst.transform.setTranslation(loc);
					optimizer.add(inst);
				}
			ModelInstance combined = optimizer.getCombinedModelInstance();
			BaseModels.dumpModel(combined.model, "combined");
			renderInstances.add(combined);
		}

		ModelInstance sphereInstance = new ModelInstance(createSphereModel(0.1f, null));
		sphereInstance.transform.setTranslation(modelPosition);
		renderInstances.add(sphereInstance);

		shadowLight.setCenter(modelPosition);
	}

	private Vector3 snapToTerrain(Vector3 pos) {
        pos.y = getHeightAt(pos);
		return pos;
	}

	private float getHeightAt(Vector3 pos) {
		Vector3 begin = new Vector3(pos).add(0, 1000, 0);
		Vector3 loc = new Vector3(pos);
		if (useRay) {
			Ray ray = new Ray(begin, down);
			if (terrain.rayIntersects(ray, loc))
				return loc.y;
			return 0;
		}
		terrain.worldToLocal(loc);
		return terrain.getHeightAt(loc);
	}

	private void createTerrain() {
		Pixmap heightMap = new Pixmap(Gdx.files.internal("textures/heightmap.png"));
		terrain = new SimpleTerrain(heightMap, 50);
		renderInstances.add(terrain.modelInstance);
	}

	private Model createBoxModel(float width, float depth, float height, Material material) {
		if (material == null)
			material = new Material();
		ModelBuilder modelBuilder = new ModelBuilder();
		modelBuilder.begin();
		MeshPartBuilder meshBuilder = modelBuilder.part("box", GL20.GL_TRIANGLES, VertexAttribute.Position().usage |VertexAttribute.Normal().usage | VertexAttribute.TexCoords(0).usage, material);

		BoxShapeBuilder.build(meshBuilder, width, height, depth);
        return modelBuilder.end();
	}

	private Model createSphereModel(float radius, Material material) {
		if (material == null)
			material = new Material();
		ModelBuilder modelBuilder = new ModelBuilder();
		modelBuilder.begin();
		MeshPartBuilder meshBuilder = modelBuilder.part("sphere", GL20.GL_TRIANGLES, VertexAttribute.Position().usage |VertexAttribute.Normal().usage | VertexAttribute.TexCoords(0).usage, material);

		SphereShapeBuilder.build(meshBuilder, radius, radius, radius, 8,8);
		return modelBuilder.end();
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

		if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE))
			Gdx.app.exit();
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
		animationController.update(deltaTime);

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
		environmentCubemap.dispose();
		diffuseCubemap.dispose();
		specularCubemap.dispose();
		brdfLUT.dispose();
		skybox.dispose();
		glProfiler.disable();
	}
}
