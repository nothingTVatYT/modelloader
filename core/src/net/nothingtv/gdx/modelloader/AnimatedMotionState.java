package net.nothingtv.gdx.modelloader;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

public class AnimatedMotionState extends DefaultMotionState {

    private final AnimatedModelInstance animatedModelInstance;
    private final Vector3 negOffset = new Vector3();

    public AnimatedMotionState(AnimatedModelInstance modelInstance) {
        super(modelInstance);
        animatedModelInstance = modelInstance;
    }

    @Override
    public void getWorldTransform(Matrix4 worldTrans) {
        worldTrans.set(animatedModelInstance.rootTransform).translate(rigidBodyOffset);
    }

    @Override
    public void setWorldTransform(Matrix4 worldTrans) {
        negOffset.set(rigidBodyOffset).scl(-1);
        animatedModelInstance.transform.set(worldTrans).translate(negOffset);
    }
}
