package net.nothingtv.gdx.tools;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;

public class DebugDraw {

    private final Array<RenderableProvider> renderables = new Array<>();
    private ModelBatch batch = new ModelBatch();
    private Camera camera;
    private Environment environment;

    public DebugDraw(Camera camera, Environment environment) {
        this.camera = camera;
        this.environment = environment;
    }

    public void drawArrow(Vector3 location, Color color) {
        drawArrow(location.x, location.y, location.z, color);
    }

    public void drawArrow(float x, float y, float z, Color color) {
        ModelInstance instance = new ModelInstance(BaseModels.createDownArrow(color));
        instance.transform.setTranslation(x, y, z);
        renderables.add(instance);
    }

    public void drawCoordinates(Vector3 location, Color color) {
        drawCoordinates(location.x, location.y, location.z, color);
    }

    public void drawCoordinates(float x, float y, float z, Color color) {
        ModelInstance instance = new ModelInstance(BaseModels.createCoordinates(color));
        instance.transform.setTranslation(x, y, z);
        renderables.add(instance);
    }

    public void drawBox(BoundingBox boundingBox, Color color) {
        ModelInstance instance = new ModelInstance(BaseModels.createWireBox(boundingBox.getWidth(), boundingBox.getHeight(), boundingBox.getDepth(), BaseMaterials.emit(color)));
        instance.transform.setTranslation(boundingBox.getCenterX(), boundingBox.getCenterY(), boundingBox.getCenterZ());
        renderables.add(instance);
    }

    public void drawTriangle(Vector3 v1, Vector3 v2, Vector3 v3, Color color) {
        renderables.add(new ModelInstance(BaseModels.createTriangle(v1, v2, v3, color)));
    }

    public void reset() {
        renderables.clear();
    }

    public void render() {
        if (renderables.isEmpty())
            return;
        batch.begin(camera);
        batch.render(renderables, environment);
        batch.end();
    }

}
