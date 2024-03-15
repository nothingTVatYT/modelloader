package net.nothingtv.gdx.terrain;

import com.badlogic.gdx.graphics.Pixmap;

public class HeightMapHeightSampler extends DefaultHeightSampler {

    private Pixmap heightMap;
    private float heightScale;
    private float heightOffset;
    private int terrainWidth, terrainHeight;
    private boolean useScaleSamplePosition;
    private float mapX, mapZ;

    public HeightMapHeightSampler(Pixmap heightMap, float heightScale, float heightOffset) {
        this.heightMap = heightMap;
        this.heightScale = heightScale;
        this.heightOffset = heightOffset;
    }

    public void set(Pixmap heightMap, float heightScale, float heightOffset) {
        this.heightMap = heightMap;
        this.heightScale = heightScale;
        this.heightOffset = heightOffset;
    }

    @Override
    public void init(Terrain terrain) {
        super.init(terrain);
        terrainWidth = terrain.config.width;
        terrainHeight = terrain.config.height;
        useScaleSamplePosition = terrainWidth != heightMap.getWidth() || terrainHeight != heightMap.getHeight();
        if (useScaleSamplePosition) {
            mapX = (float)heightMap.getWidth() / terrainWidth;
            mapZ = (float)heightMap.getHeight() / terrainWidth;
        }
    }

    @Override
    public float getHeight(int x, int z) {
        int pixel;
        if (useScaleSamplePosition) {
            int px = (int)Math.floor(x * mapX);
            int py = (int)Math.floor(z * mapZ);
            float dx = x * mapX - px;
            float dy = z * mapZ - py;
            if (px > heightMap.getWidth()-1)
                px = heightMap.getWidth()-1;
            if (py > heightMap.getHeight()-1)
                py = heightMap.getHeight()-1;
            int px1 = Math.min(px + 1, heightMap.getWidth() - 1);
            int py1 = Math.min(py + 1, heightMap.getHeight() - 1);
            pixel = heightMap.getPixel(px, py);
            int pixelR = heightMap.getPixel(px1, py);
            int pixelB = heightMap.getPixel(px, py1);
            int pixelBR = heightMap.getPixel(px1, py1);
            float red = (pixel >> 24 & 0xff) / 256f;
            float redR = (pixelR >> 24 & 0xff) / 256f;
            float redB = (pixelB >> 24 & 0xff) / 256f;
            float redBR = (pixelBR >> 24 & 0xff) / 256f;
            float h1 = red + dx * (redR - red);
            float h2 = redB + dx * (redBR - redB);
            float h = h1 + dy * (h2 - h1);
            return h * heightScale + heightOffset;
        } else pixel = heightMap.getPixel(x, z);
        // actually the red channel should be enough if we assume a gray texture in rgba
        // but this could handle colored textures if you really want to
        //return ((pixel >> 8 & 0xff) + (pixel >> 16 & 0xff) + (pixel >> 24 & 0xff)) / 3f / 256f * heightScale + heightOffset;
        return (pixel >> 24 & 0xff) / 256f * heightScale + heightOffset;
    }
}
