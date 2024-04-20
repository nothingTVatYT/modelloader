package net.nothingtv.gdx.terrain;

import net.nothingtv.game.network.Tools;
import net.nothingtv.gdx.tools.Async;

import java.util.concurrent.ConcurrentHashMap;

public class CachingHeightSampler implements HeightSampler {

    static class CachedValue {
        float value;
        long requested;
    }

    public final HeightSampler heightSampler;
    public final int capacity;
    private final ConcurrentHashMap<Long, CachedValue> cache;
    private long lastCleanUp;

    public CachingHeightSampler(HeightSampler heightSampler, int capacity) {
        this.heightSampler = heightSampler;
        this.capacity = capacity;
        this.cache = new ConcurrentHashMap<>(capacity);
        lastCleanUp = System.currentTimeMillis();
        Async.submit(this::cleanUp);
    }

    private void cleanUp() {
        while (true) {
            if (cache.mappingCount() >= capacity) {
                long deletion = lastCleanUp + 100;
                while (cache.mappingCount() > capacity) {
                    final long maxAge = deletion;
                    cache.values().removeIf(e -> e.requested < maxAge);
                    deletion += 100;
                }
                lastCleanUp = System.currentTimeMillis();
            }
            Tools.nap(5000);
        }
    }

    @Override
    public void init(Terrain terrain) {
        heightSampler.init(terrain);
    }

    @Override
    public float getHeight(float x, float z) {
        long key = (long) Float.floatToIntBits(x) << 32 | Float.floatToIntBits(z);
        CachedValue cachedValue = cache.get(key);
        if (cachedValue != null) {
            cachedValue.requested = System.currentTimeMillis();
            return cachedValue.value;
        }
        float value = heightSampler.getHeight(x, z);
        cachedValue = new CachedValue();
        cachedValue.value = value;
        cachedValue.requested = System.currentTimeMillis();
        cache.put(key, cachedValue);
        return value;
    }

    @Override
    public float getMinHeight() {
        return heightSampler.getMinHeight();
    }

    @Override
    public float getMaxHeight() {
        return heightSampler.getMaxHeight();
    }
}
