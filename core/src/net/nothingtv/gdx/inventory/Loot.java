package net.nothingtv.gdx.inventory;

import net.nothingtv.gdx.tools.Int2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public class Loot
{
    public ArrayList<GameItemVariance> items = new ArrayList<>();
    public Int2 numberItemsRange = Int2.One;
    private final Random random = new Random();

    // using Vose's Alias method (https://www.keithschwarz.com/darts-dice-coins/)
    public List<GameItem> createItems()
    {
        ArrayList<GameItem> gameItems = new ArrayList<GameItem>();
        int n = items.size();
        final float[] weightSum = {0};
        items.forEach(v -> weightSum[0] += v.randomWeight);
        if (weightSum[0] == 0)
            weightSum[0] = 1f;
        items.forEach(itemVariance -> itemVariance.probability = itemVariance.randomWeight * n / weightSum[0]);
        int[] alias = new int[n];
        float[] probability = new float[n];
        Stack<Integer> large = new Stack<>();
        Stack<Integer> small = new Stack<>();
        for (int index = 0; index < n; index++)
        {
            if (items.get(index).probability < 1)
                small.push(index);
            else
                large.push(index);
        }
        while (!large.isEmpty() && !small.isEmpty())
        {
            int l = small.pop();
            int g = large.pop();
            probability[l] = items.get(l).probability;
            alias[l] = g;
            items.get(g).probability = items.get(g).probability + items.get(l).probability - 1;
            if (items.get(g).probability < 1)
                small.push(g);
            else
                large.push(g);
        }

        while (!large.isEmpty())
        {
            int g = large.pop();
            items.get(g).probability = 1;
        }

        while (!small.isEmpty())
        {
            int l = small.pop();
            items.get(l).probability = 1;
        }

        int numberItems = random.nextInt(numberItemsRange.x, numberItemsRange.y + 1);
        for (int numberItem = 0; numberItem < numberItems; numberItem++)
        {
            int i = random.nextInt(n);
            GameItemVariance chosenVariant = random.nextFloat() < items.get(i).probability ? items.get(i) : items.get(alias[i]);

            GameItem item = chosenVariant.baseItemAsset.copy();
            item.amount = random.nextInt(chosenVariant.amountRange.x, chosenVariant.amountRange.y+1);
            item.rarity = TripleWeightedRandom(chosenVariant.rarityRange.x, chosenVariant.rarityRange.y);
            gameItems.add(item);
        }

        return gameItems;
    }

    /// return a weighted random number between min and max value inclusive
    /// minValue gets a weight of 1/2 (plus 1 - sum of all weights), the next 1/4, etc.
    /// roughly that means for every max value there are two maxValue-1 and four maxValue-2, etc.
    
    private int DoubledWeightedRandom(int minValue, int maxValue)
    {
        int d = maxValue - minValue + 1;
        if (d == 1)
            return minValue;
        float[] probabilities = new float[d];
        for (int i = 0; i < d; i++)
            probabilities[i] = 1f / (2 << i);
        float roll = random.nextFloat();
        for (int i = 0; i < d; i++)
            if (roll > probabilities[i])
                return minValue + i;
        return minValue;
    }

    /// return a weighted random number between min and max value inclusive
    /// minValue gets a weight of 1/2 + 1/6 (plus 1 - sum of all weights), the next 1/8 + 1/12, etc.
    /// roughly that means for every max value there are three maxValue-1 and nine maxValue-2, etc.
    
    private int TripleWeightedRandom(int minValue, int maxValue)
    {
        int d = maxValue - minValue + 1;
        if (d == 1)
            return minValue;
        float[] probabilities = new float[d];
        for (int i = 0; i < d; i++)
            probabilities[i] = 1f / (2 << (2*i)) + 1f / (6 << i);
        float roll = random.nextFloat();
        for (int i = 0; i < d; i++)
            if (roll > probabilities[i])
                return minValue + i;
        return minValue;
    }

}