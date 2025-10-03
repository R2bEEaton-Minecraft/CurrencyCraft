package cc.spea.currencycraft.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cc.spea.currencycraft.CurrencyCraft;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ModHelpers {
    public static long calculateTotalCurrencyValueInCents(List<ItemStack> items) {
        long totalValue = 0L;

        for (ItemStack stack : items) {
            if (stack.isEmpty()) {
                continue;
            }
            Item item = stack.getItem();
            long itemValue = CurrencyCraft.CURRENCY_VALUES.getOrDefault(item, 0L);
            totalValue += itemValue * stack.getCount();
        }

        return totalValue;
    }

    /**
     * Calculates the minimum number of currency items required to represent a total value.
     * This method accounts for Minecraft's maximum stack size of 64.
     *
     * @param totalValue The total value in cents to be converted into ItemStacks.
     * @return A List of ItemStacks representing the currency, optimized for the fewest items possible.
     */
    public static NonNullList<ItemStack> calculateItemStacksFromCents(long totalValue) {
        NonNullList<ItemStack> result = NonNullList.create();

        if (totalValue <= 0) {
            return result; // Return an empty list if there's no value.
        }

        // To implement a greedy algorithm for minimum change, we must start with the largest denominations.
        // We get the currency map, convert it to a stream of its entries,
        // and sort them in descending order based on their value.
        List<Map.Entry<Item, Long>> sortedCurrencies = CurrencyCraft.CURRENCY_VALUES.entrySet()
                .stream()
                .sorted(Map.Entry.<Item, Long>comparingByValue().reversed())
                .collect(Collectors.toList());

        // Iterate through each currency type, from highest value to lowest.
        for (Map.Entry<Item, Long> entry : sortedCurrencies) {
            Item item = entry.getKey();
            long itemValue = entry.getValue();

            // Skip any invalid currency entries (value of 0 or less) to prevent infinite loops or errors.
            if (itemValue <= 0) {
                continue;
            }

            // If the remaining totalValue is less than the current item's value, we can't use this coin.
            if (totalValue < itemValue) {
                continue;
            }

            // Calculate how many of this currency item we need in total.
            long count = totalValue / itemValue;

            // Handle the Minecraft stack limit (max 64).
            if (count > 0) {
                int fullStacks = (int) (count / 64);
                int remainder = (int) (count % 64);

                // Add all the full stacks to our result list.
                for (int i = 0; i < fullStacks; i++) {
                    result.add(new ItemStack(item, 64));
                }

                // Add the remaining items as a partial stack, if any.
                if (remainder > 0) {
                    result.add(new ItemStack(item, remainder));
                }
            }

            // Update the remaining totalValue we need to make change for.
            totalValue %= itemValue;

            // If we've made exact change, we can stop early.
            if (totalValue == 0) {
                break;
            }
        }

        return result;
    }
}