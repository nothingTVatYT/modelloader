package net.nothingtv.gdx.tools;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.linearmath.btScalarArray;

public class Physics {

    public static btDiscreteDynamicsWorld currentPhysicsWorld;

    private static btSphereShape sphereShape;
    private static final Matrix4 fromMatrix = new Matrix4();
    private static final Matrix4 toMatrix = new Matrix4();
    private static final Vector3 tmp1 = new Vector3();

    public static CastResult castSphere(float radius, Vector3 from, Vector3 direction, float maxDistance) {
        if (currentPhysicsWorld != null) {
            Vector3 to = new Vector3(direction).scl(maxDistance).add(from);
            ClosestConvexResultCallback callback = new ClosestConvexResultCallback(from, to);
            tmp1.set(direction).scl(maxDistance);
            fromMatrix.idt().translate(from);
            toMatrix.set(fromMatrix).translate(tmp1);
            return castSphere(radius, callback);
        }
        return null;
    }

    public static CastResult castSphere(float radius, Vector3 from, Vector3 direction, float maxDistance, btCollisionObject me) {
        if (currentPhysicsWorld != null) {
            Vector3 to = new Vector3(direction).scl(maxDistance).add(from);
            ClosestNotMeConvexResultCallback callback = new ClosestNotMeConvexResultCallback(me, from, to);
            tmp1.set(direction).scl(maxDistance);
            fromMatrix.idt().translate(from);
            toMatrix.idt().translate(to);
            return castSphere(radius, callback);
        }
        return null;
    }

    private static CastResult castSphere(float radius, ClosestConvexResultCallback callback) {
        btSphereShape castShape = getSphereShape();
        castShape.setUnscaledRadius(radius);
        currentPhysicsWorld.convexSweepTest(castShape, fromMatrix, toMatrix, callback);
        CastResult result = new CastResult(callback);
        if (callback.hasHit()) {
            Vector3 pos = new Vector3().mul(fromMatrix);
            Vector3 to = new Vector3().mul(toMatrix);
            result.hitPosition = new Vector3();
            result.hitPosition.set(pos).lerp(to, callback.getClosestHitFraction());
            btCollisionObject co = callback.getHitCollisionObject();
            if (co != null)
                result.hitObject = (SceneObject) co.userData;
        }
        callback.dispose();
        return result;
    }

    private static btSphereShape getSphereShape() {
        if (sphereShape == null)
            sphereShape = new btSphereShape(1);
        return sphereShape;
    }

    public static PickResult pick(Camera camera, float maxDistance) {
        if (currentPhysicsWorld != null) {
            Ray pickRay = camera.getPickRay(Gdx.input.getX(), Gdx.input.getY());
            Vector3 rayFrom = new Vector3(pickRay.origin);
            Vector3 rayTo = new Vector3(pickRay.direction).scl(maxDistance).add(rayFrom);
            AllHitsRayResultCallback resultCallback = new AllHitsRayResultCallback(rayFrom, rayTo);
            PickResult pickResult = new PickResult(resultCallback);
            currentPhysicsWorld.rayTest(rayFrom, rayTo, resultCallback);
            if (resultCallback.hasHit()) {
                btScalarArray fractions = resultCallback.getHitFractions();
                btCollisionObject collisionObject = null;
                pickResult.hitPosition = new Vector3();
                float minDist = maxDistance;
                for (int i = 0; i < fractions.size(); i++) {
                    float dist = fractions.atConst(i);
                    if (dist < minDist) {
                        collisionObject = resultCallback.getCollisionObjects().atConst(i);
                        pickResult.hitPosition.set(rayFrom).lerp(rayTo, dist);
                        minDist = dist;
                    }
                }
                if (collisionObject != null && collisionObject.userData != null) {
                    pickResult.pickedObject = (SceneObject) collisionObject.userData;
                }
            }
            resultCallback.dispose();
            return pickResult;
        }
        return null;
    }

}
