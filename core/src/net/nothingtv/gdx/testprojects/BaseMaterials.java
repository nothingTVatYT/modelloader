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
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;

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

    public static void generateAlphaMap() {
        BufferedImage image = new BufferedImage(1024, 1024, BufferedImage.TYPE_4BYTE_ABGR);
        byte[] array = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        //System.out.println("int array size " + array.length);
        int index = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                // alpha
                array[index++] = (byte)(x < 512 && y < 512 ? 255 : 0);
                // blue
                array[index++] = (byte)(x >= 512 && y < 512 ? 255 : 0);
                // green
                array[index++] = (byte)(x < 512 && y >= 512 ? 255 : 0);
                // red
                array[index++] = (byte)(x >= 512 && y >= 512 ? 255 : 0);
            }
        }
        /*
        Graphics g = image.getGraphics();
        g.clearRect(0, 0, 1024, 1024);
        java.awt.Color c1 = new java.awt.Color(0, 0, 0, 1f);
        java.awt.Color c2 = new java.awt.Color(0, 0, 1f, 0);
        java.awt.Color c3 = new java.awt.Color(0, 1f, 0, 0);
        java.awt.Color c4 = new java.awt.Color(1f, 0, 0, 0);
        g.setColor(c1);
        g.fillRect(0, 0, 512, 512);
        g.setColor(c2);
        g.fillRect(512, 0, 512, 512);
        g.setColor(c3);
        g.fillRect(0, 512, 512, 512);
        g.setColor(c4);
        g.fillRect(512, 512, 512, 512);
        */
        try {
            ImageIO.write(image, "png", new File("assets/textures/alpha-example.png"));
        } catch (IOException e) {
            System.err.println("Cannot create image");
            e.printStackTrace();
        }
    }
}
