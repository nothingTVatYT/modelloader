package net.nothingtv.gdx.modelloader;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.shaders.DepthShader;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.badlogic.gdx.math.Vector3;
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
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalShadowLight;
import net.mgsx.gltf.scene3d.scene.SceneSkybox;
import net.mgsx.gltf.scene3d.shaders.PBRDepthShaderProvider;
import net.mgsx.gltf.scene3d.shaders.PBRShaderConfig;
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;
import net.nothingtv.gdx.terrain.TerrainPBRShaderProvider;
import net.nothingtv.gdx.tools.DebugDraw;
import net.nothingtv.gdx.tools.PickResult;
import net.nothingtv.gdx.tools.SceneObject;

public abstract class BasicScreen implements Screen {
    protected Game game;
    protected ScreenConfig screenConfig = new ScreenConfig();
    protected btDbvtBroadphase broadphase;
    protected btCollisionConfiguration collisionConfig;
    protected btConstraintSolver solver;
    protected btDispatcher dispatcher;
    protected btDiscreteDynamicsWorld physicsWorld;
    protected Environment environment;
    protected Camera camera;
    protected ModelBatch pbrBatch, shadowBatch;
    protected DebugDraw debugDraw;
    protected final Array<RenderableProvider> renderableProviders = new Array<>();
    protected FirstPersonCameraController cameraController;
    protected DirectionalShadowLight directionalLight;
    protected Cubemap environmentCubemap;
    protected Cubemap diffuseCubemap;
    protected Cubemap specularCubemap;
    protected Texture brdfLUT;
    protected SceneSkybox skybox;
    protected Color backgroundColor = Color.DARK_GRAY;
    protected float gameTime;
    protected DebugDrawer debugDrawer;

    public BasicScreen(Game game) {
        this.game = game;
    }

    protected void init() {
        gameTime = 0;
        if (screenConfig.usePhysics)
            initPhysics();
        initEnvironment();
        initCamera();
        initBatches();
        initController();
        initScene();
    }

