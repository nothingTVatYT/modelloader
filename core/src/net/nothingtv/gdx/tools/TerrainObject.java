package net.nothingtv.gdx.tools;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import net.nothingtv.gdx.terrain.Terrain;

public class TerrainObject extends SceneObject {

    public Terrain terrain;

    public TerrainObject(String name, ModelInstance modelInstance, Terrain terrain) {
        super(name, modelInstance);
        this.terrain = terrain;
    }
}
