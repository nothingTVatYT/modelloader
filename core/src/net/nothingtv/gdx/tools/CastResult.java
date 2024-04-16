package net.nothingtv.gdx.tools;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.ClosestConvexResultCallback;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import net.nothingtv.gdx.terrain.Terrain;

public class CastResult {
    public SceneObject hitObject;
    public Vector3 hitPosition = new Vector3();
    private final boolean cbHasHit;
    public float closestHitFraction;
    public btCollisionObject collisionObject;
    public Terrain.TerrainChunk hitTerrainChunk;

    public CastResult(ClosestConvexResultCallback callback) {
        cbHasHit = callback.hasHit();
        if (cbHasHit) {
            closestHitFraction = callback.getClosestHitFraction();
            collisionObject = callback.getHitCollisionObject();
            callback.getHitPointWorld(hitPosition);
            if (collisionObject != null) {
                if (collisionObject.userData instanceof SceneObject so)
                    hitObject = so;
                else if (collisionObject.userData instanceof Terrain.TerrainChunk chunk)
                    hitTerrainChunk = chunk;
            }
        }
    }

    public boolean hasHit() {
        return cbHasHit;
    }
}
