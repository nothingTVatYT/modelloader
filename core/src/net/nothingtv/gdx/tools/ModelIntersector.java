package net.nothingtv.gdx.tools;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.math.GeometryUtils;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is used to find the intersection between a ray and a ModelInstance. Also handles multiple meshes.
 *
 * If it is not working as expected, double check your models to make sure transforms are applied properly (Ctrl+A in Blender).
 * Position/rotation/scales should be zero'd out in Blender before exporting.
 *
 * @author JamesTKhan
 * @version September 05, 2023
 */
public class ModelIntersector {

    private static final Vector3 v1 = new Vector3();
    private static final Vector3 v2 = new Vector3();
    private static final Vector3 v3 = new Vector3();
    private static final Matrix4 m1 = new Matrix4();

    private static final Vector3 closestV1 = new Vector3();
    private static final Vector3 closestV2 = new Vector3();
    private static final Vector3 closestV3 = new Vector3();

    private static final Vector3 closestIntersection = new Vector3();

    private static final Vector3 best = new Vector3();
    private static final Vector3 tmp = new Vector3();

    private static final Vector2 tmpVec2_1 = new Vector2();
    private static final Vector2 tmpVec2_2 = new Vector2();
    private static final Vector2 tmpVec2_3 = new Vector2();
    private static final Vector2 tmpVec2_4 = new Vector2();
    private static final Vector3 normal = new Vector3();
    private static final Ray localRay = new Ray();

    // The distance threshold to use when checking for intersections, to avoid floating point precision errors
    public static float distanceThreshold = MathUtils.FLOAT_ROUNDING_ERROR;

    // Map to cache bounding boxes for ModelInstances and Meshes
    private static final Map<ModelInstance, BoundingBox> modelBoundsCache = new HashMap<>();
    private static final Map<Mesh, BoundingBox> meshBoundsCache = new HashMap<>();

    // Tracks the closest intersections during checks
    private static float closestDistance = Float.MAX_VALUE;
    private static Node closestNode = null;
    private static NodePart closestNodePart = null;

    /**
     * The result of an intersection.
     */
    public static class IntersectionResult {
        public Node node;
        // The node part that was hit, Mesh and Material can be obtained from here
        public NodePart nodePart;
        // The intersection point in model coordinates
        public Vector3 intersection = new Vector3();
        // The triangle that was hit
        public Triangle triangle;
    }

    /**
     * A triangle in model coordinates.
     * Contains Vertex positions and UV coordinates.
     */
    public static class Triangle {
        public Vector3 v1 = new Vector3();
        public Vector3 v2 = new Vector3();
        public Vector3 v3 = new Vector3();
        public Vector2 uv1 = new Vector2();
        public Vector2 uv2 = new Vector2();
        public Vector2 uv3 = new Vector2();

        @Override
        public String toString() {
            return "Triangle{" +
                    "v1=" + v1 +
                    ", v2=" + v2 +
                    ", v3=" + v3 +
                    ", uv1=" + uv1 +
                    ", uv2=" + uv2 +
                    ", uv3=" + uv3 +
                    '}';
        }
    }

    private ModelIntersector() {
    }

    /**
     * Returns the closest intersection between the ray and the model instances.
     *
     * @param ray            The ray to check against the model instance
     * @param modelInstances The model instances to check against
     * @return The intersection result, or null if no intersection
     */
    public static IntersectionResult intersect(Ray ray, Array<ModelInstance> modelInstances) {
        IntersectionResult closestIntersection = null;
        float closestDistance = Float.MAX_VALUE;

        for (ModelInstance modelInstance : modelInstances) {
            IntersectionResult intersectionResult = intersect(ray, modelInstance);
            if (intersectionResult != null) {
                float distance = intersectionResult.intersection.dst2(ray.origin);

                if (distance < closestDistance && distance > distanceThreshold) {
                    closestIntersection = intersectionResult;
                    closestDistance = distance;
                }
            }
        }

        return closestIntersection;
    }

