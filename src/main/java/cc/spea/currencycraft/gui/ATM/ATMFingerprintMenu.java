package cc.spea.currencycraft.gui.ATM;

import cc.spea.currencycraft.CurrencyCraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for fingerprint authentication to disable a debit card.
 * This is a simple menu without special slots.
 */
public class ATMFingerprintMenu extends AbstractContainerMenu {

    public ATMFingerprintMenu(int windowId, Inventory playerInventory, FriendlyByteBuf data) {
        this(windowId, playerInventory);
    }

    public ATMFingerprintMenu(int windowId, Inventory playerInventory) {
        super(CurrencyCraft.ATM_FINGERPRINT_MENU.get(), windowId);

        // Add player inventory slots for visual consistency
        final int slotSize = 18;
        final int playerInvXOffset = 8;
        final int playerInvYOffset = 84;
        final int playerHotbarYOffset = 142;

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
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
