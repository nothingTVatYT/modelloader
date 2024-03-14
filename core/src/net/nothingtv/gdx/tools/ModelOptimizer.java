package net.nothingtv.gdx.tools;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class ModelOptimizer {
    private final Array<ModelInstance> models = new Array<>();

    public void add(ModelInstance instance) {
        models.add(instance);
    }

    private Mesh createCombinedmesh() {
        int numberOfMeshes = 0;
        int numberVertices = 0;
        int numberIndices = 0;
        int vertexSize = 0;
        for (ModelInstance i : models) {
            numberOfMeshes += i.model.meshes.size;
            for (Mesh mesh : i.model.meshes) {
                if (vertexSize == 0)
                    vertexSize = mesh.getVertexSize();
                numberVertices += mesh.getNumVertices();
                numberIndices += mesh.getNumIndices();
            }
        }
        // vertexSize is in bytes
        int floatsPerVertex = vertexSize / 4;
        System.out.printf("found %d vertices, %d indices in %d meshes in %d models, vertex size %d, floats per vertex %d.%n",
                numberVertices, numberIndices, numberOfMeshes, models.size, vertexSize, floatsPerVertex);
        float[] vertices = new float[numberVertices * floatsPerVertex];
        short[] indices = new short[numberIndices];
        int vertexIndex = 0;
        int indexIndex = 0;
        int verticesCopied = 0;
        for (ModelInstance i : models) {
            for (Mesh mesh : i.model.meshes) {
                FloatBuffer vBuffer = mesh.getVerticesBuffer(false);
                vBuffer.position(0);
                Vector3 vPos = new Vector3();
                for (int vi = 0; vi < mesh.getNumVertices(); vi++) {
                    // get and transform the position
                    vPos.x = vBuffer.get();
                    vPos.y = vBuffer.get();
                    vPos.z = vBuffer.get();
                    vPos.mul(i.transform);
                    vertices[vertexIndex++] = vPos.x;
                    vertices[vertexIndex++] = vPos.y;
                    vertices[vertexIndex++] = vPos.z;
                    // copy the remaining floats of this vertex
                    for (int j = 0; j < floatsPerVertex - 3; j++) {
                        vertices[vertexIndex++] = vBuffer.get();
                    }
                }
                vBuffer.position(0);

                ShortBuffer indexBuffer = mesh.getIndicesBuffer(false);
                int pos = indexBuffer.position();
                indexBuffer.position(0);
                for (int ix = 0; ix < mesh.getNumIndices(); ix++) {
                    indices[indexIndex++] = (short)(indexBuffer.get() + verticesCopied);
                }
                indexBuffer.position(pos);
                verticesCopied += mesh.getNumVertices();
            }
        }

        System.out.printf("Combined %d meshes into one.%n", numberOfMeshes);

        //VertexAttributes attributes = models.first().model.meshes.first().getVertexAttributes();
        Mesh combinedMesh = new Mesh(true, numberVertices, numberIndices,
                VertexAttribute.Position(), VertexAttribute.Normal(), VertexAttribute.TexCoords(0));
        combinedMesh.setVertices(vertices);
        combinedMesh.setIndices(indices);
        return combinedMesh;
    }

    public Renderable getCombinedRenderable(Environment environment) {
        Renderable renderable = new Renderable();
        Mesh combinedMesh = createCombinedmesh();
        renderable.meshPart.set("combined mesh", combinedMesh, 0, combinedMesh.getNumIndices(), GL20.GL_TRIANGLES);
        renderable.environment = environment;
        renderable.worldTransform.idt();
        renderable.material = models.first().materials.first();
        return renderable;
    }

    public ModelInstance getCombinedModelInstance() {
        Mesh combinedMesh = createCombinedmesh();
        MeshPart meshPart = new MeshPart();
        meshPart.primitiveType = models.first().model.meshParts.first().primitiveType;
        meshPart.mesh = combinedMesh;
        meshPart.offset = 0;
        meshPart.size = combinedMesh.getNumIndices();
        meshPart.id = "combined mesh part";

        Model model = new Model();
        model.meshes.add(combinedMesh);
        model.meshParts.add(meshPart);
        model.materials.addAll(models.first().materials);
        NodePart nodePart = new NodePart(meshPart, models.first().materials.first());
        nodePart.enabled = true;
        Node node = new Node();
        node.parts.add(nodePart);
        node.id = "node1";
        node.rotation.set(new Quaternion());
        node.scale.set(new Vector3(1,1,1));
        node.translation.set(Vector3.Zero);
        node.calculateTransforms(false);
        model.nodes.add(node);
        //model.nodes.first().id = "combined mesh part";
        return new ModelInstance(model);
    }
}
