package net.nothingtv.gdx.shaders;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import net.mgsx.gltf.scene3d.shaders.PBRShader;
import net.mgsx.gltf.scene3d.shaders.PBRShaderConfig;
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider;
import net.mgsx.gltf.scene3d.utils.ShaderParser;
import net.nothingtv.gdx.terrain.TerrainPBRShader;
import net.nothingtv.gdx.terrain.TerrainTextureAttribute;

public class GameShaderProvider extends PBRShaderProvider {
    public GameShaderProvider(PBRShaderConfig config) {
        super(config);
    }

    @Override
    protected Shader createShader(Renderable renderable) {
        //System.out.printf("GameShaderProvider.createShader(Renderable): create shader for material %s%n", renderable.material.id);
        return super.createShader(renderable);
    }

    @Override
    protected PBRShader createShader(Renderable renderable, PBRShaderConfig config, String prefix) {
        //System.out.printf("GameShaderProvider.createShader(Renderable, Config, Prefix): create shader for material %s%n", renderable.material.id);
        if( renderable.meshPart.mesh.isInstanced()) {
            prefix += "#define instanced\n";
            config.vertexShader = Gdx.files.internal("shaders/pbr/pbr.vs.glsl").readString();
            return new MyPBRShader(renderable, config, prefix);
        }
        if (renderable.material.has(TerrainTextureAttribute.Alpha1)) {
            config.vertexShader = ShaderParser.parse(Gdx.files.internal("shaders/pbr/pbr_terrain.vs.glsl"));
            config.fragmentShader = ShaderParser.parse(Gdx.files.internal("shaders/pbr/pbr_terrain.fs.glsl"));
            return new TerrainPBRShader(renderable, config, TerrainPBRShader.createPrefix(renderable, prefix));
        }
        config.vertexShader = PBRShaderProvider.getDefaultVertexShader();
        config.fragmentShader = PBRShaderProvider.getDefaultFragmentShader();
        return new PBRShader(renderable, config, prefix);
    }

    // override this to force #version 140, needed for the inverse() built-in
    @Override
    public String createPrefixBase(Renderable renderable, PBRShaderConfig config) {
        if(Gdx.app.getType() == Application.ApplicationType.Desktop)
            config.glslVersion = "#version 140\n" + "#define GLSL3\n";
        else
            config.glslVersion = "#version 300 es\n" + "#define GLSL3\n";
        return super.createPrefixBase(renderable, config);
    }

}
