package cc.spea.currencycraft.gui.ATM;

import cc.spea.currencycraft.CurrencyCraft;
import cc.spea.currencycraft.bank.BankAccountData;
import cc.spea.currencycraft.bank.BankAccountManager;
import cc.spea.currencycraft.items.DebitCardItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Menu for the main ATM interface with balance display, deposit, and withdrawal.
 */
public class ATMMainMenu extends AbstractContainerMenu {
    private final Container depositSlot;
    private final ContainerData balanceData;
    private final Player player;
    public static final int DEPOSIT_SLOT_INDEX = 0;

    // Client-side constructor
    public ATMMainMenu(int windowId, Inventory playerInventory, FriendlyByteBuf data) {
        // Use 4 slots for balance: ContainerData syncs as shorts (16-bit), so we need 4 slots for a long
        this(windowId, playerInventory, new SimpleContainer(1), new SimpleContainerData(4));
    }

    // Server-side constructor
    public ATMMainMenu(int windowId, Inventory playerInventory, Container depositSlot, ContainerData balanceData) {
        super(CurrencyCraft.ATM_MAIN_MENU.get(), windowId);
        this.depositSlot = depositSlot;
        this.balanceData = balanceData;
        this.player = playerInventory.player;

        checkContainerSize(depositSlot, 1);
        this.addDataSlots(balanceData);

        // Add deposit slot (centered at the top)
        this.addSlot(new Slot(depositSlot, 0, 62, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                // Only currency items can be deposited
                return CurrencyCraft.CURRENCY_VALUES.containsKey(stack.getItem());
            }
        });

        // Add player inventory slots
        final int slotSize = 18;
        final int playerInvXOffset = 8;
        final int playerInvYOffset = 84;
        final int playerHotbarYOffset = 142;

        // Player inventory (3 rows)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int x = playerInvXOffset + col * slotSize;
                int y = playerInvYOffset + row * slotSize;
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, x, y));
            }
        }

        // Player hotbar
        for (int i = 0; i < 9; ++i) {
            int x = playerInvXOffset + i * slotSize;
            this.addSlot(new Slot(playerInventory, i, x, playerHotbarYOffset));
        }

        // Initialize balance on server
        if (!playerInventory.player.level().isClientSide && playerInventory.player instanceof ServerPlayer serverPlayer) {
            updateBalance();
        }
    }

    /**
     * Updates the balance data from the bank account.
     * Call this after deposits/withdrawals to refresh the display.
     */
    public void updateBalance() {
        if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            ItemStack cardStack = player.getMainHandItem();
            if (DebitCardItem.isValidCard(cardStack)) {
                UUID cardId = DebitCardItem.getCardId(cardStack);
                String pin = DebitCardItem.getPin(cardStack);
                UUID ownerUuid = DebitCardItem.getOwnerUuid(cardStack);

                // Backwards compatibility: if card has no owner (old card), fall back to player UUID
                if (ownerUuid == null) {
                    ownerUuid = player.getUUID();
                }

                BankAccountManager manager = BankAccountManager.get(serverPlayer.server);
                BankAccountData account = manager.getAccount(ownerUuid);

                if (account.isCardValid(cardId, pin)) {
                    long balance = account.getBalance();
                    // Split balance into four shorts (16-bit values) because ContainerData syncs as shorts over network
                    // Balance = [short3][short2][short1][short0] where each is 16 bits
                    balanceData.set(0, (int) ((balance >> 48) & 0xFFFF)); // bits 48-63
                    balanceData.set(1, (int) ((balance >> 32) & 0xFFFF)); // bits 32-47
                    balanceData.set(2, (int) ((balance >> 16) & 0xFFFF)); // bits 16-31
                    balanceData.set(3, (int) (balance & 0xFFFF));         // bits 0-15
                }
            }
        }
    }

    /**
     * Gets the current balance from the container data.
     * Reconstructs a long from four 16-bit shorts (because ContainerData syncs as shorts over network).
     */
    public long getBalance() {
        // Reconstruct balance from four shorts (16-bit values)
        long short3 = balanceData.get(0) & 0xFFFFL; // bits 48-63
        long short2 = balanceData.get(1) & 0xFFFFL; // bits 32-47
        long short1 = balanceData.get(2) & 0xFFFFL; // bits 16-31
        long short0 = balanceData.get(3) & 0xFFFFL; // bits 0-15

        return (short3 << 48) | (short2 << 32) | (short1 << 16) | short0;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            // Return any items left in the deposit slot
            ItemStack stack = this.depositSlot.removeItemNoUpdate(0);
            if (!stack.isEmpty()) {
                player.getInventory().placeItemBackInInventory(stack);
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();

            // From deposit slot to player inventory
            if (index == DEPOSIT_SLOT_INDEX) {
                if (!this.moveItemStackTo(slotStack, 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            }
            // From player inventory to deposit slot (only currency)
            else if (CurrencyCraft.CURRENCY_VALUES.containsKey(slotStack.getItem())) {
                if (!this.moveItemStackTo(slotStack, DEPOSIT_SLOT_INDEX, DEPOSIT_SLOT_INDEX + 1, false)) {
                    return ItemStack.EMPTY;
                }
            }
            // Non-currency items from player inventory - don't move to deposit slot
            else {
                return ItemStack.EMPTY;
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }
}
