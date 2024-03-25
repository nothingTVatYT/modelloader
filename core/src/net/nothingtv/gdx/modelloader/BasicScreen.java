package net.nothingtv.gdx.modelloader;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.shaders.DepthShader;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.math.MathUtils;
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
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute;
import net.mgsx.gltf.scene3d.scene.SceneSkybox;
import net.mgsx.gltf.scene3d.shaders.PBRShaderConfig;
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider;
import net.nothingtv.gdx.shaders.WWShaderProvider;
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
    private Color ambientLightColor;
    protected float gameTime;
    protected DebugDrawer debugDrawer;
    protected Stage stage;
    protected Skin skin;
    protected Table table;
    protected Label fpsLabel;
    protected Label statsLabel;
    protected Window lightControls;
    private float shadowBias = 1/2048f;
    protected InputMultiplexer inputMultiplexer;
    protected GLProfiler glProfiler;

    public BasicScreen(Game game) {
        this.game = game;
    }

    protected void init() {
        gameTime = 0;
        if (screenConfig.usePhysics)
            initPhysics();
        initCamera();
        initEnvironment();
        inputMultiplexer = new InputMultiplexer();
        Gdx.input.setInputProcessor(inputMultiplexer);
        initUI();
        initController();
        initBatches();
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
        float l = screenConfig.ambientLightBrightness;
        ambientLightColor = new Color(l, l, l, 1);
        l = screenConfig.directionalLightBrightness;
        Color sunLightColor = new Color(l, l, l, 1);
        Vector3 sunDirection = new Vector3(-0.4f, -0.4f, -0.4f).nor();
        int shadowMapSize = 2048;
        float shadowViewportSize = 10;
        float shadowNear = 0.1f;
        float shadowFar = 500;
        directionalLight = new DirectionalShadowLight(shadowMapSize, shadowMapSize, shadowViewportSize, shadowViewportSize, shadowNear, shadowFar);
        directionalLight.set(sunLightColor, sunDirection);
        //directionalLight.intensity = 0.5f;
        //directionalLight.updateColor();
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, ambientLightColor));
        environment.set(new PBRFloatAttribute(PBRFloatAttribute.ShadowBias, 1f / 1024f)); // reduce shadow acne
        environment.add(directionalLight);
        environment.shadowMap = directionalLight;

        // setup quick IBL (image based lighting)
        /* IBLBuilder iblBuilder = IBLBuilder.createOutdoor(directionalLight);
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
            //renderableProviders.add(skybox);
        }*/
    }

    protected void initBatches() {
        PBRShaderConfig pbrConfig = PBRShaderProvider.createDefaultConfig();
        pbrConfig.numBones = 60;
        pbrConfig.numBoneWeights = 8;
        //pbrConfig.numDirectionalLights = 1;
        pbrConfig.numPointLights = 0;

        //pbrBatch = new ModelBatch(new TerrainPBRShaderProvider(pbrConfig));
        DefaultShader.Config shaderConfig = WWShaderProvider.createDefaultConfig();
        pbrBatch = new ModelBatch(new WWShaderProvider(shaderConfig));

        DepthShader.Config depthConfig = new DepthShader.Config(); //PBRShaderProvider.createDefaultDepthConfig();
        depthConfig.numBones = 60;
        depthConfig.numBoneWeights = 8;
        shadowBatch = new ModelBatch(new DepthShaderProvider(depthConfig));

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

    protected void initUI() {
        stage = new Stage();
        //stage.setDebugAll(true);
        skin = new Skin(Gdx.files.internal("data/uiskin.json"));
        table = new Table(skin);
        table.setFillParent(true);
        table.align(Align.topLeft);
        stage.addActor(table);
        inputMultiplexer.addProcessor(stage);
        if (screenConfig.showFPS)
            showFPS(true);
    }

    public void showFPS(boolean showIt) {
        if (showIt) {
            if (fpsLabel == null) {
                fpsLabel = new Label("0000", skin) {
                    @Override
                    public void act(float delta) {
                        setText("" + Gdx.graphics.getFramesPerSecond());
                        super.act(delta);
                    }
                };
            }
            table.add(fpsLabel);
        } else {
            if (fpsLabel != null)
                table.removeActor(fpsLabel);
        }
    }

    public void showStats(boolean showIt) {
        if (showIt) {
            if (glProfiler == null) {
                glProfiler = new GLProfiler(Gdx.graphics);
            }
            if (statsLabel == null) {
                statsLabel = new Label(" ", skin);
                statsLabel.addAction(new Action() {
                    @Override
                    public boolean act(float delta) {
                        statsLabel.setText(String.format("%.0f vertices", glProfiler.getVertexCount().total));
                        return true;
                    }
                });
            }
        }
    }

    public void showLightControls(boolean showIt) {
        if (showIt) {
            if (lightControls == null) {
                lightControls = new Window("Environment", skin);
                lightControls.defaults().spaceBottom(10);
                lightControls.row();
                lightControls.add(new Label("ambient", skin));
                Slider ambientSlider = new Slider(0, 1, 0.1f, false, skin);
                ambientSlider.setValue(screenConfig.ambientLightBrightness);
                Label valueLabel = new Label(String.format("%1.1f", screenConfig.ambientLightBrightness), skin);
                ambientSlider.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        float val = ambientSlider.getValue();
                        valueLabel.setText(String.format("%1.1f", val));
                        setAmbientLight(val);
                        event.handle();
                    }
                });
                lightControls.add(ambientSlider);
                lightControls.add(valueLabel);
                lightControls.row();

                lightControls.add(new Label("directional", skin));
                Slider directionalSlider = new Slider(0, 1, 0.1f, false, skin);
                float intensity = directionalLight.color.r;
                Label valueLabel2 = new Label(String.format("%1.1f", intensity), skin);
                directionalSlider.setValue(intensity);
                directionalSlider.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        float val = directionalSlider.getValue();
                        valueLabel2.setText(String.format("%1.1f", val));
                        //directionalLight.intensity = ((Slider)actor).getValue();
                        //directionalLight.updateColor();
                        directionalLight.color.set(val, val, val, 1);
                        event.handle();
                    }
                });
                lightControls.add(directionalSlider);
                lightControls.add(valueLabel2);
                lightControls.row();

                lightControls.add(new Label("shadow bias", skin));
                Slider shadowBiasSlider = new Slider(1, 24, 1f, false, skin);
                Label valueLabel3 = new Label(String.format("1/%d", (int)(1/shadowBias)), skin);
                shadowBiasSlider.setValue(MathUtils.log2(1f/shadowBias));
                shadowBiasSlider.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        float v = ((Slider)actor).getValue();
                        valueLabel3.setText(String.format("1/%d", MathUtils.floor((float)Math.pow(2, v))));
                        shadowBias = 1f / (float)(Math.pow(2, v));
                        setShadowBias(shadowBias);
                        event.handle();
                    }
                });
                lightControls.add(shadowBiasSlider);
                lightControls.add(valueLabel3).minWidth(100);

                lightControls.pack();
            }
            stage.addActor(lightControls);
        } else {
            if (lightControls != null)
                stage.getRoot().removeActor(lightControls);
        }
    }

    protected void initController() {
        cameraController = new FirstPersonCameraController(camera);
        cameraController.setVelocity(16);
        cameraController.setDegreesPerPixel(0.2f);
        cameraController.autoUpdate = true;
        inputMultiplexer.addProcessor(cameraController);
    }

    public void initScene() {}

    private void setAmbientLight(float val) {
        ambientLightColor.set(val, val, val, 1);
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, ambientLightColor));
    }

    private void setShadowBias(float val) {
        shadowBias = val;
        environment.set(new PBRFloatAttribute(PBRFloatAttribute.ShadowBias, shadowBias));
    }

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
        //directionalLight.setCenter(camera.position);
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

        if (stage != null) {
            stage.act(delta);
            stage.draw();
        }

        updatePostRender(delta);
        if (glProfiler != null)
            glProfiler.reset();
    }

    public void updatePostRender(float delta) {}

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
