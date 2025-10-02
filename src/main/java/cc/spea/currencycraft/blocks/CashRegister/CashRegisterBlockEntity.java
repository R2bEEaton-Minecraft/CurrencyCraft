package cc.spea.currencycraft.blocks.CashRegister;

import cc.spea.currencycraft.CurrencyCraft;
import cc.spea.currencycraft.gui.CashRegister.CashRegisterLayout;
import cc.spea.currencycraft.gui.CashRegister.CashRegisterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.LockCode;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.RegistryObject;

public class CashRegisterBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    private NonNullList<ItemStack> items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);

    private static final int totalSlots = 15 * 2;

    private static final int[] ALL_SLOTS = new int[totalSlots]; 
    static {
        for (int i = 0; i < ALL_SLOTS.length; i++) {
            ALL_SLOTS[i] = i;
        }
    }

    public CashRegisterBlockEntity(BlockPos pos, BlockState state) {
        super(CurrencyCraft.CASH_REGISTER_BLOCK_ENTITY.get(), pos, state);
    }

    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    public void clearContent() {
        this.getItems().clear();
    }

    public int getContainerSize() {
        return CashRegisterBlockEntity.totalSlots;
    }

    public ItemStack getItem(int slot) {
        return this.getItems().get(slot);
    }

    public ItemStack removeItem(int p_59613_, int p_59614_) {
        ItemStack itemstack = ContainerHelper.removeItem(this.getItems(), p_59613_, p_59614_);
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
    }

    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    public boolean isEmpty() {
        return this.getItems().stream().allMatch(ItemStack::isEmpty);
    }

    protected Component getDefaultName() {
        return Component.translatable("block.currencycraft.cash_register");
    }

    public void load(CompoundTag tag) {
        super.load(tag);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items);
    }

    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, this.items);
    }

    protected AbstractContainerMenu createMenu(int windowId, Inventory inventory) {
        return new CashRegisterMenu(windowId, inventory, this, this.dataAccess);
    }

    public ContainerData getContainerData() {
        return this.dataAccess;
    }

    @Override
    public int[] getSlotsForFace(Direction dir) {
        if (dir == Direction.DOWN) {
            return new int[0];
        } else {
            return ALL_SLOTS;
        }
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        // Get the required item for this slot from our central layout helper.
        RegistryObject<Item> validItem = CashRegisterLayout.getValidItemForSlot(index);
        
        // If the layout defines a specific item for this slot...
        if (validItem != null) {
            // ...then the incoming stack is only valid if its item matches.
            return stack.is(validItem.get());
        }
        
        // If for some reason the layout doesn't specify an item, fall back to the default behavior.
        return super.canPlaceItem(index, stack);
    }

    @Override
    public boolean canTakeItem(Container container, int slot, ItemStack stack) {
        return false;
    }

    @Override
    public boolean canPlaceItemThroughFace(int p_19235_, ItemStack p_19236_, Direction p_19237_) {
        return this.canPlaceItem(p_19235_, p_19236_);
    }

    @Override
    public boolean canTakeItemThroughFace(int p_19239_, ItemStack p_19240_, Direction p_19241_) {
        return false;
    }

    @Override
    protected net.minecraftforge.items.IItemHandler createUnSidedHandler() {
        return new net.minecraftforge.items.wrapper.SidedInvWrapper(this, Direction.UP);
    }

    public long calculateTotalCurrencyValueInCents() {
        long totalValue = 0L; // Use a long for the total

        for (ItemStack stack : this.items) {
            if (stack.isEmpty()) {
                continue;
            }

            Item item = stack.getItem();
            
            // The value from the map is now a long (in cents)
            long itemValue = CurrencyCraft.CURRENCY_VALUES.getOrDefault(item, 0L);
            
            totalValue += itemValue * stack.getCount();
        }

        return totalValue;
    }

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            long total = calculateTotalCurrencyValueInCents();
            return switch (index) {
                // We split the 64-bit long into two 32-bit integers.
                case 0 -> (int) (total >> 32); // Upper 32 bits
                case 1 -> (int) total;         // Lower 32 bits
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // This is a read-only value from the client's perspective, so we don't need to do anything here.
        }

        @Override
        public int getCount() {
            // We are syncing two integer values.
            return 2;
        }
    };
    
    public void setLock(LockCode code) {
        CompoundTag tag = new CompoundTag();
        code.addToTag(tag);
        CompoundTag currentState = new CompoundTag();
        this.saveAdditional(currentState);
        currentState.merge(tag);
        this.load(currentState);
        this.setChanged();
    }

    public LockCode getLock() {
        CompoundTag tag = new CompoundTag();
        super.saveAdditional(tag);
        LockCode savedLock = LockCode.fromTag(tag);
        return savedLock;
    }
}
