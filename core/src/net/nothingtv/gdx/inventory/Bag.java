package net.nothingtv.gdx.inventory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class Bag extends GameItem
{
    public transient GameItemContainer gameItemContainer;
    public transient Drawable iconOpenedBag;
    public String iconOpenedBagName;
    public int capacity;
    public boolean isOpen;

    public Bag() {}

    public Bag(String name, String iconName, int capacity, String iconOpenedBagName) {
        super(name, iconName, 1);
        this.capacity = capacity;
        this.isOpen = false;
        this.iconOpenedBagName = iconOpenedBagName;
    }

    public Drawable currentIcon()
    {
        if (iconOpenedBag == null && !iconOpenedBagName.isEmpty())
            iconOpenedBag = new TextureRegionDrawable(new Texture(Gdx.files.internal("inventory/" + iconOpenedBagName)));
        return isOpen ? iconOpenedBag : super.currentIcon();
    }

    @Override
    protected void copyFields(GameItem newItem) {
        super.copyFields(newItem);
        if (newItem instanceof Bag newBag) {
            newBag.gameItemContainer = gameItemContainer;
            newBag.capacity = capacity;
            newBag.iconOpenedBagName = iconOpenedBagName;
            newBag.iconOpenedBag = iconOpenedBag;
            newBag.isOpen = isOpen;
        }
    }

    @Override
    protected Bag copy() {
        Bag newBag = new Bag();
        copyFields(newBag);
        return newBag;
    }
}