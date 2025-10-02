package cc.spea.currencycraft.villager;

import com.google.common.collect.ImmutableSet;

import cc.spea.currencycraft.CurrencyCraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class CurrencyCraftVillagers {
    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(ForgeRegistries.POI_TYPES, CurrencyCraft.MODID);
    public static final DeferredRegister<VillagerProfession> VILLAGER_PROFESSIONS = DeferredRegister.create(ForgeRegistries.VILLAGER_PROFESSIONS, CurrencyCraft.MODID);

    public static final RegistryObject<PoiType> BANKER_POI = POI_TYPES.register("banker_poi",
        () -> new PoiType(ImmutableSet.copyOf(CurrencyCraft.CASH_REGISTER_BLOCK.get().getStateDefinition().getPossibleStates()),
        1, 1));

    public static final RegistryObject<VillagerProfession> BANKER = VILLAGER_PROFESSIONS.register("banker", () -> new VillagerProfession("banker",
        holder -> holder.get() == BANKER_POI.get(), holder -> holder.get() == BANKER_POI.get(),
        ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_ARMORER));

    public static void register(IEventBus eventBus) {
        POI_TYPES.register(eventBus);
        VILLAGER_PROFESSIONS.register(eventBus);
    }
}
