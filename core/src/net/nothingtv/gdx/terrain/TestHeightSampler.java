package net.nothingtv.gdx.terrain;

public class TestHeightSampler extends DefaultHeightSampler {

    @Override
    public float getHeight(float x, float z) {
        return (float)Math.sin((x+z)/2f) * 0.5f + (float)Math.sin(x-z) * 0.2f;
    }

}
