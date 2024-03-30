package net.nothingtv.gdx.modelloader;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import net.nothingtv.gdx.testprojects.TestGame;

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
public class DesktopLauncher {
	public static void main (String[] arg) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setForegroundFPS(1000);
		config.useVsync(false);
		config.setTitle("modelloader");
		config.setResizable(true);
		config.setWindowedMode(1920, 1080);
		//int samples = 4; // you can also play around with higher values like 4
		//config.setBackBufferConfig(8, 8, 8, 8, 16, 0, samples); // 8, 8, 8, 8, 16, 0 are default values
		config.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30, 4,3);
		new Lwjgl3Application(new TestGame(), config);
	}
}
