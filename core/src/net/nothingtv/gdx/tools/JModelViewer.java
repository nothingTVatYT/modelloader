package net.nothingtv.gdx.tools;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.model.Node;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JModelViewer extends JFrame {

    private final JSplitPane splitPane;
    private final JTree tree;
    private final ModelTreeModel model;
    private final PropertiesPanel propertiesPanel;

    public JModelViewer() {
        setLayout(new BorderLayout());
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
        model = new ModelTreeModel();
        tree = new JTree(model);
        tree.setEditable(false);
        tree.setMinimumSize(new Dimension(100, 40));
        tree.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        propertiesPanel = new PropertiesPanel();
        splitPane.setDividerLocation(0.5);
        splitPane.setLeftComponent(new JScrollPane(tree));
        splitPane.setRightComponent(new JScrollPane(propertiesPanel));
        add(splitPane);
        pack();

        tree.addTreeSelectionListener(e -> propertiesPanel.showObject(tree.getSelectionPath()));
        tree.setCellRenderer(new ObjectTreeCellRenderer());
        model.addTreeModelListener(new TreeModelListener() {
            @Override
            public void treeNodesChanged(TreeModelEvent e) {
            }

            @Override
            public void treeNodesInserted(TreeModelEvent e) {
            }

            @Override
            public void treeNodesRemoved(TreeModelEvent e) {
            }

            @Override
            public void treeStructureChanged(TreeModelEvent e) {
                updateTitle();
            }
        });
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                splitPane.setDividerLocation(0.5);
            }
        });
    }

    public void showModel(Object model3d) {
        model.setModel3d(model3d);
        splitPane.setDividerLocation(0.5);
        propertiesPanel.showObject(tree.getPathForRow(0));
    }

    private void updateTitle() {
        String title = "Model Viewer";
        if (model.getRoot() != null)
            title += " " + model.getRoot().toString();
        setTitle(title);
    }

    static class PropertiesPanel extends JPanel {

        JTextArea label;
        PropertiesPanel() {
            setLayout(new BorderLayout());
            label = new JTextArea("no model loaded");
            label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            add(label);
        }

        void showObject(TreePath treePath) {
            if (treePath != null) {
                Object obj = treePath.getLastPathComponent();
                Object toShow;
                if (obj instanceof NamedObject namedObject)
                    toShow = namedObject.value;
                else toShow = obj;
                if (toShow instanceof Mesh mesh) {
                    StringBuilder sb = new StringBuilder();
                    if (mesh.isInstanced())
                        sb.append("instanced ");
                    sb.append("Mesh\n");
                    sb.append(mesh.getNumVertices()).append(" vertices\n");
                    sb.append(mesh.getNumIndices()).append(" indices\n");
                    sb.append(mesh.getVertexSize()).append(" bytes per vertex\n");
                    sb.append(mesh.getVertexAttributes()).append('\n');
                    if (mesh.isInstanced())
                        sb.append(mesh.getInstancedAttributes()).append('\n');
                    label.setText(sb.toString());
                } else
                    label.setText(String.valueOf(toShow));
            }
        }
    }

    static class ModelTreeModel implements TreeModel {

        private Object model3d;
        private final EventListenerList listeners = new EventListenerList();
        private final HashMap<Class<?>, List<Field>> fieldsOfClass = new HashMap<>();
        private final HashMap<Object, List<Object>> cachedChildren = new HashMap<>();

        public void setModel3d(Object model3d) {
            this.model3d = model3d;
            fireTreeStructureChanged();
        }

        @Override
        public Object getRoot() {
            return model3d;
        }

        @Override
        public Object getChild(Object parent, int index) {
            return getChildren(parent).get(index);
        }

        @Override
        public int getChildCount(Object parent) {
            return getChildren(parent).size();
        }

        @Override
        public boolean isLeaf(Object node) {
            return getChildCount(node) == 0;
        }

        @Override
        public void valueForPathChanged(TreePath path, Object newValue) {
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            return getChildren(parent).indexOf(child);
        }

        @Override
        public void addTreeModelListener(TreeModelListener l) {
            listeners.add(TreeModelListener.class, l);
        }

        @Override
        public void removeTreeModelListener(TreeModelListener l) {
            listeners.remove(TreeModelListener.class, l);
        }

        List<Field> getFieldsOfClass(Class<?> clazz) {
            if (!fieldsOfClass.containsKey(clazz)) {
                ArrayList<Field> fieldNames = new ArrayList<>();
                for (Field field : clazz.getFields()) {
                    if (!Modifier.isStatic(field.getModifiers()))
                        fieldNames.add(field);
                }
                fieldsOfClass.put(clazz, fieldNames);
            }
            return fieldsOfClass.get(clazz);
        }

        List<Object> getChildren(Object object) {
            Object obj = object;
            if (object instanceof NamedObject namedObject) {
                obj = namedObject.value;
            }
            if (obj == null)
                return new ArrayList<>();
            if (!cachedChildren.containsKey(obj)) {
                ArrayList<Object> children = new ArrayList<>();
                if (obj instanceof Iterable<?> list) {
                    list.forEach(children::add);
                } else {
                    for (Field field : getFieldsOfClass(obj.getClass())) {
                        try {
                            children.add(new NamedObject(field, field.get(obj)));
                        } catch (IllegalAccessException e) {
                            children.add("<cannot access " + field.getName() + ">");
                        }
                    }
                    if (obj instanceof Node node) {
                        node.getChildren().forEach(children::add);
                    }
                }
                cachedChildren.put(obj, children);
            }
            return cachedChildren.get(obj);
        }

        void fireTreeStructureChanged() {
            TreePath treePath = new TreePath(getRoot());
            TreeModelEvent ev = new TreeModelEvent(this, treePath);
            for (TreeModelListener l : listeners.getListeners(TreeModelListener.class))
                l.treeStructureChanged(ev);
        }
    }

    static class NamedObject {
        Field field;
        Object value;

        public NamedObject(Field field, Object value) {
            this.field = field;
            this.value = value;
        }
    }

    static class ObjectTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            ((JLabel)super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus)).setText(valueOf(value));
            return this;
        }

        String valueOf(Object obj) {
            if (obj instanceof NamedObject namedObject)
                return String.format("%s = %s", namedObject.field.getName(), namedObject.value);
            return obj != null ? String.format("%s: %s", obj.getClass(), obj) : "<null>";
        }
    }

    public static void main(String[] args) {
        JModelViewer v = new JModelViewer();
        v.setDefaultCloseOperation(EXIT_ON_CLOSE);
        v.setLocationByPlatform(true);
        v.setSize(1500, 800);
        v.showModel(v);
        v.setVisible(true);
    }
}
