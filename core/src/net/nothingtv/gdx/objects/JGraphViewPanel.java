package net.nothingtv.gdx.objects;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

public class JGraphViewPanel extends JPanel {

    public JGraphView graphView;
    private JLabel lStatus;

    public JGraphViewPanel() {
        setLayout(new BorderLayout());
        graphView = new JGraphView();
        lStatus = new JLabel(" ");
        add(new JScrollPane(graphView));
        add(lStatus, BorderLayout.SOUTH);
        graphView.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                lStatus.setText(String.format("%d/%d | %d nodes", e.getX(), e.getY(), graphView.getGraph().size()));
            }
        });
    }

    public void setGraph(Graph graph) {
        graphView.setGraph(graph);
    }

    public void updateGraph() {
        graphView.updateView();
    }
}
