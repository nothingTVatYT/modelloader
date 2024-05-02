package net.nothingtv.gdx.objects;

import java.util.HashSet;

public class GraphNode {
    public enum NodeType { Default, LandMark }
    public float x, y, z;
    public NodeType type = NodeType.Default;

    protected HashSet<GraphEdge> edges = new HashSet<>();

    public GraphNode() {}

    public GraphNode(float x, float z) {
        this.x = x;
        this.y = 0;
        this.z = z;
    }

    public GraphNode(float x, float z, NodeType type) {
        this.x = x;
        this.y = 0;
        this.z = z;
        this.type = type;
    }

    public void attach(GraphNode other) {
        GraphEdge edge = new GraphEdge();
        edge.a = this;
        edge.b = other;
        edges.add(edge);
        other.edges.add(edge);
    }

    public GraphNode anyOtherThan(GraphNode other) {
        for (GraphEdge edge : edges) {
            if (edge.a.equals(other) || edge.b.equals(other))
                continue;
            return edge.other(this);
        }
        return null;
    }

    public float dist2(float x, float z) {
        float dx = this.x - x;
        float dz = this.z - z;
        return dx * dx + dz * dz;
    }
}
