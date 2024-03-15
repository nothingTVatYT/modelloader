package net.nothingtv.gdx.terrain;

import com.badlogic.gdx.math.Vector3;

public class DefaultHeightSampler implements HeightSampler {

    /**
     * a reference to the terrain, this will be set by the Terrain class
     */
    protected Terrain terrain;

    @Override
    public void init(Terrain terrain) {
        this.terrain = terrain;
    }

    @Override
    public float getHeight(int x, int z) {
        return 0;
    }

    @Override
    public Vector3 getNormal(int x, int z, Vector3 out) {
        out.set(Vector3.Y);
        return out;
    }
}
