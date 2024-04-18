package net.nothingtv.gdx.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import net.nothingtv.gdx.objects.PlayerInfo;

public class PlayerInfoView extends Table {

    private final Label playerName;
    private final Image playerIcon;
    private final Label fps;
    private final Image compass;
    private final Label location;
    private final PlayerInfo playerInfo;

    public PlayerInfoView(PlayerInfo player, Skin skin) {
        this.playerInfo = player;
        playerName = new Label(player.displayName, skin);
        fps = new Label("0000", skin);
        location = new Label("0000/0000", skin);
        playerIcon = new Image(player.icon);
        compass = new Image(new Texture(Gdx.files.internal("textures/arrow.png")));
        compass.setSize(24, 24);
        compass.setOrigin(Align.center);
        add(playerName).colspan(3).row();
        add(playerIcon).colspan(3).maxSize(64).row();
        add(compass).maxSize(24);
        add(location);
        add(fps);
        pack();
    }

    @Override
    public void act(float delta) {
        fps.setText(Gdx.graphics.getFramesPerSecond());
        Vector3 currentPos = playerInfo.playerObject.getPosition();
        location.setText(String.format("%4.0f/%4.0f", currentPos.x, currentPos.z));
        compass.setRotation(playerInfo.playerObject.getRotation().getAngleAround(0, 1, 0));
        super.act(delta);
    }
}
