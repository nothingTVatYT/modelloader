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

    private final float scale;
    private final int width, height;
    private float[] vertices;
    private short[] indices;
    private Mesh mesh;

    public Terrain(int width, int height, float scale) {
        this.width = width;
        this.height = height;
        this.scale = scale;
    }

    public ModelInstance createModelInstance() {
        mesh = createMesh();
        Material material = new Material("_terrain");
        Texture splat1Texture = new Texture("textures/Ground048_2K_Color.jpg");
        splat1Texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        Texture splat2Texture = new Texture("textures/Ground026_2K_Color.jpg");
        splat2Texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        Texture splat3Texture = new Texture("textures/leafy_grass_diff_2k.jpg");
        splat3Texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        Texture splat4Texture = new Texture("textures/cobblestone_floor_07_diff_2k.jpg");
        splat4Texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        material.set(TerrainTextureAttribute.createDiffuse(splat1Texture));
        material.set(TerrainTextureAttribute.createDiffuse2(splat2Texture));
        material.set(TerrainTextureAttribute.createDiffuse3(splat3Texture));
        material.set(TerrainTextureAttribute.createDiffuse4(splat4Texture));
        material.set(TerrainFloatAttribute.createUV1Scale(4f));
        material.set(TerrainFloatAttribute.createUV2Scale(4f));
        material.set(TerrainFloatAttribute.createUV3Scale(4f));
        material.set(TerrainFloatAttribute.createUV4Scale(4f));
        material.set(IntAttribute.createCullFace(GL20.GL_BACK));
        material.set(TerrainTextureAttribute.createAlpha1(new Texture("textures/alpha-example.png")));

        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        modelBuilder.part("terrain", mesh, GL20.GL_TRIANGLES, material);
        return new ModelInstance(modelBuilder.end());
    }

    private Mesh createMesh() {
        // only the position + tex coords
        vertices = new float[width * height * 5];
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
        indices = new short[triangles * 3];
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
