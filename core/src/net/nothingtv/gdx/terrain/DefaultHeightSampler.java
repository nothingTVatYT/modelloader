package net.nothingtv.gdx.terrain;

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
    public float getHeight(float x, float z) {
        return 0;
    }
}
