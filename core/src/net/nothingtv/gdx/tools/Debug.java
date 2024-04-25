package net.nothingtv.gdx.tools;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.bullet.DebugDrawer;

import java.util.HashMap;

public class Debug {

    enum RequestType { Line, Quad, Box, Transform }
    static class DrawRequest {
        String id;
        RequestType type;
        Vector3[] points;
        Vector3 color;
        Matrix4 transform;
    }

    public static Debug instance;

    private final DebugDrawer debugDrawer;
    private final HashMap<String, DrawRequest> requests = new HashMap<>();

    public Debug(DebugDrawer debugDrawer) {
        this.debugDrawer = debugDrawer;
        instance = this;
    }

    public void drawLine(String id, Vector3 from, Vector3 to, Color color) {
        DrawRequest req = new DrawRequest();
        req.id = id;
        req.type = RequestType.Line;
        req.points = new Vector3[2];
        req.points[0] = new Vector3(from);
        req.points[1] = new Vector3(to);
        req.color = new Vector3(color.r, color.g, color.b);
        requests.put(id, req);
    }

    public void drawArrow(String id, Vector3 to, Color color) {
        DrawRequest req = new DrawRequest();
        req.id = id;
        req.type = RequestType.Line;
        req.points = new Vector3[2];
        req.points[0] = new Vector3(to);
        req.points[1] = new Vector3(to).add(0, 1, 0);
        req.color = new Vector3(color.r, color.g, color.b);
        requests.put(id, req);
    }

    public void drawQuad(String id, Vector3 a, Vector3 b, Vector3 c, Vector3 d, Color color) {
        DrawRequest req = new DrawRequest();
        req.id = id;
        req.type = RequestType.Quad;
        req.points = new Vector3[4];
        req.points[0] = new Vector3(a);
        req.points[1] = new Vector3(b);
        req.points[2] = new Vector3(c);
        req.points[3] = new Vector3(d);
        req.color = new Vector3(color.r, color.g, color.b);
        requests.put(id, req);
    }

    public void drawBoundingBox(String id, BoundingBox boundingBox, Color color) {
        DrawRequest req = new DrawRequest();
        req.id = id;
        req.type = RequestType.Box;
        req.points = new Vector3[2];
        req.points[0] = new Vector3(boundingBox.min);
        req.points[1] = new Vector3(boundingBox.max);
        req.color = new Vector3(color.r, color.g, color.b);
        requests.put(id, req);
    }

    public void drawTransform(String id, Matrix4 transform) {
        DrawRequest req = new DrawRequest();
        req.id = id;
        req.type = RequestType.Transform;
        req.transform = new Matrix4(transform);
        requests.put(id, req);
    }

    public void drawDebugs() {
        for (DrawRequest request : requests.values())
            switch (request.type) {
                case Line -> debugDrawer.drawLine(request.points[0], request.points[1], request.color);
                case Quad -> {
                    debugDrawer.drawLine(request.points[0], request.points[1], request.color);
                    debugDrawer.drawLine(request.points[1], request.points[2], request.color);
                    debugDrawer.drawLine(request.points[2], request.points[3], request.color);
                    debugDrawer.drawLine(request.points[3], request.points[0], request.color);
                }
                case Box -> debugDrawer.drawBox(request.points[0], request.points[1], request.color);
                case Transform -> debugDrawer.drawTransform(request.transform, 1);
            }
    }
}
