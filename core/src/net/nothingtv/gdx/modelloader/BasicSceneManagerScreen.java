package net.nothingtv.gdx.modelloader;

import com.badlogic.gdx.*;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.shaders.DepthShader;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.DebugDrawer;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.btConstraintSolver;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import net.mgsx.gltf.scene3d.attributes.FogAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalShadowLight;
import net.mgsx.gltf.scene3d.scene.CascadeShadowMap;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.scene.SceneSkybox;
import net.mgsx.gltf.scene3d.shaders.PBRShaderConfig;
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;
import net.nothingtv.gdx.shaders.GameShaderProvider;
import net.nothingtv.gdx.shaders.MyPBRDepthShaderProvider;
import net.nothingtv.gdx.terrain.Foliage;
import net.nothingtv.gdx.terrain.Terrain;
import net.nothingtv.gdx.terrain.TerrainInstance;
import net.nothingtv.gdx.tools.*;

public abstract class BasicSceneManagerScreen implements Screen {
    protected Game game;
    protected ScreenConfig screenConfig = new ScreenConfig();
    protected btDbvtBroadphase broadphase;
    protected btCollisionConfiguration collisionConfig;
    protected btConstraintSolver solver;
    protected btDispatcher dispatcher;
    protected btDiscreteDynamicsWorld physicsWorld;
    protected SceneManager sceneManager;
    protected Camera camera;
    protected FirstPersonCameraController cameraController;
    protected DirectionalShadowLight directionalShadowLight;
    protected DirectionalLight directionalLight;
    protected Cubemap environmentCubemap;
    protected Cubemap diffuseCubemap;
    protected Cubemap specularCubemap;
    protected Texture brdfLUT;
    protected SceneSkybox skybox;
    protected Color clearColor = new Color(0.7f, 0.7f, 0.7f, 1f);
    protected float gameTime;
    protected DebugDrawer debugDrawer;
    protected Debug debug;
    protected Stage stage;
    protected Skin skin;
    protected Table table;
    protected Window lightControls;
    protected Label fpsLabel;
    protected Label statsLabel;
    protected Image crossHairImage;
    protected DecalBatch decalBatch;
    protected InputMultiplexer inputMultiplexer;
    protected PlayerObject player;
    private float shadowBias = 1/2048f;
    protected GLProfiler glProfiler;
    protected AssetManager assetManager;
    protected Thread physicsUpdateThread;
    protected volatile boolean visible;

    public BasicSceneManagerScreen(Game game) {
        this.game = game;
    }

    public static class PhysicsUpdate implements Runnable {
        BasicSceneManagerScreen screen;

        public PhysicsUpdate(BasicSceneManagerScreen screen) {
            this.screen = screen;
        }

