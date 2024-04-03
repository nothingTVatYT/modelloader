package net.nothingtv.gdx.ui;


import com.badlogic.gdx.scenes.scene2d.EventListener;

public interface InventoryViewListener extends EventListener {
    boolean slotDragging(InventoryViewEvent event);
    boolean slotDragDropped(InventoryViewEvent event);
}
