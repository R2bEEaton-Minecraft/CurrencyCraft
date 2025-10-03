package cc.spea.currencycraft.event;

import cc.spea.currencycraft.CurrencyCraft;
import cc.spea.currencycraft.villager.CurrencyCraftVillagers;
import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = CurrencyCraft.MODID)
public class ModEvents {

    @SubscribeEvent
    public static void addCustomTrades(VillagerTradesEvent event) {
        if (event.getType() == CurrencyCraftVillagers.BANKER.get()) {
            Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();

            // This list is safe because it only runs when the event fires.
            final List<Supplier<Item>> mobHeadsList = ImmutableList.of(
                    () -> Items.SKELETON_SKULL,
                    () -> Items.WITHER_SKELETON_SKULL,
                    () -> Items.ZOMBIE_HEAD,
                    () -> Items.CREEPER_HEAD,
                    () -> Items.PIGLIN_HEAD,
                    () -> Items.DRAGON_HEAD
            );

            // --- Level 1 Trades (Novice) ---
            trades.get(1).add(new BuyRandomItemFromTagTrade(ItemTags.PLANKS, 64, () -> new ItemStack(getCurrency("ten_cent_coin"), 4), 16, 2, 0.05f));
            trades.get(1).add(new BuyRandomItemFromTagTrade(ItemTags.LOGS, 16, () -> new ItemStack(getCurrency("twenty_cent_coin"), 5), 16, 5, 0.05f));
            trades.get(1).add(createBuyTrade(() -> Items.COAL, 8, () -> new ItemStack(getCurrency("fifty_cent_coin"), 2), 16, 5, 0.05f));

            // --- Level 2 Trades (Apprentice) ---
            trades.get(2).add(createBuyTrade(() -> Items.COPPER_INGOT, 6, () -> new ItemStack(getCurrency("one_unit_coin"), 4), 16, 10, 0.05f));
            trades.get(2).add(createBuyTrade(() -> Items.IRON_INGOT, 5, () -> new ItemStack(getCurrency("one_unit_coin"), 5), 16, 10, 0.05f));

            // --- Level 3 Trades (Journeyman) ---
            trades.get(3).add(createBuyTrade(() -> Items.GOLD_INGOT, 5, () -> new ItemStack(getCurrency("five_unit_note"), 5), 12, 15, 0.05f));
            trades.get(3).add(new SellRandomItemFromTagTrade(() -> new ItemStack(getCurrency("fifty_unit_note"), 10), ItemTags.MUSIC_DISCS, 1, 3, 15, 0.05f));

            // --- Level 4 Trades (Expert) ---
            trades.get(4).add(createBuyTrade(() -> Items.EMERALD, 4, () -> new ItemStack(getCurrency("fifty_unit_note"), 8), 12, 20, 0.05f));
            trades.get(4).add(createBuyTrade(() -> Items.DIAMOND, 2, () -> new ItemStack(getCurrency("fifty_unit_note"), 10), 12, 25, 0.05f));
            trades.get(4).add(new SellRandomItemFromListTrade(() -> new ItemStack(getCurrency("two_hundred_unit_note"), 10), mobHeadsList, 1, 1, 30, 0.05f));

            // --- Level 5 Trades (Master) ---
            trades.get(5).add(createBuyTrade(() -> Items.NETHERITE_INGOT, 1, () -> new ItemStack(getCurrency("two_hundred_unit_note"), 10), 5, 30, 0.05f));
            trades.get(5).add(createBuyTrade(() -> Items.NETHER_STAR, 1, () -> new ItemStack(getCurrency("two_hundred_unit_note"), 20), 3, 50, 0.05f));
            trades.get(5).add(createSellTrade(() -> new ItemStack(getCurrency("one_hundred_unit_note"), 25), () -> new ItemStack(Items.ENCHANTED_GOLDEN_APPLE), 1, 30, 0.05f));
            trades.get(5).add(createSellTrade(() -> new ItemStack(getCurrency("two_hundred_unit_note"), 25), () -> new ItemStack(Items.TOTEM_OF_UNDYING), 1, 30, 0.05f));
        }
    }

    private static Item getCurrency(String name) {
        return CurrencyCraft.CURRENCYCRAFT_ITEMS.get(name).get();
    }
    
    // --- Helper methods now accept Suppliers ---

    private static VillagerTrades.ItemListing createBuyTrade(Supplier<ItemLike> itemToBuy, int count, Supplier<ItemStack> currencyToGive, int maxUses, int xp, float priceMultiplier) {
        return (pTrader, pRandom) -> new MerchantOffer(
                new ItemStack(itemToBuy.get(), count),
                currencyToGive.get(),
                maxUses, xp, priceMultiplier
        );
    }
    
