package net.nothingtv.gdx.tools;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import net.nothingtv.gdx.terrain.Terrain;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.ConcurrentHashMap;

public class JMapVisualizer extends JFrame {

    private static JMapVisualizer instance;

    public static JMapVisualizer getInstance() {
        if (instance == null) {
            instance = new JMapVisualizer();
            instance.setVisible(true);
        }
        return instance;
    }

    static class MapObject {
        BoundingBox boundingBox;
        int flag;

        public MapObject(BoundingBox boundingBox, int flag) {
            this.boundingBox = boundingBox;
            this.flag = flag;
        }
    }

    static class MapViewPane extends JPanel {
        private ConcurrentHashMap<MapObject, MapObject> objects;
        private int cursorX, cursorY;
        int gridSize = 32;

        private int toMapX(float x) {
            return 512 + (int)(x / 4);
        }

        private int toMapY(float z) {
            return 512 + (int)(z / 4);
        }

        public void setCursorPos(float x, float z) {
            cursorX = toMapX(x);
            cursorY = toMapY(z);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(1024, 1024);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int w = getWidth();
            g.setColor(Color.yellow);
            for (int y = 0; y < getHeight(); y+=gridSize)
                g.drawLine(0, y, w, y);
            for (int x = 0; x < getWidth(); x+=gridSize)
                g.drawLine(x, 0, x, getHeight());
            for (MapObject m : objects.values()) {
                g.setColor(m.flag == 0 ? Color.LIGHT_GRAY : m.flag == 1 ? Color.gray : m.flag == 2 ? Color.darkGray : Color.pink);
                int mx = toMapX(m.boundingBox.min.x);
                int my = toMapY(m.boundingBox.min.z);
                int ox = toMapX(m.boundingBox.max.x);
                int oy = toMapY(m.boundingBox.max.z);
                g.fill3DRect(Math.min(mx, ox), Math.min(my, oy), Math.abs(ox-mx), Math.abs(oy-my), true);
            }
            g.setColor(Color.black);
            g.drawOval(cursorX-2, cursorY-2, 4, 4);
        }
    }

    static class KeyTester extends JPanel {
        JSlider slX, slZ;
        JLabel lKey;
        JLabel lX, lZ;
        Terrain terrain;

        public KeyTester() {
            setLayout(new FlowLayout(FlowLayout.LEADING));
            slX = new JSlider(-100, 100, 0);
            slZ = new JSlider(-100, 100, 0);
            lKey = new JLabel("000000000000");
            lX = new JLabel("000000");
            lZ = new JLabel("000000");
            add(slX);
            add(slZ);
            add(lX);
            add(lZ);
            add(lKey);
            slX.addChangeListener((e) -> updateValue());
            slZ.addChangeListener((e) -> updateValue());
        }

        void updateValue() {
            float x = slX.getValue() * 10f;
            float z = slZ.getValue() * 10f;
            lX.setText(String.format("%3.1f", x));
            lZ.setText(String.format("%3.1f", z));
            if (terrain != null)
                lKey.setText(String.format("%x", terrain.chunkKey(x, z)));
        }
    }
    private final ConcurrentHashMap<MapObject, MapObject> boxes = new ConcurrentHashMap<>();
    private final MapViewPane viewPane;
    private final KeyTester keyTester;
    private final JLabel statusLabel;

    private JMapVisualizer() {
        setTitle("Map Debugger");
        setLayout(new BorderLayout());
        viewPane = new MapViewPane();
        viewPane.objects = boxes;
        keyTester = new KeyTester();
        statusLabel = new JLabel(" ");
        add(keyTester, BorderLayout.NORTH);
        add(new JScrollPane(viewPane));
        add(statusLabel, BorderLayout.SOUTH);
        pack();
        viewPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                float wx = (e.getPoint().x - 512) * 4f;
                float wz = (e.getPoint().y - 512) * 4f;
                keyTester.lX.setText(String.format("%4.1f", wx));
                keyTester.lZ.setText(String.format("%4.1f", wz));
                for (MapObject mo : boxes.values()) {
                    if (mo.boundingBox.min.x <= wx && mo.boundingBox.max.x >= wx && mo.boundingBox.min.z <= wz && mo.boundingBox.max.z >= wz) {
                        statusLabel.setText(mo.boundingBox.toString());
                        break;
                    }
                }
                keyTester.terrain.ensureChunkLoaded(new Vector3(wx, 0, wz), false);
            }
        });
        setMinimumSize(new Dimension(256, 256));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    protected void setTerrainForTester(Terrain terrain) {
        keyTester.terrain = terrain;
    }

    protected void addBox(BoundingBox boundingBox, int flag) {
        MapObject mo = new MapObject(boundingBox, flag);
        boxes.put(mo, mo);
        viewPane.repaint();
    }

    protected void setCursorPos(float x, float z) {
        viewPane.setCursorPos(x, z);
        viewPane.repaint();
    }

    protected void clearMap() {
        boxes.clear();
    }

    public static void add(BoundingBox boundingBox, int flag) {
        getInstance().addBox(boundingBox, flag);
    }

    public static void clear() {
        getInstance().clearMap();
    }

    public static void setCursor(float x, float z) {
        getInstance().setCursorPos(x, z);
    }

    public static void setTerrain(Terrain terrain) {
        getInstance().setTerrainForTester(terrain);
    }
}
