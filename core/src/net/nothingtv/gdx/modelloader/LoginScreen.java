package net.nothingtv.gdx.modelloader;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import net.nothingtv.game.network.client.GameClient;

public class LoginScreen extends BasicSceneManagerScreen {
    private TextField loginField;
    private TextField passwordField;
    private TextButton loginButton;
    private Label messageLabel;
    private final GameClient gameClient;

    public LoginScreen(Game game) {
        super(game);

        gameClient = new GameClient(GameClient.GameClientConfig.localTestConfig) {
            @Override
            public void loginFailed(String message) {
                messageLabel.setText("Login failed");
            }

            @Override
            public void versionMismatch(short serverVersion) {
                messageLabel.setText("Server runs a different version (" + serverVersion + ")");
            }

            @Override
            protected String getLogin() {
                return loginField.getText();
            }

            @Override
            protected String getPassword() {
                return passwordField.getText();
            }

            @Override
            public String chooseServer(String allServers) {
                messageLabel.setText("using server " + allServers);
                return super.chooseServer(allServers);
            }

        };
    }

    @Override
    protected void initUI() {
        super.initUI();

        loginField = new TextField("", skin);
        passwordField = new TextField("", skin);
        passwordField.setPasswordCharacter('*');
        passwordField.setPasswordMode(true);
        loginButton = new TextButton("Login", skin);
        loginButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                tryLogin();
            }
        });
        loginField.addListener(event -> {
            checkLoginFields();
            return false;
        });
        passwordField.addListener(event -> {
            checkLoginFields();
            return false;
        });
        messageLabel = new Label("", skin);

        Table loginPanel = new Table(skin);
        loginPanel.setFillParent(true);
        loginPanel.pad(10).defaults().space(20);
        loginPanel.add(new Label("Login", skin));
        loginPanel.add(loginField).row();
        loginPanel.add(new Label("Password", skin));
        loginPanel.add(passwordField).row();
        loginPanel.add(loginButton).colspan(2).row();
        loginPanel.add(messageLabel).colspan(2).row();

        stage.addActor(loginPanel);
        stage.setKeyboardFocus(loginField);

        checkLoginFields();
    }

    private void checkLoginFields() {
        loginButton.setDisabled(loginField.getText().isBlank() || passwordField.getText().isBlank());
    }

    private void tryLogin() {
        Thread thr = new Thread(gameClient);
        thr.setDaemon(true);
        thr.start();
    }
}
