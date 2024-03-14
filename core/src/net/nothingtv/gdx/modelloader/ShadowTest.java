package net.nothingtv.gdx.modelloader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.shaders.DepthShader;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalShadowLight;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.shaders.PBRDepthShaderProvider;
import net.mgsx.gltf.scene3d.shaders.PBRShaderConfig;
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider;
import net.nothingtv.gdx.testprojects.BaseMaterials;
import net.nothingtv.gdx.testprojects.BaseModels;

import java.util.logging.Logger;

public class ShadowTest extends ScreenAdapter {

    private static final Logger logger = Logger.getLogger(ShadowTest.class.getName());
    private PerspectiveCamera camera;
    private CameraInputController cameraController;
    private Environment environment;
    private DirectionalShadowLight shadowLight;
    private ModelBatch modelBatch;
    private ModelBatch shadowBatch;
    private Array<ModelInstance> renderInstances;

    public ShadowTest() {
        camera = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.2f;
        camera.far = 1000f;
        camera.position.set(0, 10, 20);
        camera.lookAt(0, 0, 0);
        camera.up.set(Vector3.Y);
        camera.update();

        float lightIntensity = 1;
        environment = new Environment();
        //environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.2f, 0.2f, 0.2f, 1f));
        shadowLight = new DirectionalShadowLight(2048,2048, 30,30,1, 500);
        shadowLight.set(lightIntensity, lightIntensity, lightIntensity, new Vector3(-0.4f, -0.4f, -0.4f));
        environment.add(shadowLight);
        environment.shadowMap = shadowLight;

        cameraController = new CameraInputController(camera);
        Gdx.input.setInputProcessor(cameraController);

        PBRShaderConfig config = PBRShaderProvider.createDefaultConfig();
        config.numBones = 60;
        config.numBoneWeights = 8;
        config.numDirectionalLights = 1;
        config.numPointLights = 0;
        modelBatch = new ModelBatch(new PBRShaderProvider(config));

        DepthShader.Config depthConfig = PBRShaderProvider.createDefaultDepthConfig();
        depthConfig.numBones = 60;
        depthConfig.numBoneWeights = 8;
        shadowBatch = new ModelBatch(new PBRDepthShaderProvider(depthConfig));

        renderInstances = new Array<>();

        createFloor(20, 1, 20);

        SceneAsset cubeAsset = new GLTFLoader().load(Gdx.files.internal("models/debug-cube.gltf"));
        ModelInstance cubeInstance = new ModelInstance(cubeAsset.scene.model);
        cubeInstance.transform.setTranslation(-2, 2, 0);
        renderInstances.add(cubeInstance);

        Model box = createBox(1, 1, 1, BaseMaterials.debugMaterial());
        ModelInstance boxInstance = new ModelInstance(box);
        boxInstance.transform.setTranslation(0, 2, 0);
        //boxInstance.materials.first().set(ColorAttribute.createDiffuse(Color.YELLOW));
        renderInstances.add(boxInstance);
        boxInstance = new ModelInstance(box);
        boxInstance.transform.setTranslation(2, 2, 0);
        renderInstances.add(boxInstance);

        BaseMaterials.dumpMaterial(boxInstance.materials.first(), "generated");
        BaseMaterials.dumpMaterial(cubeInstance.materials.first(), "blender model");
        BaseModels.dumpModel(box, "generated");
    }

    @Override
    public void render(float delta) {
        cameraController.update();
        //Gdx.graphics.getBufferFormat().samples = 3;
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT | (Gdx.graphics.getBufferFormat().coverageSampling?GL20.GL_COVERAGE_BUFFER_BIT_NV:0));
        //ScreenUtils.clear(Color.BLACK, true);

        shadowLight.begin();
        shadowBatch.begin(shadowLight.getCamera());
        shadowBatch.render(renderInstances);
        shadowBatch.end();
        shadowLight.end();

        modelBatch.begin(camera);
        modelBatch.render(renderInstances, environment);
        modelBatch.end();
    }

    private void createFloor(float width, float height, float depth) {
        createFloor(width, height, depth, null);
    }

    private void createFloor(float width, float height, float depth, Material material) {
        if (material == null)
            material = BaseMaterials.missingMaterial();
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        MeshPartBuilder meshBuilder = modelBuilder.part("floor", GL20.GL_TRIANGLES, VertexAttribute.Position().usage |VertexAttribute.Normal().usage, material);

        BoxShapeBuilder.build(meshBuilder, width, height, depth);
        Model floor = modelBuilder.end();

        ModelInstance floorInstance = new ModelInstance(floor);
        floorInstance.materials.first().set(PBRColorAttribute.createBaseColorFactor(Color.BROWN));
        floorInstance.transform.trn(0, -0.5f, 0f);

        renderInstances.add(floorInstance);
    }

    private Model createBox(float width, float height, float depth, Material material) {
        if (material == null)
            material = BaseMaterials.missingMaterial();
        return new ModelBuilder().createBox(width, height, depth, material, VertexAttribute.Position().usage|VertexAttribute.Normal().usage|VertexAttribute.TexCoords(0).usage);
        //ModelBuilder modelBuilder = new ModelBuilder();
        //modelBuilder.begin();
        //MeshPartBuilder meshBuilder = modelBuilder.part("box", GL20.GL_TRIANGLES, VertexAttribute.Position().usage|VertexAttribute.Normal().usage|VertexAttribute.TexCoords(0).usage, material);

        //modelBuilder.createBox(width, height, depth, material, VertexAttribute.Position().usage|VertexAttribute.Normal().usage|VertexAttribute.TexCoords(0).usage);
        //BoxShapeBuilder.build(meshBuilder, width, height, depth);
        //return modelBuilder.end();
    }
}
