package net.nothingtv.gdx.modelloader;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import net.nothingtv.gdx.tools.CastResult;
import net.nothingtv.gdx.tools.Physics;

public class FirstPersonKinematicController extends BasePlayerController {

    private final Matrix4 tmpMatrix = new Matrix4();

    public FirstPersonKinematicController(ControllerConfig config) {
        super(config);
    }

    @Override
    public void init() {
        super.init();
        player.motionState.getWorldTransform(tmpMatrix);
        player.rigidBody.setWorldTransform(tmpMatrix);
    }

    @Override
    protected void applyMovement(float delta) {
        float heightOverGround = 3;
        CastResult result = Physics.castSphere(0.3f, player.getPosition(), down, 3f, player.rigidBody);
        if (result != null && result.hasHit()) {
            heightOverGround = player.getPosition().y - result.hitPosition.y - player.physicsBoundingBox.getHeight()/2;
        }
        grounded = heightOverGround < 0.05f;

        Vector3 velocityXZ = player.rigidBody.getLinearVelocity();
        velocityXZ.y = 0;
        currentSpeed = velocityXZ.len();
        walking = velocityXZ.len2() > config.minVelocity2;
        linearForce.setZero();
        if (movement.len2() > 1e-6f) {
            // accelerating up to max speed when grounded
            if (grounded && currentSpeed < currentMaxSpeed) {
                linearForce.set(movement.x * config.accelerationForce, movement.y * config.jumpForce, movement.z * config.accelerationForce);
                //player.localToWorldDirection(linearForce);
            } else {
                linearForce.x = 0;
                linearForce.z = 0;
            }
        }
        if (config.simulateSideFriction && Math.abs(movement.x) < 1e-6f) {
            player.worldToLocalDirection(velocityXZ);
            velocityXZ.z = 0;
            if (velocityXZ.len2() > 1e-6f) {
                player.localToWorldDirection(velocityXZ);
                linearForce.add(velocityXZ.scl(player.mass * -config.breakForce));
            }
        }
        linearForce.scl(delta);
        if (!linearForce.isZero(0.00001f))
            System.out.printf("move player by %s (grounded=%s, max speed=%.3f, speed=%.3f, movement=%s)%n", linearForce, grounded, currentMaxSpeed, currentSpeed, movement);
        player.move(linearForce);
        /*
        player.modelInstance.transform.translate(linearForce);
        player.motionState.getWorldTransform(tmpMatrix);
        player.rigidBody.setWorldTransform(tmpMatrix);

         */
    }
}
