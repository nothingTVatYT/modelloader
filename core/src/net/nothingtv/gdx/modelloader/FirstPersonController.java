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
        public float maxSpeed = 8f;
        public final SceneObject player;
        public final Camera camera;
        public float turningSpeed = 60f;
        public float minVelocity2 = 0.05f;
        public float accelerationForce = 10f;
        public float breakForce = 10f;

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
        }
        return handled;
    }

    public void update(float delta) {
        if (!mouseGrabbed) return;
        player.modelInstance.transform.getTranslation(camera.position);
        // horizontal rotation applied to the player and the camera
        player.rotate(Vector3.Y, -Gdx.input.getDeltaX() * config.turningSpeed * delta);
        // vertical rotation applied to the camera only
        cameraRotation.set(Vector3.Z);
        float angle = MathUtils.clamp((((float)Gdx.input.getY() / camera.viewportHeight) - 0.5f) * 80, -80f, 80f);
        cameraRotation.rotate(Vector3.X, angle);
        player.localToWorldDirection(cameraRotation);
        camera.direction.set(cameraRotation);
        camera.update(true);
        applyForces(delta);
    }

    private void applyForces(float delta) {
        Vector3 velocityXZ = player.rigidBody.getLinearVelocity();
        velocityXZ.y = 0;
        currentSpeed = velocityXZ.len();
        walking = velocityXZ.len2() > config.minVelocity2;
        if (movement.len2() > 1e-5f) {
            if (currentSpeed < config.maxSpeed) {
                linearForce.set(movement).scl(player.mass * config.accelerationForce);
                player.localToWorldDirection(linearForce);
            } else {
                linearForce.x = 0;
                linearForce.z = 0;
            }
        } else {
            linearForce.set(velocityXZ).y = 0;
            linearForce.scl(player.mass * -config.breakForce);
        }
        player.addForce(linearForce);
    }
}
