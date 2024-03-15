package net.nothingtv.gdx.terrain;

import com.badlogic.gdx.math.Vector3;

public class TestHeightSampler extends DefaultHeightSampler {

    @Override
    public float getHeight(int x, int z) {
        return (float)Math.sin((x+z)/2f) * 0.5f + (float)Math.sin(x-z) * 0.2f;
    }

    @Override
    public Vector3 getNormal(int x, int z, Vector3 out) {
        return super.getNormal(x, z, out);
    }
}
