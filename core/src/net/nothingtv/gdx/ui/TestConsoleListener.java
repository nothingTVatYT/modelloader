package net.nothingtv.gdx.ui;

public class TestConsoleListener implements ConsoleListener {
    @Override
    public void handle(String msg) {
        PlayerConsole.println(msg);
    }
}
