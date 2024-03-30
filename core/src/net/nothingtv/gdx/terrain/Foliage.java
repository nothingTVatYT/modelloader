package net.nothingtv.gdx.terrain;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Pool;

import java.nio.FloatBuffer;
import java.util.Random;

public class Foliage implements RenderableProvider {

    public static int RandomizeYRotation = 1;
    public int renderMethod = 2;
    public boolean useFrustumCulling = true;

    public static class FoliageType {
        public Model model;
        public Array<Matrix4> transforms;
        ModelInstance modelInstance;
        FloatBuffer instanceData;
        QuadTreeTransforms quadTree;
    }

    private final Array<FoliageType> foliageTypes;
    private final Random rnd = new Random();
    private Camera camera;
    private float cameraMinDist2 = 400;
    private float cameraMaxDist2 = 512*512;
    private final Vector3 lastPosition = new Vector3();
    private final Vector3 lastDirection = new Vector3();
    private long lastCulledFrame;

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

    public void add(Model model, Array<Vector3> positions, long flags) {
        FoliageType type = new FoliageType();
        type.model = model;
        type.transforms = new Array<>(positions.size);
        for (Vector3 pos : positions) {
            Matrix4 mat = new Matrix4().translate(pos);
            if ((flags & RandomizeYRotation) != 0)
                mat.rotate(0, 1, 0, rnd.nextFloat(360));
            type.transforms.add(mat.tra());
        }
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
                    for (Matrix4 transform : type.transforms) {
                        type.modelInstance.transform.set(transform).tra();
                        type.modelInstance.getRenderables(renderables, pool);
                    }
                }
                break;
            case 2:
                // set up instancing
                for (FoliageType type : foliageTypes) {
                    if (type.modelInstance == null) {
                        if (type.model.meshes.size > 1)
                            System.out.printf("Warning: This model for the foliage contains %d meshes.%n", type.model.meshes.size);
                        type.modelInstance = new ModelInstance(type.model);
                        type.model.meshes.first().enableInstancedRendering(true, type.transforms.size,
                                new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 0),
                                new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 1),
                                new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 2),
                                new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 3));
                        FloatBuffer mats = BufferUtils.newFloatBuffer(type.transforms.size * 16);
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
                        type.quadTree = new QuadTreeTransforms(minX, maxX, minZ, maxZ);
                        for (Matrix4 transform : type.transforms)
                            type.quadTree.insert(transform);
                        type.instanceData = mats;
                        type.model.meshes.first().setInstanceData(mats);
                    }

                    // max update 10x per second
                    if (useFrustumCulling && camera != null && Gdx.graphics.getFrameId() > (lastCulledFrame + (Gdx.graphics.getFramesPerSecond() / 10))) {
                        if (!lastPosition.epsilonEquals(camera.position) || !lastDirection.epsilonEquals(camera.direction)) {
                            FloatBuffer mats = type.instanceData;
                            mats.rewind();
                            mats.limit(type.transforms.size * 16);
                            Array<Matrix4> toBeRendered = new Array<>();
                            camera.update(true);
                            //long begin = System.nanoTime();
                            QuadTreeTransforms.inFrustum(type.quadTree, camera, cameraMinDist2, cameraMaxDist2, toBeRendered);
                            //long elapsed = System.nanoTime() - begin;
                            //System.out.printf("query of quad tree took %d ns (%1.5f ms)%n", elapsed, elapsed / 1e6f);
                            for (Matrix4 transform : toBeRendered) {
                                mats.put(transform.getValues());
                            }
                            mats.flip();

                            type.modelInstance.model.meshes.first().setInstanceData(mats);
                            lastPosition.set(camera.position);
                            lastDirection.set(camera.direction);
                            lastCulledFrame = Gdx.graphics.getFrameId();
                        }
                    }
                    type.modelInstance.getRenderables(renderables, pool);
                }
                break;
        }
    }
}