    protected void initPhysics() {
        Bullet.init();
        broadphase = new btDbvtBroadphase();
        collisionConfig = new btDefaultCollisionConfiguration();
        solver =  new btSequentialImpulseConstraintSolver();
        dispatcher = new btCollisionDispatcher(collisionConfig);
        physicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfig);
        debugDrawer = new DebugDrawer();
        debugDrawer.setDebugMode(btIDebugDraw.DebugDrawModes.DBG_NoDebug);
        physicsWorld.setDebugDrawer(debugDrawer);
        System.out.printf("Bullet \"%d\" initialized.%n", Bullet.VERSION);
    }

    protected void updatePhysics(float delta) {
        physicsWorld.stepSimulation(delta, 4, 1f/60);
    }

    protected void initEnvironment() {
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
        if (screenConfig.useSkybox) {
            skybox = new SceneSkybox(environmentCubemap);
            renderableProviders.add(skybox);
        }
    }

    protected void initBatches() {
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

        debugDraw = new DebugDraw(camera, environment);
    }

    protected void initCamera() {
        camera = new PerspectiveCamera(60, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 2000f;
        camera.position.set(-6,2,-6);
        camera.lookAt(new Vector3(0, 1, 0));
        camera.up.set(Vector3.Y);
        camera.update();
    }

    protected void initController() {
        cameraController = new FirstPersonCameraController(camera);
        cameraController.setVelocity(16);
        cameraController.setDegreesPerPixel(0.2f);
        cameraController.autoUpdate = true;
        Gdx.input.setInputProcessor(cameraController);
    }

    public void initScene() {}

    public void updateController(float delta) {
        if (cameraController != null)
            cameraController.update(delta);
    }

    public void updateScene(float delta) {
        if (skybox != null)
            skybox.update(camera, delta);
    }

    public void update(float delta) {
        updateController(delta);
        if (screenConfig.usePhysics)
            updatePhysics(delta);
        updateScene(delta);
    }

    @Override
    public void render(float delta) {
        gameTime += delta;
        update(delta);
        directionalLight.setCenter(camera.position);
        ScreenUtils.clear(backgroundColor, true);

        directionalLight.begin();
        shadowBatch.begin(directionalLight.getCamera());
        shadowBatch.render(renderableProviders);
        shadowBatch.end();
        directionalLight.end();

        pbrBatch.begin(camera);
        pbrBatch.render(renderableProviders, environment);
        pbrBatch.end();

        debugDraw.render();

        if (debugDrawer != null && !physicsWorld.isDisposed()) {
            debugDrawer.begin(camera);
            physicsWorld.debugDrawWorld();
            debugDrawer.end();
        }
    }

    /**
     * Create a new ModelInstance from this model and add it to the scene.
     * @param model the model to instantiate
     * @return the model instance created
     */
    public SceneObject add(String name, Model model) {
        ModelInstance instance = new ModelInstance(model);
        return add(name, instance);
    }

    /**
     * Add a model instance to the scene.
     * @param modelInstance the model instance to be added
     * @return the model instance added
     */
    public SceneObject add(String name, ModelInstance modelInstance) {
        renderableProviders.add(modelInstance);
        return new SceneObject(name, modelInstance);
    }

    public void wrapRigidBody(SceneObject sceneObject, float mass, btCollisionShape collisionShape) {
        DefaultMotionState motionState = new DefaultMotionState(sceneObject.modelInstance);
        Vector3 localInertia = new Vector3();
        collisionShape.calculateLocalInertia(mass, localInertia);
        btRigidBody.btRigidBodyConstructionInfo info = new btRigidBody.btRigidBodyConstructionInfo(mass, motionState, collisionShape, localInertia);
        btRigidBody rigidBody = new btRigidBody(info);
        rigidBody.userData = sceneObject;
        if (physicsWorld != null)
            physicsWorld.addRigidBody(rigidBody);
        sceneObject.setRigidBody(rigidBody, motionState);
        Vector3 vCenter = new Vector3();
        Vector3 pCenter = new Vector3();
        sceneObject.boundingBox.getCenter(vCenter);
        sceneObject.physicsBoundingBox.getCenter(pCenter);
        motionState.rigidBodyOffset.set(vCenter).sub(pCenter);
        rigidBody.translate(motionState.rigidBodyOffset);
        sceneObject.updatePhysicsBoundingBox();
        info.dispose();
    }

    public PickResult pick(float maxDistance) {
        if (physicsWorld != null) {
            Ray pickRay = camera.getPickRay(Gdx.input.getX(), Gdx.input.getY());
            Vector3 rayFrom = new Vector3(pickRay.origin);
            Vector3 rayTo = new Vector3(pickRay.direction).scl(maxDistance).add(rayFrom);
            AllHitsRayResultCallback resultCallback = new AllHitsRayResultCallback(rayFrom, rayTo);
            PickResult pickResult = new PickResult(resultCallback);
            physicsWorld.rayTest(rayFrom, rayTo, resultCallback);
            if (resultCallback.hasHit()) {
                btScalarArray fractions = resultCallback.getHitFractions();
                btCollisionObject collisionObject = null;
                Vector3 hitPosition = new Vector3();
                float minDist = maxDistance;
                int n = fractions.size();
                for (int i = 0; i < fractions.size(); i++) {
                    float dist = fractions.atConst(i);
                    if (dist < minDist) {
                        collisionObject = resultCallback.getCollisionObjects().atConst(i);
                        hitPosition.set(rayFrom).lerp(rayTo, dist);
                        System.out.printf("hit fraction %d/%d: %s (%f)%n", i+1, n, hitPosition, dist);
                        minDist = dist;
                    }
                }
                if (collisionObject != null && collisionObject.userData != null) {
                    pickResult.pickedObject = (SceneObject) collisionObject.userData;
                }
                debugDraw.drawArrow(hitPosition, Color.GREEN);
            }
            return pickResult;
        }
        return null;
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update(true);
    }

    @Override
    public void show() {
        init();
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        physicsWorld.dispose();
        dispatcher.dispose();
        broadphase.dispose();
        solver.dispose();
        collisionConfig.dispose();
    }
}
