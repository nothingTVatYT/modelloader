package net.nothingtv.gdx.terrain;

public class MapHeightSampler extends DefaultHeightSampler {
    private final float[] map;
    private final int mapSize;

    public MapHeightSampler(float[] map, int mapSize) {
        this.map = map;
        this.mapSize = mapSize;
    }

    @Override
    public float getHeight(float x, float z) {
        return map[(int)z * mapSize + (int)x];
    }
}
