package net.nothingtv.gdx.modelloader;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ScreenUtils;
import net.nothingtv.gdx.inventory.Inventory;

public class SelectScreen extends ScreenAdapter {
    private Stage stage;
    private Table table;


    public SelectScreen (Game game) {
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        table = new Table();
        table.setFillParent(true);
        table.pad(10).defaults().space(20);
        stage.addActor(table);

        Skin skin = new Skin(Gdx.files.internal("data/uiskin.json"));

        table.add(createMenuButton("Terrain Test", skin, () -> game.setScreen(new TerrainTest())));
        table.row();

        table.add(createMenuButton("Model Loader", skin, () -> game.setScreen(new ModelLoader())));
        table.row();

        table.add(createMenuButton("Shadow Test", skin, () -> game.setScreen(new ShadowTest())));
        table.row();

        table.add(createMenuButton("Game/Physics Test", skin, () -> game.setScreen(new PhysicsTest(game))));
        table.row();

        table.add(createMenuButton("Update Innventory", skin, Inventory::writeItems));
        table.row();

        table.add(createMenuButton("Exit", skin, () -> Gdx.app.exit())).width(100);
    }

    private TextButton createMenuButton(String text, Skin skin, Clicked l) {
        TextButton button = new TextButton(text, skin);
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                l.clicked();
            }
        });
        return button;
    }

    public void resize (int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void render (float delta) {
        ScreenUtils.clear(Color.DARK_GRAY, true);
        stage.act(delta);
        stage.draw();
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
            Gdx.app.exit();
    }

    public void dispose() {
        stage.dispose();
    }

    interface Clicked {
        void clicked();
    }
}
