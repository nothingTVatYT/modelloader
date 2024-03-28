package net.nothingtv.gdx.terrain;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Json;
import net.nothingtv.gdx.tools.Tools;

import java.nio.ByteBuffer;

public class TerrainSplatGenerator {
    public static class Configuration {
        public int resolution;
        public int defaultLayer = 0;
        public int numLayers = 4;
        public LayerConfiguration[] layers = new LayerConfiguration[4];
    }
    public static class LayerConfiguration {
        public float elevationWeight = 0;
        public float heightBegin;
        public float heightEnd;
        public float smoothBegin = 0.05f;
        public float smoothEnd = 0.05f;
        public float slopeWeight = 0;
        public float slopeBegin;
        public float slopeEnd;
        public float slopeSmoothBegin = 0.05f;
        public float slopeSmoothEnd = 0.05f;
    }

    private final Terrain terrain;
    public final Configuration config;

    public static Configuration createDefaultConfiguration(int numLayers) {
        Configuration configuration = new Configuration();
        configuration.numLayers = numLayers;
        for (int i = 0; i < numLayers; i++) {
            configuration.layers[i] = new LayerConfiguration();
        }
        return configuration;
    }

    public TerrainSplatGenerator(Terrain terrain, Configuration configuration) {
        this.terrain = terrain;
        this.config = configuration;
        if (this.config.resolution == 0)
            this.config.resolution = terrain.config.width;
    }

    public void update() {
        terrain.setSplatMap(generate());
    }

    public Pixmap generate() {
        int resolution = config.resolution;
        float minHeight = terrain.getMinHeight();
        float maxHeight = terrain.getMaxHeight();
        ByteBuffer buffer = ByteBuffer.allocateDirect(resolution * resolution * 4);
        Pixmap pixmap = new Pixmap(resolution, resolution, Pixmap.Format.RGBA8888);
        float[] weights = new float[4];
        Vector3 normal = new Vector3();
        float minSlope = Float.MAX_VALUE;
        float maxSlope = -Float.MAX_VALUE;
        for (int z = 0; z < resolution; z++) {
            float terrainZ = (float)z / resolution * terrain.config.height;
            for (int x = 0; x < resolution; x++) {
                float terrainX = (float)x / resolution * terrain.config.width;
                float weightSum = 0;
                for (int i = 0; i < config.numLayers; i++) {
                    LayerConfiguration layer = config.layers[i];
                    weights[i] = 0;
                    if (layer.elevationWeight > 0) {
                        float h = terrain.getHeightAt(terrainX, terrainZ);
                        float normHeight = (h - minHeight) / (maxHeight - minHeight);
                        weights[i] += layer.elevationWeight * Tools.smoothInRange(normHeight, layer.heightBegin, layer.heightEnd, layer.smoothBegin, layer.smoothEnd);
                    }
                    if (layer.slopeWeight > 0) {
                        terrain.getNormalAt(terrainX, terrainZ, normal);
                        float slope = 1f - normal.y;
                        minSlope = Math.min(minSlope, slope);
                        maxSlope = Math.max(maxSlope, slope);
                        weights[i] += layer.slopeWeight * Tools.smoothInRange(slope, layer.slopeBegin, layer.slopeEnd, layer.slopeSmoothBegin, layer.slopeSmoothEnd);
                    }
                    weightSum += weights[i];
                }
                if (weightSum < 1) {
                    weights[config.defaultLayer] = 1f - weightSum;
                    weightSum = 1f;
                }
                // the shader expects a=layer0, r=1, g=2, b=3 (msb to lsb)
                for (int i = 3; i > 0; i--) {
                    buffer.put((byte)Math.round(255f * weights[i]/weightSum));
                }
                buffer.put((byte)Math.round(255f * weights[0]/weightSum));
            }
        }
        buffer.flip();
        pixmap.setPixels(buffer);
        System.out.printf("found slopes in the range of %f to %f%n", minSlope, maxSlope);
        PixmapIO.writePNG(Gdx.files.local("splatmap-generated.png"), pixmap);
        new Json().toJson(config, Gdx.files.local("terrain.json"));
        return pixmap;
    }
}