    private static VillagerTrades.ItemListing createSellTrade(Supplier<ItemStack> currencyToTake, Supplier<ItemStack> itemToSell, int maxUses, int xp, float priceMultiplier) {
        return (pTrader, pRandom) -> new MerchantOffer(
                currencyToTake.get(),
                itemToSell.get(),
                maxUses, xp, priceMultiplier
        );
    }

    // --- Custom trade classes now store Suppliers ---

    private static class BuyRandomItemFromTagTrade implements VillagerTrades.ItemListing {
        private final TagKey<Item> itemTag;
        private final int inputCount;
        private final Supplier<ItemStack> outputStackSupplier;
        private final int maxUses;
        private final int xp;
        private final float priceMultiplier;

        public BuyRandomItemFromTagTrade(TagKey<Item> itemTag, int inputCount, Supplier<ItemStack> outputStackSupplier, int maxUses, int xp, float priceMultiplier) {
            this.itemTag = itemTag;
            this.inputCount = inputCount;
            this.outputStackSupplier = outputStackSupplier;
            this.maxUses = maxUses;
            this.xp = xp;
            this.priceMultiplier = priceMultiplier;
        }

        @Override
        public MerchantOffer getOffer(Entity pTrader, RandomSource pRandom) {
            Optional<List<Item>> itemsInTag = BuiltInRegistries.ITEM.getTag(this.itemTag).map(tag -> tag.stream().map(holder -> holder.value()).toList());

            if (itemsInTag.isPresent() && !itemsInTag.get().isEmpty()) {
                List<Item> items = itemsInTag.get();
                Item randomItem = items.get(pRandom.nextInt(items.size()));
                ItemStack inputStack = new ItemStack(randomItem, this.inputCount);
                return new MerchantOffer(inputStack, this.outputStackSupplier.get(), this.maxUses, this.xp, this.priceMultiplier);
            }
            return null;
        }
    }
    
    private static class SellRandomItemFromTagTrade implements VillagerTrades.ItemListing {
        private final Supplier<ItemStack> priceSupplier;
        private final TagKey<Item> resultItemTag;
        private final int resultCount;
        private final int maxUses;
        private final int xp;
        private final float priceMultiplier;

        public SellRandomItemFromTagTrade(Supplier<ItemStack> priceSupplier, TagKey<Item> resultItemTag, int resultCount, int maxUses, int xp, float priceMultiplier) {
            this.priceSupplier = priceSupplier;
            this.resultItemTag = resultItemTag;
            this.resultCount = resultCount;
            this.maxUses = maxUses;
            this.xp = xp;
            this.priceMultiplier = priceMultiplier;
        }

        @Override
        public MerchantOffer getOffer(Entity pTrader, RandomSource pRandom) {
            Optional<List<Item>> itemsInTag = BuiltInRegistries.ITEM.getTag(this.resultItemTag).map(tag -> tag.stream().map(holder -> holder.value()).toList());

            if (itemsInTag.isPresent() && !itemsInTag.get().isEmpty()) {
                List<Item> items = itemsInTag.get();
                Item randomItem = items.get(pRandom.nextInt(items.size()));
                ItemStack resultStack = new ItemStack(randomItem, this.resultCount);
                return new MerchantOffer(this.priceSupplier.get(), resultStack, this.maxUses, this.xp, this.priceMultiplier);
            }
            return null;
        }
    }
    
    private static class SellRandomItemFromListTrade implements VillagerTrades.ItemListing {
        private final Supplier<ItemStack> priceSupplier;
        private final List<Supplier<Item>> resultItemSuppliers;
        private final int resultCount;
        private final int maxUses;
        private final int xp;
        private final float priceMultiplier;

        public SellRandomItemFromListTrade(Supplier<ItemStack> priceSupplier, List<Supplier<Item>> resultItemSuppliers, int resultCount, int maxUses, int xp, float priceMultiplier) {
            this.priceSupplier = priceSupplier;
            this.resultItemSuppliers = resultItemSuppliers;
            this.resultCount = resultCount;
            this.maxUses = maxUses;
            this.xp = xp;
            this.priceMultiplier = priceMultiplier;
        }

        @Override
        public MerchantOffer getOffer(Entity pTrader, RandomSource pRandom) {
            if (!this.resultItemSuppliers.isEmpty()) {
                Supplier<Item> randomItemSupplier = this.resultItemSuppliers.get(pRandom.nextInt(this.resultItemSuppliers.size()));
                ItemStack resultStack = new ItemStack(randomItemSupplier.get(), this.resultCount);
                return new MerchantOffer(this.priceSupplier.get(), resultStack, this.maxUses, this.xp, this.priceMultiplier);
            }
            return null;
        }
    }
}