package net.nothingtv.gdx.terrain;

import net.nothingtv.gdx.tools.OpenSimplex2S;

public class TestHeightSampler extends DefaultHeightSampler {

    private float heightScale;
    private float horizontalScale;
    private long seed;

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
