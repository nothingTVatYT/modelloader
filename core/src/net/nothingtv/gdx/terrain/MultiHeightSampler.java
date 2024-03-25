package net.nothingtv.gdx.terrain;

import java.util.ArrayList;
import java.util.List;

public class MultiHeightSampler extends DefaultHeightSampler {

    private final List<HeightSampler> samplers = new ArrayList<>();

    public void addSampler(HeightSampler sampler) {
        samplers.add(sampler);
    }

    @Override
    public void init(Terrain terrain) {
        for (HeightSampler sampler : samplers)
            sampler.init(terrain);
    }

    @Override
    public float getHeight(float x, float z) {
        float h = 0;
        for (HeightSampler sampler : samplers)
            h += sampler.getHeight(x, z);
        return h;
    }
}
