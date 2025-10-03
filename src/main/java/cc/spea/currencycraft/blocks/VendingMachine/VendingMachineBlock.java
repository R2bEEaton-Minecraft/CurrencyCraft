package cc.spea.currencycraft.blocks.VendingMachine;

import java.util.List;

import javax.annotation.Nullable;

import cc.spea.currencycraft.CurrencyCraft;
import cc.spea.currencycraft.blocks.HorizontalEntityBlockBase;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.LockCode;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public class VendingMachineBlock extends HorizontalEntityBlockBase {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final ResourceLocation CONTENTS = ResourceLocation.parse("contents");

    public VendingMachineBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH) // from HorizontalBlockBase
                .setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); // adds FACING from HorizontalBlockBase
        builder.add(HALF);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Use HorizontalBlockBase’s placement for FACING, then add HALF
        return super.getStateForPlacement(context).setValue(HALF, DoubleBlockHalf.LOWER);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos,
                                Player player, InteractionHand hand, BlockHitResult hit) {
        
        // We can't do anything without a block entity, so check for it first.
        BlockPos blockEntityPos = (state.getValue(HALF) == DoubleBlockHalf.LOWER) ? pos : pos.below();
        BlockEntity blockEntity = world.getBlockEntity(blockEntityPos);
        if (!(blockEntity instanceof VendingMachineBlockEntity vendingMachine)) {
            // If there's no block entity, pass so other logic (like placing a torch on the side) can run.
            return InteractionResult.PASS;
        }

        ItemStack heldStack = player.getItemInHand(hand);

        // --- LOCKING LOGIC ---
        // This condition is now checked on BOTH the client and the server.
        if (heldStack.hasCustomHoverName() && vendingMachine.getLock().equals(LockCode.NO_LOCK)) {
            
            // The actual logic that changes game state ONLY runs on the server.
            if (!world.isClientSide()) {
                LockCode newLock = new LockCode(heldStack.getHoverName().getString());
                vendingMachine.setLock(newLock);
                player.displayClientMessage(Component.translatable("container.currencycraft.vending_machine.locked"), true);
                world.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }

            // Return SUCCESS on BOTH sides. This is the crucial step.
            // The client will swing the player's hand and stop the block from being placed.
            return InteractionResult.SUCCESS;
        }

        // --- CURRENCY & GUI LOGIC ---
        // If the locking logic didn't run, we proceed.
        // We only want to run the core gameplay logic on the server.
        if (!world.isClientSide()) {
            
            // Your currency logic remains server-side
            boolean isCurrency = CurrencyCraft.CURRENCY_ITEMS.values().stream()
                    .anyMatch(ro -> ro.get() == heldStack.getItem());

            if (isCurrency) {
                boolean success = vendingMachine.addCurrency(heldStack);
                if (success) {
                    player.setItemInHand(hand, new ItemStack(Items.AIR));
                    player.sendSystemMessage(Component.literal("" + vendingMachine.calculateTotalCurrencyValueInCents()));
                    return InteractionResult.CONSUME; // CONSUME is appropriate here
                }
            }
            
            // Open the screen if no other action was taken
            MenuProvider menuProvider = (MenuProvider) vendingMachine;
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, menuProvider, blockEntityPos);
            }
        }

        // Since opening a GUI is a valid interaction that should also stop block placement,
        // we return SUCCESS here as well. The server-side check above ensures the GUI only
        // opens once, but the client needs this to know the click was handled.
        return InteractionResult.SUCCESS;
    }

    // --- NEW METHOD ---
    // This is the implementation you were missing. It finds the BlockEntity
    // (which is always on the lower half) and returns it as a MenuProvider.
    @Nullable
    @Override
    public MenuProvider getMenuProvider(BlockState state, Level world, BlockPos pos) {
        BlockPos blockEntityPos = (state.getValue(HALF) == DoubleBlockHalf.LOWER) ? pos : pos.below();
        BlockEntity blockEntity = world.getBlockEntity(blockEntityPos);
        return blockEntity instanceof MenuProvider ? (MenuProvider) blockEntity : null;
    }

    public BlockState updateShape(BlockState p_52796_, Direction p_52797_, BlockState p_52798_, LevelAccessor p_52799_,
            BlockPos p_52800_, BlockPos p_52801_) {
        DoubleBlockHalf doubleblockhalf = p_52796_.getValue(HALF);
        if (p_52797_.getAxis() == Direction.Axis.Y
                && doubleblockhalf == DoubleBlockHalf.LOWER == (p_52797_ == Direction.UP)) {
            return p_52798_.is(this) && p_52798_.getValue(HALF) != doubleblockhalf
                    ? p_52796_.setValue(FACING, p_52798_.getValue(FACING))
                    : Blocks.AIR.defaultBlockState();
        } else {
            return doubleblockhalf == DoubleBlockHalf.LOWER && p_52797_ == Direction.DOWN
                    && !p_52796_.canSurvive(p_52799_, p_52800_) ? Blocks.AIR.defaultBlockState()
                            : super.updateShape(p_52796_, p_52797_, p_52798_, p_52799_, p_52800_, p_52801_);
        }
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state,
            LivingEntity placer, ItemStack stack) {
        world.setBlock(pos.above(),
                this.defaultBlockState()
                        .setValue(HALF, DoubleBlockHalf.UPPER)
                        .setValue(FACING, state.getValue(FACING)),
                3);
        if (stack.hasCustomHoverName()) {
            BlockEntity blockentity = world.getBlockEntity(pos);
            if (blockentity instanceof VendingMachineBlockEntity vendingmachineblockentity) {
                vendingmachineblockentity.setCustomName(stack.getHoverName());
            }
        }
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // This logic should only run on the server side and for players NOT in creative
        // mode.
        BlockPos bottomPos = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        BlockEntity blockEntity = level.getBlockEntity(bottomPos);

        if (blockEntity instanceof VendingMachineBlockEntity vendingmachineblockentity) {
            if (!level.isClientSide && player.isCreative() && !vendingmachineblockentity.isEmpty()) {
                ItemStack itemstack = this.asItem().getDefaultInstance();
                blockEntity.saveToItem(itemstack);
                if (vendingmachineblockentity.hasCustomName()) {
                    itemstack.setHoverName(vendingmachineblockentity.getCustomName());
                }

                ItemEntity itementity = new ItemEntity(level, (double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D,
                        (double) pos.getZ() + 0.5D, itemstack);
                itementity.setDefaultPickUpDelay();
                level.addFreshEntity(itementity);
            }
        }

        if (!level.isClientSide && !player.isCreative()) {
            if (blockEntity instanceof VendingMachineBlockEntity vendingMachineBlockEntity) {
                // Create a new ItemStack of our vending machine.
                ItemStack itemStack = new ItemStack(this);

                // Save the BlockEntity's data (inventory, custom name) to the ItemStack's NBT.
                // The saveToItem method is provided by the base BlockEntity class.
                vendingMachineBlockEntity.saveToItem(itemStack);

                // Create and spawn an ItemEntity in the world.
                ItemEntity itemEntity = new ItemEntity(level,
                        (double) pos.getX() + 0.5D,
                        (double) pos.getY() + 0.5D,
                        (double) pos.getZ() + 0.5D,
                        itemStack);

                itemEntity.setDefaultPickUpDelay();
                level.addFreshEntity(itemEntity);
            }
        }

        // CRITICAL: Call the super method to allow vanilla breaking logic to proceed.
        // This ensures the other half of the block breaks correctly via your
        // updateShape method.
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState blockState, Level world, BlockPos blockPos, BlockState newBlockState,
                         boolean isMoving) {
        // This condition prevents dropping items when the block state just changes (e.g. waterlogged).
        // We only want to drop items when the block is actually replaced with a different one.
        if (!blockState.is(newBlockState.getBlock())) {
            // --- NEW LOGIC ---
            // We only care about the lower half, as that's where the BlockEntity is.
            if (blockState.getValue(HALF) == DoubleBlockHalf.LOWER) {
                BlockEntity blockEntity = world.getBlockEntity(blockPos);
                if (blockEntity instanceof VendingMachineBlockEntity vendingMachine) {
                    // Drop the main inventory (if you have separate logic for that)
                    // Containers.dropContents(world, blockPos, vendingMachine.getItems()); // Example for main inventory
                    
                    // Drop the contents of the new currency inventory
                    vendingMachine.dropCurrencyContents();
                    
                    // This updates comparators, which is good practice.
                    world.updateNeighbourForOutputSignal(blockPos, blockState.getBlock());
                }
            }
            // --- END NEW LOGIC ---
            
            // The existing onRemove logic is mostly for comparators, let's clean it up slightly
            // and keep the super call. The above logic handles the main functionality.
            super.onRemove(blockState, world, blockPos, newBlockState, isMoving);
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (params.getOptionalParameter(LootContextParams.THIS_ENTITY) instanceof Player) {
            return java.util.Collections.emptyList();
        }
        // For non-player destruction, fall back to the default (which will use the loot
        // table).
        return super.getDrops(state, params);
    }

    public boolean hasAnalogOutputSignal(BlockState state) {
        DoubleBlockHalf half = state.getValue(HALF);
        return half == DoubleBlockHalf.LOWER;
    }

    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);

        if (blockEntity instanceof VendingMachineBlockEntity container) {
            final int slotsToCheck = container.getProductSlots();

            float totalFullness = 0.0F;

            for (int i = 0; i < slotsToCheck; i++) {
                ItemStack itemStack = container.getItem(i);
                if (!itemStack.isEmpty()) {
                    totalFullness += (float) itemStack.getCount()
                            / (float) Math.min(container.getMaxStackSize(), itemStack.getMaxStackSize());
                }
            }
            float averageFullness = totalFullness / (float) slotsToCheck;
            return Mth.floor(averageFullness * 14.0F) + (averageFullness > 0.0F ? 1 : 0);
        }
        return 0;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // use BER, not baked model
        return RenderShape.MODEL;
    }

    public void appendHoverText(ItemStack p_56193_, @Nullable BlockGetter p_56194_, List<Component> p_56195_,
            TooltipFlag p_56196_) {
        super.appendHoverText(p_56193_, p_56194_, p_56195_, p_56196_);
        CompoundTag compoundtag = BlockItem.getBlockEntityData(p_56193_);
        if (compoundtag != null) {
            if (compoundtag.contains("Items", 9)) {
                NonNullList<ItemStack> nonnulllist = NonNullList.withSize(27, ItemStack.EMPTY);
                ContainerHelper.loadAllItems(compoundtag, nonnulllist);
                int i = 0;
                int j = 0;

                for (ItemStack itemstack : nonnulllist) {
                    if (!itemstack.isEmpty()) {
                        ++j;
                        if (i <= 4) {
                            ++i;
                            MutableComponent mutablecomponent = itemstack.getHoverName().copy();
                            mutablecomponent.append(" x").append(String.valueOf(itemstack.getCount()));
                            p_56195_.add(mutablecomponent);
                        }
                    }
                }

                if (j - i > 0) {
                    p_56195_.add(Component.translatable("container.shulkerBox.more", j - i)
                            .withStyle(ChatFormatting.ITALIC));
                }
            }
        }

    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        BlockEntity be = state.getValue(HALF) == DoubleBlockHalf.LOWER
                ? new VendingMachineBlockEntity(pos, state)
                : null;
        if (be != null) {
            be.setChanged();
        }
        return be;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // We only want this to run on the server side.
        if (level.isClientSide()) {
            return null;
        }

        // Return our custom ticker function, but only if the BlockEntityType matches.
        return createTickerHelper(type, CurrencyCraft.VENDING_MACHINE_BLOCK_ENTITY.get(),
                (lvl, pos, st, be) -> be.tick(lvl, pos, st));
    }
}
