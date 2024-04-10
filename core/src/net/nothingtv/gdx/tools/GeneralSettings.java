package net.nothingtv.gdx.tools;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class GeneralSettings {

    public enum Setting { Low, Mid, High, Ultra }

    public static final GeneralSettings UltraSettings = new GeneralSettings(Setting.Ultra, 1920, 1080, 128, 1, 2000);
    public static final GeneralSettings HighSettings = new GeneralSettings(Setting.High, 1920, 1080, 128, 10, 1000);
    public static final GeneralSettings MidSettings = new GeneralSettings(Setting.Mid, 1200, 675, 64, 25, 500);
    public static final GeneralSettings LowSettings = new GeneralSettings(Setting.Mid, 800, 450, 32, 20, 125);

    public static GeneralSettings current = LowSettings;

    public Setting setting;
    public int windowWidth;
    public int windowHeight;

    public float foliageMaxDistance;
    public int foliageDivider;
    public float cameraFar;

    public static void autoSelect() {
        String env = System.getenv("GRAPHICS_SETTINGS");
        if (env != null && !env.isEmpty()) {
            select(env);
            return;
        }
        String home = System.getenv("HOME");
        Properties properties = new Properties();
        for (Path path : new Path[] {
                Path.of(home, ".local/share/gdx.config"),
                Path.of(home, ".gdxConfig")}) {
            if (Files.exists(path)) {
                try (FileInputStream fis = new FileInputStream(path.toFile())) {
                    properties.load(fis);
                    String settings = properties.getProperty("GraphicsSettings", "");
                    if (!settings.isBlank()) {
                        select(settings);
                        return;
                    }
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        // just a wild guess
        int cpus = Runtime.getRuntime().availableProcessors();
        if (cpus >= 16) select(Setting.Ultra);
        else if (cpus >= 8) select(Setting.High);
        else if (cpus >= 4) select(Setting.Mid);
        else select(Setting.Low);
    }

    public static void select(Setting setting) {
        switch (setting) {
            case Low -> current = LowSettings;
            case Mid -> current = MidSettings;
            case High -> current = HighSettings;
            case Ultra -> current = UltraSettings;
        }
    }

    public static void select(String settingsName) {
        if (Setting.Low.name().equals(settingsName))
            select(Setting.Low);
        else if (Setting.Mid.name().equals(settingsName))
            select(Setting.Mid);
        else if (Setting.High.name().equals(settingsName))
            select(Setting.High);
        else if (Setting.Ultra.name().equals(settingsName))
            select(Setting.Ultra);
    }

    public static boolean isAtLeast(Setting setting) {
        return current.setting.ordinal() >= setting.ordinal();
    }

    protected GeneralSettings(Setting setting, int windowWidth, int windowHeight, float foliageMaxDistance, int foliageDivider, float cameraFar) {
        this.setting = setting;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.foliageMaxDistance = foliageMaxDistance;
        this.foliageDivider = foliageDivider;
        this.cameraFar = cameraFar;
    }
}
