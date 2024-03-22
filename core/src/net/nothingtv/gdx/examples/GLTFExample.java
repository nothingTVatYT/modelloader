package net.nothingtv.gdx.examples;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalShadowLight;
import net.mgsx.gltf.scene3d.scene.*;
import net.mgsx.gltf.scene3d.shaders.PBRDepthShaderProvider;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;
import net.nothingtv.gdx.terrain.TerrainPBRShaderProvider;
import net.nothingtv.gdx.tools.BaseMaterials;
import net.nothingtv.gdx.tools.BaseModels;

public class GLTFExample extends ApplicationAdapter
{
    private SceneManager sceneManager;
    private SceneAsset sceneAsset;
    private Scene scene;
    private PerspectiveCamera camera;
    private Cubemap diffuseCubemap;
    private Cubemap environmentCubemap;
    private Cubemap specularCubemap;
    private Texture brdfLUT;
    private float time;
    private SceneSkybox skybox;

    @Override
    public void create() {

        // create scene
        //sceneAsset = new GLTFLoader().load(Gdx.files.internal("models/BoomBox/glTF/BoomBox.gltf"));
        sceneAsset = new GLTFLoader().load(Gdx.files.internal("models/debug-cube.gltf"));
        scene = new Scene(sceneAsset.scene);
        sceneManager = new SceneManager(new TerrainPBRShaderProvider(TerrainPBRShaderProvider.createDefaultConfig()), new PBRDepthShaderProvider(PBRDepthShaderProvider.createDefaultConfig()));
        sceneManager.addScene(scene);

        ModelInstance boxInstance = new ModelInstance(BaseModels.createBox(10, 1, 10, BaseMaterials.colorPBR(Color.BROWN)));
        boxInstance.transform.setTranslation(0, -3, 0);
        sceneManager.addScene(new Scene(boxInstance));

        // setup camera
        camera = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        float d = .02f;
        camera.near = 1f;
        camera.far = 500f;
        camera.update(true);
        sceneManager.setCamera(camera);

        // setup quick IBL (image based lighting)
        DirectionalShadowLight directionalLight = new DirectionalShadowLight();
        directionalLight.direction.set(1, -3, 1).nor();
        directionalLight.color.set(Color.WHITE);
        IBLBuilder iblBuilder = IBLBuilder.createOutdoor(directionalLight);
        environmentCubemap = iblBuilder.buildEnvMap(1024);
        diffuseCubemap = iblBuilder.buildIrradianceMap(256);
        specularCubemap = iblBuilder.buildRadianceMap(10);
        iblBuilder.dispose();

        brdfLUT = new Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png"));

        CascadeShadowMap cascadeShadowMap = new CascadeShadowMap(3);
        //cascadeShadowMap.lights.add(directionalLight);
        cascadeShadowMap.setCascades(camera, directionalLight, 0, 4);
        sceneManager.environment.add(directionalLight);
        sceneManager.setCascadeShadowMap(cascadeShadowMap);
        sceneManager.setAmbientLight(1f);
        sceneManager.environment.add(directionalLight);
        sceneManager.environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT));
        sceneManager.environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap));
        sceneManager.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap));

        // setup skybox
        skybox = new SceneSkybox(environmentCubemap);
        sceneManager.setSkyBox(skybox);
    }

    @Override
    public void resize(int width, int height) {
        sceneManager.updateViewport(width, height);
    }

    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();
        time += deltaTime;

        // animate camera
        camera.position.setFromSpherical(MathUtils.PI/4, time * .3f).scl(3f);
        camera.up.set(Vector3.Y);
        camera.lookAt(Vector3.Zero);
        camera.update();

        // render
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        sceneManager.update(deltaTime);
        sceneManager.render();
    }

    @Override
    public void dispose() {
        sceneManager.dispose();
        sceneAsset.dispose();
        environmentCubemap.dispose();
        diffuseCubemap.dispose();
        specularCubemap.dispose();
        brdfLUT.dispose();
        skybox.dispose();
    }
}