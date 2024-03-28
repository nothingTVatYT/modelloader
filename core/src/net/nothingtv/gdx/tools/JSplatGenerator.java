package net.nothingtv.gdx.tools;

import net.nothingtv.gdx.terrain.TerrainSplatGenerator;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

public class JSplatGenerator extends JFrame {

    private final JSlider slResolution;
    private final JComboBox<Integer> coDefaultLayer;
    private final JComboBox<Integer> coNumberLayers;
    private final JSlider[] slElevationWeight;
    private final JSlider[] slHeightBegin;
    private final JSlider[] slHeightEnd;
    private final JSlider[] slHeightSmoothBegin;
    private final JSlider[] slHeightSmoothEnd;
    private final JSlider[] slSlopeWeight;
    private final JSlider[] slSlopeBegin;
    private final JSlider[] slSlopeEnd;
    private final JSlider[] slSlopeSmoothBegin;
    private final JSlider[] slSlopeSmoothEnd;
    private final ImageIcon[] textureIcon;
    private final JLabel[] textureIconLabel;
    private TerrainSplatGenerator.Configuration config;
    private volatile boolean mapUpdated;

    public JSplatGenerator() {
        BorderLayout frameLayout = new BorderLayout();
        setLayout(frameLayout);
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        JPanel p = new JPanel(layout);
        c.gridwidth = GridBagConstraints.REMAINDER;
        p.add(new JLabel("Terrain Splat Generator"), c);
        c.gridwidth = 1;
        p.add(new JLabel("Resolution"), c);
        slResolution = new JSlider(1, 16, 10);
        slResolution.setMinimumSize(slResolution.getPreferredSize());
        p.add(slResolution, c);
        final JLabel lResolution = new JLabel("        2048");
        slResolution.addChangeListener(e -> lResolution.setText(String.format("%d", 1<<slResolution.getValue())));
        c.gridwidth = GridBagConstraints.REMAINDER;
        p.add(lResolution, c);

        c.gridwidth = 1;
        p.add(new JLabel("Number of Layers"), c);
        coNumberLayers = new JComboBox<>(new Integer[]{1, 2, 3, 4});
        c.gridwidth = GridBagConstraints.REMAINDER;
        p.add(coNumberLayers, c);

        c.gridwidth = 1;
        p.add(new JLabel("Default Layer"), c);
        coDefaultLayer = new JComboBox<>(new Integer[]{0, 1, 2, 3});
        c.gridwidth = GridBagConstraints.REMAINDER;
        p.add(coDefaultLayer, c);

        slElevationWeight = new JSlider[4];
        slHeightBegin = new JSlider[4];
        slHeightEnd = new JSlider[4];
        slHeightSmoothBegin = new JSlider[4];
        slHeightSmoothEnd = new JSlider[4];
        slSlopeWeight = new JSlider[4];
        slSlopeBegin = new JSlider[4];
        slSlopeEnd = new JSlider[4];
        slSlopeSmoothBegin = new JSlider[4];
        slSlopeSmoothEnd = new JSlider[4];
        textureIcon = new ImageIcon[4];
        textureIconLabel = new JLabel[4];

        for (int i = 0; i < 4; i++) {
            c.gridwidth = GridBagConstraints.REMAINDER;
            p.add(new JLabel("Layer #" + i), c);

            c.gridwidth = 1;
            c.gridheight = 10;
            c.weighty = 1;
            textureIconLabel[i] = new JLabel(new ImageIcon("assets/textures/heightmap.png"));
            textureIconLabel[i].setMinimumSize(new Dimension(128, 128));
            textureIconLabel[i].setMaximumSize(textureIconLabel[i].getMinimumSize());
            textureIconLabel[i].setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            p.add(textureIconLabel[i], c);

            c.gridheight = 1;
            p.add(new JLabel("Elevation Weight"), c);
            slElevationWeight[i] = addSliderWithLabel(p);

            p.add(new JLabel("Height Begin"), c);
            slHeightBegin[i] = addSliderWithLabel(p);

            p.add(new JLabel("Height End"), c);
            slHeightEnd[i] = addSliderWithLabel(p);

            p.add(new JLabel("Height Smooth Begin"), c);
            slHeightSmoothBegin[i] = addSliderWithLabel(p);

            p.add(new JLabel("Height Smooth End"), c);
            slHeightSmoothEnd[i] = addSliderWithLabel(p);

            p.add(new JLabel("Slope Weight"), c);
            slSlopeWeight[i] = addSliderWithLabel(p);

            p.add(new JLabel("Slope Begin"), c);
            slSlopeBegin[i] = addSliderWithLabel(p);

            p.add(new JLabel("Slope End"), c);
            slSlopeEnd[i] = addSliderWithLabel(p);

            p.add(new JLabel("Slope Smooth Begin"), c);
            slSlopeSmoothBegin[i] = addSliderWithLabel(p);

            p.add(new JLabel("Slope Smooth End"), c);
            slSlopeSmoothEnd[i] = addSliderWithLabel(p);

        }
        JButton btUpdate = new JButton(new AbstractAction() {
            {
                putValue(NAME, "Update");
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                updateMap();
            }
        });
        c.gridwidth = GridBagConstraints.REMAINDER;
        p.add(btUpdate, c);
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(p);
        pack();
        mapUpdated = false;
    }

