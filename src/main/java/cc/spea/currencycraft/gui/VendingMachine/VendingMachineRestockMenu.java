package cc.spea.currencycraft.gui.VendingMachine;

import cc.spea.currencycraft.CurrencyCraft;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class VendingMachineRestockMenu extends AbstractContainerMenu {
    private final Container container;
    private final int productSlots = 12;

    public VendingMachineRestockMenu(int windowId, Inventory playerInv, Container container) {
        super(CurrencyCraft.VENDING_MACHINE_RESTOCK_MENU.get(), windowId);
        this.container = container;

        // Product Slots
        for (int i = 0; i < productSlots; i++) {
            this.addSlot(new Slot(container, i, 8 + (i % 3) * 18, 18 + (i / 3) * 18));
        }

        // Money Slots
        for (int i = 0; i < this.container.getContainerSize() - productSlots; i++) {
            this.addSlot(new Slot(container, productSlots + i, 8 + 7 * 18 + (i % 5) * 18, 18 + (i / 5) * 18) {
                @Override
                    public boolean mayPlace(ItemStack stack) {
                        // If this is a payment slot
                        if (this.getSlotIndex() >= productSlots) {
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
                this.addSlot(new Slot(playerInv, j1 + l * 9 + 9, 36 + j1 * 18, 122 + l * 18));
            }
        }

        for(int i1 = 0; i1 < 9; ++i1) {
            this.addSlot(new Slot(playerInv, i1, 36 + i1 * 18, 180));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        int PRODUCT_SLOTS_COUNT = 12;
        int CURRENCY_SLOTS_COUNT = 25;
        int CUSTOM_SLOTS_TOTAL = PRODUCT_SLOTS_COUNT + CURRENCY_SLOTS_COUNT; // 37
        int PLAYER_INV_START = CUSTOM_SLOTS_TOTAL; // 37
        int PLAYER_HOTBAR_START = PLAYER_INV_START + 27; // 64
        int PLAYER_SLOTS_END = PLAYER_HOTBAR_START + 9; // 73

        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot != null && slot.hasItem()) {
            ItemStack sourceStack = slot.getItem();
            itemstack = sourceStack.copy();

            // Case 1: Player is shift-clicking FROM your custom inventory TO their inventory
            if (slotIndex < PLAYER_INV_START) {
                if (!this.moveItemStackTo(sourceStack, PLAYER_INV_START, PLAYER_SLOTS_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(sourceStack, itemstack);
            }
            // Case 2: Player is shift-clicking FROM their inventory TO your custom inventory
            else {
                // Check if the item is a valid currency item.
                // You should have a helper method for this, maybe referencing your CURRENCY_ITEMS map.
                boolean isCurrency = CurrencyCraft.CURRENCY_ITEMS.values().stream()
                    .anyMatch(ro -> ro.get() == sourceStack.getItem());

                if (isCurrency) {
                    // Try moving currency to the dedicated currency slots first (12-36)
                    if (!this.moveItemStackTo(sourceStack, PRODUCT_SLOTS_COUNT, PLAYER_INV_START, false)) {
                        // If that fails, try moving it to the product slots (0-11)
                        if (!this.moveItemStackTo(sourceStack, 0, PRODUCT_SLOTS_COUNT, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                } else {
                    // It's not currency, so it must be a "product" or ingredient.
                    // Only try to move it into the product slots (0-11).
                    if (!this.moveItemStackTo(sourceStack, 0, PRODUCT_SLOTS_COUNT, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (sourceStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (sourceStack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, sourceStack);
        }

        return itemstack;
    }
}
