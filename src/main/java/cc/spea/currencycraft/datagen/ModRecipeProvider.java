package cc.spea.currencycraft.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import cc.spea.currencycraft.CurrencyCraft;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
    // Data from your original snippet
    private static final int[] DENOMINATIONS = {
        1, 2, 5, 10, 20, 50, 100,
        200, 500, 1000, 2000, 5000,
        10000, 20000, 50000
    };
    private static final String[] DENOMINATION_NAMES = {
        "one_cent_coin", "two_cent_coin", "five_cent_coin", "ten_cent_coin",
        "twenty_cent_coin", "fifty_cent_coin", "one_unit_coin", "two_unit_coin",
        "five_unit_note", "ten_unit_note", "twenty_unit_note", "fifty_unit_note",
        "one_hundred_unit_note", "two_hundred_unit_note", "five_hundred_unit_note"
    };

    public ModRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> consumer) {
        generateExchangeDownRecipes(consumer);
        generateExchangeUpRecipes(consumer);
    }

    /**
     * Generates recipes for breaking a higher value currency into a lower value one.
     * e.g., 1 Fifty Unit Note -> 5 Ten Unit Notes
     */
    private void generateExchangeDownRecipes(Consumer<FinishedRecipe> consumer) {
        for (int i = DENOMINATIONS.length - 1; i >= 0; i--) {
            for (int j = i - 1; j >= 0; j--) {
                // Check if the higher denomination is perfectly divisible by the lower one
                if ((float) DENOMINATIONS[i] / DENOMINATIONS[j] % 1 == 0) {
                    Item higherValueItem = getItemByName(DENOMINATION_NAMES[i]);
                    Item lowerValueItem = getItemByName(DENOMINATION_NAMES[j]);
                    int count = DENOMINATIONS[i] / DENOMINATIONS[j];

                    ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, lowerValueItem, count)
                            .requires(higherValueItem)
                            .group(CurrencyCraft.MODID + "." + DENOMINATION_NAMES[i] + "_exchange_down")
                            .unlockedBy("has_" + DENOMINATION_NAMES[i], has(higherValueItem))
                            .save(consumer, ResourceLocation.fromNamespaceAndPath(CurrencyCraft.MODID,
                                    DENOMINATION_NAMES[j] + "_from_" + DENOMINATION_NAMES[i]));

                    // The 'break' ensures we only make a recipe for the *next lowest* divisible denomination
                    break;
                }
            }
        }
    }

    /**
     * Generates all valid combinations of smaller currency that add up to a larger one.
     * e.g., 2x Two Cents + 1x One Cent -> 1x Five Cents
     */
    private void generateExchangeUpRecipes(Consumer<FinishedRecipe> consumer) {
        // A counter to ensure unique recipe IDs for multiple combinations for the same item
        AtomicInteger recipeCounter = new AtomicInteger(0);

        for (int i = 0; i < DENOMINATIONS.length; i++) {
            List<Integer> combination = new ArrayList<>();
            findCombinations(DENOMINATIONS[i], DENOMINATIONS[i], 0, combination, 0, consumer, recipeCounter);
        }
    }

    /**
     * Recursive method to find combinations, converted from your 'generate' method.
     */
    private void findCombinations(int originalTargetValue, int remainingTarget, int startIndex, List<Integer> currentCombination, int coinCount, Consumer<FinishedRecipe> consumer, AtomicInteger counter) {
        if (remainingTarget == 0) {
            if (coinCount > 1 && coinCount <= 9) {
                // --- CHANGE ---
                // Pass the originalTargetValue to the save method
                saveCombinationRecipe(originalTargetValue, currentCombination, consumer, counter.getAndIncrement());
            }
            return;
        }

        if (remainingTarget < 0 || coinCount >= 9) {
            return;
        }

        for (int i = startIndex; i < DENOMINATIONS.length; i++) {
            // Prevent using a coin of the same or higher value as the target
            if (DENOMINATIONS[i] > remainingTarget) continue;
            
            currentCombination.add(DENOMINATIONS[i]);
            findCombinations(originalTargetValue, remainingTarget - DENOMINATIONS[i], i, currentCombination, coinCount + 1, consumer, counter);
            currentCombination.remove(currentCombination.size() - 1);
        }
    }

    /**
     * Builds and saves a single shapeless recipe from a found combination.
     */
    private void saveCombinationRecipe(int originalTargetValue, List<Integer> ingredients, Consumer<FinishedRecipe> consumer, int recipeId) {
        // This line is now safe because originalTargetValue is a valid denomination (e.g., 5, 10, etc.)
        String resultItemName = DENOMINATION_NAMES[getIndex(originalTargetValue)];
        Item resultItem = getItemByName(resultItemName);

        ShapelessRecipeBuilder builder = ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, resultItem, 1);
        
        for (int ingredientValue : ingredients) {
            builder.requires(getItemByName(DENOMINATION_NAMES[getIndex(ingredientValue)]));
        }

        String recipeName = resultItemName + "_crafting_" + recipeId;
        
        builder.group(CurrencyCraft.MODID + "." + resultItemName + "_exchange_up");
        
        builder.unlockedBy("has_" + DENOMINATION_NAMES[getIndex(ingredients.get(0))], has(getItemByName(DENOMINATION_NAMES[getIndex(ingredients.get(0))])))
            .save(consumer, ResourceLocation.fromNamespaceAndPath(CurrencyCraft.MODID, recipeName));
    }


    // --- HELPER METHODS ---

    private Item getItemByName(String name) {
        return CurrencyCraft.CURRENCY_ITEMS.get(name).get();
    }

    private int getIndex(int num) {
        for (int i = 0; i < DENOMINATIONS.length; i++) {
            if (num == DENOMINATIONS[i]) return i;
        }
        return -1;
    }
}