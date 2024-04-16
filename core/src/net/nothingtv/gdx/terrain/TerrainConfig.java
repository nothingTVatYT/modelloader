package net.nothingtv.gdx.terrain;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Array;

/**
 * Holds the configuration of the terrain
 */
public class TerrainConfig {

    public static class TerrainLayer {
        /**
         * diffuse color texture of this layer
         */
        public Texture diffuse;

        /**
         * uv scale for repeating the texture, 1 means the texture is stretched once over the terrain,
         * e.g. 16 means it's repeated 16 times
         */
        public float scaleUV;

        public TerrainLayer(Texture diffuse, float scaleUV) {
            this.diffuse = diffuse;
            this.scaleUV = scaleUV;
        }
    }
    /**
     * width and height of the terrain in vertex resolution
     */
    public int width, height;
    /**
     * width and height for a terrain chunk usd for physics
     */
    public float chunkWidth, chunkHeight;
    /**
     * resolution of the physics collision mesh in vertex per unit
     */
    public float chunkResolution = 1;

    /**
     * relative distance (normalized to 0..1 using the chunk size) at which a new terrain chunk is loaded
     */
    public float chunkLoadRelDistance = 0.1f;
    /**
     * relative distance (normalized to 0..1 using the chunk size) at which a terrain chunk is removed from the physics world
     */
    public float chunkUnloadRelDistance = 1f;
    /**
     * delete the terrain chunk after this tim in seconds after the last state changed
     */
    public float chunkDeletionTime = 60f;
    /**
     * the scale that dictates how big the terrain gets, width*scale x height*scale
     */
    public float scale;

    /**
     * The sampler that returns a height at a certain position (in 0-width and 0-height)
     */
    public HeightSampler heightSampler;

    /**
     * the alpha splat map, it needs to have four channels (i.e. ARGB) which are used to blend up to four layers
     */
    public Pixmap splatMap;

    /**
     * a factor of 1 will create one mesh, a value of 2 four meshes, etc.
     */
    public int terrainDivideFactor;

    /**
     * the layers of the terrain, currently four layers are supported
     */
    public Array<TerrainLayer> layers = new Array<>(4);

    public TerrainConfig(int width, int height, float scale) {
        this.width = width;
        this.height = height;
        this.scale = scale;
        this.terrainDivideFactor = 1;
    }

    public void addLayer(Texture diffuse, float scale) {
        TerrainLayer layer = new TerrainLayer(diffuse, scale);
        layers.add(layer);
    }
}