    /**
     * Returns the intersection between the ray and the model instance.
     *
     * @param ray           The ray to check against the model instance
     * @param modelInstance The model instance to check against
     * @return The intersection result, or null if no intersection
     */
    public static IntersectionResult intersect(Ray ray, ModelInstance modelInstance) {
        // Check if the bounds are cached for this model instance
        BoundingBox modelBounds = modelBoundsCache.get(modelInstance);

        // If not cached, calculate and cache the bounds
        if (modelBounds == null) {
            modelBounds = new BoundingBox();
            modelInstance.calculateBoundingBox(modelBounds);
            modelBoundsCache.put(modelInstance, modelBounds);
        }

        return intersect(ray, modelInstance, modelBounds);
    }

    /**
     * Returns the intersection between the ray and the model instance. Prefer this method with a cached
     * bounding box if used repeatedly.
     *
     * @param ray              The ray to check against the model instance
     * @param modelInstance    The model instance to check against
     * @param modelInstanceBox The bounding box of the model instance
     * @return The intersection result, or null if no intersection
     */
    public static IntersectionResult intersect(Ray ray, ModelInstance modelInstance, BoundingBox modelInstanceBox) {
        setLocalRay(ray, modelInstance);

        // Fast Check against bounding box
        if (!Intersector.intersectRayBounds(localRay, modelInstanceBox, null)) {
            return null;
        }

        return getClosestIntersection(localRay, modelInstance);
    }

    // Transform ray to model's local coordinates.
    private static void setLocalRay(Ray ray, ModelInstance modelInstance) {
        Matrix4 invTransform = m1.set(modelInstance.transform).inv();
        localRay.set(ray).mul(invTransform);
    }

    private static void setNodeLocalRay(Ray ray, Node node) {
        Matrix4 invTransform = m1.set(node.globalTransform).inv();
        localRay.set(ray).mul(invTransform);
    }

    private static IntersectionResult getClosestIntersection(Ray localRay, ModelInstance modelInstance) {
        // Reset for new intersection check
        closestDistance = Float.MAX_VALUE;
        closestNode = null;
        closestNodePart = null;

        IntersectionResult result = new IntersectionResult();
        intersectNodes(localRay, modelInstance.nodes, result);

        if (closestDistance < Float.MAX_VALUE) {
            // Transform intersection back to model's world coordinates.
            result.intersection.set(closestIntersection).mul(modelInstance.transform);
            result.nodePart = closestNodePart;
            result.node = closestNode;
            return result;
        } else {
            return null;
        }

    }

    private static void intersectNodes(Ray localRay, Iterable<Node> nodes, IntersectionResult result) {
        for (Node node : nodes) {
            for (NodePart nodePart : node.parts) {
                if (!nodePart.enabled) continue;

                Mesh mesh = nodePart.meshPart.mesh;

                // Check if the bounds are cached for this mesh
                BoundingBox bounds = meshBoundsCache.get(mesh);

                // If not cached, calculate and cache the bounds
                if (bounds == null) {
                    bounds = new BoundingBox();
                    mesh.calculateBoundingBox(bounds, nodePart.meshPart.offset, nodePart.meshPart.size);
                    meshBoundsCache.put(mesh, bounds);
                }

                // Fast Check against bounding box
                if (!Intersector.intersectRayBounds(localRay, bounds, null)) {
                    continue;
                }

                if (intersectRayMesh(localRay, mesh, result)) {
                    float distance = localRay.origin.dst(result.intersection);
                    if (distance < closestDistance && distance > distanceThreshold) {
                        closestDistance = distance;
                        closestIntersection.set(result.intersection);
                        closestNodePart = nodePart;
                        closestNode = node;
                    }
                }

            }

            // Check children recursively
            if (node.hasChildren()) {
                intersectNodes(localRay, node.getChildren(), result);
            }
        }

    }


