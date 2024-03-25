package net.nothingtv.gdx.modelloader;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;

public class DefaultMotionState extends btMotionState {
    public ModelInstance modelInstance;
    public Vector3 rigidBodyOffset = new Vector3();
    private Vector3 negOffset = new Vector3();

    public DefaultMotionState(ModelInstance modelInstance) {
        this.modelInstance = modelInstance;
    }

    @Override
    public void getWorldTransform(Matrix4 worldTrans) {
        worldTrans.set(modelInstance.transform).translate(rigidBodyOffset);
    }

    @Override
    public void setWorldTransform(Matrix4 worldTrans) {
        negOffset.set(rigidBodyOffset).scl(-1);
        modelInstance.transform.set(worldTrans).translate(negOffset);
    }

}
