package cc.spea.currencycraft.network.packets;

import java.util.function.Supplier;

import cc.spea.currencycraft.blocks.VendingMachine.VendingMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class C2SPurchaseVendingMachineItem {
    private final BlockPos pos;
    private final int slotIndex;

    public C2SPurchaseVendingMachineItem(BlockPos pos, int slotIndex) {
        this.pos = pos;
        this.slotIndex = slotIndex;
    }

    // Deserialization from the buffer
    public C2SPurchaseVendingMachineItem(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.slotIndex = buf.readInt();
    }

    // Serialization to the buffer
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(slotIndex);
    }

    // Server-side handler
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            if (player.level().isLoaded(pos)) {
                if (player.level().getBlockEntity(pos) instanceof VendingMachineBlockEntity be) {
                    be.purchaseItem(this.slotIndex);
                }
            }
        });
        return true;
    }
}