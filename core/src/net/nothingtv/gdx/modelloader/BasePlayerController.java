package net.nothingtv.gdx.modelloader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import net.nothingtv.gdx.tools.PlayerObject;
import net.nothingtv.gdx.tools.SceneObject;

public class BasePlayerController extends InputAdapter implements Disposable {

    public static class ControllerConfig {
        public float maxWalkingSpeed = 6f;
        public float maxRunningSpeed = 12f;
        public final PlayerObject player;
        public final Camera camera;
        public Vector3 eyeOffset = new Vector3(0, 1, 0.2f);
        public float cameraMaxOffset = 50f;
        public float turningSpeed = 60f;
        public float minVelocity2 = 0.05f;
        public float accelerationForce = 10f;
        public float jumpForce = 15f;
        public float breakForce = 5f;
        public boolean simulateSideFriction = true;

        public ControllerConfig(PlayerObject player, Camera camera) {
            this.player = player;
            this.camera = camera;
        }
    }
    protected final BasePlayerController.ControllerConfig config;
    protected final Camera camera;
    protected final PlayerObject player;
    protected final Vector3 movement = new Vector3();
    protected final Vector3 linearForce = new Vector3();
    protected boolean walking;
    protected boolean grounded;
    protected float currentMaxSpeed;
    protected boolean mouseGrabbed;
    protected boolean speedUp;
    protected float cameraToPlayerDistance;
    protected final Vector3 down = new Vector3(0, -1, 0);
    protected float currentSpeed;
    protected final Vector3 cameraRotation = new Vector3();
    protected final Vector3 cameraLocalPosition = new Vector3();

    public BasePlayerController(BasePlayerController.ControllerConfig config) {
        this.config = config;
        this.camera = config.camera;
        this.player = config.player;
    }

    public void init() {
        //updateCamera(0);
        walking = false;
        speedUp = false;
        cameraToPlayerDistance = 0;
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

    public boolean isRunning() {
        return currentSpeed > 1.3f * config.maxWalkingSpeed;
    }

    public boolean isGrounded() {
        return grounded;
    }

    public float getCameraToPlayerDistance() {
        return cameraToPlayerDistance;
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
    public boolean scrolled(float amountX, float amountY) {
        if (!mouseGrabbed) return false;
        cameraToPlayerDistance = Math.max(0, Math.min(config.cameraMaxOffset, cameraToPlayerDistance + amountY));
        return true;
    }

    @Override
    public boolean keyDown(int keycode) {
        boolean handled = false;
        if (isMouseGrabbed()) {
            handled = switch (keycode) {
                case Input.Keys.W -> {
                    movement.z = 1;
                    yield true;
                }
                case Input.Keys.S -> {
                    movement.z = -1;
                    yield true;
                }
                case Input.Keys.A -> {
                    movement.x = 1;
                    yield true;
                }
                case Input.Keys.D -> {
                    movement.x = -1;
                    yield true;
                }
                case Input.Keys.SPACE -> {
                    movement.y = 1;
                    yield true;
                }
                case Input.Keys.SHIFT_LEFT, Input.Keys.SHIFT_RIGHT -> {
                    speedUp = true;
                    yield true;
                }
                default -> false;
            };
        }
        return handled;
    }

    @Override
    public boolean keyUp(int keycode) {
        boolean handled = false;
        if (isMouseGrabbed()) {
            handled = switch (keycode) {
                case Input.Keys.W, Input.Keys.S -> {
                    movement.z = 0;
                    yield true;
                }
                case Input.Keys.A, Input.Keys.D -> {
                    movement.x = 0;
                    yield true;
                }
                case Input.Keys.SPACE -> {
                    movement.y = 0;
                    yield true;
                }
                case Input.Keys.SHIFT_LEFT, Input.Keys.SHIFT_RIGHT -> {
                    speedUp = false;
                    yield true;
                }
                default -> false;
            };
        }
        return handled;
    }

    public void update(float delta) {
        cameraLocalPosition.set(config.eyeOffset).add(0, cameraToPlayerDistance / 4, -cameraToPlayerDistance);
        camera.position.set(player.localToWorldLocation(cameraLocalPosition));
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
        if (speedUp)
            currentMaxSpeed = config.maxRunningSpeed;
        else
            currentMaxSpeed = config.maxWalkingSpeed;

        applyMovement(delta);
        if (!grounded)
            movement.y = 0;
        camera.update(true);
    }

    protected void applyMovement(float delta) {}

    @Override
    public void dispose() {
    }
}
