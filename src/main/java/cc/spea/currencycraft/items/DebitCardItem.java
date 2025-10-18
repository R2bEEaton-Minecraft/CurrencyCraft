package cc.spea.currencycraft.items;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class DebitCardItem extends Item {
    private static final String CARD_ID_KEY = "CardId";
    private static final String PIN_KEY = "Pin";
    private static final String CANCELLED_KEY = "Cancelled";

    public DebitCardItem(Properties properties) {
        super(properties);
    }

    // --- Card ID handling (unique identifier for each card) ---
    public static void setCardId(ItemStack stack, UUID cardId) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID(CARD_ID_KEY, cardId);
    }

    public static UUID getCardId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.hasUUID(CARD_ID_KEY)) {
            return tag.getUUID(CARD_ID_KEY);
        }
        return null; // no card ID yet (blank card)
    }

    public static boolean hasCardId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(CARD_ID_KEY);
    }

    public static boolean isBlankCard(ItemStack stack) {
        return !hasCardId(stack) && !isCancelled(stack);
    }

    // --- PIN handling ---
    public static void setPin(ItemStack stack, String pin) {
        if (pin == null || pin.length() != 4 || !pin.matches("\\d{4}")) {
            throw new IllegalArgumentException("PIN must be a 4-digit string (0000-9999)");
        }
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(PIN_KEY, pin);
    }

    public static String getPin(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(PIN_KEY)) {
            return tag.getString(PIN_KEY);
        }
        return null; // indicates no PIN set
    }

    public static boolean hasPin(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(PIN_KEY);
    }

    // --- Cancelled state handling ---
    public static void setCancelled(ItemStack stack, boolean cancelled) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean(CANCELLED_KEY, cancelled);
    }

    public static boolean isCancelled(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(CANCELLED_KEY);
    }

    // --- Combined validation ---
    public static boolean isValidCard(ItemStack stack) {
        return hasCardId(stack) && hasPin(stack) && !isCancelled(stack);
    }    

    // --- Tooltip ---
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (isCancelled(stack)) {
            tooltip.add(Component.translatable("text.currencycraft.debit_card.cancelled"));
        } else if (isBlankCard(stack)) {
            tooltip.add(Component.translatable("text.currencycraft.debit_card.blank"));
        } else if (hasCardId(stack)) {
            tooltip.add(Component.translatable("text.currencycraft.debit_card.active"));
        }
    }
}
