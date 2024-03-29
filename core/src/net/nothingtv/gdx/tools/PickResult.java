package net.nothingtv.gdx.tools;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.AllHitsRayResultCallback;

public class PickResult {
    public SceneObject pickedObject;
    public Vector3 hitPosition;
    private final boolean cbHasHit;

    public PickResult(AllHitsRayResultCallback callback) {
        cbHasHit = callback.hasHit();
    }

    public boolean hasHit() {
        return cbHasHit;
    }
}
