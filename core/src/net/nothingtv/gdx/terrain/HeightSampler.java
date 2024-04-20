package net.nothingtv.gdx.terrain;

/**
 * Height sampler that returns heights at certain positions
 */
public interface HeightSampler {
    /**
     * init is called by the terrain implementation before the height functions are called
     * @param terrain the terrain object using this sampler
     */
    void init(Terrain terrain);

    /**
     * Get the height at the location given in vertex space
     * @param x the x coordinate (0..terrain.width)
     * @param z the z coordinate (0..terrain.height)
     * @return the height at this location
     */
    float getHeight(float x, float z);

    float getMinHeight();
    float getMaxHeight();
}
