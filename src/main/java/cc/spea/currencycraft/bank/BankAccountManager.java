package cc.spea.currencycraft.bank;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages all bank accounts for the server using SavedData for persistence.
 */
public class BankAccountManager extends SavedData {
    private static final String DATA_NAME = "currencycraft_bank_accounts";
    private final Map<UUID, BankAccountData> accounts = new HashMap<>();

    public BankAccountManager() {
    }

    /**
     * Gets or creates a bank account for the given player UUID.
     */
    public BankAccountData getAccount(UUID playerUuid) {
        return accounts.computeIfAbsent(playerUuid, k -> new BankAccountData());
    }

    /**
     * Gets the global BankAccountManager instance for the server.
     */
    public static BankAccountManager get(MinecraftServer server) {
        DimensionDataStorage storage = server.getLevel(Level.OVERWORLD).getDataStorage();
        return storage.computeIfAbsent(
            BankAccountManager::load,
            BankAccountManager::new,
            DATA_NAME
        );
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag accountsList = new ListTag();
        for (Map.Entry<UUID, BankAccountData> entry : accounts.entrySet()) {
            CompoundTag accountTag = new CompoundTag();
            accountTag.putUUID("PlayerUUID", entry.getKey());
            accountTag.put("AccountData", entry.getValue().save());
            accountsList.add(accountTag);
        }
        tag.put("Accounts", accountsList);
        return tag;
    }

    public static BankAccountManager load(CompoundTag tag) {
        BankAccountManager manager = new BankAccountManager();
        ListTag accountsList = tag.getList("Accounts", Tag.TAG_COMPOUND);
        for (int i = 0; i < accountsList.size(); i++) {
            CompoundTag accountTag = accountsList.getCompound(i);
            UUID playerUuid = accountTag.getUUID("PlayerUUID");
            BankAccountData accountData = BankAccountData.load(accountTag.getCompound("AccountData"));
            manager.accounts.put(playerUuid, accountData);
        }
        return manager;
    }

    /**
     * Marks this data as dirty so it will be saved.
     */
    public void markDirty() {
        this.setDirty();
    }
}
