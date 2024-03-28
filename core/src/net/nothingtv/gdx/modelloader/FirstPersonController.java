package net.nothingtv.gdx.modelloader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import net.nothingtv.gdx.tools.SceneObject;

public class FirstPersonController extends InputAdapter {

    public static class ControllerConfig {
        public float maxWalkingSpeed = 8f;
        public float maxRunningSpeed = 24f;
        public final SceneObject player;
        public final Camera camera;
        public Vector3 eyeOffset = new Vector3(0, 1, 0);
        public float turningSpeed = 60f;
        public float minVelocity2 = 0.05f;
        public float accelerationForce = 10f;
        public float breakForce = 50f;
        public boolean simulateSideFriction = true;

        public ControllerConfig(SceneObject player, Camera camera) {
            this.player = player;
            this.camera = camera;
        }
    }
    private final ControllerConfig config;
    private final Camera camera;
    private final SceneObject player;
    private final Vector3 movement = new Vector3();
    private final Vector3 linearForce = new Vector3();
    private boolean walking;
    private float currentMaxSpeed;
    private boolean mouseGrabbed;

    private float currentSpeed;
    private final Vector3 cameraRotation = new Vector3();

    public FirstPersonController(ControllerConfig config) {
        this.config = config;
        this.camera = config.camera;
        this.player = config.player;
    }

    public void init() {
        //updateCamera(0);
        walking = false;
        mouseGrabbed = Gdx.input.isCursorCatched();
    }

    public boolean isMouseGrabbed() {
        return mouseGrabbed;
    }

    public SceneObject getPlayer() {
        return player;
    }

    public boolean isWalking() {
        return walking;
    }

    public float getCurrentSpeed() {
        return currentSpeed;
    }

    public void grabMouse() {
        if (mouseGrabbed) return;
        Gdx.input.setCursorCatched(true);
        mouseGrabbed = true;
    }

    public void releaseMouse() {
        if (!mouseGrabbed) return;
        Gdx.input.setCursorCatched(false);
        mouseGrabbed = false;
    }

    @Override
    public boolean keyDown(int keycode) {
        boolean handled = false;
        switch(keycode) {
            case Input.Keys.W:
                movement.z = 1;
                handled = true;
                break;
            case Input.Keys.S:
                movement.z = -1;
                handled = true;
                break;
            case Input.Keys.A:
                movement.x = 1;
                handled = true;
                break;
            case Input.Keys.D:
                movement.x = -1;
                handled = true;
                break;
        }
        return handled;
    }

    @Override
    public boolean keyUp(int keycode) {
        boolean handled = false;
        switch(keycode) {
            case Input.Keys.W:
            case Input.Keys.S:
                movement.z = 0;
                handled = true;
                break;
            case Input.Keys.A:
            case Input.Keys.D:
                movement.x = 0;
                handled = true;
                break;
        }
        return handled;
    }

    public void update(float delta) {
        player.modelInstance.transform.getTranslation(camera.position);
        camera.position.add(config.eyeOffset);
        if (mouseGrabbed) {
            // horizontal rotation applied to the player and the camera
            player.rotate(Vector3.Y, -Gdx.input.getDeltaX() * config.turningSpeed * delta);
            // vertical rotation applied to the camera only
            cameraRotation.set(Vector3.Z);
            float angle = MathUtils.clamp((((float) Gdx.input.getY() / camera.viewportHeight) - 0.5f) * 80, -80f, 80f);
            cameraRotation.rotate(Vector3.X, angle);
            player.localToWorldDirection(cameraRotation);
            camera.direction.set(cameraRotation);
        } else movement.setZero();
        if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT))
            currentMaxSpeed = config.maxRunningSpeed;
        else
            currentMaxSpeed = config.maxWalkingSpeed;
        applyForces(delta);
        camera.update(true);
    }

    private void applyForces(float delta) {
        Vector3 velocityXZ = player.rigidBody.getLinearVelocity();
        velocityXZ.y = 0;
        currentSpeed = velocityXZ.len();
        walking = velocityXZ.len2() > config.minVelocity2;
        if (movement.len2() > 1e-6f) {
            if (currentSpeed < currentMaxSpeed) {
                linearForce.set(movement).scl(player.mass * config.accelerationForce);
                player.localToWorldDirection(linearForce);
            } else {
                linearForce.x = 0;
                linearForce.z = 0;
            }
        } else {
            linearForce.set(velocityXZ);
            linearForce.scl(player.mass * -config.breakForce);
        }
        if (config.simulateSideFriction && Math.abs(movement.x) < 1e-6f) {
            player.worldToLocalDirection(velocityXZ);
            velocityXZ.z = 0;
            if (velocityXZ.len2() > 1e-6f) {
                player.localToWorldDirection(velocityXZ);
                linearForce.add(velocityXZ.scl(player.mass * -config.breakForce));
            }
        }
        player.addForce(linearForce);
    }
}
