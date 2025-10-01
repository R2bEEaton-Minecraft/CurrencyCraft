package cc.spea.currencycraft.blocks;

import cc.spea.currencycraft.CurrencyCraft;
import cc.spea.currencycraft.gui.VendingMachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class VendingMachineBlockEntity extends BaseContainerBlockEntity {
    private NonNullList<ItemStack> items = NonNullList.withSize(12, ItemStack.EMPTY);

    public VendingMachineBlockEntity(BlockPos pos, BlockState state) {
        super(CurrencyCraft.VENDING_MACHINE_BLOCK_ENTITY.get(), pos, state);
    }

    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    public void clearContent() {
        this.getItems().clear();
    }

    public int getContainerSize() {
        return 12;
    }

    public ItemStack getItem(int slot) {
      return this.getItems().get(slot);
    }

    public ItemStack removeItem(int p_59613_, int p_59614_) {
      ItemStack itemstack = ContainerHelper.removeItem(this.getItems(), p_59613_, p_59614_);
      if (!itemstack.isEmpty()) {
         this.setChanged();
      }
      return itemstack;
   }

    public ItemStack removeItemNoUpdate(int p_59630_) {
      return ContainerHelper.takeItem(this.getItems(), p_59630_);
   }

    public void setItem(int p_59616_, ItemStack p_59617_) {
      this.getItems().set(p_59616_, p_59617_);
      if (p_59617_.getCount() > this.getMaxStackSize()) {
         p_59617_.setCount(this.getMaxStackSize());
      }

      this.setChanged();
   }

    public boolean stillValid(Player player) {
      return Container.stillValidBlockEntity(this, player);
   }

    public boolean isEmpty() {
      return this.getItems().stream().allMatch(ItemStack::isEmpty);
   }

    protected Component getDefaultName() {
      return Component.translatable("gui.currencycraft.vending_machine");
    }

    public void load(CompoundTag p_155349_) {
      super.load(p_155349_);
      this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
         ContainerHelper.loadAllItems(p_155349_, this.items);
   }

   protected void saveAdditional(CompoundTag p_187489_) {
      super.saveAdditional(p_187489_);
        ContainerHelper.saveAllItems(p_187489_, this.items);
   }

    protected AbstractContainerMenu createMenu(int windowId, Inventory inventory) {
      return new VendingMachineMenu(windowId, inventory, this);
    }

    @Override
    public void setChanged() {
        // We only want to send updates from the server side.
        // The client-side world is a "logical" world and doesn't have authority to send updates.
        if (this.level != null && !this.level.isClientSide) {
            // This is the crucial line. It notifies the client that the block at this position has been updated.
            // This triggers the server to call getUpdateTag() and send a packet to the client,
            // which then calls handleUpdateTag() on the client.
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
        
        // Always call the super method to ensure the chunk is marked for saving.
        super.setChanged();
    }


    // This is the "on load" part for the client. It provides the NBT data
    // that will be sent to the client in the update packet.
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag nbt = new CompoundTag();
        saveAdditional(nbt); // Use our existing save logic
        return nbt;
    }

    // This receives the update packet on the client and reads the NBT data.
    @Override
    public void handleUpdateTag(CompoundTag nbt) {
        load(nbt); // Use our existing load logic
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        // If the target slot is our "payment" slot (index 0)
        if (index >= 12) {
            // Prevent NullPointerException if the stack is null or empty
            if (stack == null || stack.isEmpty()) {
                return false;
            }
            
            // Check if the item from the stack exists as a value in our currency map.
            return CurrencyCraft.CURRENCY_ITEMS.values().stream()
                .anyMatch(registryObject -> registryObject.get() == stack.getItem());
        }
        
        // Fallback for any other case
        return super.canPlaceItem(index, stack);
    }
}
