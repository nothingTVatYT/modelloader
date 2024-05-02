package net.nothingtv.gdx.objects;

public class GraphNodeGrid {
    public float x, y, z;
    public float width, height, depth;
    public float xOffset, zOffset;
    public boolean ignoreHeight;
    protected GraphNode[] nodes;
    private int nx;

    public boolean contains(float x, float y, float z) {
        if (this.x > x || this.z > z || this.x + width < x || this.z + depth < z)
            return false;
        return ignoreHeight || (!(this.y > y) && !(this.y + height < y));
    }

    public void update() {
        nx = Math.round(width / xOffset + 1);
    }

    public GraphNode[] nearest4(float x, float z) {
        GraphNode[] res = new GraphNode[4];
        int i = (int)Math.floor((x - this.x) / xOffset);
        int j = (int)Math.floor((z - this.z) / zOffset);
        res[0] = nodes[j * nx + i];
        res[1] = nodes[j * nx + i + 1];
        res[2] = nodes[(j+1) * nx + i];
        res[3] = nodes[(j+1) * nx + i + 1];
        return res;
    }

    public GraphNode nearest(float x, float z) {
        GraphNode[] n4 = nearest4(x, z);
        GraphNode nearestNode = null;
        float dist2 = Float.MAX_VALUE;
        for (GraphNode node : n4) {
            float d2 = node.dist2(x, z);
            if (d2 < dist2) {
                dist2 = d2;
                nearestNode = node;
            }
        }
        return nearestNode;
    }
}
