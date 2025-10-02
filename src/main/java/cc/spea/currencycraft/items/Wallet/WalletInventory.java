package cc.spea.currencycraft.items.Wallet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

public class WalletInventory extends SimpleContainer {
    public static final int SLOTS = 18; // 2 rows * 9 slots
    private final ItemStack walletStack;

    public WalletInventory(ItemStack walletStack) {
        super(SLOTS);
        this.walletStack = walletStack;
    }

    // This method is called whenever the inventory is changed. We use it to save.
    @Override
    public void setChanged() {
        super.setChanged();
        save();
    }

    /**
     * Loads the inventory from the ItemStack's NBT data.
     */
    public void load() {
        if (this.walletStack.hasTag()) {
            CompoundTag nbt = this.walletStack.getTag();
            if (nbt != null && nbt.contains("Inventory", 9)) { // 9 is the NBT type for ListTag
                // Use the built-in fromTag method to load the items
                this.fromTag(nbt.getList("Inventory", 10)); // 10 is the NBT type for CompoundTag
            }
        }
    }

    /**
     * Saves the inventory to the ItemStack's NBT data.
     */
    public void save() {
        CompoundTag nbt = this.walletStack.getOrCreateTag();
        // Use the built-in createTag method to generate the ListTag
        nbt.put("Inventory", this.createTag());
    }
}