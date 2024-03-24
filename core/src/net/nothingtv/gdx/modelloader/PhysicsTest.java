package net.nothingtv.gdx.modelloader;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
import net.nothingtv.gdx.terrain.Terrain;
import net.nothingtv.gdx.terrain.TerrainConfig;
import net.nothingtv.gdx.terrain.TestHeightSampler;
import net.nothingtv.gdx.tools.*;

public class PhysicsTest extends BasicSceneManagerScreen {
    private SceneObject floor;
    private SceneObject box;
    private SceneObject ball;
    private Terrain terrain;
    private boolean lightControlsOn = false;

    public PhysicsTest(Game game) {
        super(game);
    }

    @Override
    protected void init() {
        screenConfig.useSkybox = true;
        screenConfig.useShadows = false;
        screenConfig.ambientLightBrightness = 0.3f;
        super.init();
    }

    @Override
    public void initScene() {
        super.initScene();
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

        TerrainConfig terrainConfig = new TerrainConfig(128, 128, 1);
        terrainConfig.terrainDivideFactor = 4;
        terrainConfig.heightSampler = new TestHeightSampler(6, 0.05f, 0);
        terrainConfig.splatMap = new Texture(Gdx.files.internal("assets/textures/alpha-example.png"));
        terrainConfig.addLayer(new Texture(Gdx.files.internal("assets/textures/leafy_grass_diff_2k.jpg")), 25);
        terrainConfig.addLayer(new Texture(Gdx.files.internal("assets/textures/Ground026_2K_Color.jpg")), 25);
        terrainConfig.addLayer(new Texture(Gdx.files.internal("assets/textures/Ground048_2K_Color.jpg")), 25);
        terrainConfig.addLayer(new Texture(Gdx.files.internal("assets/textures/cobblestone_floor_07_diff_2k.jpg")), 25);
        terrain = new Terrain(terrainConfig);
        SceneObject terrainObject = add("terrain", terrain.createModelInstance());

        wrapRigidBody(terrainObject, 0, terrain.createCollisionShape());
        System.out.printf("added %s with %s (rigid body: %s)%n", terrainObject.name, terrainObject.boundingBox, terrainObject.physicsBoundingBox);

        camera.position.set(0, 24, -5);
        camera.lookAt(0, 23, 0);
        camera.up.set(Vector3.Y);
        camera.update();
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
                debugDrawer.setDebugMode(btIDebugDraw.DebugDrawModes.DBG_MAX_DEBUG_DRAW_MODE);
        }
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
            PickResult picked = pick(100);
            if (picked.hasHit() && picked.pickedObject != null) {
                System.out.printf("We hit something! (%s)%n", picked.pickedObject.name);
                BaseShapes.dumpRigidBody(picked.pickedObject.rigidBody);
            }
        }
        Vector3 randomTranslation = new Vector3(MathUtils.sin(gameTime), 0, 0);
        Vector3 diff = new Vector3(randomTranslation).sub(floor.modelInstance.transform.getTranslation(new Vector3())).scl(150);
        floor.addForce(diff);
    }

    @Override
    public void updatePostRender(float delta) {
        super.updatePostRender(delta);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
            game.setScreen(new SelectScreen(game));
    }
}
