package net.nothingtv.gdx.terrain;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;

public class TerrainTextureAttribute extends TextureAttribute {
    public final static String Alpha1Alias = "alpha1Texture";
    public final static String Diffuse2Alias = "diffuse2Texture";
    public final static String Diffuse3Alias = "diffuse3Texture";
    public final static String Diffuse4Alias = "diffuse4Texture";
    public final static long Alpha1 = register(Alpha1Alias);
    public final static long Diffuse2 = register(Diffuse2Alias);
    public final static long Diffuse3 = register(Diffuse3Alias);
    public final static long Diffuse4 = register(Diffuse4Alias);
    static { Mask |= Alpha1 | Diffuse2 | Diffuse3 | Diffuse4; }

    public static TerrainTextureAttribute createAlpha1 (final Texture texture) {
        return new TerrainTextureAttribute(Alpha1, texture);
    }

    public static TerrainTextureAttribute createDiffuse2 (final Texture texture) {
        return new TerrainTextureAttribute(Diffuse2, texture);
    }

    public static TerrainTextureAttribute createDiffuse3 (final Texture texture) {
        return new TerrainTextureAttribute(Diffuse3, texture);
    }

    public static TerrainTextureAttribute createDiffuse4 (final Texture texture) {
        return new TerrainTextureAttribute(Diffuse4, texture);
    }

    public TerrainTextureAttribute(long type) {
        super(type);
    }

    public <T extends Texture> TerrainTextureAttribute(long type, TextureDescriptor<T> textureDescription) {
        super(type, textureDescription);
    }

    public <T extends Texture> TerrainTextureAttribute(long type, TextureDescriptor<T> textureDescription, float offsetU, float offsetV, float scaleU, float scaleV, int uvIndex) {
        super(type, textureDescription, offsetU, offsetV, scaleU, scaleV, uvIndex);
    }

    public <T extends Texture> TerrainTextureAttribute(long type, TextureDescriptor<T> textureDescription, float offsetU, float offsetV, float scaleU, float scaleV) {
        super(type, textureDescription, offsetU, offsetV, scaleU, scaleV);
    }

    public TerrainTextureAttribute(long type, Texture texture) {
        super(type, texture);
    }

    public TerrainTextureAttribute(long type, TextureRegion region) {
        super(type, region);
    }

    public TerrainTextureAttribute(TextureAttribute copyFrom) {
        super(copyFrom);
    }
}
