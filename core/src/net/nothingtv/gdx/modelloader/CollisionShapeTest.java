package net.nothingtv.gdx.modelloader;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.physics.bullet.collision.PHY_ScalarType;
import com.badlogic.gdx.physics.bullet.collision.btBvhTriangleMeshShape;
import com.badlogic.gdx.physics.bullet.collision.btIndexedMesh;
import com.badlogic.gdx.physics.bullet.collision.btTriangleIndexVertexArray;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
import com.badlogic.gdx.utils.BufferUtils;
import net.nothingtv.gdx.tools.BaseMaterials;
import net.nothingtv.gdx.tools.OpenSimplex2S;
import net.nothingtv.gdx.tools.SceneObject;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class CollisionShapeTest extends BasicSceneManagerScreen {

    static class TerrainChunk {
        FloatBuffer vertexBuffer;
        ShortBuffer indexBuffer;

        public TerrainChunk(FloatBuffer vertexBuffer, ShortBuffer indexBuffer) {
            this.vertexBuffer = vertexBuffer;
            this.indexBuffer = indexBuffer;
        }
    }

    private TerrainChunk testChunk;

    public CollisionShapeTest(Game game) {
        super(game);
    }

    @Override
    public void initScene() {
        super.initScene();

        // create a test mesh from a bunch of vertices
        float[] vertexArray = new float[600];
        int ii = 0;
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                vertexArray[ii++] = x;
                vertexArray[ii++] = OpenSimplex2S.noise2(1L, x / 10f, y / 10f);
                vertexArray[ii++] = y;
                vertexArray[ii++] = 0;
                vertexArray[ii++] = 1;
                vertexArray[ii++] = 0;
            }
        }
        short[] indexArray = new short[81 * 6];
        ii = 0;
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 9; x++) {
                indexArray[ii++] = (short)(x + 10*y);
                indexArray[ii++] = (short)(x + 10*(y+1));
                indexArray[ii++] = (short)(x+1 + 10*y);
                indexArray[ii++] = (short)(x+1 + 10*y);
                indexArray[ii++] = (short)(x + 10*(y+1));
                indexArray[ii++] = (short)(x+1 + 10*(y+1));
            }
        }

        Mesh mesh = new Mesh(true, 100, 81 * 6, VertexAttribute.Position(), VertexAttribute.Normal());
        mesh.setVertices(vertexArray);
        mesh.setIndices(indexArray);
        ModelBuilder builder = new ModelBuilder();
        builder.begin();
        builder.part("testMesh", mesh, GL20.GL_TRIANGLES, BaseMaterials.colorPBR(Color.CORAL));
        Model model = builder.end();
        SceneObject testi = add("testi", model);

        // create a collision shape from model
        //btBvhTriangleMeshShape shape = btBvhTriangleMeshShape.obtain(model.meshParts);

        // create a collision shape from vertex data
        FloatBuffer vb = BufferUtils.newFloatBuffer(vertexArray.length);
        vb.put(vertexArray);
        vb.flip();
        ShortBuffer sb = BufferUtils.newShortBuffer(indexArray.length);
        sb.put(indexArray);
        sb.flip();

        testChunk = new TerrainChunk(vb, sb);

        btTriangleIndexVertexArray va = new btTriangleIndexVertexArray();
        btIndexedMesh btMesh = new btIndexedMesh();
        btMesh.set(testChunk, testChunk.vertexBuffer, 24, vertexArray.length/6, 0, testChunk.indexBuffer, 0, indexArray.length);
        va.addIndexedMesh(btMesh, PHY_ScalarType.PHY_SHORT);
        btBvhTriangleMeshShape shape = new btBvhTriangleMeshShape(va, true);

        wrapRigidBody(testi, 0, shape);
    }

    @Override
    public void updatePostRender(float delta) {
        super.updatePostRender(delta);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new SelectScreen(game));
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.D) && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
            debugDrawer.setDebugMode(btIDebugDraw.DebugDrawModes.DBG_DrawWireframe);
        }
    }
}
