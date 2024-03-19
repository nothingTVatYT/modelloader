package net.nothingtv.gdx.tools;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;

import java.nio.ShortBuffer;

public class MeshCollider {
    static class Triangle {
        short[] indices = new short[3];
        BoundingBox bounds;
        public Triangle() {}
        public Triangle(short[] indices) {
            this.indices[0] = indices[0];
            this.indices[1] = indices[1];
            this.indices[2] = indices[2];
        }
    }

    static class TriangleRayCastResult extends Octree.RayCastResult<Triangle> {
        public Triangle geometry;
        public float distance;
        public float maxDistanceSq = Float.MAX_VALUE;
    }
    static class TriangleCollider implements Octree.Collider<Triangle> {

        private final Mesh mesh;
        private final float[] vertices;
        private final Vector3 intersection = new Vector3();

        TriangleCollider(Mesh mesh, Matrix4 transform) {
            this.mesh = mesh.copy(true);
            this.mesh.transform(transform);
            vertices = new float[this.mesh.getNumVertices() * this.mesh.getVertexSize() / 4];
            this.mesh.getVertices(vertices);
        }

        @Override
        public boolean intersects(BoundingBox nodeBounds, Triangle geometry) {
            if (geometry.bounds == null) {
                geometry.bounds = new BoundingBox();
                mesh.calculateBoundingBox(geometry.bounds, geometry.indices[0], 1);
                mesh.extendBoundingBox(geometry.bounds, geometry.indices[1], 1);
                mesh.extendBoundingBox(geometry.bounds, geometry.indices[2], 1);
                if (geometry.bounds.getWidth() > 50 || geometry.bounds.getHeight() > 50 || geometry.bounds.getDepth() > 50) {
                    System.out.printf("Suspicious big bounding box: %s%n", geometry.bounds);
                }
            }
            return nodeBounds.intersects(geometry.bounds);
        }

        @Override
        public boolean intersects(Frustum frustum, Triangle geometry) {
            if (geometry.bounds == null) {
                geometry.bounds = new BoundingBox();
                mesh.calculateBoundingBox(geometry.bounds, geometry.indices[0], 1);
                mesh.extendBoundingBox(geometry.bounds, geometry.indices[1], 1);
                mesh.extendBoundingBox(geometry.bounds, geometry.indices[2], 1);
            }
            return frustum.boundsInFrustum(geometry.bounds);
        }

        @Override
        public float intersects(Ray ray, Triangle geometry) {
            if (Intersector.intersectRayTriangles(ray, vertices, geometry.indices, mesh.getVertexSize(), intersection))
                return intersection.dst(ray.origin);
            return Float.MAX_VALUE;
        }
    }

    private final Octree<Triangle> octree;

    public MeshCollider(Mesh mesh, Matrix4 transform) {
        System.out.println("Creating octree");
        BoundingBox meshBounds = mesh.calculateBoundingBox();
        octree = new Octree<>(meshBounds.min, meshBounds.max, 8, 64, new TriangleCollider(mesh, transform));
        int numTriangles = mesh.getNumIndices() / 3;
        ShortBuffer indices = mesh.getIndicesBuffer(false);
        indices.position(0);
        short[] triangleIndices = new short[3];
        for (int i = 0; i < numTriangles; i++) {
            indices.get(triangleIndices);
            octree.add(new Triangle(triangleIndices));
        }
        indices.position(0);
        System.out.println("Created octree");
    }

    public boolean rayIntersects(Ray ray, Vector3 intersection) {
        TriangleRayCastResult result = new TriangleRayCastResult();
        octree.rayCast(ray, result);
        if (result.geometry != null && result.distance < result.maxDistanceSq) {
            ray.getEndPoint(intersection, result.distance);
            return true;
        }
        return false;
    }
}
