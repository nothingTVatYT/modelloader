package net.nothingtv.gdx.inventory;

import net.nothingtv.gdx.tools.Int2;

import java.util.ArrayList;
import java.util.List;

public class InventoryChangesInfo {
    public List<Int2> changedSlots;

    public static InventoryChangesInfo Empty = new InventoryChangesInfo();

    public InventoryChangesInfo() {
        changedSlots = new ArrayList<>();
    }

    public InventoryChangesInfo(int containerId, int locationId) {
        add(containerId, locationId);
    }

    public void add(int containerId, int locationId) {
        if (changedSlots == null)
            changedSlots = new ArrayList<>();
        changedSlots.add(new Int2(containerId, locationId));
    }
}