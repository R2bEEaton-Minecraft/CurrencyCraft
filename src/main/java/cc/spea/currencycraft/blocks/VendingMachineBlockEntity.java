package cc.spea.currencycraft.blocks;

import cc.spea.currencycraft.CurrencyCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

public class VendingMachineBlockEntity extends BlockEntity {
    private final ItemStackHandler inventory = new ItemStackHandler(12);

    public VendingMachineBlockEntity(BlockPos pos, BlockState state) {
        super(CurrencyCraft.VENDING_MACHINE_BLOCK_ENTITY.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }
}
