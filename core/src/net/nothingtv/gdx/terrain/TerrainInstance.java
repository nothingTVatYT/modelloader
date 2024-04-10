package net.nothingtv.gdx.terrain;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.math.collision.BoundingBox;
import net.mgsx.gltf.scene3d.scene.Updatable;
import net.nothingtv.gdx.tools.Debug;

import java.util.HashMap;

public class TerrainInstance extends ModelInstance implements Updatable {

    private final HashMap<Node, BoundingBox> cachedBounds = new HashMap<>();
    public enum DrawMode { NoOptimization, FrustumCulling }
    public int vertices;
    public int visibleNodes;
    public DrawMode drawMode = DrawMode.FrustumCulling;
    public boolean debugBounds = false;
    public float minUpdateTime = 0.01f;
    private float updateTime;

    public TerrainInstance(Model model, Terrain terrain) {
        super(model);
        updateTime = minUpdateTime;
    }

    public void calculateBoundingBoxes() {
        for (Node node : nodes) {
            BoundingBox nodeBoundingBox = cachedBounds.get(node);
            if (nodeBoundingBox == null) {
                nodeBoundingBox = new BoundingBox();
                node.calculateWorldTransform();
                node.calculateBoundingBox(nodeBoundingBox);
                cachedBounds.put(node, nodeBoundingBox);
            }
        }
    }

    public void updateNodesForFrustum(Camera camera, float delta) {
        updateTime += delta;
        if (updateTime < minUpdateTime)
            return;
        updateTime = 0;
        vertices = 0;
        visibleNodes = 0;
        for (Node node : nodes) {
            BoundingBox nodeBoundingBox = cachedBounds.get(node);
            if (nodeBoundingBox == null) {
                nodeBoundingBox = new BoundingBox();
                node.calculateWorldTransform();
                node.calculateBoundingBox(nodeBoundingBox);
                cachedBounds.put(node, nodeBoundingBox);
            }
            boolean inFrustum = camera.frustum.boundsInFrustum(nodeBoundingBox);
            for (NodePart part : node.parts) {
                part.enabled = inFrustum;
                if (inFrustum) {
                    visibleNodes++;
                    vertices += part.meshPart.size;
                    if (debugBounds) {
                        System.out.println(camera.position + " " + camera.direction + " " + nodeBoundingBox);
                        Debug.instance.drawBoundingBox(node.id, nodeBoundingBox, Color.CYAN);
                    }
                }
            }
        }
        debugBounds = false;
    }

    @Override
    public void update(Camera camera, float delta) {
        if (drawMode == DrawMode.FrustumCulling)
            updateNodesForFrustum(camera, delta);
    }
}
