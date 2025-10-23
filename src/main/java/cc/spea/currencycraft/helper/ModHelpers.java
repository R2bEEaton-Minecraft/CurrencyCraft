package cc.spea.currencycraft.helper;

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
     * Calculates the minimum number of ItemStacks required to represent a total value.
     * This method uses dynamic programming to find the optimal combination of currency items,
     * accounting for Minecraft's maximum stack size of 64.
     *
     * @param totalValue The total value in cents to be converted into ItemStacks.
     * @return A List of ItemStacks representing the currency, optimized for the fewest item stacks possible.
     */
    public static NonNullList<ItemStack> calculateItemStacksFromCents(long totalValue) {
        NonNullList<ItemStack> result = NonNullList.create();

        if (totalValue <= 0) {
            return result; // Return an empty list if there's no value.
        }

        // Get sorted currencies (descending order by value)
        List<Map.Entry<Item, Long>> sortedCurrencies = CurrencyCraft.CURRENCY_VALUES.entrySet()
                .stream()
                .sorted(Map.Entry.<Item, Long>comparingByValue().reversed())
                .collect(Collectors.toList());

        // Filter out invalid entries
        sortedCurrencies.removeIf(entry -> entry.getValue() <= 0);

        if (sortedCurrencies.isEmpty()) {
            return result;
        }

        // For very large values, use a hybrid approach
        // First use largest denominations to bring value down to a manageable size
        long remainingValue = totalValue;
        Map.Entry<Item, Long> largestDenom = sortedCurrencies.get(0);
        long largestValue = largestDenom.getValue();

        // If value is extremely large, handle the bulk with largest denomination
        if (remainingValue > largestValue * 64 * 100) {
            long bulkCount = remainingValue / largestValue;
            long bulkValue = bulkCount * largestValue;

            // Add bulk stacks
            int fullStacks = (int) (bulkCount / 64);
            int remainder = (int) (bulkCount % 64);

            for (int i = 0; i < fullStacks; i++) {
                result.add(new ItemStack(largestDenom.getKey(), 64));
            }
            if (remainder > 0) {
                result.add(new ItemStack(largestDenom.getKey(), remainder));
            }

            remainingValue -= bulkValue;

            if (remainingValue == 0) {
                return result;
            }
        }

        // For the remaining value, use dynamic programming to find optimal combination
        int dpLimit = Math.min((int) remainingValue, 100000);

        if (remainingValue > dpLimit) {
            // If still too large after bulk processing, use greedy for the rest
            result.addAll(greedyChange(remainingValue, sortedCurrencies));
        } else {
            // Use DP for optimal solution
            result.addAll(optimalChange((int) remainingValue, sortedCurrencies));
        }

        return result;
    }

    /**
     * Uses dynamic programming to find the optimal change with minimum number of ItemStacks.
     * This considers both the denomination values and Minecraft's stack size limit.
     */
    private static NonNullList<ItemStack> optimalChange(int value, List<Map.Entry<Item, Long>> sortedCurrencies) {
        // dp[i] = minimum number of stacks needed to make value i
        int[] dp = new int[value + 1];
        // parent[i] = the denomination used to reach value i optimally
        int[] parent = new int[value + 1];
        // count[i] = how many items of that denomination were used
        int[] count = new int[value + 1];

        // Initialize with maximum value
        for (int i = 1; i <= value; i++) {
            dp[i] = Integer.MAX_VALUE;
        }

        dp[0] = 0;

        // Build up the DP table
        for (int currentValue = 0; currentValue < value; currentValue++) {
            if (dp[currentValue] == Integer.MAX_VALUE) continue;

            // Try each denomination
            for (int denomIdx = 0; denomIdx < sortedCurrencies.size(); denomIdx++) {
                long denomValue = sortedCurrencies.get(denomIdx).getValue();
                if (denomValue > Integer.MAX_VALUE) continue;
                int denom = (int) denomValue;

                // Try adding 1 to 64 of this denomination
                for (int itemCount = 1; itemCount <= 64; itemCount++) {
                    long nextValue = currentValue + (long) denom * itemCount;
                    if (nextValue > value) break;

                    int stacksNeeded = dp[currentValue] + 1;

                    if (stacksNeeded < dp[(int) nextValue]) {
                        dp[(int) nextValue] = stacksNeeded;
                        parent[(int) nextValue] = denomIdx;
                        count[(int) nextValue] = itemCount;
                    }
                }
            }
        }

        // Reconstruct the solution
        NonNullList<ItemStack> result = NonNullList.create();
        int currentValue = value;

        while (currentValue > 0 && dp[currentValue] != Integer.MAX_VALUE) {
            int denomIdx = parent[currentValue];
            int itemCount = count[currentValue];
            Map.Entry<Item, Long> entry = sortedCurrencies.get(denomIdx);

            result.add(new ItemStack(entry.getKey(), itemCount));

            currentValue -= (int) (entry.getValue() * itemCount);
        }

        // If we couldn't make exact change, fall back to greedy
        if (currentValue > 0 || dp[value] == Integer.MAX_VALUE) {
            return greedyChange(value, sortedCurrencies);
        }

        return result;
    }

    /**
     * Greedy algorithm fallback for very large values or when DP fails.
     */
    private static NonNullList<ItemStack> greedyChange(long totalValue, List<Map.Entry<Item, Long>> sortedCurrencies) {
        NonNullList<ItemStack> result = NonNullList.create();

        for (Map.Entry<Item, Long> entry : sortedCurrencies) {
            Item item = entry.getKey();
            long itemValue = entry.getValue();

            if (itemValue <= 0 || totalValue < itemValue) {
                continue;
            }

            long count = totalValue / itemValue;

            if (count > 0) {
                int fullStacks = (int) (count / 64);
                int remainder = (int) (count % 64);

                for (int i = 0; i < fullStacks; i++) {
                    result.add(new ItemStack(item, 64));
                }

                if (remainder > 0) {
                    result.add(new ItemStack(item, remainder));
                }
            }

            totalValue %= itemValue;

            if (totalValue == 0) {
                break;
            }
        }

        return result;
    }
}