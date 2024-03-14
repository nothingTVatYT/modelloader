package net.nothingtv.gdx.terrain;

import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;

public class TerrainFloatAttribute extends FloatAttribute {
    public final static String UV1ScaleAlias = "uv1Scale";
    public final static String UV2ScaleAlias = "uv2Scale";
    public final static String UV3ScaleAlias = "uv3Scale";
    public final static String UV4ScaleAlias = "uv4Scale";
    public final static long UV1Scale = register(UV1ScaleAlias);
    public final static long UV2Scale = register(UV2ScaleAlias);
    public final static long UV3Scale = register(UV3ScaleAlias);
    public final static long UV4Scale = register(UV4ScaleAlias);

    public static FloatAttribute createUV1Scale (float value) {
        return new TerrainFloatAttribute(UV1Scale, value);
    }
    public static FloatAttribute createUV2Scale (float value) {
        return new TerrainFloatAttribute(UV2Scale, value);
    }
    public static FloatAttribute createUV3Scale (float value) {
        return new TerrainFloatAttribute(UV3Scale, value);
    }
    public static FloatAttribute createUV4Scale (float value) {
        return new TerrainFloatAttribute(UV4Scale, value);
    }

    public TerrainFloatAttribute(long type) {
        super(type);
    }

    public TerrainFloatAttribute(long type, float value) {
        super(type, value);
    }
}
