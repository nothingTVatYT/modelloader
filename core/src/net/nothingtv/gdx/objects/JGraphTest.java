package net.nothingtv.gdx.objects;

import javax.swing.*;
import java.awt.*;

public class JGraphTest extends JFrame {
    private JGraphViewPanel view;

    public JGraphTest() {
        setTitle("Graph Test");
        setLayout(new BorderLayout());
        view = new JGraphViewPanel();
        add(view);
        pack();
        test();
    }

    public void test() {
        Graph graph = new Graph();
        graph.addGrid(20, 20, 900, 900, 128, 128);
        GraphNode node1 = new GraphNode();
        node1.x = 380;
        node1.z = 320;
        graph.add(node1);
        GraphNode node2 = new GraphNode();
        node2.x = 510;
        node2.z = 310;
        graph.add(node2);
        GraphNode node3 = new GraphNode();
        node3.x = 440;
        node3.z = 450;
        graph.add(node3);
        node1.attach(node2);
        node2.attach(node3);
        view.setGraph(graph);
        view.updateGraph();
    }

    public static void main(String[] args) {
        JGraphTest app = new JGraphTest();
        app.setDefaultCloseOperation(EXIT_ON_CLOSE);
        app.setLocationByPlatform(true);
        app.setVisible(true);
    }
}
