package net.nothingtv.gdx.tools;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.AllHitsRayResultCallback;
import com.badlogic.gdx.utils.Disposable;

public class PickResult implements Disposable {
    public AllHitsRayResultCallback resultCallback;
    public SceneObject pickedObject;
    public Vector3 hitPosition;

    public PickResult(AllHitsRayResultCallback callback) {
        this.resultCallback = callback;
    }

    public boolean hasHit() {
        return resultCallback.hasHit();
    }

    @Override
    public void dispose() {
        resultCallback.dispose();
    }
}
