package net.nothingtv.gdx.inventory;

import java.io.Serializable;
import java.util.ArrayList;

public class GameItems implements Serializable
{
    public ArrayList<GameItem> items;

    public GameItems() {}
    public void add(GameItem item) {
        if (items == null)
            items = new ArrayList<>();
        items.add(item);
    }
}