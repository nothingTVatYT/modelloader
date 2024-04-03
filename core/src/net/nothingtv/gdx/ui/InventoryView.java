package net.nothingtv.gdx.ui;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import net.nothingtv.gdx.inventory.GameItemContainer;
import net.nothingtv.gdx.inventory.Inventory;

import java.util.HashMap;

public class InventoryView implements InventoryViewListener {

    private final HashMap<Integer, InventoryContainerView> containerViews = new HashMap<>();
    private final Stage stage;
    private final Skin skin;
    private boolean visible;

    public InventoryView(Stage stage, Skin skin) {
        this.stage = stage;
        this.skin = skin;
        Drawable d = Inventory.emptyIcon;
        ImageTextButton.ImageTextButtonStyle slotStyle = new ImageTextButton.ImageTextButtonStyle(d, d, d, skin.getFont("default-font"));
        this.skin.add("default", slotStyle);
        visible = false;
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
            } else {
                stage.addActor(containerViews.get(container.containerId));
            }
        }
        visible = true;
    }

    public void hideInventory() {
        for (InventoryContainerView view : containerViews.values())
            view.remove();
        visible = false;
    }

    public boolean isVisible() {
        return visible;
    }

    public InventoryContainerView showContainer(GameItemContainer container) {
        InventoryContainerView view = containerViews.get(container.containerId);
        if (view == null) {
            view = new InventoryContainerView(container.name, container, skin);
            view.addListener(this);
        }
        stage.addActor(view);
        return view;
    }

    @Override
    public boolean slotDragging(InventoryViewEvent event) {
        return false;
    }

    @Override
    public boolean slotDragDropped(InventoryViewEvent event) {
        // already handled by a view?
        if (event.isHandled()) return true;
        Vector2 pos = new Vector2(event.x, event.y);
        event.slot.localToStageCoordinates(pos);
        Actor actor = stage.hit(pos.x, pos.y, true);
        System.out.printf("A slot is dropped on %s: %s%n", actor, event.slot);
        if (actor instanceof InventoryContainerView.Slot newSlot) {
            if (newSlot.getItem() == null)
                Inventory.PlayerInventory.requestItemLocationChange(event.slot.getItem(), newSlot);
            else Inventory.PlayerInventory.requestItemStack(event.slot.getItem(), newSlot.getItem());
            return true;
        }
        return false;
    }

    @Override
    public boolean handle(Event event) {
        if (!(event instanceof InventoryViewEvent ive))
            return false;
        if (ive.type == InventoryViewEvent.Type.Drop) {
            return slotDragDropped(ive);
        }
        return false;
    }
}
