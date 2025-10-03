package cc.spea.currencycraft.items.Wallet;

import cc.spea.currencycraft.CurrencyCraft;
import cc.spea.currencycraft.gui.Wallet.WalletMenu;
import cc.spea.currencycraft.helper.ModHelpers;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WalletItem extends Item implements DyeableLeatherItem {
    public WalletItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack walletStack = player.getItemInHand(hand);

        // We only want to open the GUI on the server side
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            
            // Create a MenuProvider
            MenuProvider menuProvider = new MenuProvider() {
                @Override
                public @NotNull Component getDisplayName() {
                    // This is the title of the GUI
                    return walletStack.getHoverName(); 
                }

                @Nullable
                @Override
                public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory playerInventory, @NotNull Player player) {
                    return new WalletMenu(windowId, playerInventory, walletStack);
                }
            };

            // Open the menu for the player
            NetworkHooks.openScreen(serverPlayer, menuProvider, (buffer) -> buffer.writeItem(walletStack));
        }

        return InteractionResultHolder.success(walletStack);
    }

     @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        long totalValue = getTotalValue(stack);
        if (totalValue == 0) return;
        double total = totalValue / 100.0f;
        String formattedValue = String.format("%.2f", total);

        tooltip.add(Component.translatable("tooltip.currencycraft.wallet.total", formattedValue));
    }

    /**
     * Calculates the total currency value by reading the wallet's NBT data.
     * @param walletStack The ItemStack of the wallet.
     * @return The total value in cents.
     */
    private long getTotalValue(ItemStack walletStack) {
        CompoundTag tag = walletStack.getTag();

        if (tag == null || !tag.contains("Inventory", 9)) { // 9 = ListTag type
            return 0L;
        }

        ListTag inventoryTag = tag.getList("Inventory", 10); // 10 = CompoundTag type
        NonNullList<ItemStack> itemStacks = NonNullList.create();
        for (int i = 0; i < inventoryTag.size(); i++) {
            CompoundTag itemTag = inventoryTag.getCompound(i);
            ItemStack itemStack = ItemStack.of(itemTag); // Recreate the ItemStack from its NBT

            if (itemStack.isEmpty()) {
                continue;
            }

            itemStacks.add(itemStack);
        }

        return ModHelpers.calculateTotalCurrencyValueInCents(itemStacks);
    }
}