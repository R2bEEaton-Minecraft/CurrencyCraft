package cc.spea.currencycraft.blocks;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public class VendingMachineBlock extends HorizontalEntityBlockBase {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

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
        if (!world.isClientSide()) {
            // Get the MenuProvider, which is the BlockEntity
            MenuProvider menuProvider = this.getMenuProvider(state, world, pos);

            if (menuProvider != null && player instanceof ServerPlayer) {
                // Use NetworkHooks to properly open the container screen on the client
                // This handles all the networking packets for you.
                // The third parameter is the position of the BlockEntity providing the menu.
                BlockPos blockEntityPos = (state.getValue(HALF) == DoubleBlockHalf.LOWER) ? pos : pos.below();
                NetworkHooks.openScreen((ServerPlayer) player, menuProvider, blockEntityPos);
            }
        }
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

    public BlockState updateShape(BlockState p_52796_, Direction p_52797_, BlockState p_52798_, LevelAccessor p_52799_, BlockPos p_52800_, BlockPos p_52801_) {
      DoubleBlockHalf doubleblockhalf = p_52796_.getValue(HALF);
      if (p_52797_.getAxis() == Direction.Axis.Y && doubleblockhalf == DoubleBlockHalf.LOWER == (p_52797_ == Direction.UP)) {
         return p_52798_.is(this) && p_52798_.getValue(HALF) != doubleblockhalf ? p_52796_.setValue(FACING, p_52798_.getValue(FACING)) : Blocks.AIR.defaultBlockState();
      } else {
         return doubleblockhalf == DoubleBlockHalf.LOWER && p_52797_ == Direction.DOWN && !p_52796_.canSurvive(p_52799_, p_52800_) ? Blocks.AIR.defaultBlockState() : super.updateShape(p_52796_, p_52797_, p_52798_, p_52799_, p_52800_, p_52801_);
      }
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state,
                            LivingEntity placer, ItemStack stack) {
        world.setBlock(pos.above(),
                this.defaultBlockState()
                        .setValue(HALF, DoubleBlockHalf.UPPER)
                        .setValue(FACING, state.getValue(FACING)), 3);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos blockPos, BlockState blockState, Player player) {
      if (!level.isClientSide && player.isCreative()) {
        DoubleBlockHalf doubleblockhalf = blockState.getValue(HALF);
        if (doubleblockhalf == DoubleBlockHalf.UPPER) {
            BlockPos blockpos = blockPos.below();
            BlockState blockstate = level.getBlockState(blockpos);
            if (blockstate.is(blockState.getBlock()) && blockstate.getValue(HALF) == DoubleBlockHalf.LOWER) {
                BlockState blockstate1 = blockstate.getFluidState().is(Fluids.WATER) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
                level.setBlock(blockpos, blockstate1, 35);
                level.levelEvent(player, 2001, blockpos, Block.getId(blockstate));
            }
        }
      }

      super.playerWillDestroy(level, blockPos, blockState, player);
   }

   @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        // This check is important to prevent dropping items when the block state just changes
        // (e.g., rotating the block). We only want to drop items if the block is actually removed.
        if (pState.getBlock() != pNewState.getBlock()) {
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            if (blockEntity instanceof VendingMachineBlockEntity) {
                // Use the vanilla helper method to drop all items from the container.
                Containers.dropContents(pLevel, pPos, (VendingMachineBlockEntity)blockEntity);
                // This can also be used to update comparators that might be reading the container's state.
                pLevel.updateNeighbourForOutputSignal(pPos, this);
            }
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // use BER, not baked model
        return RenderShape.MODEL;
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
}
