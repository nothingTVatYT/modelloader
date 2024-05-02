package net.nothingtv.gdx.objects;

import javax.swing.*;
import java.awt.*;

public class JGraphView extends JPanel {

    private int edgeLength = 1024;
    private Graph graph;

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(edgeLength, edgeLength);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Point p = new Point();
        Point a = new Point();
        Point b = new Point();
        graph.getAllNodes().forEach(node -> {
            nodeToScreen(node.x, node.z, p);
            if (node.type == GraphNode.NodeType.LandMark)
                g.setColor(Color.orange);
            else g.setColor(Color.blue);
            g.drawOval(p.x - 2, p.y - 2, 4, 4);
            for (GraphEdge edge : node.edges) {
                nodeToScreen(edge.a.x, edge.a.z, a);
                nodeToScreen(edge.b.x, edge.b.z, b);
                g.setColor(Color.lightGray);
                g.drawLine(a.x, a.y, b.x, b.y);
            }
        });
    }

    public void updateView() {
        repaint();
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    private void nodeToScreen(float x, float z, Point out) {
        out.x = Math.round(x);
        out.y = Math.round(z);
    }

    public Graph getGraph() {
        return graph;
    }
}
