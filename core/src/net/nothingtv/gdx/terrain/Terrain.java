package net.nothingtv.gdx.terrain;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;

public class Terrain {

    /**
     * cached height sampler
     */
    private HeightSampler heightSampler;
    public TerrainConfig config;

    public Terrain(TerrainConfig config) {
        this.config = config;
        if (config.heightSampler instanceof DefaultHeightSampler defaultHeightSampler)
            defaultHeightSampler.terrain = this;
    }

    public ModelInstance createModelInstance() {
        if (config.layers.isEmpty() || config.layers.size > 4) {
            throw new RuntimeException("Cannot create a terrain with " + config.layers.size + " layers.");
        }

        Material material = new Material("_terrain");
        int layers = config.layers.size;
        int layerIndex = 0;
        TerrainConfig.TerrainLayer layer = config.layers.get(layerIndex);
        Texture splat1Texture = layer.diffuse;
        splat1Texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        material.set(TerrainTextureAttribute.createBaseColorTexture(splat1Texture));
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
        Mesh mesh;
        if (config.terrainDivideFactor == 1) {
            mesh = createMesh(config.width, config.height, 0, 0, 1f, 1f, 0, 0, config.scale);
            modelBuilder.part("terrain", mesh, GL20.GL_TRIANGLES, material);
        } else {
            int d = config.terrainDivideFactor;
            int partWidth = config.width/d;
            int partHeight = config.height/d;
            for (int z = 0; z < d; z++)
                for (int x = 0; x < d; x++) {
                    Node node = modelBuilder.node();
                    node.id = String.format("terrain-%d-%d", x, z);
                    node.translation.set(x * partWidth * config.scale, 0, z * partHeight * config.scale);
                    mesh = createMesh(partWidth, partHeight, x * partWidth, z * partHeight, 1f/d, 1f/d, x*1f/d, z*1f/d, config.scale);
                    modelBuilder.part(node.id, mesh, GL20.GL_TRIANGLES, material);
                }
        }

        return new ModelInstance(modelBuilder.end());
    }

    private HeightSampler getHeightSampler() {
        if (heightSampler == null) {
            heightSampler = config.heightSampler;
            if (heightSampler == null) {
                heightSampler = new DefaultHeightSampler();
            }
        }
        return heightSampler;
    }

    public float getHeightAt(float x, float z) {
        return getHeightSampler().getHeight(x / config.scale, z / config.scale);
    }

    private Mesh createMesh(int width, int height, int offsetX, int offsetZ, float scaleU, float scaleV, float offsetU, float offsetV, float scale) {
        getHeightSampler();
        heightSampler.init(this);
        // position + normal + tex coordinates
        float[] vertices = new float[(width+1) * (height+1) * 8];
        int index = 0;
        for (int y = 0; y < height+1; y++) {
            for (int x = 0; x < width+1; x++) {
                float v = heightSampler.getHeight(x + offsetX, y + offsetZ);
                vertices[index++] = x * scale;
                vertices[index++] = v;
                vertices[index++] = y * scale;
                vertices[index++] = 0;
                vertices[index++] = 1f;
                vertices[index++] = 0;
                vertices[index++] = (float)x / width * scaleU + offsetU;
                vertices[index++] = (float)y / height * scaleV + offsetV;
            }
        }

        // update normals (normal = Vec3(2*(R-L), 2*(B-T), -4).Normalize())
        index = 0;
        float ph;
        Vector3 p = new Vector3();
        for (int y = 0; y < height+1; y++) {
            for (int x = 0; x < width+1; x++) {
                // this vertex
                ph = vertices[index+1];
                // the vertex to the right
                float hr = x < width ? vertices[index+9] : ph;
                // the vertex above
                float ha = y > 0 ? vertices[index-8*(width+1)+1] : ph;
                // the vertex to the left
                float hl = x > 0 ? vertices[index-7] : ph;
                // the vertex below
                float hb = y < height ? vertices[index+8*(width+1)+1] : ph;
                p.set(2 * (hr-hl), 4, 2 * (hb-ha)).nor();
                index += 3;
                vertices[index++] = p.x;
                vertices[index++] = p.y;
                vertices[index++] = p.z;
                index += 2;
            }
        }

        // we create width x height vertices forming width-1 x height-1 quads or 2 x (width-1) x (height-1) triangles
        int triangles = 2 * (width) * (height);
        short[] indices = new short[triangles * 3];
        index = 0;
        short yOffset = (short)(width+1);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
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
                VertexAttribute.Position(), VertexAttribute.Normal(), VertexAttribute.TexCoords(0));
        mesh.setVertices(vertices);
        mesh.setIndices(indices);
        return mesh;
    }
}
