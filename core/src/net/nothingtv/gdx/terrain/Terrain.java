package net.nothingtv.gdx.terrain;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Disposable;
import net.nothingtv.gdx.tools.Async;
import net.nothingtv.gdx.tools.Debug;
import net.nothingtv.gdx.tools.Physics;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Terrain implements Disposable {

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

    class TerrainMotionState extends btMotionState {
        @Override
        public void getWorldTransform(Matrix4 worldTrans) {
            worldTrans.set(modelInstance.transform).translate(rigidBodyOffset);
            System.out.printf("getWorldTransform: set translation to %s%n", worldTrans.getTranslation(new Vector3()));
        }

        @Override
        public void setWorldTransform(Matrix4 worldTrans) {
            modelInstance.transform.set(worldTrans);
            System.out.println("set world transform called on the terrain");
        }
    }
    /**
     * cached height sampler
     */
    private HeightSampler heightSampler;
    public TerrainConfig config;
    private TerrainInstance modelInstance;
    private btCollisionShape collisionShape;
    private final Vector3 rigidBodyOffset = new Vector3();
    public btRigidBody rigidBody;
    protected Array<TerrainChunk> chunks = new Array<>();
    private final List<TerrainChunk> toBeRemoved = new ArrayList<>();
    private final Vector3 currentPos = new Vector3();
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
            if (config.erosionIterations > 0) {
                int w = config.width+1;
                int h = config.height+1;
                float[] map = new float[w * h];
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        map[y * w + x] = heightSampler.getHeight(x, y);
                    }
                }
                Erosion erosion = new Erosion();
                erosion.Erode(map, w, config.erosionIterations, false);
                heightSampler = new MapHeightSampler(map, w);
            }
        }
        return heightSampler;
    }

    /**
     * initialize the physics part of the terrain
     * @param pos a position to load the chunk for
     */
    public void init(Vector3 pos) {
        if (config.chunkWidth == 0) {
            config.chunkWidth = config.width * config.scale / config.terrainDivideFactor;
            config.chunkHeight = config.height * config.scale / config.terrainDivideFactor;
        }
        ensureChunkLoaded(pos, true);
    }

    protected void ensureChunkLoaded(Vector3 pos, boolean waitForIt) {
        for (TerrainChunk chunk : chunks) {
            if (chunk.boundingBox.contains(pos)) {
                if (!chunk.isVisible())
                    loadChunk(chunk, true);
                return;
            }
        }
        // create a new one
        TerrainChunk chunk = new TerrainChunk();
        float chunkX = MathUtils.floor(pos.x / config.chunkWidth) * config.chunkWidth;
        float chunkZ = MathUtils.floor(pos.z / config.chunkHeight) * config.chunkHeight;
        Vector3 min = new Vector3(chunkX, 0, chunkZ);
        Vector3 max = new Vector3(chunkX + config.chunkWidth, 1, chunkZ * config.chunkHeight);
        chunk.boundingBox = new BoundingBox(min, max);
        chunk.boundingBox.ext(pos);
        chunk.setState(TerrainChunk.ChunkState.Init);
        chunks.add(chunk);
        loadChunk(chunk, waitForIt);
    }

    protected void loadChunk(TerrainChunk chunk, boolean waitForIt) {
        if (chunk.isVisible()) return;
        if (!chunk.isPrepared()) {
            if (!chunk.isPreparing()) {
                chunk.futureShape = prepareChunkAsync(chunk);
            }
            if (!waitForIt)
                return;
            try {
                chunk.futureShape.get();
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Cannot prepare terrain chunk", e);
            }
        }
        activateChunk(chunk);
    }

    protected void activateChunk(TerrainChunk chunk) {
        if (!chunk.isPrepared()) {
            LOG.log(Level.WARNING, "Cannot activate a chunk that is not prepared.");
            return;
        }
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
    }

    protected Future<btBvhTriangleMeshShape> prepareChunkAsync(TerrainChunk chunk) {
        chunk.setState(TerrainChunk.ChunkState.InPreparation);
        return Async.submit(() -> prepareChunk(chunk));
    }

    protected btBvhTriangleMeshShape prepareChunk(TerrainChunk chunk) {
        chunk.setState(TerrainChunk.ChunkState.InPreparation);
        // for physics we need the position only
        int vertexSize = 3;
        // how many vertices should be used
        int vy = MathUtils.ceil(config.chunkHeight * config.chunkResolution);
        int vx = MathUtils.ceil(config.chunkWidth * config.chunkResolution);

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
        //debugDrawQuad(x, z, d);
        out.set(-2 * (r-l), 4, -2 * (b-t)).nor();
    }

    private void debugDrawQuad(float x, float z, float offset) {
        Vector3 a = new Vector3(x-offset, 0, z);
        Vector3 b = new Vector3(x, 0, z-offset);
        Vector3 c = new Vector3(x+offset, 0, z);
        Vector3 d = new Vector3(x, 0, z+offset);
        ground(a);
        ground(b);
        ground(c);
        ground(d);
        Debug.instance.drawQuad("n plane", a, b, c, d, Color.RED);
    }

    public void ground(Vector3 pos) {
        pos.y = getHeightAt(pos.x, pos.z);
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

    public btCollisionShape createCollisionShape() {
        collisionShape = Bullet.obtainStaticNodeShape(modelInstance.nodes);
        return collisionShape;
    }

    private TerrainChunk getChunkAt(Vector3 pos) {
        for (TerrainChunk chunk : chunks) {
            if (chunk.boundingBox.contains(pos))
                return chunk;
        }
        return null;
    }

    public void update(Vector3 pos) {
        // check for neighboring terrain chunks
        currentPos.set(pos);
        currentPos.y = getHeightAt(currentPos.x, currentPos.z);
        TerrainChunk currentChunk = getChunkAt(currentPos);
        if (currentChunk == null) return;
        float rx = (currentPos.x - currentChunk.boundingBox.min.x) / currentChunk.boundingBox.getWidth();
        float rz = (currentPos.z - currentChunk.boundingBox.min.z) / currentChunk.boundingBox.getDepth();
        if (rx < config.chunkLoadRelDistance) {
            tmpPos.set(currentPos).x -= currentChunk.boundingBox.getWidth();
            ensureChunkLoaded(tmpPos, false);
        }
        if (rx > 1f - config.chunkLoadRelDistance) {
            tmpPos.set(currentPos).x += currentChunk.boundingBox.getWidth();
            ensureChunkLoaded(tmpPos, false);
        }
        if (rz < config.chunkLoadRelDistance) {
            tmpPos.set(currentPos).z -= currentChunk.boundingBox.getHeight();
            ensureChunkLoaded(tmpPos, false);
        }
        if (rz > 1f - config.chunkLoadRelDistance) {
            tmpPos.set(currentPos).z += currentChunk.boundingBox.getHeight();
            ensureChunkLoaded(tmpPos, false);
        }
        checkLoadedChunks(pos);
    }

    protected void unloadChunk(TerrainChunk chunk) {
        if (chunk.isVisible()) {
            Physics.currentPhysicsWorld.removeCollisionObject(chunk.collisionObject);
            chunk.setState(TerrainChunk.ChunkState.Prepared);
        }
    }

    public void checkLoadedChunks(Vector3 pos) {
        for (TerrainChunk chunk : chunks) {
            if (!chunk.isVisible()) {
                if (chunk.isPrepared() && System.currentTimeMillis() - chunk.lastStateChange > config.chunkDeletionTime)
                    toBeRemoved.add(chunk);
                continue;
            }
            float dx = Math.max(chunk.boundingBox.min.x - pos.x, currentPos.x - chunk.boundingBox.max.x) / chunk.boundingBox.getWidth();
            float dz = Math.max(chunk.boundingBox.min.z - pos.z, currentPos.z - chunk.boundingBox.max.z) / chunk.boundingBox.getDepth();
            if (dx > config.chunkUnloadRelDistance && dz > config.chunkUnloadRelDistance) {
                unloadChunk(chunk);
            }
        }
        toBeRemoved.forEach(c -> { chunks.removeValue(c, true); c.dispose(); });
        toBeRemoved.clear();
    }

    public btRigidBody createRigidBody() {
        btMotionState motionState = new TerrainMotionState();
        rigidBody = new btRigidBody(0f, motionState, createCollisionShape());
        rigidBody.userData = modelInstance;
        return rigidBody;
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

    @Override
    public void dispose() {
        collisionShape.dispose();
        rigidBody.dispose();
    }
}
