package net.nothingtv.gdx.terrain;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.math.collision.BoundingBox;
import net.mgsx.gltf.scene3d.scene.Updatable;

import java.util.HashMap;

public class TerrainInstance extends ModelInstance implements Updatable {

    private final HashMap<Node, BoundingBox> cachedBounds = new HashMap<>();

    public TerrainInstance(Model model) {
        super(model);
    }

    @Override
    public void update(Camera camera, float delta) {
        for (Node node : nodes) {
            BoundingBox nodeBoundingBox = cachedBounds.get(node);
            if (nodeBoundingBox == null) {
                nodeBoundingBox = new BoundingBox();
                node.calculateBoundingBox(nodeBoundingBox);
                cachedBounds.put(node, nodeBoundingBox);
            }
            boolean inFrustum = camera.frustum.boundsInFrustum(nodeBoundingBox);
            for (NodePart part : node.parts) {
                part.enabled = inFrustum;
            }
        }
    }
}