        @Override
        public void run() {
            long lastRun = System.currentTimeMillis();
            while (screen.visible) {
                long ts = System.currentTimeMillis();
                if (ts - lastRun >= 17) {
                    screen.updatePhysics((ts - lastRun) / 1000f);
                    lastRun = ts;
                } else {
                    try {
                        Thread.sleep(2);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }
    }

    protected void init() {
        gameTime = 0;
        visible = true;
        assetManager = new AssetManager();
        if (screenConfig.usePhysics)
            initPhysics();
        initCamera();
        initEnvironment();
        inputMultiplexer = new InputMultiplexer();
        Gdx.input.setInputProcessor(inputMultiplexer);
        initUI();
        initController();
        initBatches();
        initDecals();
        initScene();
        if (physicsUpdateThread != null)
            physicsUpdateThread.start();
    }

    protected void initPhysics() {
        Bullet.init();
        broadphase = new btDbvtBroadphase();
        collisionConfig = new btDefaultCollisionConfiguration();
        solver =  new btSequentialImpulseConstraintSolver();
        dispatcher = new btCollisionDispatcher(collisionConfig);
        physicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfig);
        Physics.currentPhysicsWorld = physicsWorld;
        debugDrawer = new DebugDrawer();
        debugDrawer.setDebugMode(btIDebugDraw.DebugDrawModes.DBG_NoDebug);
        physicsWorld.setDebugDrawer(debugDrawer);
        System.out.printf("Bullet \"%d\" initialized.%n", Bullet.VERSION);
        debug = new Debug(debugDrawer);
        if (screenConfig.useDedicatedPhysicsThread)
            physicsUpdateThread = new Thread(new PhysicsUpdate(this));
    }

    protected void updatePhysics(float delta) {
        physicsWorld.stepSimulation(delta, 5, 1/60f);
        updateFixedTime(delta);
    }

    protected void updateFixedTime(float delta) {}

    protected void initEnvironment() {
        PBRShaderConfig pbrConfig = PBRShaderProvider.createDefaultConfig();
        pbrConfig.numBones = 60;
        pbrConfig.numBoneWeights = 8;
        //pbrConfig.numDirectionalLights = 1;
        pbrConfig.numPointLights = 0;
        pbrConfig.numSpotLights = 0;
        pbrConfig.glslVersion = "140";

        DepthShader.Config depthConfig = PBRShaderProvider.createDefaultDepthConfig();
        depthConfig.numBones = pbrConfig.numBones;
        depthConfig.numBoneWeights = pbrConfig.numBoneWeights;

        sceneManager = new SceneManager(new GameShaderProvider(pbrConfig), new MyPBRDepthShaderProvider());

        sceneManager.setCamera(camera);
        sceneManager.setAmbientLight(screenConfig.ambientLightBrightness);

        float l = screenConfig.directionalLightBrightness;
        Color sunLightColor = new Color(l, l, l, 1);
        Vector3 sunDirection = new Vector3(-0.4f, -0.4f, -0.4f).nor();
        if (screenConfig.useShadows) {
            int shadowMapSize = 2048;
            float shadowViewportSize = 30;
            float shadowNear = 0.1f;
            float shadowFar = 200;
            directionalShadowLight = new DirectionalShadowLight(shadowMapSize, shadowMapSize, shadowViewportSize, shadowViewportSize, shadowNear, shadowFar);
            directionalShadowLight.set(sunLightColor, sunDirection);

            sceneManager.environment.add(directionalShadowLight);
            sceneManager.environment.set( new PBRFloatAttribute(PBRFloatAttribute.ShadowBias, shadowBias)); // reduce shadow acne

            CascadeShadowMap csm = new CascadeShadowMap(3);
            csm.setCascades(camera, directionalShadowLight, 0, 4);
            csm.lights.add(directionalShadowLight);
            sceneManager.setCascadeShadowMap(csm);
            directionalLight = directionalShadowLight;
        } else {
            directionalLight = new DirectionalLight();
            directionalLight.color.set(sunLightColor);
            directionalLight.direction.set(sunDirection);
            sceneManager.environment.add(directionalLight);
        }

        // setup quick IBL (image based lighting)
        IBLBuilder iblBuilder = IBLBuilder.createOutdoor(directionalLight);
        environmentCubemap = iblBuilder.buildEnvMap(1024);
        diffuseCubemap = iblBuilder.buildIrradianceMap(256);
        specularCubemap = iblBuilder.buildRadianceMap(10);
        iblBuilder.dispose();

        // This texture is provided by the library, no need to have it in your assets.
        brdfLUT = new Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png"));

        if (screenConfig.useIBL) {
            sceneManager.environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT));
            sceneManager.environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap));
            sceneManager.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap));
        }
        sceneManager.environment.set(FogAttribute.createFog(camera.far * 0.7f, camera.far * 1.1f, 1f));
        sceneManager.environment.set(ColorAttribute.createFog(0.7f, 0.7f, 0.7f, 1f));

        if (screenConfig.useSkybox) {
            skybox = new SceneSkybox(environmentCubemap);
            sceneManager.setSkyBox(skybox);
        }
    }

    protected void initBatches() {
    }

    protected void initCamera() {
        camera = new PerspectiveCamera(60, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = GeneralSettings.current.cameraFar;
        camera.position.set(-6,2,-6);
        camera.lookAt(new Vector3(0, 1, 0));
        camera.up.set(Vector3.Y);
        camera.update(true);
    }

    protected void initDecals() {
        if (screenConfig.useDecals)
            decalBatch = new DecalBatch(100, new CameraGroupStrategy(camera));
    }

    protected void initUI() {
        stage = new Stage();
        skin = new Skin(Gdx.files.internal("data/uiskin.json"));
        table = new Table(skin);
        table.setFillParent(true);
        table.align(Align.topLeft);
        stage.addActor(table);
        inputMultiplexer.addProcessor(stage);
        if (screenConfig.showFPS)
            showFPS(true);
    }

    protected void enableMouseInUI() {
        if (inputMultiplexer.getProcessors().contains(stage, true))
            return;
        inputMultiplexer.addProcessor(stage);
    }

    protected void disableMouseInUI() {
        inputMultiplexer.removeProcessor(stage);
    }

    public void showCrossHair(boolean showIt) {
        if (showIt) {
            if (crossHairImage == null) {
                crossHairImage = new Image(new Texture(Gdx.files.internal("assets/textures/crosshair.png")));
                crossHairImage.setSize(32, 32);
                crossHairImage.setPosition(Gdx.graphics.getWidth()/2f, Gdx.graphics.getHeight()/2f);
            }
            stage.addActor(crossHairImage);
        } else {
            if (crossHairImage != null)
                crossHairImage.remove();
        }
    }

    public void showFPS(boolean showIt) {
        if (showIt) {
            if (fpsLabel == null) {
                fpsLabel = new Label("0000", skin);
                fpsLabel.addAction(new Action() {
                    @Override
                    public boolean act(float delta) {
                        fpsLabel.setText(String.valueOf(Gdx.graphics.getFramesPerSecond()));
                        return false;
                    }
                });
            }
            table.add(fpsLabel);
        } else {
            System.out.println("FPS label removed");
            if (fpsLabel != null) {
                fpsLabel.clear();
                table.removeActor(fpsLabel);
            }
            fpsLabel = null;
        }
    }

    public void showStats(boolean showIt) {
        if (showIt) {
            if (statsLabel == null) {
                if (glProfiler == null) {
                    glProfiler = new GLProfiler(Gdx.graphics);
                    glProfiler.enable();
                }
                statsLabel = new Label(" ", skin);
                table.row();
                table.add(statsLabel);
            }
        } else {
            if (statsLabel != null) {
                glProfiler.disable();
                statsLabel.clear();
                table.removeActor(statsLabel);
                statsLabel = null;
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
                        valueLabel.setText(String.format("%1.1f", ambientSlider.getValue()));
                        sceneManager.setAmbientLight(((Slider)actor).getValue());
                        event.handle();
                    }
                });
                lightControls.add(ambientSlider);
                lightControls.add(valueLabel);
                lightControls.row();

                lightControls.add(new Label("directional", skin));
                Slider directionalSlider = new Slider(0, 1, 0.1f, false, skin);
                float intensity = screenConfig.useShadows ? directionalShadowLight.intensity : directionalLight.color.r;
                Label valueLabel2 = new Label(String.format("%1.1f", intensity), skin);
                directionalSlider.setValue(intensity);
                directionalSlider.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        float val = directionalSlider.getValue();
                        valueLabel2.setText(String.format("%1.1f", val));
                        if (screenConfig.useShadows) {
                            directionalShadowLight.intensity = val;
                            directionalShadowLight.updateColor();
                        } else {
                            directionalLight.color.set(intensity, intensity, intensity, 1);
                        }
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

    protected void addInputController(InputProcessor controller) {
        inputMultiplexer.addProcessor(controller);
    }

    protected void initController() {
        if (screenConfig.usePlayerController) return;
        cameraController = new FirstPersonCameraController(camera);
        cameraController.setVelocity(16);
        cameraController.setDegreesPerPixel(0.2f);
        cameraController.autoUpdate = true;
        inputMultiplexer.addProcessor(cameraController);
    }

    public void initScene() {}

    public void updateController(float delta) {
        if (screenConfig.usePlayerController) return;
        if (cameraController != null)
            cameraController.update(delta);
    }

    public void updateScene(float delta) {
        sceneManager.update(delta);
    }

    public void update(float delta) {
        updateController(delta);
        if (screenConfig.usePhysics && physicsUpdateThread == null)
            updatePhysics(delta);
        updateScene(delta);
    }

    @Override
    public void render(float delta) {
        if (screenConfig.showStats)
            glProfiler.reset();
        gameTime += delta;
        update(delta);
        Gdx.gl.glClearColor(clearColor.r, clearColor.g, clearColor.b, clearColor.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        sceneManager.render();

        if (debugDrawer != null && !physicsWorld.isDisposed()) {
            debugDrawer.begin(camera);
            physicsWorld.debugDrawWorld();
            debug.drawDebugs();
            debugDrawer.end();
        }

        if (decalBatch != null) {
            decalBatch.flush();
        }

        if (statsLabel != null) {
            String statsText = "";
            if (glProfiler.isEnabled())
                statsText += String.format("%.0f V", glProfiler.getVertexCount().total);
            statsText += updateStats();
            statsLabel.setText(statsText);
        }

        if (stage != null) {
            stage.act(delta);
            stage.draw();
        }

        updatePostRender(delta);
    }

    protected String updateStats() { return ""; }

    public void updatePostRender(float delta) {}

    public PlayerObject addPlayer(Model model) {
        ModelInstance instance = new ModelInstance(model);
        return addPlayer(instance);
    }

    public PlayerObject addPlayer(ModelInstance modelInstance) {
        sceneManager.getRenderableProviders().add(modelInstance);
        return new PlayerObject("Player", modelInstance);
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
        sceneManager.getRenderableProviders().add(modelInstance);
        return new SceneObject(name, modelInstance);
    }

    public TerrainObject add(String name, TerrainInstance modelInstance, Terrain terrain) {
        sceneManager.getRenderableProviders().add(modelInstance);
        return new TerrainObject(name, modelInstance, terrain);
    }

    public void add(Foliage foliage) {
        sceneManager.getRenderableProviders().add(foliage);
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
        sceneObject.mass = mass;
        motionState.rigidBodyOffset.set(vCenter).sub(pCenter);
        rigidBody.translate(motionState.rigidBodyOffset);
        sceneObject.updatePhysicsBoundingBox();
        info.dispose();
    }

    @Override
    public void resize(int width, int height) {
        sceneManager.updateViewport(width, height);
    }

    @Override
    public void show() {
        init();
    }

    @Override
    public void hide() {
        visible = false;
        dispose();
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    protected void setShadowBias(float val) {
        sceneManager.environment.set( new PBRFloatAttribute(PBRFloatAttribute.ShadowBias, val)); // reduce shadow acne
    }

    @Override
    public void dispose() {
        if (glProfiler != null)
            glProfiler.disable();
        stage.dispose();
        if (decalBatch != null)
            decalBatch.dispose();
        sceneManager.dispose();
        Physics.currentPhysicsWorld = null;
        physicsWorld.dispose();
        dispatcher.dispose();
        broadphase.dispose();
        solver.dispose();
        collisionConfig.dispose();
    }
}
