package net.nothingtv.gdx.terrain;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;

public class TerrainShaderProvider extends DefaultShaderProvider {
    DefaultShader.Config terrainConfig;

    public TerrainShaderProvider(DefaultShader.Config defaultConfig) {
        super(defaultConfig);
        terrainConfig = new DefaultShader.Config();
        terrainConfig.vertexShader = Gdx.files.internal("shaders/terrain.vert").readString();
        terrainConfig.fragmentShader = Gdx.files.internal("shaders/terrain.frag").readString();
    }

    @Override
    protected Shader createShader(Renderable renderable) {
        if (renderable.material.has(TerrainTextureAttribute.Alpha1))
            return new TerrainShader(renderable, terrainConfig, TerrainShader.createPrefix(renderable, terrainConfig));
        else
            return super.createShader(renderable);
    }
}

