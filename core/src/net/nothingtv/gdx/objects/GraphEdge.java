package net.nothingtv.gdx.objects;

public class GraphEdge {
    public GraphNode a, b;

    public GraphNode other(GraphNode a) {
        if (this.a.equals(a))
            return b;
        return this.a;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GraphEdge other) {
            return this.a.equals(other.a) && this.b.equals(other.b) || this.a.equals(other.b) && this.b.equals(other.a);
        }
        return false;
    }
}
