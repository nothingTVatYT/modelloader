package net.nothingtv.gdx.objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

public class Graph {
    HashSet<GraphNode> nodeSet = new HashSet<>();
    List<GraphNodeGrid> grids = new ArrayList<>();
    boolean attachToNearestOnly = true;

    public int size() {
        return nodeSet.size();
    }

    public void add(GraphNode node) {
        if (nodeSet.contains(node))
            return;
        nodeSet.add(node);
        for (GraphNodeGrid grid : grids) {
            if (grid.contains(node.x, node.y, node.z)) {
                if (attachToNearestOnly)
                    node.attach(grid.nearest(node.x, node.z));
                else
                    for (GraphNode gridNode : grid.nearest4(node.x, node.z))
                        node.attach(gridNode);
            }
        }
    }

    public void addGrid(float x, float z, float width, float height, float xOffset, float zOffset) {
        int nx = (int)Math.ceil(width / xOffset);
        int nz = (int)Math.ceil(height / zOffset);
        xOffset = width / nx;
        zOffset = height / nz;
        nx++;
        nz++;
        GraphNodeGrid grid = new GraphNodeGrid();
        grid.x = x;
        grid.z = z;
        grid.width = width;
        grid.depth = height;
        grid.xOffset = xOffset;
        grid.zOffset = zOffset;
        grid.ignoreHeight = true;
        grids.add(grid);
        GraphNode[] nodes = new GraphNode[nx * nz];
        for (int j = 0; j < nz; j++) {
            for (int i = 0; i < nx; i++) {
                nodes[j * nx + i] = new GraphNode(x + i*xOffset, z + j*zOffset, GraphNode.NodeType.LandMark);
                if (i > 0)
                    nodes[j * nx + i].attach(nodes[j * nx + i - 1]);
                if (j > 0)
                    nodes[j * nx + i].attach(nodes[(j-1) * nx + i]);
            }
        }
        grid.nodes = nodes;
        grid.update();
        nodeSet.addAll(Arrays.stream(nodes).toList());
    }

    public Stream<GraphNode> getAllNodes() {
        return nodeSet.stream();
    }
}
