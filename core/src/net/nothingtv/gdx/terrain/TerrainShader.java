package net.nothingtv.gdx.terrain;

import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.shaders.BaseShader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;

public class TerrainShader extends DefaultShader {
    public static class Inputs {
        public final static Uniform alpha1Texture = new Uniform("u_alpha1Texture", TerrainTextureAttribute.Alpha1);
        public final static Uniform diffuse2Texture = new Uniform("u_diffuse2Texture", TerrainTextureAttribute.Diffuse2);
        public final static Uniform diffuse3Texture = new Uniform("u_diffuse3Texture", TerrainTextureAttribute.Diffuse3);
        public final static Uniform diffuse4Texture = new Uniform("u_diffuse4Texture", TerrainTextureAttribute.Diffuse4);
    }

    public static class Setters {
        public final static Setter alpha1Texture = new LocalSetter() {
            @Override
            public void set (BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                final int unit = shader.context.textureBinder
                        .bind(((TextureAttribute)(combinedAttributes.get(TerrainTextureAttribute.Alpha1))).textureDescription);
                shader.set(inputID, unit);
            }
        };
        public final static Setter diffuse2Texture = new LocalSetter() {
            @Override
            public void set (BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                final int unit = shader.context.textureBinder
                        .bind(((TextureAttribute)(combinedAttributes.get(TerrainTextureAttribute.Diffuse2))).textureDescription);
                shader.set(inputID, unit);
            }
        };
        public final static Setter diffuse3Texture = new LocalSetter() {
            @Override
            public void set (BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                final int unit = shader.context.textureBinder
                        .bind(((TextureAttribute)(combinedAttributes.get(TerrainTextureAttribute.Diffuse3))).textureDescription);
                shader.set(inputID, unit);
            }
        };
        public final static Setter diffuse4Texture = new LocalSetter() {
            @Override
            public void set (BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                final int unit = shader.context.textureBinder
                        .bind(((TextureAttribute)(combinedAttributes.get(TerrainTextureAttribute.Diffuse4))).textureDescription);
                shader.set(inputID, unit);
            }
        };
    }
    public final int u_alpha1Texture;
    public final int u_diffuse2Texture;
    public final int u_diffuse3Texture;
    public final int u_diffuse4Texture;

    public TerrainShader(Renderable renderable, Config config) {
        super(renderable, config);
        u_alpha1Texture = register(Inputs.alpha1Texture, Setters.alpha1Texture);
        u_diffuse2Texture = register(Inputs.diffuse2Texture, Setters.diffuse2Texture);
        u_diffuse3Texture = register(Inputs.diffuse3Texture, Setters.diffuse3Texture);
        u_diffuse4Texture = register(Inputs.diffuse4Texture, Setters.diffuse4Texture);
    }

    public TerrainShader(Renderable renderable, Config config, String prefix) {
        super(renderable, config, prefix);
        u_alpha1Texture = register(Inputs.alpha1Texture, Setters.alpha1Texture);
        u_diffuse2Texture = register(Inputs.diffuse2Texture, Setters.diffuse2Texture);
        u_diffuse3Texture = register(Inputs.diffuse3Texture, Setters.diffuse3Texture);
        u_diffuse4Texture = register(Inputs.diffuse4Texture, Setters.diffuse4Texture);
    }

    public static String createPrefix(Renderable renderable, Config config) {
        String prefix = DefaultShader.createPrefix(renderable, config);
        if (renderable.material.has(TerrainTextureAttribute.Alpha1))
            prefix += "#define alpha1TextureFlag\n";
        if (renderable.material.has(TerrainTextureAttribute.Diffuse2))
            prefix += "#define diffuse2TextureFlag\n";
        if (renderable.material.has(TerrainTextureAttribute.Diffuse3))
            prefix += "#define diffuse3TextureFlag\n";
        if (renderable.material.has(TerrainTextureAttribute.Diffuse4))
            prefix += "#define diffuse4TextureFlag\n";
        return prefix;
    }
}
