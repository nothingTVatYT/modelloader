package net.nothingtv.gdx.modelloader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import net.nothingtv.gdx.terrain.TerrainData;
import net.nothingtv.gdx.terrain.TerrainShaderProvider;
import net.nothingtv.gdx.testprojects.BaseMaterials;

public class TerrainTest extends ScreenAdapter {
    private Environment environment;
    private Camera camera;
    private FirstPersonCameraController controller;
    private ModelBatch batch;
    private Renderable renderable;
    private ModelInstance modelInstance;

    @Override
    public void show() {
        init();
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Color.DARK_GRAY, true);
        update(delta);
        batch.begin(camera);
        if (renderable != null)
            batch.render(renderable);
        else
            batch.render(modelInstance);
        batch.end();
    }

    private void update(float delta) {
        controller.update(delta);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
            Gdx.app.exit();
    }

    private void init() {
        //BaseMaterials.generateAlphaMap();
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.9f, 0.9f, 0.7f, 1f));

        camera = new PerspectiveCamera(60, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 2000f;
        camera.position.set(0,1,-3);
        camera.direction.set(Vector3.Z);
        camera.up.set(Vector3.Y);
        camera.update();

        DefaultShader.Config config = new DefaultShader.Config();
        config.numDirectionalLights = 1;
        config.numPointLights = 0;
        config.numBones = 0;
        batch = new ModelBatch(new TerrainShaderProvider(config));

        TerrainData terrainData = new TerrainData(32, 32, 1);
        //renderable = terrainData.createRenderable(environment);
        modelInstance = terrainData.createModelInstance();

        controller = new FirstPersonCameraController(camera);
        controller.setVelocity(16);
        controller.setDegreesPerPixel(0.2f);
        Gdx.input.setInputProcessor(controller);
    }
}
