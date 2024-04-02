package net.nothingtv.gdx.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import net.nothingtv.gdx.inventory.GameItem;
import net.nothingtv.gdx.inventory.GameItemContainer;
import net.nothingtv.gdx.inventory.Inventory;
import net.nothingtv.gdx.inventory.InventoryChangesInfo;

public class InventoryContainerView extends Window {

    private static Skin mySkin;
    private final static Color[] BorderColors = {Color.GRAY, Color.WHITE, Color.GREEN, Color.BLUE, Color.GOLD, new Color(1f, 1f, 1f, 0.7f), new Color(1f, 1f, 1f, 0.3f)};
    private static Drawable[] BorderDrawables;
    private static Drawable EmptyBackground;

    public static class Slot extends Widget {
        private GameItem item;
        public int containerId;
        public int locationId;
        private TextTooltip toolTip;
        private Image image;
        private Label label;
        private Drawable background;

        public Slot() {
            super();
            image = new Image((Drawable) null);
            image.setSize(60, 60);
            label = new Label("", mySkin);
            label.setFontScale(0.7f);
            background = EmptyBackground;
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            super.draw(batch, parentAlpha);
            if (background != null)
                background.draw(batch, getX(), getY(), 64, 64);
            image.setPosition(getX() + 2, getY() + 2);
            image.draw(batch, parentAlpha);
            label.setPosition(getX(), getY()+4);
            label.draw(batch, parentAlpha);
        }

        public void setItem(GameItem item) {
            if (this.item == item)
                return;
            this.item = item;
            if (this.item != null) {
                if (this.item.amount != 1)
                    label.setText(String.valueOf(item.amount));
                image.setDrawable(item.currentIcon());
                int idx = item.rarity;
                if (idx < 0 || idx >= BorderDrawables.length)
                    idx = 1;
                background = BorderDrawables[idx];
                if (toolTip == null) {
                    toolTip = new TextTooltip(item.name, mySkin);
                    addListener(toolTip);
                } else
                    toolTip.getActor().setText(this.item.name);
            }
            else {
                if (toolTip != null) removeListener(toolTip);
                label.setText("");
                background = EmptyBackground;
            }
        }

        @Override
        public float getPrefWidth() {
            return 64;
        }

        @Override
        public float getPrefHeight() {
            return 64;
        }
    }

    private final GameItemContainer container;
    private final Slot[] slots;

    public InventoryContainerView(String name, GameItemContainer container, Skin skin) {
        super(name, skin);
        BorderDrawables = new Drawable[BorderColors.length];
        for (int i = 0; i < BorderDrawables.length; i++)
            BorderDrawables[i] = skin.newDrawable(Inventory.emptyIcon, BorderColors[i]);
        EmptyBackground = BorderDrawables[BorderDrawables.length-1];
        mySkin = skin;
        this.container = container;
        slots = new Slot[container.capacity];
        Inventory.PlayerInventory.addInventoryChangeListener(this::updateUI);
        for (int i = 0; i < container.capacity; i++) {
            slots[i] = new Slot();
        }
        int columns = suggestColumnsForContainerSize(container.capacity);
        for (int i = 0; i < slots.length; i++) {
            if (i > 0 && i % columns == 0)
                row();
            add(slots[i]);
        }
        updateUI(null);
        pack();
    }

    private void updateUI(InventoryChangesInfo info) {
        for (int i = 0; i < slots.length; i++) {
            slots[i].setItem(container.items[i]);
        }
    }

    protected int suggestColumnsForContainerSize(int capacity) {
        if (capacity <= 20) return 4;
        if (capacity <= 30) return 6;
        return 8;
    }
}
