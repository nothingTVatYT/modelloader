package net.nothingtv.gdx.tools;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import net.mgsx.gltf.scene3d.scene.Updatable;

public class NpcObject extends SceneObject implements Updatable {

    public boolean walking = false;
    private final Vector3 velocityXZ = new Vector3();

    public NpcObject(String name, ModelInstance modelInstance) {
        super(name, modelInstance);
    }

    @Override
    public void update(Camera camera, float v) {
        if (!walking) {
            // counteract horizontal sliding
            velocityXZ.set(rigidBody.getLinearVelocity());
            velocityXZ.y = 0;
            velocityXZ.scl(-10 * mass);
            rigidBody.applyCentralForce(velocityXZ);
        }
    }
}
