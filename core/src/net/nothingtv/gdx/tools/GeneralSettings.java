package net.nothingtv.gdx.tools;

public class GeneralSettings {

    public static enum Setting { Low, Mid, High, Ultra }

    public static final GeneralSettings UltraSettings = new GeneralSettings(Setting.Ultra, 1920, 1080, 128, 1, 2000);
    public static final GeneralSettings HighSettings = new GeneralSettings(Setting.High, 1920, 1080, 128, 10, 1000);
    public static final GeneralSettings MidSettings = new GeneralSettings(Setting.Mid, 1200, 675, 64, 100, 500);
    public static final GeneralSettings LowSettings = new GeneralSettings(Setting.Mid, 800, 450, 64, 500, 250);

    public static GeneralSettings current = LowSettings;

    public Setting setting;
    public int windowWidth;
    public int windowHeight;

    public float foliageMaxDistance;
    public int foliageDivider;
    public float cameraFar;

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
