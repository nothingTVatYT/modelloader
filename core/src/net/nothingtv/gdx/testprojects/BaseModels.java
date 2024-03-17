package net.nothingtv.gdx.testprojects;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;

public class BaseModels {

    public static Model createSphere(float radius, Material material) {
        return new ModelBuilder().createSphere(radius, radius, radius, 16, 12, material, VertexAttributes.Usage.Position|VertexAttributes.Usage.Normal|VertexAttributes.Usage.TextureCoordinates);
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
        return String.format("%s id %s parts %s", node.toString(), node.id, node.parts);
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
