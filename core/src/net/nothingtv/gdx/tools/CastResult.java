package net.nothingtv.gdx.tools;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.ClosestConvexResultCallback;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;

public class CastResult {
    public SceneObject hitObject;
    public Vector3 hitPosition = new Vector3();
    private final boolean cbHasHit;
    public float closestHitFraction;
    public btCollisionObject collisionObject;

    public CastResult(ClosestConvexResultCallback callback) {
        cbHasHit = callback.hasHit();
        if (cbHasHit) {
            closestHitFraction = callback.getClosestHitFraction();
            collisionObject = callback.getHitCollisionObject();
            callback.getHitPointWorld(hitPosition);
            if (collisionObject != null)
                hitObject = (SceneObject) collisionObject.userData;
        }
    }

    public boolean hasHit() {
        return cbHasHit;
    }
}
