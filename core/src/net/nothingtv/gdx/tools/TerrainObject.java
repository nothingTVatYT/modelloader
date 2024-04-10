package net.nothingtv.gdx.tools;

import net.nothingtv.gdx.terrain.Terrain;
import net.nothingtv.gdx.terrain.TerrainInstance;

public class TerrainObject extends SceneObject {

    public Terrain terrain;

    public TerrainObject(String name, TerrainInstance modelInstance, Terrain terrain) {
        super(name, modelInstance);
        this.terrain = terrain;
    }

    public TerrainInstance getTerrainInstance() {
        return (TerrainInstance) modelInstance;
    }
}
