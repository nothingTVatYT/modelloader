package net.nothingtv.gdx.terrain;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Mesh;
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
import com.badlogic.gdx.utils.Pool;

import java.nio.FloatBuffer;
import java.util.HashMap;

public class Foliage implements RenderableProvider {

    public static int RandomizeYRotation = 1;

    public static class FoliageArea {
        public Model model;
        public Vector3 center;
        public float radius;
        public int numberInstances;
        public Terrain terrain;
        public long seed;
        FloatBuffer instanceData;
        boolean sharedInstanceData;
        long flags;
        protected boolean visible;
    }

    public static class FoliageModelData {
        Model model;
        ModelInstance modelInstance;
        Array<FoliageArea> areas = new Array<>();
        int currentMaxInstances;
        FloatBuffer transforms;
    }

    private final HashMap<Model, FoliageModelData> foliagePerModel = new HashMap<>();

    private final Array<FoliageArea> areas = new Array<>();
    private final Array<FoliageArea> visibleAreas = new Array<>();
    private Camera camera;
    private float cameraMaxDist2 = 512*512;
    private final Vector3 lastPosition = new Vector3();
    private final Vector3 lastDirection = new Vector3();

    public void setCamera(Camera camera) {
        this.camera = camera;
        lastPosition.set(camera.position);
        lastDirection.set(camera.direction);
    }

    public void setCameraMaxDist(float dist) {
        cameraMaxDist2 = dist * dist;
    }

    public void add(Model model, Vector3 center, float radius, float density, Terrain terrain, long flags) {
        FoliageArea area = new FoliageArea();
        area.model = model;
        area.center = new Vector3(center);
        area.radius = radius;
        area.numberInstances = Math.round(MathUtils.PI * radius * radius * density);
        area.terrain = terrain;
        area.flags = flags;
        area.seed = (long)Float.floatToIntBits(center.x) ^ (long)Float.floatToIntBits(center.y) << 3 ^ (long)Float.floatToIntBits(center.z) << 6;
        area.sharedInstanceData = area.model.nodes.size == 1;
        area.visible = false;
        areas.add(area);
    }

    @Override
    public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool) {
        boolean needUpdate = false;
        // detect changes
        for (FoliageArea area : areas) {
            if (area.center == null)
                    continue;
            if (area.center.dst2(camera.position) - area.radius > cameraMaxDist2) {
                if (area.visible) {
                    visibleAreas.removeValue(area, true);
                    needUpdate = true;
                    area.visible = false;
                }
                continue;
            }
            if (!area.visible) {
                needUpdate = true;
                visibleAreas.add(area);
                area.visible = true;
            }
        }
        // construct instance data per model
        if (needUpdate) {
            // group by model
            foliagePerModel.values().forEach(v -> v.areas.clear());
            for (FoliageArea area : visibleAreas) {
                FoliageModelData fmd = foliagePerModel.get(area.model);
                if (fmd == null) {
                    fmd = new FoliageModelData();
                    fmd.model = area.model;
                    foliagePerModel.put(fmd.model, fmd);
                }
                fmd.areas.add(area);
            }
            // check model instances
            for (FoliageModelData fmd : foliagePerModel.values()) {
                int currentMaxInstances = 0;
                for (FoliageArea a : fmd.areas)
                    currentMaxInstances += a.numberInstances;
                if (currentMaxInstances == 0)
                    continue;
                if (fmd.modelInstance == null)
                    fmd.modelInstance = new ModelInstance(fmd.model);
                // check instance status and buffer size
                if (currentMaxInstances > fmd.currentMaxInstances) {
                    // disable if we enabled instancing before and we need a bigger buffer
                    if (fmd.currentMaxInstances > 0) {
                        for (Mesh mesh : fmd.model.meshes)
                            mesh.disableInstancedRendering();
                    }
                    for (Mesh mesh : fmd.model.meshes) {
                        mesh.enableInstancedRendering(true, currentMaxInstances,
                                new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 0),
                                new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 1),
                                new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 2),
                                new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 3));
                    }
                    fmd.transforms = BufferUtils.newFloatBuffer(currentMaxInstances * 16);
                    fmd.currentMaxInstances = currentMaxInstances;
                } else {
                    fmd.transforms.position(0);
                    fmd.transforms.limit(fmd.transforms.capacity());
                }
                Matrix4 tmpMatrix = new Matrix4();
                for (FoliageArea area : fmd.areas) {
                    if (area.instanceData == null) {
                        area.instanceData = BufferUtils.newFloatBuffer(area.numberInstances * 16);
                        RandomXS128 prng = new RandomXS128(area.seed);
                        for (int i = 0; i < area.numberInstances; i++) {
                            float t = 2 * MathUtils.PI * prng.nextFloat();
                            float u = prng.nextFloat() + prng.nextFloat();
                            float r = u > 1f ? 2f - u : u;
                            float x = r * MathUtils.cos(t) * area.radius + area.center.x;
                            float z = r * MathUtils.sin(t) * area.radius + area.center.z;
                            float y = area.terrain.getHeightAt(x, z);
                            tmpMatrix.idt();
                            if ((area.flags & RandomizeYRotation) != 0)
                                tmpMatrix.rotate(0, 1, 0, prng.nextFloat(360));
                            tmpMatrix.setTranslation(x, y, z);
                            tmpMatrix.tra();
                            area.instanceData.put(tmpMatrix.getValues());
                        }
                        area.instanceData.flip();
                    } else area.instanceData.position(0);
                    fmd.transforms.put(area.instanceData);
                }
                fmd.transforms.flip();
                for (Mesh mesh : fmd.model.meshes)
                    mesh.setInstanceData(fmd.transforms);
            }
        }
        for (FoliageModelData fmd : foliagePerModel.values()) {
            if (!fmd.areas.isEmpty())
                fmd.modelInstance.getRenderables(renderables, pool);
        }
    }
}
