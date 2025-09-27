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
    private static final String OWNER_KEY = "Owner";
    private static final String PIN_KEY = "Pin";
    private static final String OWNER_NAME_KEY = "OwnerName";

    public DebitCardItem(Properties properties) {
        super(properties);
    }

    // --- Owner handling ---
    public static void setOwner(ItemStack stack, UUID owner, String ownerName) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID(OWNER_KEY, owner);
        tag.putString(OWNER_NAME_KEY, ownerName);
    }

    public static UUID getOwner(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.hasUUID(OWNER_KEY)) {
            return tag.getUUID(OWNER_KEY);
        }
        return null; // no owner yet
    }

    public static String getOwnerName(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        // Owner name is stored as a string, so check for a string tag instead of hasUUID
        if (tag != null && tag.contains(OWNER_NAME_KEY)) {
            return tag.getString(OWNER_NAME_KEY);
        }
        return null; // no owner yet
    }

    public static boolean hasOwner(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(OWNER_KEY);
    }

    // --- PIN handling ---
    public static void setPin(ItemStack stack, int pin) {
        if (pin < 0 || pin > 9999) {
            throw new IllegalArgumentException("PIN must be a 4-digit integer (0000–9999)");
        }
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(PIN_KEY, pin);
    }

    public static int getPin(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(PIN_KEY)) {
            return tag.getInt(PIN_KEY);
        }
        return -1; // indicates no PIN set
    }

    public static boolean hasPin(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(PIN_KEY);
    }    

        // --- Tooltip ---
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String owner = getOwnerName(stack);
        if (owner != null) {
            tooltip.add(Component.translatable("text.debit_card.owner", owner));
        } else {
            tooltip.add(Component.translatable("text.debit_card.unassigned"));
        }
    }
}
