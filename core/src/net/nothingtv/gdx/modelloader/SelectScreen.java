package net.nothingtv.gdx.modelloader;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ScreenUtils;

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

        //table.setDebug(true); // This is optional, but enables debug lines for tables.

        // Add widgets to the table here.

        Skin skin = new Skin(Gdx.files.internal("data/uiskin.json"));

        TextButton button1 = new TextButton("Terrain Test", skin);
        button1.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new TerrainTest());
            }
        });
        table.add(button1);
        table.row();

        TextButton button2 = new TextButton("Model Loader", skin);
        button2.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new ModelLoader());
            }
        });
        table.add(button2);
        table.row();

        TextButton button3 = new TextButton("Shadow Test", skin);
        button3.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new ShadowTest());
            }
        });
        table.add(button3);
        table.row();

        TextButton button4 = new TextButton("Physics Test", skin);
        button4.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new PhysicsTest(game));
            }
        });
        table.add(button4);
        table.row();

        TextButton button5 = new TextButton("Exit", skin);
        button5.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.exit();
            }
        });
        table.add(button5).width(100);
    }

    public void resize (int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void render (float delta) {
        ScreenUtils.clear(Color.DARK_GRAY, true);
        stage.act(delta);
        stage.draw();
    }

    public void dispose() {
        stage.dispose();
    }
}
