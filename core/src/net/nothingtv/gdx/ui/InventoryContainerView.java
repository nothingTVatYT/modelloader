package net.nothingtv.gdx.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Pools;
import net.nothingtv.gdx.inventory.GameItem;
import net.nothingtv.gdx.inventory.GameItemContainer;
import net.nothingtv.gdx.inventory.Inventory;
import net.nothingtv.gdx.inventory.InventoryChangesInfo;
import net.nothingtv.gdx.tools.Int2;

public class InventoryContainerView extends Window implements InventoryViewListener {

    private static Skin mySkin;
    private static final float SLOT_WIDTH = 64;
    private static final float SLOT_HEIGHT = 64;
    private static final float SLOT_BORDER_SIZE = 2;
    private static final float MIN_DRAG_DIST2 = 9;
    private final static Color[] BorderColors = {Color.GRAY, Color.WHITE, Color.GREEN, Color.BLUE, Color.GOLD, new Color(1f, 1f, 1f, 0.7f), new Color(1f, 1f, 1f, 0.3f)};
    private static Drawable[] BorderDrawables;
    private static Drawable EmptyBackground;

    public static class Slot extends Widget {
        private GameItem item;
        public int containerId;
        public int locationId;
        private TextTooltip toolTip;
        private final Image image;
        private final Label label;
        private Drawable background;
        private final Vector2 clickPosition = new Vector2();
        private int clickButton;
        private boolean dragging;

        public Slot() {
            super();
            image = new Image((Drawable) null);
            image.setSize(SLOT_WIDTH - 2 * SLOT_BORDER_SIZE, SLOT_HEIGHT - 2 * SLOT_BORDER_SIZE);
            label = new Label("", mySkin);
            label.setFontScale(0.7f);
            background = EmptyBackground;
            setTouchable(Touchable.enabled);
            addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    System.out.printf("event touchDown on %s: %f/%f pointer %d button %d%n", this, x, y, pointer, button);
                    clickPosition.set(x, y);
                    clickButton = button;
                    return true;
                }

                @Override
                public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                    System.out.printf("event touchUp on %s: %f/%f pointer %d button %d dragging %s%n", this, x, y, pointer, button, dragging);
                    if (dragging && button == clickButton) {
                        InventoryViewEvent viewEvent = Pools.obtain(InventoryViewEvent.class);
                        viewEvent.button = clickButton;
                        viewEvent.setLocalPosition(x, y);
                        viewEvent.setStartPosition(clickPosition.x, clickPosition.y);
                        viewEvent.slot = (Slot)event.getTarget();
                        viewEvent.type = InventoryViewEvent.Type.Drop;
                        fire(viewEvent);
                        Pools.free(viewEvent);
                    }
                }

                @Override
                public void touchDragged(InputEvent event, float x, float y, int pointer) {
                    if (item != null && clickPosition.dst2(x, y) > MIN_DRAG_DIST2) {
                        InventoryViewEvent viewEvent = Pools.obtain(InventoryViewEvent.class);
                        viewEvent.button = clickButton;
                        viewEvent.setLocalPosition(x, y);
                        viewEvent.setStartPosition(clickPosition.x, clickPosition.y);
                        viewEvent.slot = (Slot)event.getTarget();
                        viewEvent.type = InventoryViewEvent.Type.Drag;
                        dragging = true;
                        fire(viewEvent);
                        Pools.free(viewEvent);
                    }
                }
            });
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            super.draw(batch, parentAlpha);
            if (background != null)
                background.draw(batch, getX(), getY(), SLOT_WIDTH, SLOT_HEIGHT);
            image.setPosition(getX() + SLOT_BORDER_SIZE, getY() + SLOT_BORDER_SIZE);
            image.draw(batch, parentAlpha);
            label.setPosition(getX(), getY()+4);
            label.draw(batch, parentAlpha);
        }

        public void setItem(GameItem item) {
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
                image.setDrawable(null);
                background = EmptyBackground;
            }
        }

        @Override
        public float getPrefWidth() {
            return SLOT_WIDTH;
        }

        @Override
        public float getPrefHeight() {
            return SLOT_HEIGHT;
        }

        public GameItem getItem() {
            return item;
        }

        @Override
        public String toString() {
            if (item == null)
                return "empty Slot";
            else return "Slot with " + item;
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
            Slot slot = new Slot();
            slot.containerId = container.containerId;
            slot.locationId = i;
            slot.addListener(this);
            slots[i] = slot;
        }
        int columns = suggestColumnsForContainerSize(container.capacity);
        for (int i = 0; i < slots.length; i++) {
            if (i > 0 && i % columns == 0)
                row();
            add(slots[i]);
        }
        updateUI(InventoryChangesInfo.Empty);
        pack();
    }

    private void updateUI(InventoryChangesInfo info) {
        if (info.changedSlots.isEmpty()) {
            updateSlots();
        } else {
            boolean needsUpdate = false;
            for (Int2 contLoc : info.changedSlots) {
                if (contLoc.x == container.containerId) {
                    needsUpdate = true;
                }
            }
            if (needsUpdate)
                updateSlots();
        }
    }

    private void updateSlots() {
        boolean[] slotsHandled = new boolean[slots.length];
        for (int i = 0; i < container.items.length; i++) {
            GameItem item = container.items[i];
            if (item == null)
                continue;
            int locId = item.locationId;
            if (locId < 0 || locId >= slots.length)
                continue;
            slots[locId].setItem(item);
            slotsHandled[locId] = true;
        }
        for (int i = 0; i < slots.length; i++)
            if (!slotsHandled[i])
                slots[i].setItem(null);
    }

    protected int suggestColumnsForContainerSize(int capacity) {
        if (capacity <= 20) return 4;
        if (capacity <= 36) return 6;
        if (capacity <= 48) return 8;
        return 10;
    }

    @Override
    public boolean slotDragging(InventoryViewEvent event) {
        return true;
    }

    @Override
    public boolean slotDragDropped(InventoryViewEvent event) {
        Vector2 pos = new Vector2(event.x, event.y);
        event.slot.localToActorCoordinates(this, pos);
        Actor actor = hit(pos.x, pos.y, true);
        System.out.printf("A slot is dropped on %s: %s%n", actor, event.slot);
        if (actor instanceof Slot newSlot) {
            if (newSlot == event.slot) return true;
            if (newSlot.item == null)
                Inventory.PlayerInventory.requestItemLocationChange(event.slot.item, newSlot);
            else Inventory.PlayerInventory.requestItemStack(event.slot.item, newSlot.item);
            return true;
        }
        return false;
    }

    @Override
    public boolean handle(Event event) {
        if (! (event instanceof InventoryViewEvent ive))
            return false;
        if (ive.type == InventoryViewEvent.Type.Drag)
            return slotDragging(ive);
        else if (ive.type == InventoryViewEvent.Type.Drop)
            return slotDragDropped(ive);
        return false;
    }

}
