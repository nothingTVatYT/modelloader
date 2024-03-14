
package net.nothingtv.gdx.terrain;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Disposable;
import net.nothingtv.gdx.testprojects.BaseMaterials;
import net.nothingtv.gdx.tools.MeshCollider;

/** Simple test showing how to use a height map. Uses {@link HeightField}.
 * @author Xoppa */
public class SimpleTerrain implements Disposable {
	HeightField field;
	public ModelInstance modelInstance;
	private float size;
	private final Matrix4 invMatrix = new Matrix4();
	private final MeshCollider meshCollider;

	public SimpleTerrain (Pixmap pixmap, float magnitude) {
		size = 200;
		field = new HeightField(true, pixmap, true, Usage.Position | Usage.Normal | Usage.TextureCoordinates);
		pixmap.dispose();
		field.corner00.set(0, 0, 0);
		field.corner10.set(size, 0, 0);
		field.corner01.set(0, 0, size);
		field.corner11.set(size, 0, size);
		field.color00.set(0, 0, 1, 1);
		field.color01.set(0, 1, 1, 1);
		field.color10.set(1, 0, 1, 1);
		field.color11.set(1, 1, 1, 1);
		field.magnitude.set(0f, magnitude, 0f);
		field.update();

		Material material = BaseMaterials.debugMaterial();
		//TextureAttribute.createDiffuse(texture));
		ModelBuilder modelBuilder = new ModelBuilder();
		modelBuilder.begin();
		modelBuilder.part("terrain", field.mesh, GL20.GL_TRIANGLES, material);
		modelInstance = new ModelInstance(modelBuilder.end());
		meshCollider = new MeshCollider(field.mesh, modelInstance.transform);
	}

	/** Calculate the height at the position taking the heightmap scale and magnitude into account
	 * @param pos the position in local space (y is ignored)
	 * @return the height at this point
	 */
	public float getHeightAt(Vector3 pos) {
		float gridWidth = size / (field.width-1);
		float gridHeight = size / (field.height-1);
		int vertX = (int)Math.floor(pos.x / size * (field.width-1));
		int vertY = (int)Math.floor(pos.z / size * (field.height-1));
		float xCoord = (pos.x - (float)vertX * gridWidth) / gridWidth;
		float zCoord = (pos.z - (float)vertY * gridHeight) / gridHeight;
		Vector3 tmp = new Vector3();
		float ay = field.getPositionAt(tmp, vertX, vertY).y;
		float by = field.getPositionAt(tmp, vertX +1, vertY).y;
		float cy = field.getPositionAt(tmp, vertX, vertY + 1).y;
		float dy = field.getPositionAt(tmp, vertX + 1, vertY + 1).y;
		//float ay = field.getHeightAt(vertX, vertY);
		//float by = field.getHeightAt(vertX + 1, vertY);
		//float cy = field.getHeightAt(vertX, vertY + 1);
		//float dy = field.getHeightAt(vertX + 1, vertY + 1);

		float height;

		if (xCoord <= (1 - zCoord)) { // we are in upper left triangle of the square
			float hx0 = ay + xCoord * (by - ay);
			height = hx0 + zCoord * (cy - hx0);
		} else { // bottom right triangle
			float hx0 = cy + xCoord * (dy - cy);
			height = by + zCoord * (hx0 - by);
		}
		return height;
	}

	public boolean rayIntersects(Ray ray, Vector3 result) {
		return meshCollider.rayIntersects(ray, result);
	}

	public Vector3 worldToLocal(Vector3 pos) {
		invMatrix.set(modelInstance.transform).inv();
		pos.mul(invMatrix);
		return pos;
	}

	@Override
	public void dispose () {
		field.dispose();
	}
}
