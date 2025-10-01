package cc.spea.currencycraft.gui;

import cc.spea.currencycraft.CurrencyCraft;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class VendingMachineMenu extends AbstractContainerMenu {
    private final Container container;

    public VendingMachineMenu(int windowId, Inventory playerInv, Container container) {
        super(CurrencyCraft.VENDING_MACHINE_MENU.get(), windowId);
        this.container = container;

        for (int i = 0; i < this.container.getContainerSize(); i++) {
            this.addSlot(new Slot(container, i, 8 + (i % 3) * 18, 18 + (i / 3) * 18) {
                @Override
                    public boolean mayPlace(ItemStack stack) {
                        // We are duplicating the logic from the BlockEntity here.
                        // This is necessary for the client-side visual feedback.
                        
                        // If this is the payment slot (index 0)
                        if (this.getSlotIndex() >= 12) {
                            // Prevent NullPointerException if the stack is null or empty
                            if (stack == null || stack.isEmpty()) {
                                return false;
                            }
                            
                            // Check if the item from the stack exists as a value in our currency map.
                            return CurrencyCraft.CURRENCY_ITEMS.values().stream()
                                .anyMatch(registryObject -> registryObject.get() == stack.getItem());
                        }
                        return true;
                }
            });
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
        return this.container.stillValid(player);
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
