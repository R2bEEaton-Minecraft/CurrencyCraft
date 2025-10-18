package cc.spea.currencycraft.network.packets;

import cc.spea.currencycraft.bank.BankAccountData;
import cc.spea.currencycraft.bank.BankAccountManager;
import cc.spea.currencycraft.items.DebitCardItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet sent from client to server to set up a blank debit card with a PIN.
 */
public class C2SSetupDebitCard {
    private final String pin;

    public C2SSetupDebitCard(String pin) {
        this.pin = pin;
    }

    public C2SSetupDebitCard(FriendlyByteBuf buf) {
        this.pin = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(pin);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            // Validate PIN format
            if (pin == null || pin.length() != 4 || !pin.matches("\\d{4}")) {
                player.displayClientMessage(Component.translatable("text.currencycraft.atm.invalid_pin"), true);
                return;
            }

            // Get the item in the player's main hand
            ItemStack heldItem = player.getMainHandItem();
            if (heldItem.isEmpty() || !(heldItem.getItem() instanceof DebitCardItem)) {
                player.displayClientMessage(Component.translatable("text.currencycraft.atm.no_card"), true);
                return;
            }

            // Check if card is blank
            if (!DebitCardItem.isBlankCard(heldItem)) {
                player.displayClientMessage(Component.translatable("text.currencycraft.atm.card_not_blank"), true);
                return;
            }

            // Generate a unique card ID
            UUID cardId = UUID.randomUUID();

            // Set up the card with the ID and PIN
            DebitCardItem.setCardId(heldItem, cardId);
            DebitCardItem.setPin(heldItem, pin);

            // Get or create the player's bank account and link this card
            BankAccountManager manager = BankAccountManager.get(player.server);
            BankAccountData account = manager.getAccount(player.getUUID());

            // Check if player already has an active card
            if (account.hasActiveCard()) {
                player.displayClientMessage(Component.translatable("text.currencycraft.atm.card_already_exists"), true);
                // Still set up the card, but it won't be active until the old one is disabled
                return;
            }

            account.setActiveCard(cardId, pin);
            manager.markDirty();

            player.displayClientMessage(Component.translatable("text.currencycraft.atm.card_setup_success"), true);
            player.closeContainer();
        });
        return true;
    }
}
