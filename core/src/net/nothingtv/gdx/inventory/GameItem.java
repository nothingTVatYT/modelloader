package net.nothingtv.gdx.inventory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class GameItem {
    public String name;

    public String iconName;
    public transient Drawable icon;
    public int amount = 1;
    public int maxStackAmount = 1;
    public int containerId = -1;
    public int locationId = -1;
    public int rarity = 1;

    public Drawable currentIcon() {
        if (icon == null && !iconName.isEmpty()) {
            icon = new TextureRegionDrawable(new Texture(Gdx.files.internal("inventory/" + iconName)));
        }
        return icon;
    }

    public GameItem() {
    }

    public GameItem(String name, String iconName, int maxStackAmount) {
        this.name = name;
        this.iconName = iconName;
        this.maxStackAmount = maxStackAmount;
    }

    protected void copyFields(GameItem newItem) {
        newItem.name = name;
        newItem.icon = icon;
        newItem.iconName = iconName;
        newItem.amount = amount;
        newItem.maxStackAmount = maxStackAmount;
        newItem.containerId = containerId;
        newItem.locationId = locationId;
        newItem.rarity = rarity;
    }

    protected GameItem copy() {
        GameItem newItem = new GameItem();
        copyFields(newItem);
        return newItem;
    }

    @Override
    public String toString() {
        return "GameItem{" +
                "name='" + name + '\'' +
                ", iconName='" + iconName + '\'' +
                ", icon=" + icon +
                ", amount=" + amount +
                ", maxStackAmount=" + maxStackAmount +
                ", containerId=" + containerId +
                ", locationId=" + locationId +
                ", rarity=" + rarity +
                '}';
    }
}
