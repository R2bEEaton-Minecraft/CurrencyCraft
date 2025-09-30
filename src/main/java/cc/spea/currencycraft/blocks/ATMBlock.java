package cc.spea.currencycraft.blocks;

import cc.spea.currencycraft.CurrencyCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public class ATMBlock extends HorizontalBlockBase {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    public ATMBlock(Properties properties) {
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
        ItemStack held = player.getItemInHand(hand);

        if (held.getItem() == CurrencyCraft.DEBIT_CARD.get()) {
            return InteractionResult.SUCCESS;
        }

        return super.use(state, world, pos, player, hand, hit);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState,
                                  LevelAccessor world, BlockPos currentPos, BlockPos facingPos) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (facing.getAxis() == Direction.Axis.Y) {
            if (half == DoubleBlockHalf.LOWER && facing == Direction.UP) {
                return facingState.is(this) && facingState.getValue(HALF) == DoubleBlockHalf.UPPER
                        ? state
                        : Blocks.AIR.defaultBlockState();
            }
            if (half == DoubleBlockHalf.UPPER && facing == Direction.DOWN) {
                return facingState.is(this) && facingState.getValue(HALF) == DoubleBlockHalf.LOWER
                        ? state
                        : Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, facing, facingState, world, currentPos, facingPos);
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
    public void playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        DoubleBlockHalf half = state.getValue(HALF);
        BlockPos otherPos = (half == DoubleBlockHalf.LOWER) ? pos.above() : pos.below();
        BlockState otherState = world.getBlockState(otherPos);
        if (otherState.is(this)) {
            world.destroyBlock(otherPos, !player.isCreative());
        }
        super.playerWillDestroy(world, pos, state, player);
    }
}
