package cc.spea.currencycraft.gui.CashRegister;

import cc.spea.currencycraft.CurrencyCraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Single source of truth for the Cash Register's layout, item validation, and placeholder textures.
 * Used by both the server-side BlockEntity and the client-side Menu.
 */
public final class CashRegisterLayout {

    private static final List<String> NOTE_NAMES = List.of(
            "five_unit_note", "ten_unit_note", "twenty_unit_note", "fifty_unit_note",
            "one_hundred_unit_note", "two_hundred_unit_note", "five_hundred_unit_note"
    );

    private static final List<String> COIN_NAMES = List.of(
            "one_cent_coin", "two_cent_coin", "five_cent_coin", "ten_cent_coin",
            "twenty_cent_coin", "fifty_cent_coin", "one_unit_coin", "two_unit_coin"
    );

    // A map of Slot Index -> Valid Item
    private static final Map<Integer, RegistryObject<Item>> VALID_ITEMS_MAP;

    // Static initializer block to build the map once when the class is loaded.
    static {
        VALID_ITEMS_MAP = IntStream.range(0, 30)
                .boxed()
                .collect(Collectors.toMap(
                        index -> index,
                        CashRegisterLayout::determineValidItemForSlot
                ));
    }

    /**
     * Gets the specific item that is allowed in the given slot index.
     * @param slotIndex The index of the slot (0-29).
     * @return The RegistryObject for the valid item, or null if the slot is invalid.
     */
    @Nullable
    public static RegistryObject<Item> getValidItemForSlot(int slotIndex) {
        return VALID_ITEMS_MAP.get(slotIndex);
    }

    /**
     * Gets the placeholder texture for a given slot index.
     * @param slotIndex The index of the slot (0-29).
     * @return The ResourceLocation for the texture, or null if none.
     */
    @Nullable
    public static ResourceLocation getPlaceholderTextureForSlot(int slotIndex) {
        RegistryObject<Item> validItem = getValidItemForSlot(slotIndex);
        if (validItem != null) {
            String name = validItem.getId().getPath();
            return ResourceLocation.fromNamespaceAndPath(CurrencyCraft.MODID, "textures/gui/sprites/container/slots/" + name + ".png");
        }
        return null;
    }

    /**
     * Core layout logic based on the new 7, 7, 8, 8 configuration.
     */
    @Nullable
    private static RegistryObject<Item> determineValidItemForSlot(int index) {
        // --- Rows 1 & 2: Notes (7 slots each, total 14) ---
        if (index >= 0 && index < 14) {
            int col = index % 7; // Column within the 7-slot grid
            int noteNameIndex = 6 - col; // Map right-to-left (col 6 -> index 0, col 0 -> index 6)
            if (noteNameIndex < NOTE_NAMES.size()) {
                return CurrencyCraft.CURRENCY_ITEMS.get(NOTE_NAMES.get(noteNameIndex));
            }
        }
        
        // --- Rows 3 & 4: Coins (8 slots each, total 16) ---
        if (index >= 14 && index < 30) {
            int relativeIndex = index - 14; // Index relative to the start of the coin section (0-15)
            int col = relativeIndex % 8; // Column within the 8-slot grid
            int coinNameIndex = 7 - col; // Map right-to-left (col 7 -> index 0, col 0 -> index 7)
            if (coinNameIndex < COIN_NAMES.size()) {
                return CurrencyCraft.CURRENCY_ITEMS.get(COIN_NAMES.get(coinNameIndex));
            }
        }

        return null; // Should not happen for indices 0-29
    }
}