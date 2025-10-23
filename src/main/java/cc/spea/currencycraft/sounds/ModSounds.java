package cc.spea.currencycraft.sounds;

import cc.spea.currencycraft.CurrencyCraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, CurrencyCraft.MODID);

    // Cash Register Sounds
    public static final RegistryObject<SoundEvent> CASH_REGISTER_OPEN = registerSoundEvent("cash_register_open");
    public static final RegistryObject<SoundEvent> CASH_REGISTER_CLOSE = registerSoundEvent("cash_register_close");
    public static final RegistryObject<SoundEvent> CASH_REGISTER_COIN = registerSoundEvent("cash_register_coin");
    public static final RegistryObject<SoundEvent> CASH_REGISTER_NOTE = registerSoundEvent("cash_register_note");

    // Vending Machine Sounds
    public static final RegistryObject<SoundEvent> VENDING_MACHINE_INSERT_COIN = registerSoundEvent("vending_machine_insert_coin");
    public static final RegistryObject<SoundEvent> VENDING_MACHINE_INSERT_NOTE = registerSoundEvent("vending_machine_insert_note");
    public static final RegistryObject<SoundEvent> VENDING_MACHINE_DISPENSE = registerSoundEvent("vending_machine_dispense");
    public static final RegistryObject<SoundEvent> VENDING_MACHINE_REJECT = registerSoundEvent("vending_machine_reject");
    public static final RegistryObject<SoundEvent> VENDING_MACHINE_CHANGE = registerSoundEvent("vending_machine_change");

    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CurrencyCraft.MODID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
