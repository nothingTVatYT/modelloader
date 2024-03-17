package net.nothingtv.gdx.testprojects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;
import com.badlogic.gdx.math.MathUtils;
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.nothingtv.gdx.tools.Tools;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class BaseMaterials {

    public static Material missingMaterial() {
        Material material = new Material("_base_for_missing");
        material.set(PBRColorAttribute.createDiffuse(Color.BLUE));
        material.set(PBRColorAttribute.createBaseColorFactor(Color.GRAY));
        material.set(IntAttribute.createCullFace(GL20.GL_BACK));
        return material;
    }

    public static Material whiteColor() {
        Material material = new Material("_base_white");
        material.set(ColorAttribute.createDiffuse(Color.WHITE));
        material.set(IntAttribute.createCullFace(GL20.GL_BACK));
        return material;
    }

    public static Material whiteColorPBR() {
        Material material = new Material("_pbr_white");
        material.set(PBRColorAttribute.createBaseColorFactor(Color.WHITE));
        material.set(IntAttribute.createCullFace(GL20.GL_BACK));
        return material;
    }

    public static Material debugMaterial() {
        Material material = new Material();
        material.id = "_base_debug";
        material.set(PBRColorAttribute.createBaseColorFactor(Color.WHITE));
        material.set(PBRTextureAttribute.createBaseColorTexture(new Texture(Gdx.files.internal("textures/debug-Albedo.png"), true)));
        material.set(PBRTextureAttribute.createNormalTexture(new Texture(Gdx.files.internal("textures/debug-Normal.png"), true)));
        material.set(PBRTextureAttribute.createMetallicRoughnessTexture(new Texture(Gdx.files.internal("textures/debug-Metal.png"), true)));
        material.set(PBRFloatAttribute.createMetallic(1f));
        material.set(PBRFloatAttribute.createRoughness(0.85f));
        material.set(IntAttribute.createCullFace(GL20.GL_BACK));
        material.set(PBRFloatAttribute.createNormalScale(1f));
        return material;
    }

    public static void dumpMaterial(Material material, String owner) {
        StringBuilder sb = new StringBuilder();
        sb.append("Material: ").append(material.id);
        if (!owner.isEmpty())
            sb.append(" (").append(owner).append(")\n");
        for (Attribute a : material) {
            sb.append(" ").append(a.toString()).append(" (").append(a.getClass().getName()).append(' ').append(a.type).append(") = ");
            String value = "";
            if (a instanceof PBRTextureAttribute textureAttribute)
                value = textureInfo(textureAttribute.textureDescription.texture);
            else if (a instanceof ColorAttribute colorAttribute)
                value = String.format("Color(%1.3f,%1.3f,%1.3f,%1.3f)", colorAttribute.color.r, colorAttribute.color.g, colorAttribute.color.b, colorAttribute.color.a);
            else if (a instanceof FloatAttribute floatAttribute)
                value = ""+floatAttribute.value;
            else if (a instanceof IntAttribute intAttribute)
                value = ""+intAttribute.value;
            sb.append(value).append('\n');
        }
        System.out.print(sb);
    }

    public static String textureInfo(Texture texture) {
        return String.format("\"%s\", uWrap=%s, vWrap=%s, width=%d, height=%d, magFilter=%s, minFilter=%s, aniFilter=%f",
                texture.toString(), texture.getUWrap(), texture.getVWrap(), texture.getWidth(), texture.getHeight(), texture.getMagFilter(), texture.getMinFilter(), texture.getAnisotropicFilter());
    }

    public static void writeGrayImage(float[] values, String name) {
        float max = 0;
        float min = Float.MAX_VALUE;
        for (float value : values) { max = Math.max(max, value); min = Math.min(min, value); }
        int width = (int)Math.sqrt(values.length);
        BufferedImage image = new BufferedImage(width, width, BufferedImage.TYPE_3BYTE_BGR);
        byte[] array = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        int index = 0;
        for (int i = 0; i < values.length; i++) {
            int val = Math.round((values[i]-min) / (max-min) * 255f);
            array[index++] = (byte)val;
            array[index++] = (byte)val;
            array[index++] = (byte)val;
        }
        try {
            ImageIO.write(image, "png", new File("assets/textures/" + name));
        } catch (IOException e) {
            System.err.println("Cannot create image " + e);
        }
    }

    public static void generateAlphaMap() {
        int width = 1024;
        int height = 1024;
        int halfWidth = width / 2;
        int halfHeight = height / 2;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        byte[] array = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        int index = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            float smoothedY = 1f-Tools.smoothStep((float)y, halfHeight * 0.9f, halfHeight * 1.1f);
            for (int x = 0; x < image.getWidth(); x++) {
                float smoothedX = 1f-Tools.smoothStep((float)x, halfWidth * 0.9f, halfWidth * 1.1f);
                // alpha
                array[index++] = (byte)(Tools.clamp01(smoothedX * smoothedY) * 255);
                // blue
                array[index++] = (byte)(Tools.clamp01((1f-smoothedX) * smoothedY) * 255);
                // green
                array[index++] = (byte)(Tools.clamp01(smoothedX * (1f-smoothedY)) * 255);
                // red
                array[index++] = (byte)(Tools.clamp01((1f-smoothedX) * (1f-smoothedY)) * 255);
            }
        }
        try {
            ImageIO.write(image, "png", new File("assets/textures/alpha-example.png"));
        } catch (IOException e) {
            System.err.println("Cannot create image " + e);
        }
    }
}
