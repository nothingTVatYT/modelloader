package net.nothingtv.gdx.inventory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import net.nothingtv.gdx.ui.InventoryContainerView;

import java.util.*;

public class Inventory {
    private List<GameItem> _allKnownItems = new ArrayList<>();
    private final HashMap<String, GameItem> _allItems = new HashMap<>();
    public ArrayList<GameItem> itemsList;
    private final HashMap<Integer, GameItemContainer> _containers = new HashMap<>();
    public static final String[] RarityName = {"Simple", "Normal", "Good", "Rare", "Epic", "Unknown"};
    public static Drawable emptyIcon = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("assets/textures/box-dark.png"))));
    public static Inventory PlayerInventory;
    private final HashSet<InventoryChangeListener> listeners = new HashSet<>();

    public void addInventoryChangeListener(InventoryChangeListener l) {
        listeners.add(l);
    }

    public static void init() {
        if (PlayerInventory == null)
            PlayerInventory = new Inventory();
    }

    private Inventory() {
        indexAllItems();
        // TODO: Load inventory from file or network package

        _containers.put(0, new GameItemContainer("Backpack",0, 20));
        _containers.put(1, new GameItemContainer("Belt Bag", 1, 6));
        _containers.put(3, new GameItemContainer("Pocket", 3, 2));

        var c0 = _containers.get(0);
        var beltBag = _containers.get(1);

        itemsList = new ArrayList<>();
        itemsList.addAll(List.of(new GameItem[]{
                //newGameItemInstance("Bearflower", c0, 0, 34),
                newGameItemInstance("Letter", c0, 1, 1, -1),
                newGameItemInstance("Bread", c0, 1, 30, -1),
                newGameItemInstance("Unfinished Bag", c0, 4, 1, -1),
                newGameItemInstance("Leather Bag", c0, 5, 1, -1),
                newGameItemInstance("Medal", c0, 6, 2, -1),
                newGameItemInstance("Medal", c0, 10, 1, -1),
                newGameItemInstance("Medal", beltBag, 0, 1, -1),
                newGameItemInstance("Medal", c0, 7, 5, 1),
                newGameItemInstance("Medal", c0, 8, 5, 2),
                newGameItemInstance("Medal", c0, 9, 5, 3),
                newGameItemInstance("Broken Medal", beltBag, 1, 1, -1)
        }));

        PlayerInventory = this;
    }

    private void indexAllItems() {
        GameItems items = new Json().fromJson(GameItems.class, Gdx.files.internal("assets/_allItems.json"));
        if (items != null) {
            _allKnownItems.addAll(items.items);
            for (GameItem item : _allKnownItems)
                _allItems.put(item.name, item);
        } else {
            System.err.println("There are no items defined.");
        }
    }

    public static void writeItems() {
        GameItems items = new GameItems();
        items.add(new GameItem("Bread", "bread2.png", 10));
        items.add(new GameItem("Bear Flower", "bearflower.png", 20));
        items.add(new Bag("Leather Bag", "Leather-Bag-icon.png", 6, "Leather-Bag-Open-icon.png"));
        items.add(new GameItem("Unfinished Bag", "Leather-Bag-Open-icon.png", 1));
        items.add(new GameItem("Medal", "medal.png", 50));
        items.add(new GameItem("Broken Medal", "medalBroken.png", 50));
        items.add(new GameItem("Letter", "paper.png", 50));
        new Json(JsonWriter.OutputType.json).toJson(items, Gdx.files.local("assets/_allItems.json"));
    }

    /*
    private void IndexAllItems()
    {
        // rebuild index of all items
        try
        {
            var path = $"{Globals.ProjectFolder}/Content/Definitions/Items";
            var allKnownItems = new GameItems();
            foreach (var assetFile in GetAllFilesRecursive(path))
            {
                if (assetFile.EndsWith("_allItems.json"))
                    continue;
                var asset = Content.Load<JsonAsset>(assetFile);
                if (asset != null)
                    allKnownItems.Items.Add(asset);
            }

            AllKnownItemsAsset.SetInstance(allKnownItems);
            AllKnownItemsAsset.Save();
            _allKnownItems.Clear();
            _allKnownItems.AddRange(allKnownItems.Items);
        }
        catch (Exception e)
        {
            Debug.Log(e);
        }
        _allKnownItems.Clear();
        if (AllKnownItemsAsset.Instance is GameItems gameItems)
            _allKnownItems.AddRange(gameItems.Items);
#endif
        _allItems = new Dictionary<string, GameItem>();
        foreach (var assetItem in _allKnownItems)
        {
            if (assetItem.Instance is GameItem item)
                _allItems[item.Name] = item;
        }
    }*/

    public Iterable<GameItemContainer> getContainers() {
        return Collections.unmodifiableCollection(_containers.values());
    }

    public GameItem newGameItemInstance(String name, GameItemContainer container, int locationId, int amount,
                                        int rarity) {
        var item = newGameItemInstance(name, 0, locationId, amount, rarity);
        return container.add(item);
    }

    public GameItem newGameItemInstance(String name, int containerId, int locationId, int amount, int rarity) {
        GameItem itemTemplate = _allItems.get(name);
        GameItem item = itemTemplate.copy();
        item.containerId = containerId;
        item.locationId = locationId;
        item.amount = amount;
        if (rarity >= 0)
            item.rarity = rarity;

        return item;
    }

    public void rejectItemInView(GameItem item) {
        InventoryChangesInfo changes = new InventoryChangesInfo();
        Suggestion suggestion = new Suggestion();
        if (suggestItemLocation(item, suggestion)) {
            if (suggestion.stack != null) {
                _containers.get(item.containerId).remove(item);
                requestItemStack(item, suggestion.stack);
                changes.add(suggestion.stack.containerId, suggestion.stack.locationId);
            } else {
                item.locationId = suggestion.locationId;
                _containers.get(suggestion.containerId).add(_containers.get(item.containerId).remove(item));
                changes.add(item.containerId, item.locationId);
            }
        }

        fireInventoryChanged(InventoryChangesInfo.Empty);
    }

    private void fireInventoryChanged(InventoryChangesInfo info) {
        for (InventoryChangeListener l : listeners)
            l.inventoryChanged(info);
    }

    public boolean requestItemLocationChange(GameItem item, InventoryContainerView.Slot newSlot) {
        InventoryChangesInfo info = new InventoryChangesInfo(item.containerId, item.locationId);
        item.locationId = newSlot.locationId;
        if (item.containerId != newSlot.containerId) {
            _containers.get(newSlot.containerId).add(_containers.get(item.containerId).remove(item));
        }
        info.add(newSlot.containerId, newSlot.locationId);
        fireInventoryChanged(info);
        return true;
    }

    public boolean requestItemPickup(GameItem item) {
        Suggestion suggestion = new Suggestion();
        if (suggestItemLocation(item, suggestion)) {
            if (suggestion.stack != null) {
                suggestion.stack.amount++;
                fireInventoryChanged(new InventoryChangesInfo(suggestion.stack.containerId, suggestion.stack.locationId));
            } else {
                item.locationId = suggestion.locationId;
                itemsList.add(_containers.get(suggestion.containerId).add(item));
                fireInventoryChanged(new InventoryChangesInfo(item.containerId, item.locationId));
            }
            return true;
        }

        return false;
    }

    public boolean requestItemUse(GameItem item) {
        if (item instanceof Bag bag) {
            bag.isOpen = !bag.isOpen;
            return true;
        }
        if (item.amount > 1)
            item.amount--;
        else {
            _containers.get(item.containerId).remove(item);
            itemsList.remove(item);
        }

        fireInventoryChanged(new InventoryChangesInfo(item.containerId, item.locationId));
        return true;
    }

    public boolean requestItemStack(GameItem item, GameItem stack) {
        if (!item.name.equals(stack.name) || item.rarity != stack.rarity) return false;
        if (stack.maxStackAmount == 1) return false;
        var numberItems = item.amount;
        if (stack.amount + numberItems > stack.maxStackAmount)
            numberItems = stack.maxStackAmount - stack.amount;
        item.amount -= numberItems;
        stack.amount += numberItems;
        if (item.amount == 0) {
            _containers.get(item.containerId).remove(item);
            itemsList.remove(item);

        }
        var changes = new InventoryChangesInfo();
        changes.add(item.containerId, item.locationId);
        changes.add(stack.containerId, stack.locationId);
        fireInventoryChanged(changes);
        return true;
    }

    public GameItem requestItemUnstack(GameItem item, int amount) {
        if (amount > 0 && item.amount > amount) {
            GameItem newItem = item.copy();
            newItem.amount = amount;
            item.amount -= amount;
            itemsList.add(newItem);
            return newItem;
        }
        fireInventoryChanged(InventoryChangesInfo.Empty);
        return null;
    }

    static class Suggestion {
        int containerId;
        int locationId;
        GameItem stack;
    }

    private boolean suggestItemLocation(GameItem item, Suggestion suggestion) {
        if (item.maxStackAmount > 1) {
            for (GameItemContainer container : _containers.values()) {
                for (GameItem same : container.findItem(item)) {
                    if (same.amount < same.maxStackAmount) {
                        suggestion.containerId = same.containerId;
                        suggestion.locationId = same.locationId;
                        suggestion.stack = same;
                        return true;
                    }
                }
            }
        }

        suggestion.stack = null;
        for (var container : _containers.values()) {
            if (!container.isFull()) {
                suggestion.containerId = container.containerId;
                suggestion.locationId = container.nextFreeLocation();
                return true;
            }
        }

        suggestion.containerId = -1;
        suggestion.locationId = -1;
        return false;
    }
}
