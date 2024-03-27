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
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.DebugDrawer;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.btConstraintSolver;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
import com.badlogic.gdx.physics.bullet.linearmath.btScalarArray;
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
import net.nothingtv.gdx.tools.BaseMaterials;
import net.nothingtv.gdx.tools.Debug;

public class TerrainTest extends ScreenAdapter {
    private Environment environment;
    private Camera camera;
    private FirstPersonCameraController controller;
    private ModelBatch pbrBatch, shadowBatch;
    private Terrain terrain;
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
    private DebugDrawer debugDraw;
    private Debug debug;
    private btBroadphaseInterface broadphase;
    private btCollisionConfiguration collisionConfig;
    private btDispatcher dispatcher;
    private btDiscreteDynamicsWorld physicsWorld;
    private btConstraintSolver solver;
    private btRigidBody terrainBody;

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

        if (debugDraw != null && !physicsWorld.isDisposed()) {
            debugDraw.begin(camera);
            physicsWorld.debugDrawWorld();
            debug.drawDebugs();
            debugDraw.end();
        }
    }

    private void update(float delta) {
        controller.update(delta);
        skybox.update(camera, delta);
        physicsWorld.stepSimulation(delta);

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
            Gdx.app.exit();
        if (Gdx.input.isKeyJustPressed(Input.Keys.G) && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT))
            new Thread(BaseMaterials::generateAlphaMap).start();
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            Ray ray = camera.getPickRay(Gdx.input.getX(), Gdx.input.getY());
            Vector3 rayFrom = new Vector3(ray.origin);
            // rayTo = rayFrom + (direction scaled)
            Vector3 rayTo = new Vector3(ray.direction).scl(200).add(rayFrom);
            AllHitsRayResultCallback cba = new AllHitsRayResultCallback(rayFrom, rayTo);
            ClosestRayResultCallback cb = new ClosestRayResultCallback(rayFrom, rayTo);
            cb.setCollisionObject(null);
            cb.setClosestHitFraction(1f);
            cba.setClosestHitFraction(1f);
            physicsWorld.rayTest(rayFrom, rayTo, cba);
            System.out.printf("Checking ray (%s to %s) on the physics world with %d objects.%n", rayFrom, rayTo, physicsWorld.getNumCollisionObjects());
            if (cba.hasHit()) {
                Vector3 endPoint = new Vector3();
                //cb.getHitPointWorld(endPoint);
                Vector3 boundMin = new Vector3();
                Vector3 boundMax = new Vector3();
                ((btRigidBody)cba.getCollisionObject()).getAabb(boundMin, boundMax);
                btScalarArray fractions = cba.getHitFractions();
                for (int i = 0; i < fractions.size(); i++)
                    System.out.printf("Hit Fraction #%d: %f%n", i, fractions.atConst(i));
                /*
                endPoint.set(rayFrom).lerp(rayTo, cb.getClosestHitFraction());
                System.out.printf("We hit something at %s (%f) with bounds %s to %s.%n", endPoint, cb.getClosestHitFraction(), boundMin, boundMax);
                if (terrain.boundingBox.contains(endPoint))
                    testObject.transform.setTranslation(endPoint);

                 */
            } else {
                Vector3 boundMin = new Vector3();
                Vector3 boundMax = new Vector3();
                terrainBody.getAabb(boundMin, boundMax);
                BoundingBox bounds = new BoundingBox();
                terrainInstance.calculateBoundingBox(bounds);
                System.out.printf("Hit nothing. Terrain would be at %s - %s, terrain model is at %s%n", boundMin, boundMax, bounds);
            }
            /*
            ModelIntersector.IntersectionResult result = ModelIntersector.intersect(ray, terrainInstance);
            if (result != null) {
                testObject.transform.setTranslation(result.intersection);
                System.out.printf("intersect with node %s at %s%n", result.node.id, result.triangle);
                debugDraw.drawTriangle(result.triangle.v1, result.triangle.v2, result.triangle.v3, Color.CYAN);
                debugDraw.drawCoordinates(result.triangle.v1, Color.BLUE);
                debugDraw.drawCoordinates(result.triangle.v2, Color.BLUE);
                debugDraw.drawCoordinates(result.triangle.v3, Color.BLUE);
                debugDraw.drawBox(result.node.calculateBoundingBox(new BoundingBox()), Color.CYAN);
            }*/
        }
        directionalLight.setCenter(camera.position);
        directionalLight.direction.rotate(Vector3.X, 30 * delta).rotate(Vector3.Y, 45 * delta);
    }

    private void init() {
        Bullet.init();
        initPhysics();

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
        terrainConfig.splatMap = new Pixmap(Gdx.files.internal("textures/alpha-example.png"));
        terrainConfig.terrainDivideFactor = 4;
        terrainConfig.setHeightMap(new Pixmap(Gdx.files.internal("textures/heightmap.png")), 50, -40);
        terrain = new Terrain(terrainConfig);
        terrainInstance = terrain.createModelInstance();
        //terrainInstance.transform.setTranslation(1, -1, 1);
        terrainBody = terrain.createRigidBody();
        physicsWorld.addCollisionObject(terrainBody);

        debugDraw = new DebugDrawer();
        debugDraw.setDebugMode(btIDebugDraw.DebugDrawModes.DBG_NoDebug);
        physicsWorld.setDebugDrawer(debugDraw);
        debug = new Debug(debugDraw);

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

        debug.drawArrow("terrain origin", terrainInstance.transform.getTranslation(new Vector3()), Color.BROWN);
        //BaseModels.dumpModel(terrainInstance.model, "terrain");
        controller = new FirstPersonCameraController(camera);
        controller.setVelocity(16);
        controller.setDegreesPerPixel(0.2f);
        Gdx.input.setInputProcessor(controller);
    }

    private void initPhysics() {
        broadphase = new btDbvtBroadphase();
        collisionConfig = new btDefaultCollisionConfiguration();
        solver =  new btSequentialImpulseConstraintSolver();
        dispatcher = new btCollisionDispatcher(collisionConfig);
        physicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfig);
        System.out.printf("Bullet %d initialized.%n", Bullet.VERSION);
    }

    @Override
    public void dispose() {
        super.dispose();
        environmentCubemap.dispose();
        diffuseCubemap.dispose();
        specularCubemap.dispose();
        brdfLUT.dispose();
        skybox.dispose();
        physicsWorld.dispose();
        collisionConfig.dispose();
        broadphase.dispose();
        dispatcher.dispose();
        terrainBody.dispose();
        solver.dispose();
    }
}
