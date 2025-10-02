package cc.spea.currencycraft.gui.Wallet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import cc.spea.currencycraft.CurrencyCraft;
import cc.spea.currencycraft.items.Wallet.WalletInventory;

public class WalletMenu extends AbstractContainerMenu {
    private final ItemStack walletStack;
    private final WalletInventory walletInventory;

    private static final int WALLET_SLOTS = WalletInventory.SLOTS;

    // Client-side constructor
    public WalletMenu(int windowId, Inventory playerInventory, FriendlyByteBuf data) {
        this(windowId, playerInventory, data.readItem());
    }

    // Server-side constructor
    public WalletMenu(int windowId, Inventory playerInventory, ItemStack walletStack) {
        super(CurrencyCraft.WALLET_MENU.get(), windowId); // Assumes you have a MenuType registry
        this.walletStack = walletStack;
        this.walletInventory = new WalletInventory(walletStack);
        this.walletInventory.load();

        final int slotSize = 18;
        final int walletXOffset = 8;
        final int walletYOffset = 18;

        // Wallet inventory slots (2 rows of 9)
        for (int row = 0; row < 2; ++row) {
            for (int col = 0; col < 9; ++col) {
                int x = walletXOffset + col * slotSize;
                int y = walletYOffset + row * slotSize;
                this.addSlot(new Slot(this.walletInventory, col + row * 9, x, y) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return CurrencyCraft.CURRENCY_ITEMS.values().stream()
                            .anyMatch(registryObject -> registryObject.get() == stack.getItem());
                    }
                });
            }
        }
        
        final int playerInvXOffset = 8;
        final int playerInvYOffset = 68;
        final int playerHotbarYOffset = 126;

        // Player inventory
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int x = playerInvXOffset + col * slotSize;
                int y = playerInvYOffset + row * slotSize;
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, x, y));
            }
        }

        // Player hotbar
        for (int i = 0; i < 9; ++i) {
            int x = playerInvXOffset + i * slotSize;
            this.addSlot(new Slot(playerInventory, i, x, playerHotbarYOffset));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        // Checks if the player is still holding the wallet. Closes the GUI if not.
        return player.getMainHandItem().equals(this.walletStack) || player.getOffhandItem().equals(this.walletStack);
    }
    
    @Override
    public void removed(Player player) {
        super.removed(player);
        this.walletInventory.save(); // Ensure inventory is saved when closed
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();

            // From wallet to player inventory
            if (index < WALLET_SLOTS) {
                if (!this.moveItemStackTo(slotStack, WALLET_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } 
            // From player inventory to wallet
            else if (!this.moveItemStackTo(slotStack, 0, WALLET_SLOTS, false)) {
                return ItemStack.EMPTY;
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }
}