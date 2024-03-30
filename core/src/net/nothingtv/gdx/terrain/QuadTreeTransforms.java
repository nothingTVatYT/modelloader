package net.nothingtv.gdx.terrain;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;

// from https://gist.github.com/AbhijeetMajumdar/c7b4f10df1b87f974ef4


public class QuadTreeTransforms {
    final TransformsBranch root;

    public int transformsPerNode = 16;
    float minDistance2 = 100, maxDistance2 = 512;
    public float minX, minZ, maxX, maxZ;

    public QuadTreeTransforms(float minX, float minZ, float maxX, float maxZ) {
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
        this.root = new TransformsBranch(this, 1, new NodeBoundary(minX, minZ, maxX, maxZ));
    }

    public void setCameraMinMax(float min, float max) {
        minDistance2 = min * min;
        maxDistance2 = max * max;
    }

    public void setCameraMin2Max2(float min2, float max2) {
        minDistance2 = min2;
        maxDistance2 = max2;
    }
    /**
     * Inserts a matrix in the quad tree
     * @param matrix a transposed matrix to be added
     */
    public void insert(Matrix4 matrix) {
        root.insert(matrix.val[Matrix4.M30], matrix.val[Matrix4.M31], matrix.val[Matrix4.M32], matrix);
    }

    public void inFrustum(Camera camera, Array<Matrix4> result) {
        inFrustum(root, camera, result);
    }

    void inFrustum(TransformsBranch branch, Camera camera, Array<Matrix4> result) {
        if (branch == null)
            return;

        if (branch.boundingBox == null) {
            branch.updateBoundingBox();
        }

        if (!camera.frustum.boundsInFrustum(branch.boundingBox))
            return;

        for (Node node : branch.nodes) {
            if (camera.position.dst2(node.x, node.y, node.z) < branch.tree.maxDistance2 &&
                    (camera.position.dst2(node.x, node.y, node.z) < branch.tree.minDistance2 ||
                            camera.frustum.pointInFrustum(node.x, node.y, node.z)))
                result.add(node.matrix);
        }
        inFrustum(branch.northWest, camera, result);
        inFrustum(branch.northEast, camera, result);
        inFrustum(branch.southWest, camera, result);
        inFrustum(branch.southEast, camera, result);
    }

    static class Node {
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

    static class NodeBoundary {
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

    static class TransformsBranch {
    int level;
    Array<Node> nodes;
    TransformsBranch northWest = null;
    TransformsBranch northEast = null;
    TransformsBranch southWest = null;
    TransformsBranch southEast = null;
    NodeBoundary nodeBoundary;
    BoundingBox boundingBox;
    QuadTreeTransforms tree;

    TransformsBranch(QuadTreeTransforms tree, int level, NodeBoundary nodeBoundary) {
        this.level = level;
        nodes = new Array<>();
        this.tree = tree;
        this.nodeBoundary = nodeBoundary;
    }

    void split() {
        float xOffset = this.nodeBoundary.xMin
                + (this.nodeBoundary.xMax - this.nodeBoundary.xMin) / 2;
        float yOffset = this.nodeBoundary.yMin
                + (this.nodeBoundary.yMax - this.nodeBoundary.yMin) / 2;

        northWest = new TransformsBranch(tree, this.level + 1, new NodeBoundary(
                this.nodeBoundary.xMin, this.nodeBoundary.yMin, xOffset,
                yOffset));
        northEast = new TransformsBranch(tree, this.level + 1, new NodeBoundary(xOffset,
                this.nodeBoundary.yMin, this.nodeBoundary.xMax, yOffset));
        southWest = new TransformsBranch(tree, this.level + 1, new NodeBoundary(
                this.nodeBoundary.xMin, yOffset, xOffset,
                this.nodeBoundary.yMax));
        southEast = new TransformsBranch(tree, this.level + 1, new NodeBoundary(xOffset, yOffset,
                this.nodeBoundary.xMax, this.nodeBoundary.yMax));

    }

    void insert(float x, float y, float z, Matrix4 matrix) {
        if (!this.nodeBoundary.inRange(x, z)) {
            return;
        }

        Node node = new Node(matrix);
        if (nodes.size < tree.transformsPerNode) {
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
}
