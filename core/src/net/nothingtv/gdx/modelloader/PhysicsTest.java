package net.nothingtv.gdx.modelloader;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.nothingtv.gdx.inventory.GameItem;
import net.nothingtv.gdx.inventory.Inventory;
import net.nothingtv.gdx.terrain.*;
import net.nothingtv.gdx.tools.*;
import net.nothingtv.gdx.ui.InventoryView;

import java.util.Random;

public class PhysicsTest extends BasicSceneManagerScreen {
    private SceneObject floor;
    private SceneObject box;
    private SceneObject ball;
    private TerrainObject terrainObject;
    private Foliage foliage;
    private boolean lightControlsOn = false;
    private boolean useSplatGenerator = false;
    private final Vector3 initialPos = new Vector3(200, 0, 200);
    protected FirstPersonController playerController;
    private FootStepsSFX footSteps;
    private Label speedLabel;
    private Decal playerDecal;
    private TerrainSplatGenerator splatGenerator;
    private JSplatGenerator splatGeneratorUI;
    private JModelViewer modelViewer;
    private InventoryView inventoryView;

    public PhysicsTest(Game game) {
        super(game);
    }

    @Override
    protected void init() {
        screenConfig.useSkybox = true;
        screenConfig.useShadows = GeneralSettings.isAtLeast(GeneralSettings.Setting.High);
        screenConfig.usePlayerController = true;
        screenConfig.ambientLightBrightness = 0.3f;
        screenConfig.showStats = true;
        //Gdx.app.setLogLevel(Application.LOG_DEBUG);
        super.init();
    }

