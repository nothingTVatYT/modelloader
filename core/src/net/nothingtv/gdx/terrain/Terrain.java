package net.nothingtv.gdx.terrain;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;

public class Terrain {

    private Mesh mesh;
    public TerrainConfig config;

    public Terrain(int width, int height, float scale) {
        this(new TerrainConfig(width, height, scale));
    }

    public Terrain(TerrainConfig config) {
        this.config = config;
    }

    public ModelInstance createModelInstance() {
        if (config.layers.isEmpty() || config.layers.size > 4) {
            throw new RuntimeException("Cannot create a terrain with " + config.layers.size + " layers.");
        }
        mesh = createMesh(config.width, config.height, config.scale);

        Material material = new Material("_terrain");
        int layers = config.layers.size;
        int layerIndex = 0;
        TerrainConfig.TerrainLayer layer = config.layers.get(layerIndex);
        Texture splat1Texture = layer.diffuse;
        splat1Texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        material.set(TerrainTextureAttribute.createDiffuse(splat1Texture));
        material.set(TerrainFloatAttribute.createUV1Scale(layer.scaleUV));
        if (layers > 1) {
            layer = config.layers.get(++layerIndex);
            Texture splat2Texture = layer.diffuse;
            splat2Texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
            material.set(TerrainTextureAttribute.createDiffuse2(splat2Texture));
            material.set(TerrainFloatAttribute.createUV2Scale(layer.scaleUV));
            if (layers > 2) {
                layer = config.layers.get(++layerIndex);
                Texture splat3Texture = layer.diffuse;
                splat3Texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
                material.set(TerrainTextureAttribute.createDiffuse3(splat3Texture));
                material.set(TerrainFloatAttribute.createUV3Scale(layer.scaleUV));
                if (layers > 3) {
                    layer = config.layers.get(++layerIndex);
                    Texture splat4Texture = layer.diffuse;
                    splat4Texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
                    material.set(TerrainTextureAttribute.createDiffuse4(splat4Texture));
                    material.set(TerrainFloatAttribute.createUV4Scale(layer.scaleUV));
                }
            }
        }
        material.set(IntAttribute.createCullFace(GL20.GL_BACK));
        material.set(TerrainTextureAttribute.createAlpha1(config.splatMap));

        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        modelBuilder.part("terrain", mesh, GL20.GL_TRIANGLES, material);
        return new ModelInstance(modelBuilder.end());
    }

    private Mesh createMesh(int width, int height, float scale) {
        // only the position + tex coords
        float[] vertices = new float[width * height * 5];
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                vertices[index++] = x * scale;
                vertices[index++] = (float)Math.sin((x+y)/2f) * 0.5f + (float)Math.sin(x-y) * 0.2f;
                vertices[index++] = y * scale;
                vertices[index++] = (float)x / width;
                vertices[index++] = (float)y / height;
            }
        }

        // we create width x height vertices forming width-1 x height-1 quads or 2 x (width-1) x (height-1) triangles
        int triangles = 2 * (width-1) * (height-1);
        short[] indices = new short[triangles * 3];
        index = 0;
        short yOffset = (short)width;
        for (int y = 0; y < height-1; y++) {
            for (int x = 0; x < width-1; x++) {
                // top left triangles are 0,width,1 and 1,width,width+1
                // the following are offset by x and y*yOffset
                indices[index++] = (short)(x + y * yOffset);
                indices[index++] = (short)(yOffset + x + y * yOffset);
                indices[index++] = (short)(1 + x + y * yOffset);
                indices[index++] = (short)(1 + x + y * yOffset);
                indices[index++] = (short)(yOffset + x + y * yOffset);
                indices[index++] = (short)(yOffset + 1 + x + y * yOffset);
            }
        }
        Mesh mesh = new Mesh(true, vertices.length, indices.length,
                VertexAttribute.Position(), VertexAttribute.TexCoords(0));
        mesh.setVertices(vertices);
        mesh.setIndices(indices);
        return mesh;
    }
}
