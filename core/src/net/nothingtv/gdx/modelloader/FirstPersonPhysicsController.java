package net.nothingtv.gdx.modelloader;

import com.badlogic.gdx.math.Vector3;
import net.nothingtv.gdx.tools.CastResult;
import net.nothingtv.gdx.tools.Physics;

public class FirstPersonPhysicsController extends BasePlayerController {

    public FirstPersonPhysicsController(BasePlayerController.ControllerConfig config) {
        super(config);
    }

    @Override
    protected void applyMovement(float delta) {
        float heightOverGround = 100;
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
                linearForce.set(movement.x * config.accelerationForce, movement.y * config.jumpForce, movement.z * config.accelerationForce).scl(player.mass);
                player.localToWorldDirection(linearForce);
            } else {
                linearForce.x = 0;
                linearForce.z = 0;
            }
        } else if (grounded) {
            // breaking when grounded
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