    @Override
    public void initScene() {
        super.initScene();

        Inventory.init();
        inventoryView = new InventoryView(stage, skin);

        floor = add("floor", BaseModels.createBox(10, 1, 10, BaseMaterials.whiteColor()));
        wrapRigidBody(floor, 10, BaseShapes.createBoxShape(floor.modelInstance));
        floor.setLinearFactor(SceneObject.LockY);
        floor.setAngularFactor(SceneObject.LockAll);
        floor.moveTo(new Vector3(0, 20, 0));
        System.out.printf("added %s with %s (rigid body: %s)%n", floor.name, floor.boundingBox, floor.physicsBoundingBox);
        box = add("box", BaseModels.createBox(1, 1, 1, BaseMaterials.color(Color.RED)));
        wrapRigidBody(box, 1, BaseShapes.createBoxShape(box.modelInstance));
        box.move(new Vector3(0, 23, 0));
        box.rotate(Vector3.X, 25f);
        ball = add("ball", BaseModels.createSphere(1, BaseMaterials.emit(Color.GREEN)));
        wrapRigidBody(ball, 1, BaseShapes.createSphereShape(ball.modelInstance));
        ball.moveTo(new Vector3(30, 30, 30));

        boolean hi = GeneralSettings.isAtLeast(GeneralSettings.Setting.High);
        FileHandle layer1Tex = hi ? Gdx.files.internal("assets/textures/Ground023_2K_Color.png") : Gdx.files.internal("assets/textures/Ground023_1K_Color.png");
        FileHandle layer2Tex = hi ? Gdx.files.internal("assets/textures/Grass004_2K_Color.png") : Gdx.files.internal("assets/textures/Grass004_1K_Color.png");
        FileHandle layer3Tex = hi ? Gdx.files.internal("assets/textures/Ground048_2K_Color.jpg") : Gdx.files.internal("assets/textures/Ground048_1K_Color.png");
        FileHandle layer4Tex = hi ? Gdx.files.internal("assets/textures/Rock031_2K_Color.png") : Gdx.files.internal("assets/textures/Rock031_1K_Color.png");

        int tf = 1 << (GeneralSettings.current.setting.ordinal() -1);
        float uvScale = 400 * tf;
        TerrainConfig terrainConfig = new TerrainConfig(1024/tf, 1024/tf, tf);
        terrainConfig.terrainDivideFactor = 8;
        terrainConfig.heightSampler = new NoiseHeightSampler(1, 5, 4, 8, 4f);
        terrainConfig.erosionIterations = 0;
        terrainConfig.splatMap = new Pixmap(Gdx.files.internal("assets/textures/splatmap.png"));

        terrainConfig.addLayer(new Texture(layer1Tex), uvScale);
        terrainConfig.addLayer(new Texture(layer2Tex), uvScale);
        terrainConfig.addLayer(new Texture(layer3Tex), uvScale);
        terrainConfig.addLayer(new Texture(layer4Tex), uvScale);
        Terrain terrain = new Terrain(terrainConfig);
        terrainObject = add("terrain", terrain.createModelInstance(), terrain);

        wrapRigidBody(terrainObject, 0, terrain.createCollisionShape());
        System.out.printf("added %s with %s (rigid body: %s)%n", terrainObject.name, terrainObject.boundingBox, terrainObject.physicsBoundingBox);

        if (useSplatGenerator) {
            TerrainSplatGenerator.Configuration splatConfig = TerrainSplatGenerator.createDefaultConfiguration(4);
            splatConfig.resolution = 1024;
            splatConfig.layers[0].elevationWeight = 0;
            splatConfig.layers[1].elevationWeight = 0;
            splatConfig.layers[1].heightBegin = 0.3f;
            splatConfig.layers[1].heightEnd = 0.5f;
            splatConfig.layers[2].elevationWeight = 1;
            splatConfig.layers[2].heightBegin = 0.1f;
            splatConfig.layers[2].heightEnd = 0.7f;
            splatConfig.layers[3].elevationWeight = 1;
            splatConfig.layers[3].heightBegin = 0.7f;
            splatConfig.layers[3].heightEnd = 1f;
            splatConfig.layers[3].slopeWeight = 1f;
            splatConfig.layers[3].slopeBegin = 0.21f;
            splatConfig.layers[3].slopeEnd = 1f;
            splatGenerator = new TerrainSplatGenerator(terrain, splatConfig);
            splatGenerator.update();

            splatGeneratorUI = JSplatGenerator.showUI(splatConfig);
            splatGeneratorUI.setTexture(0, layer1Tex.readBytes());
            splatGeneratorUI.setTexture(1, layer2Tex.readBytes());
            splatGeneratorUI.setTexture(2, layer3Tex.readBytes());
            splatGeneratorUI.setTexture(3, layer4Tex.readBytes());
        }

        player = add("player", BaseModels.createCapsule(0.3f, 2f, BaseMaterials.color(Color.WHITE)));
        wrapRigidBody(player, 75, BaseShapes.createCapsuleShape(player.modelInstance));
        player.setAngularFactor(SceneObject.LockAll);

        initialPos.y = terrain.getHeightAt(initialPos.x, initialPos.z) + 1.3f;
        player.moveTo(initialPos);

        if (screenConfig.usePlayerController) {
            FirstPersonController.ControllerConfig controllerConfig = new FirstPersonController.ControllerConfig(player, camera);
            playerController = new FirstPersonController(controllerConfig);
            playerController.getPlayer().moveTo(initialPos);
            playerController.init();
            playerController.grabMouse();
            addInputController(playerController);

            footSteps = new FootStepsSFX(playerController);
            footSteps.initializeDefaults();

            showCrossHair(true);

            speedLabel = new Label("00.00 m/s", skin);
            speedLabel.addAction(new Action() {
                @Override
                public boolean act(float delta) {
                    speedLabel.setText(String.format("%2.2f m/s", playerController.getCurrentSpeed()));
                    return false;
                }
            });
            table.row();
            table.add(speedLabel);
        } else {
            camera.position.set(0, 24, -5);
            camera.lookAt(0, 23, 0);
            camera.up.set(Vector3.Y);
            camera.update();
        }

        // add foliage
        foliage = new Foliage();
        Random rnd = new Random(123);
        Vector3 foliageCenter = new Vector3(512, 0, 512);
        float foliageRadius = 512;
        Model tree1 = new GLBLoader().load(Gdx.files.internal("models/tree1.glb")).scene.model;
        Model grass0 = new GLBLoader().load(Gdx.files.internal("models/grass0.glb")).scene.model;
        Model grass1 = new GLBLoader().load(Gdx.files.internal("models/grass1.glb")).scene.model;
        Model grass2 = new GLBLoader().load(Gdx.files.internal("models/grass2.glb")).scene.model;
        foliage.add(tree1, Foliage.createRandomPositions(terrain, rnd, foliageCenter, foliageRadius, 5000/GeneralSettings.current.foliageDivider), Foliage.RandomizeYRotation);
        foliage.add(grass0, Foliage.createRandomPositions(terrain, rnd, foliageCenter, foliageRadius, 100000/GeneralSettings.current.foliageDivider), Foliage.RandomizeYRotation);
        foliage.add(grass1, Foliage.createRandomPositions(terrain, rnd, foliageCenter, foliageRadius, 100000/GeneralSettings.current.foliageDivider), Foliage.RandomizeYRotation);
        foliage.add(grass2, Foliage.createRandomPositions(terrain, rnd, foliageCenter, foliageRadius, 100000/GeneralSettings.current.foliageDivider), Foliage.RandomizeYRotation);
        foliage.setCamera(camera);
        foliage.setCameraMinDist(10);
        foliage.setCameraMaxDist(GeneralSettings.current.foliageMaxDistance);
        add(foliage);
        /*
        modelViewer = new JModelViewer();
        modelViewer.showModel(tree1);
        modelViewer.setVisible(true);
        */

        showStats(screenConfig.showStats);
    }

