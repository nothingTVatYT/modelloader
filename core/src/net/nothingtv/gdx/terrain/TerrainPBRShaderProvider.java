package net.nothingtv.gdx.terrain;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Renderable;
import net.mgsx.gltf.scene3d.shaders.PBRShader;
import net.mgsx.gltf.scene3d.shaders.PBRShaderConfig;
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider;
import net.mgsx.gltf.scene3d.utils.ShaderParser;

public class TerrainPBRShaderProvider extends PBRShaderProvider {

    public TerrainPBRShaderProvider(PBRShaderConfig defaultConfig) {
        super(defaultConfig);
        this.config.vertexShader = ShaderParser.parse(Gdx.files.internal("shaders/pbr/pbr.vs.glsl"));
        this.config.fragmentShader = ShaderParser.parse(Gdx.files.internal("shaders/pbr/pbr_terrain.fs.glsl"));
        //this.config.vertexShader = ShaderParser.parse(Gdx.files.internal("shaders/ww.vert"));
        //this.config.fragmentShader = ShaderParser.parse(Gdx.files.internal("shaders/ww.frag"));
    }

    @Override
    public String createPrefixBase(Renderable renderable, PBRShaderConfig config) {
        String prefix = super.createPrefixBase(renderable, config);
        if (renderable.material.has(TerrainTextureAttribute.Alpha1))
            prefix += "#define alpha1TextureFlag\n";
        if (renderable.material.has(TerrainTextureAttribute.Diffuse2))
            prefix += "#define diffuse2TextureFlag\n";
        if (renderable.material.has(TerrainTextureAttribute.Diffuse3))
            prefix += "#define diffuse3TextureFlag\n";
        if (renderable.material.has(TerrainTextureAttribute.Diffuse4))
            prefix += "#define diffuse4TextureFlag\n";
        if (renderable.material.has(TerrainFloatAttribute.UV1Scale))
            prefix += "#define uv1ScaleFlag\n";
        if (renderable.material.has(TerrainFloatAttribute.UV2Scale))
            prefix += "#define uv2ScaleFlag\n";
        if (renderable.material.has(TerrainFloatAttribute.UV3Scale))
            prefix += "#define uv3ScaleFlag\n";
        if (renderable.material.has(TerrainFloatAttribute.UV4Scale))
            prefix += "#define uv4ScaleFlag\n";
        return prefix;
    }

    @Override
    protected PBRShader createShader(Renderable renderable, PBRShaderConfig config, String prefix) {
        if (renderable.material.has(TerrainTextureAttribute.Alpha1))
            return new TerrainPBRShader(renderable, config, prefix);
        else
            return super.createShader(renderable, config, prefix);
    }
}
