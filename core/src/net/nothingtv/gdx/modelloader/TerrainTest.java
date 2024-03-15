package net.nothingtv.gdx.modelloader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import net.nothingtv.gdx.terrain.Terrain;
import net.nothingtv.gdx.terrain.TerrainConfig;
import net.nothingtv.gdx.terrain.TerrainShaderProvider;
import net.nothingtv.gdx.testprojects.BaseMaterials;

public class TerrainTest extends ScreenAdapter {
    private Environment environment;
    private Camera camera;
    private FirstPersonCameraController controller;
    private ModelBatch batch, shadowBatch;
    private ModelInstance modelInstance;
    private DirectionalShadowLight directionalLight;

    @Override
    public void show() {
        init();
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Color.DARK_GRAY, true);
        update(delta);
        directionalLight.begin();
        shadowBatch.begin(directionalLight.getCamera());
        shadowBatch.render(modelInstance);
        shadowBatch.end();
        directionalLight.end();

        batch.begin(camera);
        batch.render(modelInstance, environment);
        batch.end();
    }

    private void update(float delta) {
        controller.update(delta);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
            Gdx.app.exit();
        if (Gdx.input.isKeyJustPressed(Input.Keys.G) && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT))
            new Thread(BaseMaterials::generateAlphaMap).start();
    }

    private void init() {
        Color sunLightColor = Color.WHITE;
        Color ambientLightColor = Color.DARK_GRAY;
        Vector3 sunDirection = new Vector3(1, -1, 1).nor();
        int shadowMapSize = 1024;
        float shadowViewportSize = 60;
        float shadowNear = 1;
        float shadowFar = 1000;
        directionalLight = new DirectionalShadowLight(shadowMapSize, shadowMapSize, shadowViewportSize, shadowViewportSize, shadowNear, shadowFar);
        directionalLight.set(sunLightColor, sunDirection);

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, ambientLightColor));
        environment.add(directionalLight);
        environment.shadowMap = directionalLight;

        camera = new PerspectiveCamera(60, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 2000f;
        camera.position.set(6,10,0);
        camera.lookAt(new Vector3(16, 0, 16));
        camera.up.set(Vector3.Y);
        camera.update();

        DefaultShader.Config config = new DefaultShader.Config();
        config.numDirectionalLights = 1;
        config.numPointLights = 0;
        config.numBones = 0;
        batch = new ModelBatch(new TerrainShaderProvider(config));

        shadowBatch = new ModelBatch(new TerrainShaderProvider(config));

        TerrainConfig terrainConfig = new TerrainConfig(256, 256, 1);
        terrainConfig.addLayer(new Texture("textures/Ground048_2K_Color.jpg"), 16f);
        terrainConfig.addLayer(new Texture("textures/Ground026_2K_Color.jpg"), 16f);
        terrainConfig.addLayer(new Texture("textures/leafy_grass_diff_2k.jpg"), 16f);
        terrainConfig.addLayer(new Texture("textures/cobblestone_floor_07_diff_2k.jpg"), 16f);
        terrainConfig.splatMap = new Texture("textures/alpha-example.png");
        //terrainConfig.heightSampler = new TestHeightSampler();
        terrainConfig.setHeightMap(new Pixmap(Gdx.files.internal("textures/heightmap.png")), 50, -40);
        Terrain terrain = new Terrain(terrainConfig);
        modelInstance = terrain.createModelInstance();

        controller = new FirstPersonCameraController(camera);
        controller.setVelocity(16);
        controller.setDegreesPerPixel(0.2f);
        Gdx.input.setInputProcessor(controller);
    }
}
