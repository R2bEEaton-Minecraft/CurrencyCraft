package cc.spea.currencycraft.network.packets;

import cc.spea.currencycraft.bank.BankAccountData;
import cc.spea.currencycraft.bank.BankAccountManager;
import cc.spea.currencycraft.bank.BankConfig;
import cc.spea.currencycraft.helper.ModHelpers;
import cc.spea.currencycraft.items.DebitCardItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet sent from client to server to deposit cash into the bank account.
 * The deposit slot should be handled by the menu's slot system.
 */
public class C2SATMDeposit {
    private final int depositSlotIndex;

    public C2SATMDeposit(int depositSlotIndex) {
        this.depositSlotIndex = depositSlotIndex;
    }

    public C2SATMDeposit(FriendlyByteBuf buf) {
        this.depositSlotIndex = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(depositSlotIndex);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            // Validate the player has the correct menu open
            if (!(player.containerMenu instanceof cc.spea.currencycraft.gui.ATM.ATMMainMenu menu)) {
                return;
            }

            // Get the deposit slot
            ItemStack depositStack = menu.getSlot(depositSlotIndex).getItem();
            if (depositStack.isEmpty()) {
                player.displayClientMessage(Component.translatable("text.currencycraft.atm.no_deposit"), true);
                return;
            }

            // Calculate the value of the deposit
            long depositValue = ModHelpers.calculateTotalCurrencyValueInCents(java.util.Collections.singletonList(depositStack));
            if (depositValue <= 0) {
                player.displayClientMessage(Component.translatable("text.currencycraft.atm.invalid_deposit"), true);
                return;
            }

            // Validate deposit amount
            if (!BankConfig.isValidDepositAmount(depositValue)) {
                player.displayClientMessage(Component.translatable("text.currencycraft.atm.deposit_limit_exceeded"), true);
                return;
            }

            // Get the debit card
            ItemStack cardStack = player.getMainHandItem();
            if (!DebitCardItem.isValidCard(cardStack)) {
                player.displayClientMessage(Component.translatable("text.currencycraft.atm.invalid_card"), true);
                return;
            }

            UUID cardId = DebitCardItem.getCardId(cardStack);
            String pin = DebitCardItem.getPin(cardStack);
            UUID ownerUuid = DebitCardItem.getOwnerUuid(cardStack);

            // Backwards compatibility: if card has no owner (old card), fall back to player UUID
            if (ownerUuid == null) {
                ownerUuid = player.getUUID();
            }

            // Get bank account using card owner's UUID and validate card
            BankAccountManager manager = BankAccountManager.get(player.server);
            BankAccountData account = manager.getAccount(ownerUuid);

            if (!account.isCardValid(cardId, pin)) {
                player.displayClientMessage(Component.translatable("text.currencycraft.atm.card_not_authorized"), true);
                return;
            }

            // Calculate deposit after fees
            long netDeposit = BankConfig.calculateDepositAmount(depositValue);
            if (netDeposit <= 0) {
                player.displayClientMessage(Component.translatable("text.currencycraft.atm.deposit_too_small"), true);
                return;
            }

            // Consume the deposit items
            depositStack.shrink(depositStack.getCount());

            // Add to account balance
            account.deposit(netDeposit);
            manager.markDirty();

            // Play deposit success sound
            player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_FRAME_ADD_ITEM,
                SoundSource.PLAYERS, 0.7F, 1.2F);

            // Send success message
            player.displayClientMessage(Component.translatable("text.currencycraft.atm.deposit_success",
                String.format("%.2f", netDeposit / 100.0)), true);

            // Refresh the menu to update balance display
            menu.updateBalance();
        });
        return true;
    }
}
