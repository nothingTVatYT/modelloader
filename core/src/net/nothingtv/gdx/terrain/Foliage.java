package net.nothingtv.gdx.terrain;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Pool;
import net.nothingtv.gdx.tools.Async;

import java.nio.FloatBuffer;
import java.util.Random;

public class Foliage implements RenderableProvider, Disposable {

    public static int RandomizeYRotation = 1;
    public int renderMethod = 3;
    public boolean useFrustumCulling = true;

    @Override
    public void dispose() {
        for (FoliageType foliageType : foliageTypes)
            if (foliageType.dataUpdater != null)
                foliageType.dataUpdater.cancel();
    }

    public static class FoliageType {
        public Model model;
        public Array<Matrix4> transforms;
        public Vector3 center;
        public float radius;
        public int numberInstances;
        public Terrain terrain;
        public long seed;
        ModelInstance modelInstance;
        FloatBuffer instanceData;
        QuadTreeTransforms quadTree;
        Array<Array<Matrix4>> subTransforms;
        boolean sharedInstanceData;
        InstanceDataUpdater dataUpdater;
        long flags;
    }

    private final Array<FoliageType> foliageTypes;
    private final Random rnd = new Random();
    private Camera camera;
    private float cameraMinDist2 = 400;
    private float cameraMaxDist2 = 512*512;
    private final Vector3 lastPosition = new Vector3();
    private final Vector3 lastDirection = new Vector3();
    private boolean neverUpdated = true;

    public Foliage() {
        foliageTypes = new Array<>();
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
        lastPosition.set(camera.position);
        lastDirection.set(camera.direction);
    }

    public void setCameraMinDist(float dist) {
        cameraMinDist2 = dist * dist;
    }

    public void setCameraMaxDist(float dist) {
        cameraMaxDist2 = dist * dist;
    }

    public static Array<Vector3> createRandomPositions(Terrain terrain, Random rnd, Vector3 foliageCenter, float foliageRadius, int number) {
        Array<Vector3> positions0 = new Array<>();
        for (int i = 0; i < number; i++) {
            float angle = rnd.nextFloat(2 * (float)Math.PI);
            float radius = foliageRadius * (float)Math.sqrt(rnd.nextFloat());
            Vector3 v = new Vector3(foliageCenter).add((float)Math.cos(angle) * radius, 0, (float)Math.sin(angle) * radius);
            v.y = terrain.getHeightAt(v.x, v.z);
            positions0.add(v);
        }
        return positions0;
    }

    public void add(Model model, Vector3 center, float radius, int numberInstances, Terrain terrain, long flags) {
        FoliageType type = new FoliageType();
        type.model = model;
        type.center = new Vector3(center);
        type.radius = radius;
        type.numberInstances = numberInstances;
        type.terrain = terrain;
        type.flags = flags;
        type.seed = (long)Float.floatToIntBits(center.x) ^ (long)Float.floatToIntBits(center.y) << 3 ^ (long)Float.floatToIntBits(center.z) << 6;
        type.sharedInstanceData = type.model.nodes.size == 1;
        foliageTypes.add(type);
    }

    public void add(Model model, Array<Vector3> positions, long flags) {
        FoliageType type = new FoliageType();
        type.model = model;
        type.transforms = new Array<>(positions.size);
        type.numberInstances = positions.size;
        for (Vector3 pos : positions) {
            Matrix4 mat = new Matrix4().translate(pos);
            if ((flags & RandomizeYRotation) != 0)
                mat.rotate(0, 1, 0, rnd.nextFloat(360));
            if (model.meshes.size > 1 && model.nodes.size > 1) {
                type.subTransforms = new Array<>(model.meshes.size-1);
                for (int i = 1; i < model.meshes.size; i++) {
                    Matrix4 subMat = new Matrix4(model.nodes.get(i).localTransform).mul(mat);
                    type.subTransforms.get(i).add(subMat);
                }
            }
            type.transforms.add(mat.tra());
        }
        type.sharedInstanceData = type.model.nodes.size == 1;
        foliageTypes.add(type);
    }

