package net.nothingtv.gdx.inventory;

import net.nothingtv.gdx.tools.Int2;

public class GameItemVariance
{
    public GameItem baseItemAsset;
    public Int2 rarityRange = Int2.One;
    public Int2 amountRange = Int2.One;
    public float randomWeight = 1;
    public float probability = 0;
}