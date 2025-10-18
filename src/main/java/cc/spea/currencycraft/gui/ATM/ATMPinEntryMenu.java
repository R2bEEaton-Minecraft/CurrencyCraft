package cc.spea.currencycraft.gui.ATM;

import cc.spea.currencycraft.CurrencyCraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for entering PIN to access account with a debit card.
 */
public class ATMPinEntryMenu extends AbstractContainerMenu {

    public ATMPinEntryMenu(int windowId, Inventory playerInventory, FriendlyByteBuf data) {
        this(windowId, playerInventory);
    }

    public ATMPinEntryMenu(int windowId, Inventory playerInventory) {
        super(CurrencyCraft.ATM_PIN_ENTRY_MENU.get(), windowId);
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
