package net.nothingtv.gdx.modelloader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.shaders.DepthShader;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalShadowLight;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneSkybox;
import net.mgsx.gltf.scene3d.shaders.PBRDepthShaderProvider;
import net.mgsx.gltf.scene3d.shaders.PBRShaderConfig;
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;
import net.nothingtv.gdx.terrain.Terrain;
import net.nothingtv.gdx.terrain.TerrainConfig;
import net.nothingtv.gdx.terrain.TerrainPBRShaderProvider;
import net.nothingtv.gdx.testprojects.BaseMaterials;

public class TerrainTest extends ScreenAdapter {
    private Environment environment;
    private Camera camera;
    private FirstPersonCameraController controller;
    private ModelBatch pbrBatch, shadowBatch;
    private ModelInstance terrainInstance;
    private ModelInstance testObject;
    private DirectionalShadowLight directionalLight;
    private final Array<RenderableProvider> renderableProviders = new Array<>();
    private boolean useIBL = true;
    private Cubemap environmentCubemap;
    private Cubemap diffuseCubemap;
    private Cubemap specularCubemap;
    private Texture brdfLUT;
    private SceneSkybox skybox;

    @Override
    public void show() {
        init();
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Color.DARK_GRAY, true);
        //Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT | (Gdx.graphics.getBufferFormat().coverageSampling?GL20.GL_COVERAGE_BUFFER_BIT_NV:0));
        update(delta);
        directionalLight.begin();
        shadowBatch.begin(directionalLight.getCamera());
        shadowBatch.render(renderableProviders);
        shadowBatch.end();
        directionalLight.end();

        pbrBatch.begin(camera);
        pbrBatch.render(renderableProviders, environment);
        pbrBatch.end();
    }

    private void update(float delta) {
        controller.update(delta);
        skybox.update(camera,  delta);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
            Gdx.app.exit();
        if (Gdx.input.isKeyJustPressed(Input.Keys.G) && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT))
            new Thread(BaseMaterials::generateAlphaMap).start();
        directionalLight.direction.rotate(Vector3.X, 30 * delta).rotate(Vector3.Y, 45 * delta);
    }

    private void init() {
        Color sunLightColor = Color.WHITE;
        Color ambientLightColor = Color.GRAY;
        Vector3 sunDirection = new Vector3(-0.4f, -0.4f, -0.4f).nor();
        int shadowMapSize = 2048;
        float shadowViewportSize = 60;
        float shadowNear = 0.1f;
        float shadowFar = 500;
        directionalLight = new DirectionalShadowLight(shadowMapSize, shadowMapSize, shadowViewportSize, shadowViewportSize, shadowNear, shadowFar);
        directionalLight.set(sunLightColor, sunDirection);

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, ambientLightColor));
        environment.add(directionalLight);
        environment.shadowMap = directionalLight;

        if (useIBL) {
            // setup quick IBL (image based lighting)
            IBLBuilder iblBuilder = IBLBuilder.createOutdoor(directionalLight);
            environmentCubemap = iblBuilder.buildEnvMap(1024);
            diffuseCubemap = iblBuilder.buildIrradianceMap(256);
            specularCubemap = iblBuilder.buildRadianceMap(10);
            iblBuilder.dispose();

            // This texture is provided by the library, no need to have it in your assets.
            brdfLUT = new Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png"));

            environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT));
            environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap));
            environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap));
            skybox = new SceneSkybox(environmentCubemap);
            renderableProviders.add(skybox);
        }

        camera = new PerspectiveCamera(60, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 2000f;
        camera.position.set(-6,20,-6);
        camera.lookAt(new Vector3(16, 4, 16));
        camera.up.set(Vector3.Y);
        camera.update();

        PBRShaderConfig pbrConfig = PBRShaderProvider.createDefaultConfig();
        pbrConfig.numBones = 60;
        pbrConfig.numBoneWeights = 8;
        pbrConfig.numDirectionalLights = 1;
        pbrConfig.numPointLights = 0;

        pbrBatch = new ModelBatch(new TerrainPBRShaderProvider(pbrConfig));

        DepthShader.Config depthConfig = PBRShaderProvider.createDefaultDepthConfig();
        depthConfig.numBones = 60;
        depthConfig.numBoneWeights = 8;
        shadowBatch = new ModelBatch(new PBRDepthShaderProvider(depthConfig));

        TerrainConfig terrainConfig = new TerrainConfig(256, 256, 1);
        terrainConfig.addLayer(new Texture("textures/Ground048_2K_Color.jpg"), 16f);
        terrainConfig.addLayer(new Texture("textures/Ground026_2K_Color.jpg"), 16f);
        terrainConfig.addLayer(new Texture("textures/leafy_grass_diff_2k.jpg"), 16f);
        terrainConfig.addLayer(new Texture("textures/cobblestone_floor_07_diff_2k.jpg"), 16f);
        terrainConfig.splatMap = new Texture("textures/alpha-example.png");
        terrainConfig.terrainDivideFactor = 2;
        terrainConfig.setHeightMap(new Pixmap(Gdx.files.internal("textures/heightmap.png")), 50, -40);
        Terrain terrain = new Terrain(terrainConfig);
        terrainInstance = terrain.createModelInstance();

        SceneAsset cubeAsset = new GLTFLoader().load(Gdx.files.internal("models/debug-cube.gltf"));
        testObject = new ModelInstance(cubeAsset.scene.model);
        //testObject = new ModelInstance(BaseModels.createSphere(1, BaseMaterials.whiteColorPBR()));
        Vector3 testLocation = new Vector3(17, 0, 17);
        BoundingBox boundingBox = new BoundingBox();
        testObject.calculateBoundingBox(boundingBox);
        testLocation.y = terrain.getHeightAt(testLocation.x, testLocation.z) + boundingBox.getHeight()/2;
        testObject.transform.setTranslation(testLocation);

        renderableProviders.add(terrainInstance);
        renderableProviders.add(testObject);

        controller = new FirstPersonCameraController(camera);
        controller.setVelocity(16);
        controller.setDegreesPerPixel(0.2f);
        Gdx.input.setInputProcessor(controller);
    }

    @Override
    public void dispose() {
        super.dispose();
        environmentCubemap.dispose();
        diffuseCubemap.dispose();
        specularCubemap.dispose();
        brdfLUT.dispose();
        skybox.dispose();
    }
}
