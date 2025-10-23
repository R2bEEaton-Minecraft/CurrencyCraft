package cc.spea.currencycraft.network.packets;

import java.util.function.Supplier;

import cc.spea.currencycraft.blocks.VendingMachine.VendingMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

public class C2SPurchaseVendingMachineItem {
    private final BlockPos pos;
    private final int slotIndex;
    private final boolean buyAll;

    public C2SPurchaseVendingMachineItem(BlockPos pos, int slotIndex, boolean buyAll) {
        this.pos = pos;
        this.slotIndex = slotIndex;
        this.buyAll = buyAll;
    }

    // Deserialization from the buffer
    public C2SPurchaseVendingMachineItem(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.slotIndex = buf.readInt();
        this.buyAll = buf.readBoolean();
    }

    // Serialization to the buffer
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(slotIndex);
        buf.writeBoolean(buyAll);
    }

    // Server-side handler
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            if (player.level().isLoaded(pos)) {
                if (player.level().getBlockEntity(pos) instanceof VendingMachineBlockEntity be) {
                    if (buyAll) {
                        // Buy as many as possible
                        int purchaseCount = 0;
                        boolean keepBuying = true;

                        while (keepBuying) {
                            boolean success = be.purchaseItem(this.slotIndex);
                            if (success) {
                                purchaseCount++;
                            } else {
                                keepBuying = false;

                                // Only show error if we couldn't buy any
                                if (purchaseCount == 0) {
                                    player.displayClientMessage(
                                        Component.translatable("text.currencycraft.vending_machine.out_of_order"),
                                        true
                                    );
                                    player.level().playSound(
                                        null,
                                        pos,
                                        SoundEvents.ANVIL_LAND,
                                        SoundSource.BLOCKS,
                                        0.5F,
                                        0.8F
                                    );
                                }
                            }
                        }
                    } else {
                        // Buy single item
                        boolean success = be.purchaseItem(this.slotIndex);

                        // If purchase failed, send out of order message and play error sound
                        if (!success) {
                            player.displayClientMessage(
                                Component.translatable("text.currencycraft.vending_machine.out_of_order"),
                                true
                            );
                            player.level().playSound(
                                null,
                                pos,
                                SoundEvents.ANVIL_LAND,
                                SoundSource.BLOCKS,
                                0.5F,
                                0.8F
                            );
                        }
                    }
                }
            }
        });
        return true;
    }
}