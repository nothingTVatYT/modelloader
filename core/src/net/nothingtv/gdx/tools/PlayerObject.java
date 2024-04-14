package net.nothingtv.gdx.tools;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;

public class PlayerObject extends SceneObject {

    protected float stepHeight = 0.35f;
    private final Matrix4 tmpMatrix = new Matrix4();

    public PlayerObject(String name, ModelInstance modelInstance) {
        super(name, modelInstance);
    }

    public btCollisionObject getCollisionObject() {
        return rigidBody;
    }
}
