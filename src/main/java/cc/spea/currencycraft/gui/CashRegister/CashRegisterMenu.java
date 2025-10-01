package cc.spea.currencycraft.gui.CashRegister;

import cc.spea.currencycraft.CurrencyCraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CashRegisterMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;
    
    private record PlaceholderData(ResourceLocation texture, RegistryObject<Item> item) {}

    // --- START OF NEW CODE ---
    private static final List<String> COIN_NAMES = List.of(
            "one_cent_coin", "two_cent_coin", "five_cent_coin", "ten_cent_coin",
            "twenty_cent_coin", "fifty_cent_coin", "one_unit_coin", "two_unit_coin"
    );

    private static final List<String> NOTE_NAMES = List.of(
            "five_unit_note", "ten_unit_note", "twenty_unit_note", "fifty_unit_note",
            "one_hundred_unit_note", "two_hundred_unit_note", "five_hundred_unit_note"
    );
    // --- END OF NEW CODE ---


    public CashRegisterMenu(int windowId, Inventory playerInv, Container container, ContainerData data) {
        super(CurrencyCraft.CASH_REGISTER_MENU.get(), windowId);
        this.container = container;
        checkContainerSize(container, 30);
        this.data = data;

        int slotIndex = 0;
        final int X_START = 8;
        final int Y_START = 18 + 12;
        final int SLOT_SPACING = 18;

        // NEW LAYOUT: 7, 7, 8, 8
        // Rows 1 & 2: Notes (7 slots each)
        for (int row = 0; row < 2; ++row) {
            for (int col = 0; col < 7; ++col) {
                addCustomSlot(container, slotIndex, X_START + col * SLOT_SPACING, Y_START + row * SLOT_SPACING);
                slotIndex++;
            }
        }

        // Rows 3 & 4: Coins (8 slots each)
        for (int row = 0; row < 2; ++row) {
            for (int col = 0; col < 8; ++col) {
                addCustomSlot(container, slotIndex, X_START + col * SLOT_SPACING, Y_START + (row + 2) * SLOT_SPACING);
                slotIndex++;
            }
        }


        // Player Inventory (Unchanged)
        for(int l = 0; l < 3; ++l) {
            for(int j1 = 0; j1 < 9; ++j1) {
                this.addSlot(new Slot(playerInv, j1 + l * 9 + 9, 8 + j1 * 18, 116 + l * 18));
            }
        }

        for(int i1 = 0; i1 < 9; ++i1) {
            this.addSlot(new Slot(playerInv, i1, 8 + i1 * 18, 174));
        }
    }
    
    private void addCustomSlot(Container container, int index, int x, int y) {
        // Query the central layout for all necessary info
        RegistryObject<Item> validItem = CashRegisterLayout.getValidItemForSlot(index);
        ResourceLocation placeholder = CashRegisterLayout.getPlaceholderTextureForSlot(index);
        this.addSlot(new PlaceholderSlot(container, index, x, y, placeholder, validItem));
    }

    public Container getContainer() {
        return this.container;
    }
    
    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player p_40199_, int p_40200_) {
        // ... (This method remains unchanged)
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(p_40200_);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (p_40200_ < this.container.getContainerSize()) {
                if (!this.moveItemStackTo(itemstack1, this.container.getContainerSize(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, this.container.getContainerSize(), false)) {
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

    public long getTotalValueInCents() {
        int upper = this.data.get(0);
        int lower = this.data.get(1);
        // The '& 0xFFFFFFFFL' is crucial to prevent sign extension on the lower bits.
        return ((long) upper << 32) | (lower & 0xFFFFFFFFL);
    }    
}