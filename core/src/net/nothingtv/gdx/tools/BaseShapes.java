package net.nothingtv.gdx.tools;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btCapsuleShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.collision.btSphereShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;

public class BaseShapes {
    private static final Vector3 tmp1 = new Vector3();
    private static final Vector3 tmp2 = new Vector3();
    public static btCollisionShape createBoxShape(ModelInstance modelInstance) {
        Vector3 halfExtends = new Vector3();
        BoundingBox bounds = new BoundingBox();
        modelInstance.calculateBoundingBox(bounds);
        return new btBoxShape(bounds.getDimensions(halfExtends).scl(0.5f));
    }

    public static btCollisionShape createSphereShape(ModelInstance modelInstance) {
        Vector3 halfExtends = new Vector3();
        BoundingBox bounds = new BoundingBox();
        modelInstance.calculateBoundingBox(bounds);
        return new btSphereShape(bounds.getDimensions(halfExtends).scl(0.5f).x);
    }

    public static btCollisionShape createCapsuleShape(ModelInstance modelInstance) {
        BoundingBox bounds = new BoundingBox();
        modelInstance.calculateBoundingBox(bounds);
        float radius = Math.max(bounds.getDepth(), bounds.getWidth()) / 2;
        float height = bounds.getHeight() - 2 * radius;
        return new btCapsuleShape(radius, height);
    }

    public static void dumpRigidBody(btRigidBody rigidBody) {
        Vector3 translation = new Vector3();
        Quaternion rotation = new Quaternion();
        rigidBody.getWorldTransform().getTranslation(translation);
        rigidBody.getWorldTransform().getRotation(rotation);
        System.out.printf("rigidbody at %s (pitch %f, roll %f, yaw %f)%n damping linear %f, angular %f, contact %f%n friction %f%n", translation,
                rotation.getPitch(), rotation.getRoll(), rotation.getYaw(),
                rigidBody.getLinearDamping(), rigidBody.getAngularDamping(), rigidBody.getContactDamping(),
                rigidBody.getFriction());
        tmp1.set(rigidBody.getLinearFactor());
        tmp2.set(rigidBody.getAngularFactor());
        System.out.printf(" factor linear %s, angular %s%n", tmp1, tmp2);
    }
}
