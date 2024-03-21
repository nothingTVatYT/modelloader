package net.nothingtv.gdx.tools;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;
import com.badlogic.gdx.utils.Disposable;

public class SceneObject implements Disposable {
    public String name;
    public ModelInstance modelInstance;
    public btRigidBody rigidBody;
    public btMotionState motionState;
    public BoundingBox boundingBox;
    public BoundingBox physicsBoundingBox;
    private final Matrix4 tmpMatrix = new Matrix4();
    public static final Vector3 LockX = new Vector3(0, 1, 1);
    public static final Vector3 LockY = new Vector3(1, 0, 1);
    public static final Vector3 LockZ = new Vector3(1, 1, 0);
    public static final Vector3 LockXZ = new Vector3(0, 1, 0);
    public static final Vector3 LockAll = new Vector3(0, 0, 0);

    public SceneObject(String name, ModelInstance modelInstance) {
        this.name = name;
        this.modelInstance = modelInstance;
        updateBoundingBox();
    }

    public SceneObject(String name, ModelInstance modelInstance, btRigidBody rigidBody, BoundingBox boundingBox) {
        this.name = name;
        this.modelInstance = modelInstance;
        this.rigidBody = rigidBody;
        this.boundingBox = boundingBox;
    }

    public void setRigidBody(btRigidBody rigidBody, btMotionState motionState) {
        this.rigidBody = rigidBody;
        this.motionState = motionState;
        updatePhysicsBoundingBox();
    }

    public void updatePhysicsBoundingBox() {
        if (rigidBody != null) {
            if (physicsBoundingBox == null)
                physicsBoundingBox = new BoundingBox();
            Vector3 minimum = new Vector3();
            Vector3 maximum = new Vector3();
            rigidBody.getAabb(minimum, maximum);
            physicsBoundingBox.set(minimum, maximum);
        }
    }

    /**
     * relative move the object, an attached rigid body is warped (i.e. not physically accurate)
     * @param translation the delta to be applied to the world position
     */
    public void move(Vector3 translation) {
        modelInstance.transform.translate(translation);
        if (rigidBody != null)
            //rigidBody.translate(translation);
            rigidBody.proceedToTransform(modelInstance.transform);
    }

    /**
     * Warp the object to this location, the rigidbody won't get any force but warped as well
     * @param position the position to warp to
     */
    public void moveTo(Vector3 position) {
        modelInstance.transform.setTranslation(position);
        if (rigidBody != null) {
            rigidBody.getWorldTransform(tmpMatrix);
            tmpMatrix.setTranslation(position);
            rigidBody.setWorldTransform(tmpMatrix);
        }
    }

    public void addForce(Vector3 force) {
        if (rigidBody != null) {
            rigidBody.applyCentralForce(force);
        }
    }

    /**
     * Set the factor that influences how forces are applied (often called linear constraint)
     * @param factor the factor, you can use the Lock* constants
     */
    public void setLinearFactor(Vector3 factor) {
        rigidBody.setLinearFactor(factor);
    }

    /**
     * Set the factor that influences how forces are applied (often called angular constraint)
     * @param factor the factor, you can use the Lock* constants
     */
    public void setAngularFactor(Vector3 factor) {
        rigidBody.setAngularFactor(factor);
    }

    public void rotate(Vector3 axis, float degrees) {
        modelInstance.transform.rotate(axis, degrees);
        if (rigidBody != null) {
            rigidBody.getWorldTransform(tmpMatrix);
            tmpMatrix.rotate(axis, degrees);
            rigidBody.setWorldTransform(tmpMatrix);
        }
    }

    public void updateBoundingBox() {
        if (boundingBox == null)
            boundingBox = new BoundingBox();
        modelInstance.calculateBoundingBox(boundingBox);
    }

    @Override
    public void dispose() {
        rigidBody.dispose();
        motionState.dispose();
    }
}
