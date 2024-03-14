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
        public final static Uniform uv1Scale = new Uniform("u_uv1Scale", TerrainFloatAttribute.UV1Scale);
        public final static Uniform uv2Scale = new Uniform("u_uv2Scale", TerrainFloatAttribute.UV2Scale);
        public final static Uniform uv3Scale = new Uniform("u_uv3Scale", TerrainFloatAttribute.UV3Scale);
        public final static Uniform uv4Scale = new Uniform("u_uv4Scale", TerrainFloatAttribute.UV4Scale);
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
        public final static Setter uv1Scale = new LocalSetter() {
            @Override
            public void set (BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, ((TerrainFloatAttribute)(combinedAttributes.get(TerrainFloatAttribute.UV1Scale))).value);
            }
        };
        public final static Setter uv2Scale = new LocalSetter() {
            @Override
            public void set (BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, ((TerrainFloatAttribute)(combinedAttributes.get(TerrainFloatAttribute.UV2Scale))).value);
            }
        };
        public final static Setter uv3Scale = new LocalSetter() {
            @Override
            public void set (BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, ((TerrainFloatAttribute)(combinedAttributes.get(TerrainFloatAttribute.UV3Scale))).value);
            }
        };
        public final static Setter uv4Scale = new LocalSetter() {
            @Override
            public void set (BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, ((TerrainFloatAttribute)(combinedAttributes.get(TerrainFloatAttribute.UV4Scale))).value);
            }
        };

    }
    public final int u_alpha1Texture;
    public final int u_diffuse2Texture;
    public final int u_diffuse3Texture;
    public final int u_diffuse4Texture;
    public final int u_uv1Scale;
    public final int u_uv2Scale;
    public final int u_uv3Scale;
    public final int u_uv4Scale;

    public TerrainShader(Renderable renderable, Config config, String prefix) {
        super(renderable, config, prefix);
        u_alpha1Texture = register(Inputs.alpha1Texture, Setters.alpha1Texture);
        u_diffuse2Texture = register(Inputs.diffuse2Texture, Setters.diffuse2Texture);
        u_diffuse3Texture = register(Inputs.diffuse3Texture, Setters.diffuse3Texture);
        u_diffuse4Texture = register(Inputs.diffuse4Texture, Setters.diffuse4Texture);
        u_uv1Scale = register(Inputs.uv1Scale, Setters.uv1Scale);
        u_uv2Scale = register(Inputs.uv2Scale, Setters.uv2Scale);
        u_uv3Scale = register(Inputs.uv3Scale, Setters.uv3Scale);
        u_uv4Scale = register(Inputs.uv4Scale, Setters.uv4Scale);
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
}