    /**
     * Intersects the given ray with the mesh. Returns true if intersection occurs and stores the intersection data
     * in the given IntersectionResult. Iterates through all triangles so faster bounding box checks should be done
     * before calling this method.
     * <p>
     * Modified from Intersector.intersectRayTriangles to take in a Mesh and check the position offset
     *
     * @return true if intersection occurs.
     */
    public static boolean intersectRayMesh(Ray ray, Mesh mesh, IntersectionResult intersection) {
        float min_dist = Float.MAX_VALUE;
        boolean hit = false;

//        float[] vertices = new float[mesh.getNumVertices() * vertexSize];
//        mesh.getVertices(vertices);
//        short[] indices = new short[mesh.getNumIndices()];
//        mesh.getIndices(indices);

        FloatBuffer verticesBuffer = mesh.getVerticesBuffer(false);
        ShortBuffer indicesBuffer = mesh.getIndicesBuffer(false);

        if (indicesBuffer.limit() % 3 != 0) throw new GdxRuntimeException("triangle list size is not a multiple of 3");

        // Need offset for position vertices
        VertexAttribute pos = mesh.getVertexAttribute(VertexAttributes.Usage.Position);
        VertexAttribute uv = mesh.getVertexAttribute(VertexAttributes.Usage.TextureCoordinates);

        int vertexSize = mesh.getVertexSize() / 4;
        int posOffset = pos.offset / 4;
        int maxIndex = verticesBuffer.limit() / vertexSize;

        for (int i = 0; i < indicesBuffer.limit(); i += 3) {
            int index1 = indicesBuffer.get(i);
            int index2 = indicesBuffer.get(i + 1);
            int index3 = indicesBuffer.get(i + 2);

            // Ensure indices are within bounds
            if (index1 < 0 || index1 >= maxIndex ||
                    index2 < 0 || index2 >= maxIndex ||
                    index3 < 0 || index3 >= maxIndex) {
                // Handle the out-of-bounds indices here, this could happen on a bad model, like non-manifold vertices
                Gdx.app.debug("ModelIntersector", "Check model for bad / non-manifold vertices, skipping triangles.");
                break;
            }

            int i1 = index1 * vertexSize + posOffset;
            int i2 = index2 * vertexSize + posOffset;
            int i3 = index3 * vertexSize + posOffset;

            v1.set(verticesBuffer.get(i1), verticesBuffer.get(i1 + 1), verticesBuffer.get(i1 + 2));
            v2.set(verticesBuffer.get(i2), verticesBuffer.get(i2 + 1), verticesBuffer.get(i2 + 2));
            v3.set(verticesBuffer.get(i3), verticesBuffer.get(i3 + 1), verticesBuffer.get(i3 + 2));

            // Perform ray intersection on the triangle
            boolean result = Intersector.intersectRayTriangle(ray, v1, v2, v3, tmp);

            if (result) {
                float dist = ray.origin.dst2(tmp);
                if (dist < min_dist) {
                    min_dist = dist;
                    best.set(tmp);
                    hit = true;

                    closestV1.set(v1);
                    closestV2.set(v2);
                    closestV3.set(v3);

                    if (uv == null) continue;
                    int uvOffset = uv.offset / 4;
//                    i1 = indices[i] * vertexSize + uvOffset;
//                    i2 = indices[i + 1] * vertexSize + uvOffset;
//                    i3 = indices[i + 2] * vertexSize + uvOffset;
//                    tmpVec2_1.set(vertices[i1], vertices[i1 + 1]);
//                    tmpVec2_2.set(vertices[i2], vertices[i2 + 1]);
//                    tmpVec2_3.set(vertices[i3], vertices[i3 + 1]);

                    i1 = indicesBuffer.get(i) * vertexSize + uvOffset;
                    i2 = indicesBuffer.get(i + 1) * vertexSize + uvOffset;
                    i3 = indicesBuffer.get(i + 2) * vertexSize + uvOffset;
                    tmpVec2_1.set(verticesBuffer.get(i1), verticesBuffer.get(i1 + 1));
                    tmpVec2_2.set(verticesBuffer.get(i2), verticesBuffer.get(i2 + 1));
                    tmpVec2_3.set(verticesBuffer.get(i3), verticesBuffer.get(i3 + 1));
                }
            }
        }

        if (!hit)
            return false;
        else {
            if (intersection != null) {
                Triangle triangle = new Triangle();
                triangle.v1.set(closestV1);
                triangle.v2.set(closestV2);
                triangle.v3.set(closestV3);

                if (uv != null) {
                    triangle.uv1.set(tmpVec2_1);
                    triangle.uv2.set(tmpVec2_2);
                    triangle.uv3.set(tmpVec2_3);
                }

                intersection.intersection.set(best);
                intersection.triangle = triangle;
            }
            return true;
        }
    }

