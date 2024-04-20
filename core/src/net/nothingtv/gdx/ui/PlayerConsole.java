package net.nothingtv.gdx.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class PlayerConsole extends Table {

    private static PlayerConsole current;
    private final TextArea textArea;
    private final TextField inputField;
    private ConsoleListener listener;

    public PlayerConsole(Skin skin) {
        current = this;
        textArea = new TextArea("", skin);
        textArea.setDisabled(true);
        inputField = new TextField("", skin);
        ScrollPane scrollPane = new ScrollPane(textArea);
        add(scrollPane).width((float)Gdx.graphics.getWidth() / 2).height(80).row();
        add(inputField).fillX();
        pack();
        inputField.addListener(new InputListener() {
            @Override
            public boolean keyTyped(InputEvent event, char character) {
                if (character == '\n' && listener != null) {
                    listener.handle(inputField.getText());
                    inputField.setText("");
                    return true;
                }
                return false;
            }
        });
    }

    public static void setListener(ConsoleListener listener) {
        if (current == null)
            throw new GdxRuntimeException("There is no PlayerConsole yet.");
        current.listener = listener;
    }

    private static void addText(String text) {
        if (current == null)
            throw new GdxRuntimeException("There is no PlayerConsole yet.");
        current.textArea.appendText(text);
    }

    public static void print(String msg) {
        addText(msg);
    }

    public static void println(String msg) {
        print(msg + "\n");
    }

    public static void printf(String fmt, Object ... args) {
        addText(String.format(fmt, args));
    }
}
