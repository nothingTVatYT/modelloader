package net.nothingtv.gdx.terrain;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import net.nothingtv.gdx.tools.Async;
import net.nothingtv.gdx.tools.JMapVisualizer;
import net.nothingtv.gdx.tools.Physics;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Terrain {

    private static final Logger LOG = Logger.getLogger(Terrain.class.getName());

    public static class TerrainChunk implements Disposable {
        public enum ChunkState { Init, Prepared, InPreparation, Visible }
        BoundingBox boundingBox;
        private ChunkState state;
        long lastStateChange;
        FloatBuffer vertexBuffer;
        ShortBuffer indexBuffer;
        btCollisionShape shape;
        btCollisionObject collisionObject;
        Future<btBvhTriangleMeshShape> futureShape;

        public boolean isVisible() {
            synchronized (this) {
                return state == ChunkState.Visible;
            }
        }

        public boolean isPrepared() {
            synchronized (this) {
                return state == ChunkState.Visible || state == ChunkState.Prepared;
            }
        }
        public boolean isPreparing() {
            synchronized (this) {
                return state == ChunkState.InPreparation;
            }
        }

        public void setState(ChunkState newState) {
            synchronized (this) {
                state = newState;
                lastStateChange = System.currentTimeMillis();
            }
        }

        @Override
        public String toString() {
            return "TerrainChunk{" +
                    "boundingBox=" + boundingBox +
                    ", state=" + state +
                    '}';
        }

        @Override
        public void dispose() {
            if (collisionObject != null)
                collisionObject.dispose();
            if (shape != null)
                shape.dispose();
            if (futureShape != null && !futureShape.isDone()) {
                futureShape.cancel(true);
            }
        }
    }

    /**
     * cached height sampler
     */
    private HeightSampler heightSampler;
    public TerrainConfig config;
    private TerrainInstance modelInstance;
    private final ConcurrentHashMap<Long, TerrainChunk> chunks = new ConcurrentHashMap<>();
    private final List<Long> toBeRemoved = new ArrayList<>();
    private final Vector3 tmpPos = new Vector3();

    private float minHeight;
    private float maxHeight;

    public Terrain(TerrainConfig config) {
        this.config = config;
        if (config.heightSampler instanceof DefaultHeightSampler defaultHeightSampler)
            defaultHeightSampler.terrain = this;
    }

    public float getMinHeight() {
        return minHeight;
    }

    public float getMaxHeight() {
        return maxHeight;
    }

    public TerrainInstance createModelInstance() {
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
        Texture splatMap = new Texture(config.splatMap);
        splatMap.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        material.set(TerrainTextureAttribute.createAlpha1(splatMap));

        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        Mesh mesh;
        minHeight = Float.MAX_VALUE;
        maxHeight = -Float.MAX_VALUE;
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

        modelInstance = new TerrainInstance(modelBuilder.end(), this);
        return modelInstance;
    }

    private HeightSampler getHeightSampler() {
        if (heightSampler == null) {
            heightSampler = config.heightSampler;
            if (heightSampler == null) {
                heightSampler = new DefaultHeightSampler();
            }
            heightSampler.init(this);
        }
        return heightSampler;
    }

    /**
     * initialize the physics part of the terrain
     * @param pos a position to load the chunk for
     */
    public void init(Vector3 pos) {
        if (config.chunkEdgeLength == 0) {
            config.chunkEdgeLength = config.width * config.scale / config.terrainDivideFactor;
        }
        ensureChunkLoaded(pos, true);
    }

    public long chunkKey(float x, float z) {
        int cz = (int)Math.floor(z / config.chunkEdgeLength);
        int cx = (int)Math.floor(x / config.chunkEdgeLength);
        return (cz < 0 ? 1000000000000000L : 0L) | (Math.abs(cz) & 0xfffffffL) << 62 | (cx < 0 ? 10000000L : 0L) | (Math.abs(cx) & 0xfffffff);
    }

    private TerrainChunk getChunkAt(Vector3 pos) {
        return chunks.get(chunkKey(pos.x, pos.z));
    }

    public void ensureChunkLoaded(Vector3 pos, boolean waitForIt) {
        TerrainChunk chunk = getChunkAt(pos);
        if (chunk != null && chunk.isVisible())
            return;
        if (chunk == null) {
            chunk = new TerrainChunk();
            float chunkX = config.chunkEdgeLength * (int)Math.floor(pos.x / config.chunkEdgeLength);
            float chunkZ = config.chunkEdgeLength * (int)Math.floor(pos.z / config.chunkEdgeLength);
            Vector3 min = new Vector3(chunkX, pos.y-1, chunkZ);
            Vector3 max = new Vector3(chunkX + config.chunkEdgeLength, pos.y+1, chunkZ + config.chunkEdgeLength);
            chunk.boundingBox = new BoundingBox(min, max);
            chunk.setState(TerrainChunk.ChunkState.Init);
            long key = chunkKey(pos.x, pos.z);
            if (!chunk.boundingBox.contains(pos)) {
                System.err.printf("bounding box %s is wrong, does not contain %s%n", chunk.boundingBox, pos);
            }
            if (chunks.containsKey(key)) {
                System.err.println("duplicate key in terrain chunks");
                throw new GdxRuntimeException("duplicate key in terrain chunks");
            }
            chunks.put(key, chunk);
            JMapVisualizer.add(chunk.boundingBox, chunk.state.ordinal());
        }
        loadChunk(chunk, waitForIt);
    }

    protected void loadChunk(TerrainChunk chunk, boolean waitForIt) {
        if (chunk.isVisible()) return;
        //System.out.printf("loading chunk %s (%d/%d), waiting=%s%n", chunk, (int)(chunk.boundingBox.getCenterX() / config.chunkEdgeLength), (int)(chunk.boundingBox.getCenterZ() / config.chunkEdgeLength), waitForIt);
        if (!chunk.isPrepared()) {
            if (!chunk.isPreparing()) {
                chunk.futureShape = prepareChunkAsync(chunk);
            }
            if (!waitForIt)
                return;
            try {
                chunk.futureShape.get();
            } catch (Exception e) {
                System.err.println("cannot prepare terrain chunk");
                LOG.log(Level.WARNING, "Cannot prepare terrain chunk", e);
            }
        }
        activateChunk(chunk);
    }

    protected void activateChunk(TerrainChunk chunk) {
        if (!chunk.isPrepared()) {
            System.err.println("activate called on chunk that is not prepared");
            LOG.log(Level.WARNING, "Cannot activate a chunk that is not prepared.");
            return;
        }
        //System.out.printf("activate chunk %s%n", chunk);
        Vector3 localInertia = new Vector3();
        chunk.shape.calculateLocalInertia(0, localInertia);
        btRigidBody.btRigidBodyConstructionInfo info = new btRigidBody.btRigidBodyConstructionInfo(0, null, chunk.shape, localInertia);
        btRigidBody rigidBody = new btRigidBody(info);
        rigidBody.userData = chunk;
        if (Physics.currentPhysicsWorld != null)
            Physics.currentPhysicsWorld.addRigidBody(rigidBody);
        info.dispose();
        chunk.collisionObject = rigidBody;
        chunk.setState(TerrainChunk.ChunkState.Visible);
        JMapVisualizer.add(chunk.boundingBox, chunk.state.ordinal());
    }

    protected Future<btBvhTriangleMeshShape> prepareChunkAsync(TerrainChunk chunk) {
        chunk.setState(TerrainChunk.ChunkState.InPreparation);
        JMapVisualizer.add(chunk.boundingBox, chunk.state.ordinal());
        return Async.submit(() -> prepareChunk(chunk));
    }

    protected btBvhTriangleMeshShape prepareChunk(TerrainChunk chunk) {
        chunk.setState(TerrainChunk.ChunkState.InPreparation);
        JMapVisualizer.add(chunk.boundingBox, chunk.state.ordinal());
        // for physics we need the position only
        int vertexSize = 3;
        // how many vertices should be used
        int vy = MathUtils.ceil(config.chunkEdgeLength * config.chunkResolution)+1;
        int vx = MathUtils.ceil(config.chunkEdgeLength * config.chunkResolution)+1;

        FloatBuffer vb = BufferUtils.newFloatBuffer(vx * vy * vertexSize);

        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (int z = 0; z < vy; z++) {
            float worldZ = z * config.chunkResolution + chunk.boundingBox.min.z;
            for (int x = 0; x < vx; x++) {
                float worldX = x * config.chunkResolution + chunk.boundingBox.min.x;
                float worldY = getHeightAt(worldX, worldZ);
                vb.put(worldX);
                vb.put(worldY);
                vb.put(worldZ);
                minY = Math.min(minY, worldY);
                maxY = Math.max(maxY, worldY);
            }
        }
        vb.flip();

        ShortBuffer ib = BufferUtils.newShortBuffer((vx-1) * (vy-1) * 6);
        for (int z = 0; z < vy-1; z++) {
            for (int x = 0; x < vx-1; x++) {
                ib.put((short)(x + vx*z));
                ib.put((short)(x + vx*(z+1)));
                ib.put((short)(x+1 + vx*z));
                ib.put((short)(x+1 + vx*z));
                ib.put((short)(x + vx*(z+1)));
                ib.put((short)(x+1 + vx*(z+1)));
            }
        }
        ib.flip();

        btTriangleIndexVertexArray va = new btTriangleIndexVertexArray();
        btIndexedMesh btMesh = new btIndexedMesh();
        btMesh.set(chunk, vb, vertexSize * 4, vx * vy, 0, ib, 0, ib.limit());
        va.addIndexedMesh(btMesh, PHY_ScalarType.PHY_SHORT);
        btBvhTriangleMeshShape shape = new btBvhTriangleMeshShape(va, true);
        chunk.vertexBuffer = vb;
        chunk.indexBuffer = ib;
        chunk.shape = shape;

        chunk.boundingBox.min.y = minY;
        chunk.boundingBox.max.y = maxY;
        chunk.boundingBox.update();

        chunk.setState(TerrainChunk.ChunkState.Prepared);
        JMapVisualizer.add(chunk.boundingBox, chunk.state.ordinal());
        return shape;
    }

    public float getHeightAt(float x, float z) {
        return getHeightSampler().getHeight(x / config.scale, z / config.scale);
    }

    public void getNormalAt(float x, float z, Vector3 out) {
        float d = 1f;
        float l = getHeightAt(x-d, z) - minHeight;
        float r = getHeightAt(x+d, z) - minHeight;
        float t = getHeightAt(x, z-d) - minHeight;
        float b = getHeightAt(x, z+d) - minHeight;
        out.set(-2 * (r-l), 4, -2 * (b-t)).nor();
    }

    public int getSplatAt(float x, float z) {
        int splatX = Math.round(x / config.width * config.splatMap.getWidth());
        int splatZ = Math.round(z / config.height * config.splatMap.getHeight());
        return config.splatMap.getPixel(splatX, splatZ);
    }

    public void setSplatMap(Pixmap pixmap) {
        Texture alpha = new Texture(pixmap);
        alpha.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        ((TextureAttribute)modelInstance.materials.first().get(TerrainTextureAttribute.Alpha1)).textureDescription.texture = alpha;
    }

    public void update(Vector3 pos) {
        JMapVisualizer.clear();
        for (TerrainChunk chunk : chunks.values()) {
            JMapVisualizer.add(chunk.boundingBox, chunk.state.ordinal());
        }
        // check for neighboring terrain chunks
        TerrainChunk currentChunk = getChunkAt(pos);
        if (currentChunk == null) {
            System.err.printf("Found no current terrain chunk for %s!%n", pos);
            return;
        }
        tmpPos.set(pos).add(0, 0, -config.chunkLoadDistance);
        ensureChunkLoaded(tmpPos, false);
        tmpPos.set(pos).add(0, 0, config.chunkLoadDistance);
        ensureChunkLoaded(tmpPos, false);
        tmpPos.set(pos).add(config.chunkLoadDistance, 0, -config.chunkLoadDistance);
        ensureChunkLoaded(tmpPos, false);
        tmpPos.set(pos).add(config.chunkLoadDistance, 0, 0);
        ensureChunkLoaded(tmpPos, false);
        tmpPos.set(pos).add(config.chunkLoadDistance, 0, config.chunkLoadDistance);
        ensureChunkLoaded(tmpPos, false);
        tmpPos.set(pos).add(-config.chunkLoadDistance, 0, -config.chunkLoadDistance);
        ensureChunkLoaded(tmpPos, false);
        tmpPos.set(pos).add(-config.chunkLoadDistance, 0, 0);
        ensureChunkLoaded(tmpPos, false);
        tmpPos.set(pos).add(-config.chunkLoadDistance, 0, config.chunkLoadDistance);
        ensureChunkLoaded(tmpPos, false);
        checkLoadedChunks(pos);
    }

    protected void unloadChunk(TerrainChunk chunk) {
        if (chunk.isVisible()) {
            Physics.currentPhysicsWorld.removeCollisionObject(chunk.collisionObject);
            chunk.setState(TerrainChunk.ChunkState.Prepared);
            //System.out.printf("Terrain chunk %s removed from physics simulation.%n", chunk);
        }
    }

    public void checkLoadedChunks(Vector3 pos) {
        for (Map.Entry<Long, TerrainChunk> entry : chunks.entrySet()) {
            TerrainChunk chunk = entry.getValue();
            if (!chunk.isVisible()) {
                if (chunk.isPrepared() && System.currentTimeMillis() - chunk.lastStateChange > config.chunkDeletionTime)
                    toBeRemoved.add(entry.getKey());
                continue;
            }
            float dx = Math.abs(chunk.boundingBox.getCenterX() - pos.x);
            float dz = Math.abs(chunk.boundingBox.getCenterZ() - pos.z);
            if (dx + dz > config.chunkUnloadDistance) {
                unloadChunk(chunk);
            }
        }
        toBeRemoved.forEach(c -> chunks.remove(c).dispose());
        toBeRemoved.clear();
    }

    private Mesh createMesh(int width, int height, int offsetX, int offsetZ, float scaleU, float scaleV, float offsetU, float offsetV, float scale) {
        getHeightSampler();

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
                minHeight = Math.min(minHeight, v);
                maxHeight = Math.max(maxHeight, v);
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
        //System.out.printf("created mesh for %dx%d with %d vertices and %d indices%n", width, height, vertices.length/8, indices.length);
        return mesh;
    }
}
