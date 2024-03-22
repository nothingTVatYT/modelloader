package net.nothingtv.gdx.modelloader;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import net.nothingtv.gdx.examples.GLTFExample;

public class DemoLauncher {
    public static void main (String[] arg) {

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        new Lwjgl3Application(new GLTFExample(), config);
    }
}