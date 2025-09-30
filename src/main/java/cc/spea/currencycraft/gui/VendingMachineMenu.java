package cc.spea.currencycraft.gui;

import cc.spea.currencycraft.CurrencyCraft;
import cc.spea.currencycraft.blocks.VendingMachineBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class VendingMachineMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;

    public VendingMachineMenu(int windowId, Inventory playerInv, VendingMachineBlockEntity be) {
        super(CurrencyCraft.VENDING_MACHINE_MENU.get(), windowId);
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

        // Our vending machine slots (12)
        ItemStackHandler handler = be.getInventory();
        for (int i = 0; i < handler.getSlots(); i++) {
            this.addSlot(new SlotItemHandler(handler, i, 8 + (i % 3) * 18, 18 + (i / 3) * 18));
        }

        // Player inventory slots
        for(int l = 0; l < 3; ++l) {
            for(int j1 = 0; j1 < 9; ++j1) {
                this.addSlot(new Slot(playerInv, j1 + l * 9 + 9, 8 + j1 * 18, 103 + l * 18));
            }
        }

        for(int i1 = 0; i1 < 9; ++i1) {
            this.addSlot(new Slot(playerInv, i1, 8 + i1 * 18, 161));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, CurrencyCraft.VENDING_MACHINE_BLOCK.get());
    }

    public ItemStack quickMoveStack(Player player, int slotIndex) {
      ItemStack itemstack = ItemStack.EMPTY;
      Slot slot = this.slots.get(slotIndex);
      if (slot != null && slot.hasItem()) {
         ItemStack itemstack1 = slot.getItem();
         itemstack = itemstack1.copy();
         if (slotIndex < 12) {
            if (!this.moveItemStackTo(itemstack1, 12, this.slots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(itemstack1, 0, 12, false)) {
            return ItemStack.EMPTY;
         }

         if (itemstack1.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }
      }

      return itemstack;
   }
}
