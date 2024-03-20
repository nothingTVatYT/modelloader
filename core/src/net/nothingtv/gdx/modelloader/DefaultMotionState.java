package net.nothingtv.gdx.modelloader;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;

public class DefaultMotionState extends btMotionState {
    public ModelInstance modelInstance;
    public Vector3 rigidBodyOffset = new Vector3();

    public DefaultMotionState(ModelInstance modelInstance) {
        this.modelInstance = modelInstance;
    }

    @Override
    public void getWorldTransform(Matrix4 worldTrans) {
        worldTrans.set(modelInstance.transform).translate(rigidBodyOffset);
        //System.out.printf("getWorldTransform: set translation to %s%n", worldTrans.getTranslation(new Vector3()));
    }

    @Override
    public void setWorldTransform(Matrix4 worldTrans) {
        modelInstance.transform.set(worldTrans);
        //System.out.println("set world transform called on " + modelInstance);
    }

}
