package cc.spea.currencycraft.network.packets;

import cc.spea.currencycraft.bank.BankAccountData;
import cc.spea.currencycraft.bank.BankAccountManager;
import cc.spea.currencycraft.bank.BankConfig;
import cc.spea.currencycraft.helper.ModHelpers;
import cc.spea.currencycraft.items.DebitCardItem;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet sent from client to server to withdraw cash from the bank account.
 */
public class C2SATMWithdraw {
    private final long amount;

    public C2SATMWithdraw(long amount) {
        this.amount = amount;
    }

    public C2SATMWithdraw(FriendlyByteBuf buf) {
        this.amount = buf.readLong();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeLong(amount);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            // Validate withdrawal amount
            if (!BankConfig.isValidWithdrawalAmount(amount)) {
                player.displayClientMessage(Component.translatable("text.currencycraft.atm.invalid_withdrawal"), true);
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

            // Calculate total cost including fees
            long totalCost = BankConfig.calculateWithdrawalCost(amount);

            // Check if player has sufficient balance
            if (account.getBalance() < totalCost) {
                player.displayClientMessage(Component.translatable("text.currencycraft.atm.insufficient_funds"), true);
                return;
            }

            // Withdraw from account
            if (!account.withdraw(totalCost)) {
                player.displayClientMessage(Component.translatable("text.currencycraft.atm.withdrawal_failed"), true);
                return;
            }

            // Generate money items
            NonNullList<ItemStack> moneyStacks = ModHelpers.calculateItemStacksFromCents(amount);

            // Add to player inventory or drop
            for (ItemStack stack : moneyStacks) {
                if (!player.getInventory().add(stack)) {
                    // Drop items on the ground if inventory is full
                    ItemEntity itemEntity = new ItemEntity(
                        player.level(),
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        stack
                    );
                    player.level().addFreshEntity(itemEntity);
                }
            }

            manager.markDirty();

            // Send success message
            player.displayClientMessage(Component.translatable("text.currencycraft.atm.withdrawal_success",
                String.format("%.2f", amount / 100.0)), true);

            if (BankConfig.WITHDRAWAL_FEE > 0) {
                player.displayClientMessage(Component.translatable("text.currencycraft.atm.withdrawal_fee",
                    String.format("%.2f", BankConfig.WITHDRAWAL_FEE / 100.0)), true);
            }

            // Refresh menu if open
            if (player.containerMenu instanceof cc.spea.currencycraft.gui.ATM.ATMMainMenu menu) {
                menu.updateBalance();
            }
        });
        return true;
    }
}
