package cc.spea.currencycraft.network.packets;

import cc.spea.currencycraft.bank.BankAccountData;
import cc.spea.currencycraft.bank.BankAccountManager;
import cc.spea.currencycraft.gui.ATM.ATMMainMenu;
import cc.spea.currencycraft.items.DebitCardItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet sent from client to server to verify PIN and open main ATM menu.
 */
public class C2SVerifyPinAndOpenATM {
    private final String enteredPin;

    public C2SVerifyPinAndOpenATM(String enteredPin) {
        this.enteredPin = enteredPin;
    }

    public C2SVerifyPinAndOpenATM(FriendlyByteBuf buf) {
        this.enteredPin = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(enteredPin);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            // Validate PIN format
            if (enteredPin == null || enteredPin.length() != 4 || !enteredPin.matches("\\d{4}")) {
                player.displayClientMessage(Component.translatable("text.currencycraft.atm.invalid_pin"), true);
                player.closeContainer();
                return;
            }

            // Get the debit card
            ItemStack cardStack = player.getMainHandItem();
            if (!DebitCardItem.isValidCard(cardStack)) {
                player.displayClientMessage(Component.translatable("text.currencycraft.atm.invalid_card"), true);
                player.closeContainer();
                return;
            }

            UUID cardId = DebitCardItem.getCardId(cardStack);
            String storedPin = DebitCardItem.getPin(cardStack);

            // Verify the entered PIN matches the card's PIN
            if (!enteredPin.equals(storedPin)) {
                player.displayClientMessage(Component.translatable("text.currencycraft.atm.incorrect_pin"), true);
                player.closeContainer();
                return;
            }

            // Get bank account and validate card
            BankAccountManager manager = BankAccountManager.get(player.server);
            BankAccountData account = manager.getAccount(player.getUUID());

            if (!account.isCardValid(cardId, storedPin)) {
                player.displayClientMessage(Component.translatable("text.currencycraft.atm.card_not_authorized"), true);
                player.closeContainer();
                return;
            }

            // PIN is correct - open main ATM menu
            NetworkHooks.openScreen(player, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("container.currencycraft.atm_main");
                }

                @Override
                public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
                    return new ATMMainMenu(windowId, playerInventory, new SimpleContainer(1), new SimpleContainerData(2));
                }
            });
        });
        return true;
    }
}
