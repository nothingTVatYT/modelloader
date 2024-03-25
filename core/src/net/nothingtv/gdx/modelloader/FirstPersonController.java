package net.nothingtv.gdx.modelloader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import net.nothingtv.gdx.tools.SceneObject;

public class FirstPersonController extends InputAdapter {

    private Camera camera;
    private SceneObject player;
    private Vector3 movement = new Vector3();
    private Vector3 linearForce = new Vector3();
    private float accelerationForce = 10f;
    private float breakForce = 10f;
    private float turningSpeed = 0.25f;
    private float pitchSpeed = 80;
    private float minVelocity2 = 0.05f;
    private boolean walking;
    private boolean mouseGrabbed;
    private int screenWidth, screenHeight;
    private final Vector3 cameraRotation = new Vector3();

    public FirstPersonController(Camera camera, SceneObject player) {
        this.camera = camera;
        this.player = player;
    }

    public void init() {
        //updateCamera(0);
        walking = false;
        mouseGrabbed = Gdx.input.isCursorCatched();
        updateScreenSize();
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

    public void updateScreenSize() {
        screenWidth = MathUtils.round(camera.viewportWidth);
        screenHeight = MathUtils.round(camera.viewportHeight);
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
        player.rotate(Vector3.Y, -Gdx.input.getDeltaX() * turningSpeed);
        // vertical rotation applied to the camera only
        cameraRotation.set(Vector3.Z);
        float angle = MathUtils.clamp((((float)Gdx.input.getY() / screenHeight) - 0.5f) * 80, -80f, 80f);
        cameraRotation.rotate(Vector3.X, angle);
        player.localToWorldDirection(cameraRotation);
        camera.direction.set(cameraRotation);
        camera.update(true);
        applyForces(delta);
    }

    private void applyForces(float delta) {
        Vector3 velocity = player.rigidBody.getLinearVelocity();
        walking = velocity.len2() > minVelocity2;
        if (movement.len2() > 0) {
            linearForce.set(movement).scl(player.mass * accelerationForce);
            System.out.printf("applying force %s%n", linearForce);
            player.localToWorldDirection(linearForce);
        } else {
            linearForce.set(velocity).y = 0;
            linearForce.scl(player.mass * -breakForce);
            System.out.printf("breaking with %s%n", linearForce);
        }
        player.addForce(linearForce);
    }
}
