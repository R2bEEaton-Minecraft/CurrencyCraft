package cc.spea.currencycraft.bank;

import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a single player's bank account data.
 */
public class BankAccountData {
    private long balance; // Balance in cents
    private UUID activeCardId; // The UUID of the currently active debit card
    private String cardPin; // The 4-digit PIN for the active card

    public BankAccountData() {
        this.balance = 0L;
        this.activeCardId = null;
        this.cardPin = null;
    }

    public long getBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        this.balance = Math.max(0, balance); // Prevent negative balance
    }

    public void deposit(long amount) {
        if (amount > 0) {
            this.balance += amount;
        }
    }

    public boolean withdraw(long amount) {
        if (amount > 0 && this.balance >= amount) {
            this.balance -= amount;
            return true;
        }
        return false;
    }

    public UUID getActiveCardId() {
        return activeCardId;
    }

    public String getCardPin() {
        return cardPin;
    }

    public boolean hasActiveCard() {
        return activeCardId != null && cardPin != null;
    }

    public void setActiveCard(UUID cardId, String pin) {
        this.activeCardId = cardId;
        this.cardPin = pin;
    }

    public void disableActiveCard() {
        this.activeCardId = null;
        this.cardPin = null;
    }

    public boolean isCardValid(UUID cardId, String pin) {
        return this.activeCardId != null &&
               this.activeCardId.equals(cardId) &&
               this.cardPin != null &&
               this.cardPin.equals(pin);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Balance", this.balance);
        if (this.activeCardId != null) {
            tag.putUUID("ActiveCardId", this.activeCardId);
        }
        if (this.cardPin != null) {
            tag.putString("CardPin", this.cardPin);
        }
        return tag;
    }

    public static BankAccountData load(CompoundTag tag) {
        BankAccountData data = new BankAccountData();
        data.balance = tag.getLong("Balance");
        if (tag.hasUUID("ActiveCardId")) {
            data.activeCardId = tag.getUUID("ActiveCardId");
        }
        if (tag.contains("CardPin")) {
            data.cardPin = tag.getString("CardPin");
        }
        return data;
    }
}
