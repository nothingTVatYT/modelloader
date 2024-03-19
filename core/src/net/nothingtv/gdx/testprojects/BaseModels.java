package net.nothingtv.gdx.testprojects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.ArrowShapeBuilder;
import com.badlogic.gdx.math.Vector3;

public class BaseModels {

    public static Model createSphere(float radius, Material material) {
        return new ModelBuilder().createSphere(radius, radius, radius, 16, 12, material,
                VertexAttributes.Usage.Position|VertexAttributes.Usage.Normal|VertexAttributes.Usage.TextureCoordinates);
    }

    public static Model createDownArrow(Color color) {
        ModelBuilder builder = new ModelBuilder();
        builder.begin();
        Node node = builder.node();
        MeshPartBuilder partBuilder = builder.part("down-arrow", GL20.GL_TRIANGLES,
                VertexAttributes.Usage.Position|VertexAttributes.Usage.Normal|VertexAttributes.Usage.TextureCoordinates, BaseMaterials.emit(color));
        ArrowShapeBuilder.build(partBuilder, 0,1,0, 0,0,0, 0.1f, 0.5f, 8);
        node.translation.set(0, -0.05f, 0);
        return builder.end();
    }

    public static Model createCoordinates(Color color) {
        return createCoordinates(BaseMaterials.emit(color));
    }

    public static Model createCoordinates(Material material) {
        return new ModelBuilder().createXYZCoordinates(1, material,
                VertexAttributes.Usage.Position|VertexAttributes.Usage.Normal|VertexAttributes.Usage.TextureCoordinates);
    }

    public static Model createTriangle(Vector3 v1, Vector3 v2, Vector3 v3, Color color) {
        return createTriangle(v1, v2, v3, BaseMaterials.emit(color));
    }

    public static Model createTriangle(Vector3 v1, Vector3 v2, Vector3 v3, Material material) {
        ModelBuilder builder = new ModelBuilder();
        builder.begin();
        MeshPartBuilder partBuilder = builder.part("triangle", GL20.GL_TRIANGLES,
                VertexAttributes.Usage.Position|VertexAttributes.Usage.Normal|VertexAttributes.Usage.TextureCoordinates, material);
        partBuilder.triangle(v1, v2, v3);
        return builder.end();
    }

    public static Model createWireBox(float width, float height, float depth, Material material) {
        return new ModelBuilder().createBox(width, height, depth, GL20.GL_LINES, material, VertexAttributes.Usage.Position|VertexAttributes.Usage.Normal|VertexAttributes.Usage.TextureCoordinates);
    }

    public static void dumpModel(Model model, String owner) {
        StringBuilder sb = new StringBuilder();
        sb.append("Model ").append(model.toString());
        if (!owner.isEmpty())
            sb.append('(').append(owner).append(')');
        sb.append('\n');
        if (model.meshes != null && !model.meshes.isEmpty()) {
            sb.append("Meshes:\n");
            for (Mesh mesh : model.meshes) {
                sb.append(meshInfo(mesh)).append('\n');
            }
        }
        if (model.meshParts != null && !model.meshParts.isEmpty()) {
            sb.append("Mesh parts:\n");
            for (MeshPart meshPart : model.meshParts) {
                sb.append(meshPartInfo(meshPart)).append('\n');
            }
        }
        if (model.nodes != null && !model.nodes.isEmpty()) {
            sb.append("Nodes:\n");
            for (Node node : model.nodes) {
                sb.append(nodeInfo(node)).append('\n');
                for (NodePart nodePart : node.parts)
                    sb.append(nodePartInfo(nodePart)).append('\n');
            }
        }
        System.out.print(sb);
    }

    private static String nodeInfo(Node node) {
        return String.format("%s id %s parts %s at %s", node.toString(), node.id, node.parts, node.translation);
    }

    private static String nodePartInfo(NodePart nodePart) {
        return String.format("%s part %s material %s", nodePart.toString(), nodePart.meshPart, nodePart.material);
    }

    private static String meshInfo(Mesh mesh) {
        return String.format("%s vertices: %d, max vertices: %d, indices: %d, max indices: %d attributes %s",
                mesh.toString(), mesh.getNumVertices(), mesh.getMaxVertices(), mesh.getNumIndices(), mesh.getMaxIndices(), mesh.getVertexAttributes());
    }

    private static String meshPartInfo(MeshPart meshPart) {
        return String.format("%s mesh %s, offset %d, size %d, center %s", meshPart.toString(), meshPart.mesh, meshPart.offset, meshPart.size, meshPart.center);
    }
}
