package net.nothingtv.gdx.modelloader;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import net.nothingtv.gdx.examples.GLTFExample;

public class DemoLauncher {
    public static void main (String[] arg) {

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setWindowedMode(1920, 1080);
        new Lwjgl3Application(new GLTFExample(), config);
    }
}