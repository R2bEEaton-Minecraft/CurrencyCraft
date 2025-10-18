package cc.spea.currencycraft.gui.ATM;

import cc.spea.currencycraft.CurrencyCraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for setting up a blank debit card with a PIN.
 * This is a simple menu without slots, just for PIN input.
 */
public class ATMSetupMenu extends AbstractContainerMenu {

    public ATMSetupMenu(int windowId, Inventory playerInventory, FriendlyByteBuf data) {
        this(windowId, playerInventory);
    }

    public ATMSetupMenu(int windowId, Inventory playerInventory) {
        super(CurrencyCraft.ATM_SETUP_MENU.get(), windowId);
    }

    @Override
    public boolean stillValid(Player player) {
        return true; // Player can access from anywhere near ATM
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // No quick move needed
    }
}
