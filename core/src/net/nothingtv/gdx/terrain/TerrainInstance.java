package net.nothingtv.gdx.terrain;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import net.mgsx.gltf.exporters.GLTFExporter;
import net.mgsx.gltf.scene3d.scene.Updatable;

import java.nio.FloatBuffer;

public class TerrainInstance extends ModelInstance implements Updatable {

    public int vertices;
    public int visibleNodes;
    public boolean debugBounds = false;
    public Terrain terrain;
    private float[] procVertices;
    private float[] segmentGrid;
    private short[] procIndices;
    private Node procNode;
    private MeshPart procMeshPart;
    private final float fov = 120;
    private float uvScale = 0.1f;
    private final float depthFactor;
    private final Matrix4 camMatrix = new Matrix4();
    private final Vector3 tmpVector = new Vector3();
    private final Vector3 tmpNormal = new Vector3();
    private float lastCameraAngle;

    public TerrainInstance(Model model, Terrain terrain) {
        super(model);
        this.terrain = terrain;
        this.depthFactor = terrain.config.depthFactor;
    }

    public void proceduralNodes(Camera camera) {
        if (procNode == null) {
            lastCameraAngle = Float.MAX_VALUE;
            int arcs = (int) Math.ceil(Math.log(camera.far) / Math.log(depthFactor));
            int rays = Math.max(arcs * 2, 30);
            nodes.forEach(n -> n.parts.forEach(p -> p.enabled = false));
            procNode = new Node();
            NodePart procPart = new NodePart();
            procPart.material = nodes.first().parts.first().material;
            procMeshPart = new MeshPart();

            // calculate the segment grid in local space
            segmentGrid = new float[2 * rays * arcs];
            procVertices = new float[8 * rays * arcs];
            procIndices = new short[6 * (rays -1) * (arcs -1)];

            System.out.printf("TerrainInstance: create a segment of %d rays and %d arcs%n", rays, arcs);

            Vector3[] localRay = new Vector3[rays];
            for (int i = 0; i < rays; i++) {
                float a = (float)(i-(rays -1)/2) / rays * fov;
                localRay[i] = new Vector3(MathUtils.sinDeg(a), 0, MathUtils.cosDeg(a));
            }

            int vi = 0;
            for (int z = 0; z < arcs; z++) {
                float dist = (float)Math.pow(depthFactor, z);
                for (int x = 0; x < rays; x++) {
                    segmentGrid[vi++] = localRay[x].x * dist;
                    segmentGrid[vi++] = localRay[x].z * dist - 2.2f;
                }
            }

            int ii = 0;
            for (int a = 0; a < (arcs -1); a++) {
                for (int r = 0; r < (rays -1); r++) {
                    // top left triangles are 0,width,1 and 1,width,width+1
                    // the following are offset by r and a*rays
                    procIndices[ii++] = (short)(r + a * rays);
                    procIndices[ii++] = (short)(rays + r + a * rays);
                    procIndices[ii++] = (short)(1 + r + a * rays);
                    procIndices[ii++] = (short)(rays + r + a * rays);
                    procIndices[ii++] = (short)(rays +1 + r + a * rays);
                    procIndices[ii++] = (short)(1 + r + a * rays);
                }
            }
            procMeshPart.mesh = new Mesh(false, procVertices.length, procIndices.length,
                    VertexAttribute.Position(), VertexAttribute.Normal(), VertexAttribute.TexCoords(0));
            procMeshPart.primitiveType = GL20.GL_TRIANGLES;
            procMeshPart.offset = 0;
            procMeshPart.size = procIndices.length;
            procPart.meshPart = procMeshPart;
            procNode.parts.add(procPart);
            nodes.add(procNode);
            visibleNodes = 1;
            vertices = ii;
            procMeshPart.mesh.setIndices(procIndices);
        }

        FloatBuffer vertexBuffer = procMeshPart.mesh.getVerticesBuffer(true);
        vertexBuffer.position(0);
        vertexBuffer.limit(vertexBuffer.capacity());

        // TODO: use a matrix instead of the various calculations
        //camMatrix.idt().trn(camera.position.x, 0, camera.position.z).rotateTowardDirection(dir, Vector3.Y);
        camMatrix.setToLookAt(camera.direction, Vector3.Y);
        float cameraAngle = new Quaternion().setFromMatrix(camMatrix).getAngleAround(Vector3.Y);
        if (Math.abs(lastCameraAngle - cameraAngle) < 2)
            return;
        lastCameraAngle = cameraAngle;
        for (int i = 0; i < procVertices.length/8; i++) {
            tmpVector.set(segmentGrid[i*2], 0, segmentGrid[i*2+1]);
            tmpVector.rotate(Vector3.Y, 180-cameraAngle);
            //tmpVector.add(MathUtils.floor(camera.position.x), 0, MathUtils.floor(camera.position.z));
            tmpVector.add(camera.position.x, 0, camera.position.z);
            tmpVector.x = MathUtils.round(tmpVector.x);
            tmpVector.z = MathUtils.round(tmpVector.z);
            terrain.getNormalAt(tmpVector.x, tmpVector.z, tmpNormal);
            vertexBuffer.put(tmpVector.x);
            vertexBuffer.put(terrain.getHeightAt(tmpVector.x, tmpVector.z));
            vertexBuffer.put(tmpVector.z);
            vertexBuffer.put(tmpNormal.x);
            vertexBuffer.put(tmpNormal.y);
            vertexBuffer.put(tmpNormal.z);
            vertexBuffer.put(tmpVector.x * uvScale);
            vertexBuffer.put(tmpVector.z * uvScale);
        }
        vertexBuffer.flip();

        if (debugBounds) {
            System.out.printf("Exporting mesh to %s%n", Gdx.files.external("/tmp/mesh-dump.gltf").file().getAbsolutePath());
            new GLTFExporter().export(procMeshPart.mesh, GL20.GL_TRIANGLES, Gdx.files.external("/tmp/mesh-dump.gltf"));
            debugBounds = false;
        }
    }

    @Override
    public void update(Camera camera, float delta) {
        proceduralNodes(camera);
    }
}
