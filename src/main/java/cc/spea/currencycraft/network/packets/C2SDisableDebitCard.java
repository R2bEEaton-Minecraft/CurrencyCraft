package cc.spea.currencycraft.network.packets;

import cc.spea.currencycraft.bank.BankAccountData;
import cc.spea.currencycraft.bank.BankAccountManager;
import cc.spea.currencycraft.items.DebitCardItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet sent from client to server to disable the player's active debit card via fingerprint login.
 */
public class C2SDisableDebitCard {

    public C2SDisableDebitCard() {
    }

    public C2SDisableDebitCard(FriendlyByteBuf buf) {
        // No data needed
    }

    public void toBytes(FriendlyByteBuf buf) {
        // No data to write
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            // Get bank account
            BankAccountManager manager = BankAccountManager.get(player.server);
            BankAccountData account = manager.getAccount(player.getUUID());

            // Check if player has an active card
            if (!account.hasActiveCard()) {
                player.displayClientMessage(Component.translatable("text.currencycraft.atm.no_active_card"), true);
                return;
            }

            // Get the active card ID before disabling
            var activeCardId = account.getActiveCardId();

            // Disable the card in the account
            account.disableActiveCard();
            manager.markDirty();

            // Find and cancel any debit card items in the player's inventory with this card ID
            boolean foundCard = false;
            for (ItemStack stack : player.getInventory().items) {
                if (stack.getItem() instanceof DebitCardItem) {
                    if (activeCardId.equals(DebitCardItem.getCardId(stack))) {
                        DebitCardItem.setCancelled(stack, true);
                        foundCard = true;
                    }
                }
            }

            // Play card disabled sound
            player.level().playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                SoundSource.PLAYERS, 0.5F, 1.5F);

            player.displayClientMessage(Component.translatable("text.currencycraft.atm.card_disabled"), true);
            player.closeContainer();
        });
        return true;
    }
}
