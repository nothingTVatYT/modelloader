package net.nothingtv.gdx.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import net.nothingtv.gdx.terrain.TerrainShader;

public class WWShaderProvider extends DefaultShaderProvider {

    public WWShaderProvider(DefaultShader.Config config) {
        super(config);
    }

    @Override
    protected Shader createShader(Renderable renderable) {
        System.out.printf("WWShaderProvider: create shader for %s%n", renderable.material.id);
        if (renderable.material.id.equals("_skyMaterial")) {
            DefaultShader.Config skyConfig = new DefaultShader.Config(
                    Gdx.files.internal("shaders/sky.vert").readString(),
                    Gdx.files.internal("shaders/sky.frag").readString());
            String prefix = "#version 330\n" + DefaultShader.createPrefix(renderable, skyConfig);
            return new DefaultShader(renderable, skyConfig, prefix);
        } else if (renderable.material.id.equals("_terrain")) {
            DefaultShader.Config terrainConfig = new DefaultShader.Config(
                    Gdx.files.internal("shaders/ww.vert").readString(),
                    Gdx.files.internal("shaders/ww.frag").readString());
            String prefix = "#version 330\n" + TerrainShader.createPrefix(renderable, terrainConfig);
            return new TerrainShader(renderable, terrainConfig, prefix);
        }
        String prefix = "#version 330\n" + DefaultShader.createPrefix(renderable, config);
        return new DefaultShader(renderable, config, prefix);
    }

    public static DefaultShader.Config createDefaultConfig() {
        DefaultShader.Config config = new DefaultShader.Config();
        config.numBones = 60;
        config.vertexShader = Gdx.files.internal("shaders/ww.vert").readString();
        config.fragmentShader = Gdx.files.internal("shaders/ww.frag").readString();
        return config;
    }
}