    /**
     * Calculate barycentric coordinates for an intersection point.
     * Useful for calculating UV coordinates. See {@link #calculateUVCoordinates(IntersectionResult, Vector2, Vector2)}
     *
     * @param result The intersection result
     * @return The barycentric coordinates
     */
    public static Vector2 calculateBarycentricCoordinates(IntersectionResult result, Vector2 out) {
        Triangle triangle = result.triangle;
        Vector3 intersection = result.intersection;

        normal.set(triangle.v2).sub(triangle.v1).crs(new Vector3().set(triangle.v3).sub(triangle.v1)).nor();
        normal.x = Math.abs(normal.x);
        normal.y = Math.abs(normal.y);
        normal.z = Math.abs(normal.z);

        Vector2 a = tmpVec2_1;
        Vector2 b = tmpVec2_2;
        Vector2 c = tmpVec2_3;
        Vector2 intersection2D = tmpVec2_4;

        // Find dominant axis so that we can use the other two for the 2D barycentric calculation
        if (normal.x > normal.y && normal.x > normal.z) {
            // Dominant in X axis
            // Use Y and Z for the 2D barycentric
            a.set(triangle.v1.y, triangle.v1.z);
            b.set(triangle.v2.y, triangle.v2.z);
            c.set(triangle.v3.y, triangle.v3.z);
            intersection2D.set(intersection.y, intersection.z);
        } else if (normal.y > normal.x && normal.y > normal.z) {
            // Dominant in Y axis
            // Use X and Z for the 2D barycentric
            a.set(triangle.v1.x, triangle.v1.z);
            b.set(triangle.v2.x, triangle.v2.z);
            c.set(triangle.v3.x, triangle.v3.z);
            intersection2D.set(intersection.x, intersection.z);
        } else {
            // Dominant in Z axis
            // Use X and Y for the 2D barycentric
            a.set(triangle.v1.x, triangle.v1.y);
            b.set(triangle.v2.x, triangle.v2.y);
            c.set(triangle.v3.x, triangle.v3.y);
            intersection2D.set(intersection.x, intersection.y);
        }

        // Calc barycentric coords
        GeometryUtils.toBarycoord(intersection2D, a, b, c, out);
        return out;
    }

    /**
     * Calculate UV coordinates for an IntersectionResult using barycentric coordinates.
     *
     * @param intersectionResult The intersection result
     * @param barycentric        The barycentric coordinates
     * @return The UV coordinates
     */
    public static Vector2 calculateUVCoordinates(IntersectionResult intersectionResult, Vector2 barycentric, Vector2 out) {
        Triangle tri = intersectionResult.triangle;

        // Calc UV coords
        float u = 1f - barycentric.x - barycentric.y;
        float intersectU = u * tri.uv1.x + barycentric.x * tri.uv2.x + barycentric.y * tri.uv3.x;
        float intersectV = u * tri.uv1.y + barycentric.x * tri.uv2.y + barycentric.y * tri.uv3.y;

        return out.set(intersectU, intersectV);
    }

    /**
     * Clears the bounding box caches. You can call this if you changed out the model instances / meshes in your scene
     * to clean up the cache and prevent it from growing too large with obsolete entries.
     */
    public static void clearCache() {
        modelBoundsCache.clear();
        meshBoundsCache.clear();
    }

}
