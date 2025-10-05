package cc.spea.currencycraft.gui.VendingMachine;

import cc.spea.currencycraft.CurrencyCraft;
import cc.spea.currencycraft.blocks.VendingMachine.VendingMachineBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class VendingMachinePurchaseMenu extends AbstractContainerMenu {
    private final Container container;

    public VendingMachinePurchaseMenu(int windowId, Inventory playerInv, Container container) {
        super(CurrencyCraft.VENDING_MACHINE_PURCHASE_MENU.get(), windowId);
        this.container = container;
    }

    // --- NEW ---: Provides a safe way for the screen to access the BlockEntity
    public VendingMachineBlockEntity getBlockEntity() {
        if (this.container instanceof VendingMachineBlockEntity) {
            return (VendingMachineBlockEntity) this.container;
        }
        // This should not happen
        throw new IllegalStateException("Container is not a VendingMachineBlockEntity!");
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player p_38941_, int p_38942_) {
        return null;
    }
}