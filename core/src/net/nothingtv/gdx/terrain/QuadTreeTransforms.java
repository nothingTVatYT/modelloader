package net.nothingtv.gdx.terrain;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;

// from https://gist.github.com/AbhijeetMajumdar/c7b4f10df1b87f974ef4

class Node {
    float x, y, z;
    Matrix4 matrix;

    // This class expects a transposed matrix
    Node(Matrix4 matrix) {
        this.matrix = matrix;
        this.x = matrix.val[Matrix4.M30];
        this.y = matrix.val[Matrix4.M31];
        this.z = matrix.val[Matrix4.M32];
    }
}

class NodeBoundary {
    public NodeBoundary(float xMin, float yMin, float xMax, float yMax) {
        super();
        /*
         *  Storing two diagonal points
         */
        this.xMin = xMin;
        this.yMin = yMin;
        this.xMax = xMax;
        this.yMax = yMax;
    }

    public boolean inRange(float x, float y) {
        return (x >= this.xMin && x <= this.xMax
                && y >= this.yMin && y <= this.yMax);
    }

    float xMin, yMin, xMax, yMax;
}

public class QuadTreeTransforms {
    final int MAX_CAPACITY = 16;
    int level;
    Array<Node> nodes;
    QuadTreeTransforms northWest = null;
    QuadTreeTransforms northEast = null;
    QuadTreeTransforms southWest = null;
    QuadTreeTransforms southEast = null;
    NodeBoundary nodeBoundary;
    BoundingBox boundingBox;

    public QuadTreeTransforms(float minX, float maxX, float minZ, float maxZ) {
        this(1, new NodeBoundary(minX, minZ, maxX, maxZ));
    }

    QuadTreeTransforms(int level, NodeBoundary nodeBoundary) {
        this.level = level;
        nodes = new Array<>();
        this.nodeBoundary = nodeBoundary;
    }

    public static void inFrustum(QuadTreeTransforms tree, Camera camera, float minDistance2, float maxDistance2, Array<Matrix4> result) {
        if (tree == null)
            return;

        if (tree.boundingBox == null) {
            tree.updateBoundingBox();
        }

        if (!camera.frustum.boundsInFrustum(tree.boundingBox))
            return;

        for (Node node : tree.nodes) {
            if (camera.position.dst2(node.x, node.y, node.z) < maxDistance2 &&
                    (camera.position.dst2(node.x, node.y, node.z) < minDistance2 ||
                    camera.frustum.pointInFrustum(node.x, node.y, node.z)))
                result.add(node.matrix);
        }
        inFrustum(tree.northWest, camera, minDistance2, maxDistance2, result);
        inFrustum(tree.northEast, camera, minDistance2, maxDistance2, result);
        inFrustum(tree.southWest, camera, minDistance2, maxDistance2, result);
        inFrustum(tree.southEast, camera, minDistance2, maxDistance2, result);
    }

    void split() {
        float xOffset = this.nodeBoundary.xMin
                + (this.nodeBoundary.xMax - this.nodeBoundary.xMin) / 2;
        float yOffset = this.nodeBoundary.yMin
                + (this.nodeBoundary.yMax - this.nodeBoundary.yMin) / 2;

        northWest = new QuadTreeTransforms(this.level + 1, new NodeBoundary(
                this.nodeBoundary.xMin, this.nodeBoundary.yMin, xOffset,
                yOffset));
        northEast = new QuadTreeTransforms(this.level + 1, new NodeBoundary(xOffset,
                this.nodeBoundary.yMin, this.nodeBoundary.xMax, yOffset));
        southWest = new QuadTreeTransforms(this.level + 1, new NodeBoundary(
                this.nodeBoundary.xMin, yOffset, xOffset,
                this.nodeBoundary.yMax));
        southEast = new QuadTreeTransforms(this.level + 1, new NodeBoundary(xOffset, yOffset,
                this.nodeBoundary.xMax, this.nodeBoundary.yMax));

    }

    /**
     * Inserts a matrix in the quad tree
     * @param matrix a transposed matrix to be added
     */
    public void insert(Matrix4 matrix) {
        insert(matrix.val[Matrix4.M30], matrix.val[Matrix4.M31], matrix.val[Matrix4.M32], matrix);
    }

    void insert(float x, float y, float z, Matrix4 matrix) {
        if (!this.nodeBoundary.inRange(x, z)) {
            return;
        }

        Node node = new Node(matrix);
        if (nodes.size < MAX_CAPACITY) {
            nodes.add(node);
            return;
        }
        // Exceeded the capacity so split it in FOUR
        if (northWest == null) {
            split();
        }

        // Check coordinates belongs to which partition
        if (this.northWest.nodeBoundary.inRange(x, z))
            this.northWest.insert(x, y, z, matrix);
        else if (this.northEast.nodeBoundary.inRange(x, z))
            this.northEast.insert(x, y, z, matrix);
        else if (this.southWest.nodeBoundary.inRange(x, z))
            this.southWest.insert(x, y, z, matrix);
        else if (this.southEast.nodeBoundary.inRange(x, z))
            this.southEast.insert(x, y, z, matrix);
        else
            System.out.printf("ERROR : Unhandled partition %f %f%n", x, z);
    }

    private void updateBoundingBox() {
        if (boundingBox == null)
            boundingBox = new BoundingBox();
        if (northWest != null) {
            northWest.updateBoundingBox();
            boundingBox.ext(northWest.boundingBox);
        }
        if (northEast != null) {
            northEast.updateBoundingBox();
            boundingBox.ext(northEast.boundingBox);
        }
        if (southWest != null) {
            southWest.updateBoundingBox();
            boundingBox.ext(southWest.boundingBox);
        }
        if (southEast != null) {
            southEast.updateBoundingBox();
            boundingBox.ext(southEast.boundingBox);
        }
        if (!nodes.isEmpty()) {
            for (Node node : nodes) {
                boundingBox.ext(node.x, node.y, node.z);
            }
        }
    }
}
