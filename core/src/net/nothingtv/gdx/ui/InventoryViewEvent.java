package net.nothingtv.gdx.ui;

import com.badlogic.gdx.scenes.scene2d.Event;

public class InventoryViewEvent extends Event {

    public enum Type {None, Drag, Drop, Use}
    public int button;
    public float x;
    public float y;
    public float originX;
    public float originY;
    public Type type;

    public InventoryContainerView.Slot slot;
    public void setLocalPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void setStartPosition(float x, float y) {
        this.originX = x;
        this.originY = y;
    }
}
