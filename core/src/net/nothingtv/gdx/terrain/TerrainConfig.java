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
    public Texture splatMap;

    /**
     * a factor of 1 will create one mesh, a value of 2 four meshes, etc.
     */
    public int terrainDivideFactor;

    public int erosionIterations;
    /**
     * the layers of the terrain, currently four layers are supported
     */
    public Array<TerrainLayer> layers = new Array<>(4);

    public TerrainConfig(int width, int height, float scale) {
        this.width = width;
        this.height = height;
        this.scale = scale;
        this.terrainDivideFactor = 1;
        this.erosionIterations = 0;
    }

    public void addLayer(Texture diffuse, float scale) {
        TerrainLayer layer = new TerrainLayer(diffuse, scale);
        layers.add(layer);
    }

    public void setHeightMap(Pixmap heightMap, float heightScale, float heightOffset) {
        if (heightSampler instanceof HeightMapHeightSampler hhs)
            hhs.set(heightMap, heightScale, heightOffset);
        else {
            this.heightSampler = new HeightMapHeightSampler(heightMap, heightScale, heightOffset);
        }
    }
}
