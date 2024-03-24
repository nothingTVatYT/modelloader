package net.nothingtv.gdx.terrain;

import net.nothingtv.gdx.tools.OpenSimplex2S;

public class TestHeightSampler extends DefaultHeightSampler {

    private float heightScale;
    private float horizontalScale;
    private long seed;

    /**
     * Generate a heightmap using OpenSimplex noise
     * @param heightScale the scale factor to generate the vertices height from the noise values
     * @param horizontalScale the noise scale, lower values means more details
     * @param seed the seed for the pseudo random number generator
     */
    public TestHeightSampler(float heightScale, float horizontalScale, long seed) {
        this.heightScale = heightScale;
        this.horizontalScale = horizontalScale;
        this.seed = seed;
    }

    @Override
    public float getHeight(float x, float z) {
        return OpenSimplex2S.noise2(seed, x * horizontalScale, z * horizontalScale) * heightScale;
    }

}
