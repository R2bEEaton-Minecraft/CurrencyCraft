package cc.spea.currencycraft.blocks;

import cc.spea.currencycraft.CurrencyCraft;
import cc.spea.currencycraft.bank.BankAccountData;
import cc.spea.currencycraft.bank.BankAccountManager;
import cc.spea.currencycraft.gui.ATM.ATMFingerprintMenu;
import cc.spea.currencycraft.gui.ATM.ATMSetupMenu;
import cc.spea.currencycraft.items.DebitCardItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
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
import net.minecraftforge.network.NetworkHooks;

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
        BlockPos blockpos = context.getClickedPos();
        Level level = context.getLevel();
        return blockpos.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(blockpos.above()).canBeReplaced(context) ? super.getStateForPlacement(context).setValue(HALF, DoubleBlockHalf.LOWER) : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);

        // Scenario 1: Player is holding a blank debit card -> Check if they already have an active card
        if (held.getItem() == CurrencyCraft.DEBIT_CARD.get() && DebitCardItem.isBlankCard(held)) {
            BankAccountManager manager = BankAccountManager.get(serverPlayer.server);
            BankAccountData account = manager.getAccount(player.getUUID());

            if (account.hasActiveCard()) {
                // Player already has an active card - can't set up another
                player.displayClientMessage(Component.translatable("text.currencycraft.atm.card_already_exists"), true);
                return InteractionResult.FAIL;
            }

            // No active card - allow setup
            NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("container.currencycraft.atm_setup");
                }

                @Override
                public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
                    return new ATMSetupMenu(windowId, playerInventory);
                }
            });
            return InteractionResult.SUCCESS;
        }

        // Scenario 2: Player is holding a valid debit card -> Open PIN entry menu
        if (held.getItem() == CurrencyCraft.DEBIT_CARD.get() && DebitCardItem.isValidCard(held)) {
            NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("container.currencycraft.atm_pin_entry");
                }

                @Override
                public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
                    return new cc.spea.currencycraft.gui.ATM.ATMPinEntryMenu(windowId, playerInventory);
                }
            });
            return InteractionResult.SUCCESS;
        }

        // Scenario 3: Player holding a cancelled card
        if (held.getItem() == CurrencyCraft.DEBIT_CARD.get() && DebitCardItem.isCancelled(held)) {
            player.displayClientMessage(Component.translatable("text.currencycraft.atm.card_cancelled"), true);
            return InteractionResult.FAIL;
        }

        // Scenario 4: Player not holding a debit card -> Check if they have an active card
        BankAccountManager manager = BankAccountManager.get(serverPlayer.server);
        BankAccountData account = manager.getAccount(player.getUUID());

        if (account.hasActiveCard()) {
            // Player has an active card - open fingerprint menu to disable it
            NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("container.currencycraft.atm_fingerprint");
                }

                @Override
                public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
                    return new ATMFingerprintMenu(windowId, playerInventory);
                }
            });
            return InteractionResult.SUCCESS;
        } else {
            // Player has no active card - show message
            player.displayClientMessage(Component.translatable("text.currencycraft.atm.no_card_to_disable"), true);
            return InteractionResult.FAIL;
        }
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
