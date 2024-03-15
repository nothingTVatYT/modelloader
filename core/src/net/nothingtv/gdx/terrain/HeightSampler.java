package net.nothingtv.gdx.terrain;

import com.badlogic.gdx.math.Vector3;

public interface HeightSampler {
    void init(Terrain terrain);
    float getHeight(int x, int z);
    Vector3 getNormal(int x, int z, Vector3 out);
}
