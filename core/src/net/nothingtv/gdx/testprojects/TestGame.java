package net.nothingtv.gdx.testprojects;

import com.badlogic.gdx.Game;
import net.nothingtv.gdx.modelloader.SelectScreen;
import net.nothingtv.gdx.modelloader.ShadowTest;

public class TestGame extends Game {

    @Override
    public void create() {
        setScreen(new SelectScreen(this));
    }
}
