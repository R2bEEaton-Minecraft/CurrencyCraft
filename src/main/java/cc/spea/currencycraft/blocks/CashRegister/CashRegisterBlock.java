package cc.spea.currencycraft.blocks.CashRegister;

import java.util.List;

import javax.annotation.Nullable;

import cc.spea.currencycraft.blocks.HorizontalEntityBlockBase;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.LockCode;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

public class CashRegisterBlock extends HorizontalEntityBlockBase {

    public CashRegisterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Block.box(0, 0, 0, 16, 7, 16);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }    
    
    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state,
            LivingEntity placer, ItemStack stack) {
        if (stack.hasCustomHoverName()) {
            BlockEntity blockentity = world.getBlockEntity(pos);
            if (blockentity instanceof CashRegisterBlockEntity cashRegisterBlockEntity) {
                cashRegisterBlockEntity.setCustomName(stack.getHoverName());
            }
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CashRegisterBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos,
                                Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof CashRegisterBlockEntity cashRegister)) {
            return InteractionResult.PASS;
        }

        ItemStack heldStack = player.getItemInHand(hand);

        if (heldStack.hasCustomHoverName() && cashRegister.getLock().equals(LockCode.NO_LOCK)) {
            if (!world.isClientSide()) {
                LockCode newLock = new LockCode(heldStack.getHoverName().getString());
                cashRegister.setLock(newLock);
                player.displayClientMessage(Component.translatable("container.currencycraft.cash_register.locked"), true);
                world.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }

            return InteractionResult.SUCCESS;
        }

        if (!world.isClientSide()) {
            MenuProvider menuProvider = (MenuProvider) cashRegister;
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, menuProvider, pos);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof CashRegisterBlockEntity cashregisterblockentity) {
            if (!level.isClientSide && player.isCreative() && !cashregisterblockentity.isEmpty()) {
                ItemStack itemstack = this.asItem().getDefaultInstance();
                blockEntity.saveToItem(itemstack);
                if (cashregisterblockentity.hasCustomName()) {
                    itemstack.setHoverName(cashregisterblockentity.getCustomName());
                }

                ItemEntity itementity = new ItemEntity(level, (double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D,
                        (double) pos.getZ() + 0.5D, itemstack);
                itementity.setDefaultPickUpDelay();
                level.addFreshEntity(itementity);
            }
        }

        if (!level.isClientSide && !player.isCreative()) {
            if (blockEntity instanceof CashRegisterBlockEntity cashregisterblockentity) {
                // Create a new ItemStack of our vending machine.
                ItemStack itemStack = new ItemStack(this);

                // Save the BlockEntity's data (inventory, custom name) to the ItemStack's NBT.
                // The saveToItem method is provided by the base BlockEntity class.
                cashregisterblockentity.saveToItem(itemStack);

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
        if (!blockState.is(newBlockState.getBlock())) {
            world.updateNeighbourForOutputSignal(blockPos, blockState.getBlock());
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
}
