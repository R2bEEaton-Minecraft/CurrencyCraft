package cc.spea.currencycraft.gui.VendingMachine;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;

import cc.spea.currencycraft.blocks.VendingMachine.VendingMachineBlockEntity;

public class VendingMachineMenuProvider implements MenuProvider {

    private final VendingMachineBlockEntity blockEntity;
    private final boolean isRestockMenu;

    public VendingMachineMenuProvider(VendingMachineBlockEntity blockEntity, boolean isRestockMenu) {
        this.blockEntity = blockEntity;
        this.isRestockMenu = isRestockMenu;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return blockEntity.getDisplayName();
    }

    @Override
    public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory playerInventory, @NotNull Player player) {
        if (this.isRestockMenu) {
            return new VendingMachineRestockMenu(windowId, playerInventory, this.blockEntity);
        } else {
            return new VendingMachinePurchaseMenu(windowId, playerInventory, this.blockEntity);
        }
    }
}