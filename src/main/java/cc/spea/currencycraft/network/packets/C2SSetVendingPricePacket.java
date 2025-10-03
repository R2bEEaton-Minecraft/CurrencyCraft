// Create this in a new file, e.g., network/packets/C2SSetVendingPricePacket.java

package cc.spea.currencycraft.network.packets;

import java.util.function.Supplier;

import cc.spea.currencycraft.blocks.VendingMachine.VendingMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class C2SSetVendingPricePacket {
    private final BlockPos pos;
    private final int slotIndex;
    private final long priceInCents;

    public C2SSetVendingPricePacket(BlockPos pos, int slotIndex, long priceInCents) {
        this.pos = pos;
        this.slotIndex = slotIndex;
        this.priceInCents = priceInCents;
    }

    // Deserialization from the buffer
    public C2SSetVendingPricePacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.slotIndex = buf.readInt();
        this.priceInCents = buf.readLong();
    }

    // Serialization to the buffer
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(slotIndex);
        buf.writeLong(priceInCents);
    }

    // Server-side handler
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            // Basic security: Check if the player is allowed to interact with the block
            if (player.level().isLoaded(pos)) {
                if (player.level().getBlockEntity(pos) instanceof VendingMachineBlockEntity be) {
                    // You could add ownership/permission checks here
                    be.setPriceInCents(this.slotIndex, this.priceInCents);
                }
            }
        });
        return true;
    }
}