    @Override
    protected void initDecals() {
        super.initDecals();
        playerDecal = Decal.newDecal(new TextureRegion(new Texture(Gdx.files.internal("assets/textures/questionmark.png"))), true);
        playerDecal.setDimensions(0.5f, 0.5f);
    }

    @Override
    public void updateController(float delta) {
        super.updateController(delta);
        if (playerController != null) {
            playerController.update(delta);
            footSteps.update(delta);
            if (playerController.getCameraToPlayerDistance() > 0) {
                playerDecal.setPosition(player.getPosition());
                playerDecal.translate(0, 1.5f, 0);
                playerDecal.lookAt(camera.position, camera.up);
                decalBatch.add(playerDecal);
            }
        }
    }

    @Override
    public void updateScene(float delta) {
        super.updateScene(delta);
        if (Gdx.input.isKeyJustPressed(Input.Keys.L) && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
            lightControlsOn = !lightControlsOn;
            showLightControls(lightControlsOn);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.D) && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
            if (debugDrawer.getDebugMode() > 0)
                debugDrawer.setDebugMode(btIDebugDraw.DebugDrawModes.DBG_NoDebug);
            else
                debugDrawer.setDebugMode(btIDebugDraw.DebugDrawModes.DBG_FastWireframe|btIDebugDraw.DebugDrawModes.DBG_DrawWireframe);
        }
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
            PickResult picked = Physics.pick(camera, 100);
            if (picked != null && picked.hasHit() && picked.pickedObject != null) {
                if (picked.pickedObject == terrainObject) {
                    Vector3 terrainLocal = new Vector3(picked.hitPosition);
                    terrainObject.worldToLocalLocation(terrainLocal);
                    int splat = terrainObject.terrain.getSplatAt(terrainLocal.x, terrainLocal.z);
                    Vector3 n = new Vector3();
                    terrainObject.terrain.getNormalAt(terrainLocal.x, terrainLocal.z, n);
                    float h = terrainObject.terrain.getHeightAt(terrainLocal.x, terrainLocal.z);
                    System.out.printf("splat at %s: %08x, height %f, normal %s%n", terrainLocal, splat, h, n);
                    debug.drawLine("hit normal", picked.hitPosition, new Vector3(picked.hitPosition).add(n), Color.YELLOW);
                } else {
                    System.out.printf("We hit something! (%s)%n", picked.pickedObject.name);
                    BaseShapes.dumpRigidBody(picked.pickedObject.rigidBody);
                }
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.I)) {
            if (inventoryView.isVisible())
                inventoryView.hideInventory();
            else
                inventoryView.showPlayerInventory();
        }

        // test item pickup
        if (Gdx.input.isKeyJustPressed(Input.Keys.N) && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
            GameItem bread = Inventory.PlayerInventory.newGameItemInstance("Bread", 0, 0, 1, 1);
            Inventory.PlayerInventory.requestItemPickup(bread);
            inventoryView.showInventory(Inventory.PlayerInventory);
        }

        if (screenConfig.usePlayerController && Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            if (playerController.isMouseGrabbed()) {
                playerController.releaseMouse();
                enableMouseInUI();
            } else {
                playerController.grabMouse();
                disableMouseInUI();
            }
            showCrossHair(playerController.isMouseGrabbed());
        }
        Vector3 randomTranslation = new Vector3(MathUtils.sin(gameTime), 0, 0);
        Vector3 diff = new Vector3(randomTranslation).sub(floor.modelInstance.transform.getTranslation(new Vector3())).scl(150);
        floor.addForce(diff);

        // add a mini game ;)
        if (ball.getPosition().y < -200) {
            Vector3 newPos = new Vector3(camera.position).add(MathUtils.random(-3f, 3f), 10, MathUtils.random(-3f, 3f));
            ball.moveTo(newPos);
        }
        if (player.getPosition().y < -200) {
            player.moveTo(initialPos);
        }

        if (useSplatGenerator && splatGeneratorUI.isMapUpdateRequested()) {
            splatGenerator.update();
            splatGeneratorUI.resetRequest();
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }

    @Override
    public void updatePostRender(float delta) {
        super.updatePostRender(delta);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (splatGeneratorUI != null)
                splatGeneratorUI.dispose();
            game.setScreen(new SelectScreen(game));
        }
    }

    @Override
    public void dispose() {
        if (foliage != null)
            foliage.dispose();
        if (modelViewer != null)
            modelViewer.dispose();
        super.dispose();
    }
}
