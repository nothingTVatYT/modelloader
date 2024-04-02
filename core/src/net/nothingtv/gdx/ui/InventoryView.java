package net.nothingtv.gdx.ui;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import net.nothingtv.gdx.inventory.GameItemContainer;
import net.nothingtv.gdx.inventory.Inventory;

import java.util.HashMap;

public class InventoryView {

    private final HashMap<Integer, InventoryContainerView> containerViews = new HashMap<>();
    private final Stage stage;
    private final Skin skin;

    public InventoryView(Stage stage, Skin skin) {
        this.stage = stage;
        this.skin = skin;
        Drawable d = Inventory.emptyIcon;
        ImageTextButton.ImageTextButtonStyle slotStyle = new ImageTextButton.ImageTextButtonStyle(d, d, d, skin.getFont("default-font"));
        this.skin.add("default", slotStyle);
    }

    public void showPlayerInventory() {
        showInventory(Inventory.PlayerInventory);
    }

    public void showInventory(Inventory inventory) {
        float y = 0;
        for (GameItemContainer container : inventory.getContainers()) {
            if (!containerViews.containsKey(container.containerId)) {
                InventoryContainerView view = showContainer(container);
                containerViews.put(container.containerId, view);
                view.setY(y);
                y += view.getPrefHeight();
            }
        }
    }

    public InventoryContainerView showContainer(GameItemContainer container) {
        InventoryContainerView view = containerViews.get(container.containerId);
        if (view == null) {
            view = new InventoryContainerView(container.name, container, skin);
        }
        stage.addActor(view);
        return view;
    }
}
