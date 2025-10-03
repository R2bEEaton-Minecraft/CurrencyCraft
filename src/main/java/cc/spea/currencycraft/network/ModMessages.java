// Create this in a new file: network/ModMessages.java

package cc.spea.currencycraft.network;

import cc.spea.currencycraft.CurrencyCraft;
import cc.spea.currencycraft.network.packets.C2SSetVendingPricePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {
    private static SimpleChannel INSTANCE;

    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    // A unique name for our communication channel.
    private static final String PROTOCOL_VERSION = "1";

    public static void register() {
        SimpleChannel net = NetworkRegistry.newSimpleChannel(
                ResourceLocation.fromNamespaceAndPath(CurrencyCraft.MODID, "messages"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        );

        INSTANCE = net;

        // Register the packet. The class, encoder, decoder, and handler are provided.
        net.messageBuilder(C2SSetVendingPricePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SSetVendingPricePacket::toBytes)
                .decoder(C2SSetVendingPricePacket::new)
                .consumerMainThread(C2SSetVendingPricePacket::handle)
                .add();
        
        // You can register more packets here by repeating the above block
    }

    // Helper method to send a packet from the client to the server
    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.send(PacketDistributor.SERVER.noArg(), message);
    }

    // Helper method to send a packet from the server to a specific player
    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}