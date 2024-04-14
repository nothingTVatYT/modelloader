package net.nothingtv.gdx.modelloader;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ScreenUtils;
import net.nothingtv.gdx.inventory.Inventory;
import net.nothingtv.gdx.terrain.TerrainInstance;
import net.nothingtv.gdx.tools.GeneralSettings;
import net.nothingtv.gdx.tools.TestSettings;

public class SelectScreen extends ScreenAdapter {
    private final Stage stage;
    private final Skin skin;

    public SelectScreen (Game game) {
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        Table table = new Table();
        table.setFillParent(true);
        table.pad(10).defaults().space(20);
        stage.addActor(table);

        skin = new Skin(Gdx.files.internal("data/uiskin.json"));

        table.add(createMenuButton("Terrain Test", () -> game.setScreen(new TerrainTest()))).row();

        table.add(createMenuButton("Model Loader", () -> game.setScreen(new ModelLoader()))).row();

        table.add(createMenuButton("Shadow Test", () -> game.setScreen(new ShadowTest()))).row();

        table.add(createMenuButton("_Game/Physics Test", () -> game.setScreen(new PhysicsTest(game)))).row();

        table.add(createMenuButton("Online Game Test", () -> game.setScreen(new LoginScreen(game)))).row();

        table.add(createMenuButton("Controller Test", () -> game.setScreen(new ControllerTest(game)))).row();

        table.add(createMenuButton("Update Inventory", Inventory::writeItems)).row();

        table.add(createMenuButton("Exit", () -> Gdx.app.exit())).width(100);

        Table settingsPanel = new Table();

        SelectBox<GeneralSettings.Setting> graphicsSettingBox = new SelectBox<>(skin);
        graphicsSettingBox.setItems(GeneralSettings.Setting.Low, GeneralSettings.Setting.Mid, GeneralSettings.Setting.High, GeneralSettings.Setting.Ultra);
        graphicsSettingBox.setSelected(GeneralSettings.current.setting);
        graphicsSettingBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GeneralSettings.select(graphicsSettingBox.getSelected());
            }
        });
        settingsPanel.add(graphicsSettingBox);

        SelectBox<TerrainInstance.DrawMode> terrainSettingBox = new SelectBox<>(skin);
        terrainSettingBox.setItems(TerrainInstance.DrawMode.NoOptimization, TerrainInstance.DrawMode.FrustumCulling, TerrainInstance.DrawMode.ProceduralNode);
        terrainSettingBox.setSelected(TestSettings.terrainDrawMode);
        terrainSettingBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                TestSettings.terrainDrawMode = terrainSettingBox.getSelected();
            }
        });

        settingsPanel.add(terrainSettingBox);
        settingsPanel.pack();

        stage.addActor(settingsPanel);
    }

    private TextButton createMenuButton(String text, Clicked l) {
        TextButton button = new TextButton(text, skin);
        if (text.startsWith("_")) {
            Label label = button.getLabel();
            label.setText(text.substring(1));
            label.setFontScale(1.2f * label.getFontScaleX());
        }
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