    @Override
    public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool) {
        switch (renderMethod) {
            case 1:
                // first test: naively just set all renderables
                for (FoliageType type : foliageTypes) {
                    if (type.modelInstance == null) {
                        type.modelInstance = new ModelInstance(type.model);
                    }
                    if (type.transforms != null) {
                        for (Matrix4 transform : type.transforms) {
                            type.modelInstance.transform.set(transform).tra();
                            type.modelInstance.getRenderables(renderables, pool);
                        }
                    }
                }
                break;
            case 2:
                boolean cameraChanged = camera != null && (!camera.position.epsilonEquals(lastPosition, 1e-3f) || !camera.direction.epsilonEquals(lastDirection, 1e-3f));
                // set up instancing
                for (FoliageType type : foliageTypes) {
                    if (type.modelInstance == null) {
                        if (type.model.meshes.size > 1)
                            System.out.printf("Warning: This model for the foliage contains %d meshes and %d nodes.%n", type.model.meshes.size, type.model.nodes.size);
                        type.modelInstance = new ModelInstance(type.model);
                        type.model.meshes.first().enableInstancedRendering(true, type.numberInstances,
                                new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 0),
                                new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 1),
                                new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 2),
                                new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 3));
                        FloatBuffer mats = BufferUtils.newFloatBuffer(type.numberInstances * 16);
                        float minX = Float.MAX_VALUE;
                        float maxX = -Float.MAX_VALUE;
                        float minZ = Float.MAX_VALUE;
                        float maxZ = -Float.MAX_VALUE;
                        // matrix is transposed!
                        for (Matrix4 transform : type.transforms) {
                            minX = Math.min(minX, transform.val[Matrix4.M30]);
                            maxX = Math.max(maxX, transform.val[Matrix4.M30]);
                            minZ = Math.min(minZ, transform.val[Matrix4.M32]);
                            maxZ = Math.max(maxZ, transform.val[Matrix4.M32]);
                            mats.put(transform.getValues());
                        }
                        mats.flip();
                        System.out.printf("Create a quad tree with bounds %f/%f - %f/%f%n", minX, minZ, maxX, maxZ);
                        type.quadTree = new QuadTreeTransforms(minX, minZ, maxX, maxZ);
                        type.quadTree.setCameraMin2Max2(cameraMinDist2, cameraMaxDist2);
                        for (Matrix4 transform : type.transforms)
                            type.quadTree.insert(transform);
                        type.instanceData = mats;
                        type.model.meshes.first().setInstanceData(mats);
                        if (type.model.meshes.size > 1) {
                            if (type.sharedInstanceData) {
                                for (int i = 1; i < type.model.meshes.size; i++) {
                                    type.model.meshes.get(i).enableInstancedRendering(true, type.transforms.size,
                                            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 0),
                                            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 1),
                                            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 2),
                                            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 3));
                                    type.model.meshes.get(i).setInstanceData(mats);
                                }
                            }
                        }
                        if (useFrustumCulling && camera != null) {
                            type.dataUpdater = new InstanceDataUpdater(type, camera);
                            Async.submit(type.dataUpdater);
                        }
                    }

                    if (type.dataUpdater != null && type.dataUpdater.hasNewData && (cameraChanged || neverUpdated)) {
                        type.modelInstance.model.meshes.first().setInstanceData(type.dataUpdater.getBuffer());
                        if (type.model.meshes.size > 1 && type.sharedInstanceData) {
                            for (int i = 1; i < type.model.meshes.size; i++)
                                type.model.meshes.get(i).setInstanceData(type.dataUpdater.getBuffer());
                        }
                        type.dataUpdater.calculateNext(50);
                        neverUpdated = false;
                    }
                    type.modelInstance.getRenderables(renderables, pool);
                }
                if (camera != null) {
                    lastPosition.set(camera.position);
                    lastDirection.set(camera.direction);
                }
                break;
            case 3:
                for (FoliageType type : foliageTypes) {
                    if (type.center == null)
                        continue;
                    if (type.center.dst2(camera.position) - type.radius > cameraMaxDist2)
                        continue;
                    if (type.modelInstance == null) {
                        type.modelInstance = new ModelInstance(type.model);
                        type.model.meshes.first().enableInstancedRendering(true, type.numberInstances,
                                new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 0),
                                new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 1),
                                new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 2),
                                new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 3));
                        FloatBuffer mats = BufferUtils.newFloatBuffer(type.numberInstances * 16);
                        Matrix4 tmpMatrix = new Matrix4();
                        RandomXS128 prng = new RandomXS128(type.seed);
                        for (int i = 0; i < type.numberInstances; i++) {
                            float t = 2 * MathUtils.PI * prng.nextFloat();
                            float u = prng.nextFloat() + prng.nextFloat();
                            float r = u > 1f ? 2f - u : u;
                            float x = r * MathUtils.cos(t) * type.radius + type.center.x;
                            float z = r * MathUtils.sin(t) * type.radius + type.center.z;
                            float y = type.terrain.getHeightAt(x, z);
                            tmpMatrix.idt();
                            if ((type.flags & RandomizeYRotation) != 0)
                                tmpMatrix.rotate(0, 1, 0, prng.nextFloat(360));
                            tmpMatrix.setTranslation(x, y, z);
                            tmpMatrix.tra();
                            mats.put(tmpMatrix.getValues());
                        }
                        mats.flip();
                        type.instanceData = mats;
                        type.model.meshes.first().setInstanceData(mats);
                        if (type.model.meshes.size > 1) {
                            if (type.sharedInstanceData) {
                                for (int i = 1; i < type.model.meshes.size; i++) {
                                    type.model.meshes.get(i).enableInstancedRendering(true, type.numberInstances,
                                            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 0),
                                            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 1),
                                            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 2),
                                            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 3));
                                    type.model.meshes.get(i).setInstanceData(mats);
                                }
                            }
                        }
                    }
                    type.modelInstance.getRenderables(renderables, pool);
                }
                break;
        }
    }

    static class InstanceDataUpdater implements Runnable {

        FoliageType type;
        Camera camera;
        volatile boolean canceled;
        volatile boolean hasNewData;
        FloatBuffer mats;
        long resetAt;
        Array<Matrix4> toBeRendered = new Array<>();

        InstanceDataUpdater(FoliageType type, Camera camera) {
            this.type = type;
            this.camera = camera;
        }

        public synchronized void calculateNext(int milliSeconds) {
            resetAt = System.currentTimeMillis() + milliSeconds;
            hasNewData = false;
            notify();
        }

        public void cancel() {
            canceled = true;
        }

        public FloatBuffer getBuffer() {
            return mats;
        }

        @Override
        public void run() {
            canceled = false;
            hasNewData = false;
            calculateBuffer();
            while (!canceled) {
                if (resetAt > 0) {
                    if (System.currentTimeMillis() >= resetAt) {
                        calculateBuffer();
                        resetAt = 0;
                        continue;
                    }
                    try {
                        Thread.sleep(Math.min(100, resetAt - System.currentTimeMillis()));
                        continue;
                    } catch (InterruptedException e) {
                        continue;
                    }
                }
                synchronized (this) {
                    try {
                        wait(100);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }

        void calculateBuffer() {
            if (type.transforms != null)
                calculateCulling();
        }

        void calculateCulling() {
            mats = type.instanceData;
            mats.rewind();
            mats.limit(type.transforms.size * 16);
            toBeRendered.clear();
            type.quadTree.inFrustum(camera, toBeRendered);
            for (Matrix4 transform : toBeRendered) {
                mats.put(transform.getValues());
            }
            mats.flip();
            hasNewData = true;
        }
    }
}