    private void updateMap() {
        config.resolution = 1<<slResolution.getValue();
        config.defaultLayer = (Integer)coDefaultLayer.getSelectedItem();
        config.numLayers = (Integer)coNumberLayers.getSelectedItem();
        for (int i = 0; i < 4; i++) {
            TerrainSplatGenerator.LayerConfiguration layer = config.layers[i];
            if (layer == null)
                continue;
            layer.elevationWeight = slElevationWeight[i].getValue() / 1000f;
            layer.heightBegin = slHeightBegin[i].getValue() / 1000f;
            layer.heightEnd = slHeightEnd[i].getValue() / 1000f;
            layer.smoothBegin = slHeightSmoothBegin[i].getValue() / 1000f;
            layer.smoothEnd = slHeightSmoothEnd[i].getValue() / 1000f;
            layer.slopeWeight = slSlopeWeight[i].getValue() / 1000f;
            layer.slopeBegin = slSlopeBegin[i].getValue() / 1000f;
            layer.slopeEnd = slSlopeEnd[i].getValue() / 1000f;
            layer.slopeSmoothBegin = slSlopeSmoothBegin[i].getValue() / 1000f;
            layer.slopeSmoothEnd = slSlopeSmoothEnd[i].getValue() / 1000f;
        }
        mapUpdated = true;
    }

    public boolean isMapUpdateRequested() {
        return mapUpdated;
    }

    public void resetRequest() {
        mapUpdated = false;
    }

    public void linkConfiguration(TerrainSplatGenerator.Configuration configuration) {
        this.config = configuration;
        updateUI();
    }

    public void setTexture(int layer, byte[] data) {
        textureIcon[layer] = new ImageIcon(data);
        textureIconLabel[layer].setIcon(textureIcon[layer]);
    }

    private void updateUI() {
        slResolution.setValue((int)Math.floor(Math.log(config.resolution) / Math.log(2)));
        coDefaultLayer.setSelectedIndex(config.defaultLayer);
        coNumberLayers.setSelectedItem(config.numLayers);
        for (int i = 0; i < 4; i++) {
            TerrainSplatGenerator.LayerConfiguration layer = config.layers[i];
            slElevationWeight[i].setValue(Math.round(layer.elevationWeight * 1000f));
            slHeightBegin[i].setValue(Math.round(layer.heightBegin * 1000f));
            slHeightEnd[i].setValue(Math.round(layer.heightEnd * 1000f));
            slHeightSmoothBegin[i].setValue(Math.round(layer.smoothBegin * 1000f));
            slHeightSmoothEnd[i].setValue(Math.round(layer.smoothEnd * 1000f));
            slSlopeWeight[i].setValue(Math.round(layer.slopeWeight * 1000f));
            slSlopeBegin[i].setValue(Math.round(layer.slopeBegin * 1000f));
            slSlopeEnd[i].setValue(Math.round(layer.slopeEnd * 1000f));
            slSlopeSmoothBegin[i].setValue(Math.round(layer.slopeSmoothBegin * 1000f));
            slSlopeSmoothEnd[i].setValue(Math.round(layer.slopeSmoothEnd * 1000f));
        }
    }

    private JSlider addSliderWithLabel(JPanel p) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = 1;
        JSlider slider = new JSlider(0, 1000, 0);
        slider.setMinimumSize(slider.getPreferredSize());
        JLabel lValue = new JLabel("0.000");
        slider.addChangeListener(e -> lValue.setText(String.format("%1.3f", (float)((JSlider)e.getSource()).getValue() / 1000f)));
        p.add(slider, c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        p.add(lValue, c);
        c.gridwidth = 1;
        return slider;
    }

    public static JSplatGenerator showUI(TerrainSplatGenerator.Configuration config) {
        JSplatGenerator frame = new JSplatGenerator();
        frame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        frame.linkConfiguration(config);
        frame.setVisible(true);
        return frame;
    }

    public static void main(String[] args) {
        JSplatGenerator frame = new JSplatGenerator();
        frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
