package net.nothingtv.gdx.terrain;

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

    public static class FoliageType {
        public Model model;
        public Array<Matrix4> transforms;
        ModelInstance modelInstance;
        FloatBuffer instanceData;

    }

    private final Array<FoliageType> foliageTypes;
    private final Random rnd = new Random();

    public Foliage() {
        foliageTypes = new Array<>();
    }

    public void add(Model model, Array<Vector3> positions, long flags) {
        FoliageType type = new FoliageType();
        type.model = model;
        type.transforms = new Array<>(positions.size);
        for (Vector3 pos : positions) {
            Matrix4 mat = new Matrix4().translate(pos);
            if ((flags & RandomizeYRotation) != 0)
                mat.rotate(0, 1, 0, rnd.nextFloat(360));
            type.transforms.add(mat);
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
                        type.modelInstance.transform.set(transform);
                        type.modelInstance.getRenderables(renderables, pool);
                    }
                }
                break;
            case 2:
                // set up instancing
                for (FoliageType type : foliageTypes) {
                    if (type.modelInstance == null) {
                        type.model.meshes.first().enableInstancedRendering(true, type.transforms.size,
                                new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 0),
                                new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 1),
                                new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 2),
                                new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 3));
                        type.modelInstance = new ModelInstance(type.model);
                        FloatBuffer mats = BufferUtils.newFloatBuffer(type.transforms.size * 16);
                        Matrix4 tmp = new Matrix4();
                        for (Matrix4 transform : type.transforms) {
                            tmp.set(transform);
                            mats.put(tmp.tra().getValues());
                        }
                        mats.flip();
                        type.instanceData = mats;
                        type.model.meshes.first().setInstanceData(mats);
                    }
                    type.modelInstance.getRenderables(renderables, pool);
                }
                break;
        }
    }
}
