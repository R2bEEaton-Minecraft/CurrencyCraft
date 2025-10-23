// VendingMachineBlockEntity

package cc.spea.currencycraft.blocks.VendingMachine;

import java.util.List;

import cc.spea.currencycraft.CurrencyCraft;
import cc.spea.currencycraft.helper.ModHelpers;
import cc.spea.currencycraft.sounds.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.LockCode;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class VendingMachineBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    private NonNullList<ItemStack> items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
    private long insertedValueInCents = 0L;
    private int ejectTimer = 0;
    private static final int EJECT_DELAY_TICKS = 600;
    private boolean outOfOrder = false;

    private final int productSlots = 12;
    private static final int totalSlots = 37;

    // --- NEW ---: Array to store prices for each product slot in cents
    private long[] productPrices = new long[productSlots];

    private static final int[] ALL_SLOTS = new int[totalSlots]; 
    static {
        for (int i = 0; i < ALL_SLOTS.length; i++) {
            ALL_SLOTS[i] = i;
        }
    }

    public VendingMachineBlockEntity(BlockPos pos, BlockState state) {
        super(CurrencyCraft.VENDING_MACHINE_BLOCK_ENTITY.get(), pos, state);
    }

    // --- NEW ---: Getter for a product's price
    public long getPriceInCents(int slot) {
        if (slot >= 0 && slot < this.productSlots) {
            return this.productPrices[slot];
        }
        return 0L;
    }

    // --- NEW ---: Setter for a product's price
    public void setPriceInCents(int slot, long price) {
        if (slot >= 0 && slot < this.productSlots) {
            this.productPrices[slot] = price;
            this.setChanged(); // Mark as dirty to save and sync
        }
    }

    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    public void clearContent() {
        this.getItems().clear();
    }

    public int getContainerSize() {
        return VendingMachineBlockEntity.totalSlots;
    }

    public int getProductSlots() {
        return this.productSlots;
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
        return Component.translatable("block.currencycraft.vending_machine");
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items);
        
        if (tag.contains("InsertedValueInCents")) {
            this.insertedValueInCents = tag.getLong("InsertedValueInCents");
        }

        if (tag.contains("EjectTimer")) {
            this.ejectTimer = tag.getInt("EjectTimer");
        }

        if (tag.contains("OutOfOrder")) {
            this.outOfOrder = tag.getBoolean("OutOfOrder");
        }

        // --- NEW ---: Load product prices
        if (tag.contains("ProductPrices", 12)) { // 12 = LongArrayTag ID
            long[] loadedPrices = tag.getLongArray("ProductPrices");
            // Ensure the loaded array is the correct size, in case it was saved with a different size
            if (loadedPrices.length == this.productSlots) {
                this.productPrices = loadedPrices;
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, this.items);

        tag.putLong("InsertedValueInCents", this.insertedValueInCents);
        tag.putInt("EjectTimer", this.ejectTimer);
        tag.putBoolean("OutOfOrder", this.outOfOrder);

        // --- NEW ---: Save product prices
        tag.putLongArray("ProductPrices", this.productPrices);
    }

    @Override
    public void setChanged() {
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
        super.setChanged();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public void handleUpdateTag(CompoundTag nbt) {
        load(nbt);
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
        if (index >= getProductSlots()) {
            if (stack == null || stack.isEmpty()) {
                return false;
            }
            return CurrencyCraft.CURRENCY_ITEMS.values().stream()
                    .anyMatch(registryObject -> registryObject.get() == stack.getItem());
        }
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

    public boolean addCurrency(ItemStack currencyStack) {
        if (currencyStack.isEmpty()) {
            return false;
        }

        long value = ModHelpers.calculateTotalCurrencyValueInCents(List.of(currencyStack));
        this.insertedValueInCents += value;
        this.ejectTimer = EJECT_DELAY_TICKS;
        this.setChanged();

        // Play different sounds for coins vs notes (custom sounds)
        if (this.level != null && !this.level.isClientSide) {
            // Notes are 500+ cents (5+ units), coins are less
            if (value >= 500) {
                // Note sound - bill acceptor
                this.level.playSound(null, this.worldPosition, ModSounds.VENDING_MACHINE_INSERT_NOTE.get(),
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.7F, 1.0F);
            } else {
                // Coin sound - coin slot
                this.level.playSound(null, this.worldPosition, ModSounds.VENDING_MACHINE_INSERT_COIN.get(),
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.7F, 1.0F);
            }
        }

        return true;
    }
    
    public void dropCurrencyContents() {
        if (this.level != null && !this.level.isClientSide) {
            BlockState blockState = this.getBlockState();
            Direction facingDirection = blockState.getValue(HorizontalDirectionalBlock.FACING);
            BlockPos dropPosition = this.getBlockPos().relative(facingDirection);
            Containers.dropContents(this.level, dropPosition, ModHelpers.calculateItemStacksFromCents(this.insertedValueInCents));

            // Play change ejection sound (custom sound)
            this.level.playSound(null, this.worldPosition, ModSounds.VENDING_MACHINE_CHANGE.get(),
                net.minecraft.sounds.SoundSource.BLOCKS, 0.7F, 1.0F);
        }
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (this.ejectTimer > 0) {
            this.ejectTimer--;
            
            if (this.ejectTimer == 0 && this.insertedValueInCents > 0) {
                this.dropCurrencyContents();
                this.insertedValueInCents = 0;
                this.setChanged();
            }
        }
    }

    public long calculateTotalCurrencyValueInCents() {
        return this.insertedValueInCents;
    }

    public boolean isOutOfOrder() {
        return this.outOfOrder;
    }

    public void setOutOfOrder(boolean outOfOrder) {
        this.outOfOrder = outOfOrder;
        this.setChanged();
    }

    public void clearOutOfOrder() {
        if (this.outOfOrder) {
            this.outOfOrder = false;
            this.setChanged();
        }
    }

    /**
     * Checks if the given currency stacks can fit in the money inventory.
     * Returns true if all items can be added, false if any would be dropped.
     */
    private boolean canFitCurrency(NonNullList<ItemStack> currencyStacks) {
        // Create a copy of the money inventory to simulate insertion
        NonNullList<ItemStack> tempInventory = NonNullList.create();
        for (int i = 12; i < this.items.size(); i++) {
            tempInventory.add(this.items.get(i).copy());
        }

        // Try to insert each currency stack into the temporary inventory
        for (ItemStack stackToAdd : currencyStacks) {
            if (stackToAdd.isEmpty()) {
                continue;
            }

            ItemStack stackToInsert = stackToAdd.copy();

            // PASS 1: Try to merge with existing stacks
            for (int i = 0; i < tempInventory.size(); i++) {
                ItemStack slotStack = tempInventory.get(i);
                if (!slotStack.isEmpty() && ItemStack.isSameItemSameTags(slotStack, stackToInsert)) {
                    int transferAmount = Math.min(stackToInsert.getCount(), slotStack.getMaxStackSize() - slotStack.getCount());

                    if (transferAmount > 0) {
                        slotStack.grow(transferAmount);
                        stackToInsert.shrink(transferAmount);
                    }
                }

                if (stackToInsert.isEmpty()) {
                    break;
                }
            }

            // PASS 2: Try to place in empty slots
            if (!stackToInsert.isEmpty()) {
                for (int i = 0; i < tempInventory.size(); i++) {
                    if (tempInventory.get(i).isEmpty()) {
                        tempInventory.set(i, stackToInsert);
                        stackToInsert = ItemStack.EMPTY;
                        break;
                    }
                }
            }

            // If there's still items remaining, the inventory can't fit everything
            if (!stackToInsert.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    public boolean purchaseItem(int slotIndex) {
        // System.out.println("Attempt purchase! " + slotIndex);

        if (this.insertedValueInCents < productPrices[slotIndex]) {
            return false;
        }

        ItemStack itemStack = this.items.get(slotIndex);

        if (itemStack.isEmpty()) {
            return false;
        }

        // Check if the payment currency can fit in the money inventory
        NonNullList<ItemStack> salesStacks = ModHelpers.calculateItemStacksFromCents(productPrices[slotIndex]);
        if (!canFitCurrency(salesStacks)) {
            // Money inventory is full - cannot complete purchase
            this.setOutOfOrder(true);
            return false;
        }

        this.insertedValueInCents -= productPrices[slotIndex];

        BlockState blockState = this.getBlockState();
        Direction facingDirection = blockState.getValue(HorizontalDirectionalBlock.FACING);
        BlockPos dropPosition = this.getBlockPos().relative(facingDirection);

        // List to keep track of any items that could not be fully inserted
        NonNullList<ItemStack> remainingItems = NonNullList.create();

        for (ItemStack stackToAdd : salesStacks) {
            if (stackToAdd.isEmpty()) {
                continue;
            }

            // Make a mutable copy to work with, as we will be changing its count
            ItemStack stackToInsert = stackToAdd.copy();

            // --- PASS 1: Merge with existing stacks ---
            // Iterate from the starting slot to the end of the inventory
            for (int i = 12; i < this.items.size(); i++) {
                ItemStack slotStack = this.items.get(i);
                // Check if the stack in the slot can be merged with our item
                if (!slotStack.isEmpty() && ItemStack.isSameItemSameTags(slotStack, stackToInsert)) {
                    int transferAmount = Math.min(stackToInsert.getCount(), slotStack.getMaxStackSize() - slotStack.getCount());
                    
                    if (transferAmount > 0) {
                        slotStack.grow(transferAmount);
                        stackToInsert.shrink(transferAmount);
                    }
                }
                // If we've inserted the whole stack, we're done with this item
                if (stackToInsert.isEmpty()) {
                    break;
                }
            }

            // --- PASS 2: Place in empty slots ---
            // If the stack is still not empty, try finding an empty slot for the remainder
            if (!stackToInsert.isEmpty()) {
                for (int i = 12; i < this.items.size(); i++) {
                    if (this.items.get(i).isEmpty()) {
                        this.items.set(i, stackToInsert);
                        stackToInsert = ItemStack.EMPTY; // The rest has been placed
                        break;
                    }
                }
            }

            // --- Final Step: Add to remaining if still not empty ---
            // If the stack still has items, it means the inventory is full.
            if (!stackToInsert.isEmpty()) {
                remainingItems.add(stackToInsert);
            }
        }

        // After trying to add all items, drop any that didn't fit.
        if (!remainingItems.isEmpty()) {
            // You need to define 'dropPosition'. This is typically the BlockPos of your Block Entity.
            Containers.dropContents(this.level, dropPosition, remainingItems);
        }

        ItemStack copyStack = itemStack.copyWithCount(1);
        itemStack.setCount(itemStack.getCount() - 1);

        if (this.level != null && !this.level.isClientSide) {
            Containers.dropContents(this.level, dropPosition, NonNullList.withSize(1, copyStack));

            // Play purchase success sound - item dispensing (custom sound)
            this.level.playSound(null, this.worldPosition, ModSounds.VENDING_MACHINE_DISPENSE.get(),
                net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, 1.0F);
        }

        this.ejectTimer = EJECT_DELAY_TICKS;
        this.setChanged();

        return true;
    }

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

    @Override
    protected AbstractContainerMenu createMenu(int p_58627_, Inventory p_58628_) {
        return null;
    }
}