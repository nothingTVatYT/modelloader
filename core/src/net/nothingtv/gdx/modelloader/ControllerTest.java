package net.nothingtv.gdx.modelloader;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
import net.nothingtv.gdx.tools.BaseMaterials;
import net.nothingtv.gdx.tools.BaseModels;
import net.nothingtv.gdx.tools.BaseShapes;
import net.nothingtv.gdx.tools.SceneObject;

public class ControllerTest extends BasicSceneManagerScreen {

    private FirstPersonKinematicController playerController;
    private final Vector3 tmp1 = new Vector3();

    public ControllerTest(Game game) {
        super(game);
        screenConfig.usePlayerController = true;
    }

    @Override
    protected void initController() {
        super.initController();
        player = addPlayer(BaseModels.createCapsule(0.3f, 2f, BaseMaterials.whiteColorPBR()));
        player.mass = 0;
        wrapRigidBody(player, player.mass, BaseShapes.createCapsuleShape(player.modelInstance));
        player.setAngularFactor(SceneObject.LockAll);
        BasePlayerController.ControllerConfig controllerConfig = new BasePlayerController.ControllerConfig(player, camera);
        controllerConfig.simulateSideFriction = false;
        playerController = new FirstPersonKinematicController(controllerConfig);
        player.moveTo(new Vector3(0, 1.8f, 0));
        playerController.init();
        playerController.grabMouse();
        addInputController(playerController);
    }

    @Override
    public void initScene() {
        super.initScene();
        SceneObject floor = add("floor", BaseModels.createBox(100, 1, 100, BaseMaterials.colorPBR(Color.BROWN)));
        wrapRigidBody(floor, 0, BaseShapes.createBoxShape(floor.modelInstance));
        SceneObject obstacle = add("wall1", BaseModels.createBox(10, 2, 1, BaseMaterials.colorPBR(Color.BROWN)));
        wrapRigidBody(obstacle, 0, BaseShapes.createBoxShape(obstacle.modelInstance));
        obstacle.moveTo(new Vector3(25, 1, 20));
    }

    @Override
    public void updateController(float delta) {
        super.updateController(delta);
        playerController.update(delta);
    }

    @Override
    public void updateScene(float delta) {
        super.updateScene(delta);
        if (Gdx.input.isKeyJustPressed(Input.Keys.D) && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
            if (debugDrawer.getDebugMode() > 0)
                debugDrawer.setDebugMode(btIDebugDraw.DebugDrawModes.DBG_NoDebug);
            else
                debugDrawer.setDebugMode(btIDebugDraw.DebugDrawModes.DBG_FastWireframe|btIDebugDraw.DebugDrawModes.DBG_DrawWireframe|btIDebugDraw.DebugDrawModes.DBG_DrawNormals);
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
        tmp1.set(player.getPosition()).add(player.getForward());
        debug.drawLine("playerDirection", player.getPosition(), tmp1, Color.BLACK);
    }
}
