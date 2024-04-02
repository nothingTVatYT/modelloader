package net.nothingtv.gdx.inventory;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class GameItemContainer
{
    public String name;
    public int containerId;
    public int capacity;
    public GameItem[] items;

    public GameItemContainer(String name, int containerId, int capacity)
    {
        this.name = name;
        this.containerId = containerId;
        this.capacity = capacity;
        this.items = new GameItem[capacity];
    }

    public GameItem add(GameItem item)
    {
        if (item == null)
            return null;
        if (item.locationId < 0 || item.locationId >= capacity)
            item.locationId = 0;
        if (items[item.locationId] != null)
        {
            var index = nextFreeLocation();
            if (index < 0)
                return null;
            item.locationId = index;
        }
        
        items[item.locationId] = item;
        item.containerId = containerId;
        return item;
    }

    public GameItem remove(GameItem item)
    {
        if (item == null)
            return null;
        int index;
        for (index = 0; index < items.length; index++)
            if (item.equals(items[index]))
                break;
        if (index >= items.length)
            return null;
        items[index] = null;
        return item;
    }

    public boolean isFull()
    {
        return Arrays.stream(items).noneMatch(Objects::isNull);
    }

    public List<GameItem> findItem(GameItem item)
    {
        return Arrays.stream(items).filter(existingItem -> existingItem != null && existingItem.name.equals(item.name) && existingItem.rarity == item.rarity).toList();
    }
    
    public int nextFreeLocation()
    {
        for (int index = 0; index < items.length; index++)
            if (items[index] == null)
                return index;
        return -1;
    }
}