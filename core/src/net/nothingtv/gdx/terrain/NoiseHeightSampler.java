package net.nothingtv.gdx.terrain;

import net.nothingtv.gdx.tools.OpenSimplex2S;

public class NoiseHeightSampler extends DefaultHeightSampler {

    private int seed;
    private float heightScale;
    private int octaves;
    private float waveLength;
    private float mapWidth, mapHeight;
    private float exponent;

    public NoiseHeightSampler(int seed, float heightScale, int octaves, float waveLength, float exponent) {
        this.seed = seed;
        this.heightScale = heightScale;
        this.octaves = octaves;
        this.waveLength = waveLength;
        this.exponent = exponent;
    }

    @Override
    public void init(Terrain terrain) {
        super.init(terrain);
        mapWidth = terrain.config.width;
        mapHeight = terrain.config.height;
    }

    @Override
    public float getHeight(float x, float z) {
        float nx = x / mapWidth - 0.5f;
        float nz = z / mapHeight - 0.5f;
        float e = 0;
        float factor;
        float gain = 0;
        for (int i = 0; i < octaves; i++) {
            factor = 1f/(1<<i);
            e += factor * (1f+OpenSimplex2S.noise2(seed + i, nx * waveLength * (1<<i), nz * waveLength * (1<<i)));
            gain += factor;
        }
        return (float)Math.pow(e / gain, exponent) * heightScale;
    }

}
