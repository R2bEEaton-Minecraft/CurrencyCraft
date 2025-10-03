package cc.spea.currencycraft.blocks.VendingMachine;

import java.util.List;

import org.checkerframework.checker.nullness.qual.NonNull;

import cc.spea.currencycraft.CurrencyCraft;
import cc.spea.currencycraft.gui.VendingMachine.VendingMachineRestockMenu;
import cc.spea.currencycraft.helper.ModHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class VendingMachineBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    private NonNullList<ItemStack> items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
    private long insertedValueInCents = 0L;
    private int ejectTimer = 0;
    private static final int EJECT_DELAY_TICKS = 600;

    private final int productSlots = 12;
    private static final int totalSlots = 37;

    private static final int[] ALL_SLOTS = new int[totalSlots]; 
    static {
        for (int i = 0; i < ALL_SLOTS.length; i++) {
            ALL_SLOTS[i] = i;
        }
    }

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

    public void load(CompoundTag tag) {
        super.load(tag);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items);
        
        if (tag.contains("InsertedValueInCents")) {
            this.insertedValueInCents = 0L;
        }

        if (tag.contains("EjectTimer")) {
            this.ejectTimer = tag.getInt("EjectTimer");
        }
    }

    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, this.items);

        // Save the list to the main tag with our custom key "CurrencyItems"
        tag.putLong("InsertedValueInCents", this.insertedValueInCents);
        tag.putInt("EjectTimer", this.ejectTimer);
    }

    protected AbstractContainerMenu createMenu(int windowId, Inventory inventory) {
        return new VendingMachineRestockMenu(windowId, inventory, this);
    }

    @Override
    public void setChanged() {
        // We only want to send updates from the server side.
        // The client-side world is a "logical" world and doesn't have authority to send
        // updates.
        if (this.level != null) {
            // This is the crucial line. It notifies the client that the block at this
            // position has been updated.
            // This triggers the server to call getUpdateTag() and send a packet to the
            // client,
            // which then calls handleUpdateTag() on the client.
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }

        // Always call the super method to ensure the chunk is marked for saving.
        super.setChanged();
    }

    // This is the "on load" part for the client. It provides the NBT data
    // that will be sent to the client in the update packet.
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    // This receives the update packet on the client and reads the NBT data.
    @Override
    public void handleUpdateTag(CompoundTag nbt) {
        load(nbt); // Use our existing load logic
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

            // Check if the item from the stack exists as a value in our currency map.
            return CurrencyCraft.CURRENCY_ITEMS.values().stream()
                    .anyMatch(registryObject -> registryObject.get() == stack.getItem());
        }

        // Fallback for any other case
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
        
        this.insertedValueInCents += ModHelpers.calculateTotalCurrencyValueInCents(List.of(currencyStack));
        this.ejectTimer = EJECT_DELAY_TICKS;
        this.setChanged();

        return true;
    }

    // --- NEW METHOD ---
    /**
     * Drops all items from the currency inventory into the world.
     * Called when the block is broken.
     */
    public void dropCurrencyContents() {
        if (this.level != null && !this.level.isClientSide) {
            Containers.dropContents(this.level, this.getBlockPos(), ModHelpers.calculateItemStacksFromCents(this.insertedValueInCents));
        }
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        // Don't do anything if the timer isn't running.
        if (this.ejectTimer > 0) {
            this.ejectTimer--; // Decrease the timer by one tick.
            
            // When the timer hits zero, eject the items.
            if (this.ejectTimer == 0) {
                this.dropCurrencyContents();
                this.insertedValueInCents = 0;
                this.setChanged(); // Mark as dirty to save the cleared inventory
            }
        }
    }

    public long calculateTotalCurrencyValueInCents() {
        return this.insertedValueInCents;
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
}
