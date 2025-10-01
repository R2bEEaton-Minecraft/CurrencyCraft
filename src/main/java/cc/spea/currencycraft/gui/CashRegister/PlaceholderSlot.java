package cc.spea.currencycraft.gui.CashRegister;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class PlaceholderSlot extends Slot {
    private final ResourceLocation placeholder;
    private final RegistryObject<Item> validItem; // <-- NEW: The item allowed in this slot

    public PlaceholderSlot(Container container, int index, int x, int y, @Nullable ResourceLocation placeholder, @Nullable RegistryObject<Item> validItem) {
        super(container, index, x, y);
        this.placeholder = placeholder;
        this.validItem = validItem; // <-- NEW: Store the valid item
    }

    @Nullable
    public ResourceLocation getPlaceholder() {
        return this.placeholder;
    }

    /**
     * This is the core logic. It checks if an item stack can be placed in this slot.
     */
    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        // If no specific item is defined for this slot, allow anything (default behavior).
        if (this.validItem == null) {
            return true;
        }

        // Otherwise, only allow the stack if its item matches our valid item.
        // Using stack.is() is the preferred way to check item equality.
        return stack.is(this.validItem.get());
    }
}