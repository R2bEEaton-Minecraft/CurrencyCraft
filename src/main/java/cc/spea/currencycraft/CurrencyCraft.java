package cc.spea.currencycraft;

import com.mojang.logging.LogUtils;

import cc.spea.currencycraft.blocks.ATMBlock;
import cc.spea.currencycraft.items.DebitCardItem;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(CurrencyCraft.MODID)
public class CurrencyCraft
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "currencycraft";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<Block> ATM_BLOCK = BLOCKS.register("atm", () -> new ATMBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));
    public static final RegistryObject<Item> ATM_BLOCK_ITEM = ITEMS.register("atm", () -> new BlockItem(ATM_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> DEBIT_CARD = ITEMS.register("debit_card", () -> new DebitCardItem(new Item.Properties()));

    // Helper method to register items
    private static RegistryObject<Item> registerItem(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    // List of currency item names
    public static final String[] CURRENCY_ITEM_NAMES = {
        "one_cent_coin",
        "two_cent_coin",
        "five_cent_coin",
        "ten_cent_coin",
        "twenty_cent_coin",
        "fifty_cent_coin",
        "one_unit_coin",
        "two_unit_coin",
        "five_unit_note",
        "ten_unit_note",
        "twenty_unit_note",
        "fifty_unit_note",
        "one_hundred_unit_note",
        "two_hundred_unit_note",
        "five_hundred_unit_note",
    };

    // Map to hold RegistryObjects for currency items
    public static final java.util.Map<String, RegistryObject<Item>> CURRENCY_ITEMS = new java.util.LinkedHashMap<>();
    static {
        for (String name : CURRENCY_ITEM_NAMES) {
            CURRENCY_ITEMS.put(name, registerItem(name));
        }
        CURRENCY_ITEMS.put("atm", ATM_BLOCK_ITEM);
        CURRENCY_ITEMS.put("debit_card", DEBIT_CARD);
    }

    // Example: Access individual items
    public static final RegistryObject<Item> ONE_CENT_COIN = CURRENCY_ITEMS.get("one_cent_coin");
    public static final RegistryObject<Item> TWO_CENT_COIN = CURRENCY_ITEMS.get("two_cent_coin");
    public static final RegistryObject<Item> FIVE_CENT_COIN = CURRENCY_ITEMS.get("five_cent_coin");
    public static final RegistryObject<Item> TEN_CENT_COIN = CURRENCY_ITEMS.get("ten_cent_coin");
    public static final RegistryObject<Item> TWENTY_CENT_COIN = CURRENCY_ITEMS.get("twenty_cent_coin");
    public static final RegistryObject<Item> FIFTY_CENT_COIN = CURRENCY_ITEMS.get("fifty_cent_coin");
    public static final RegistryObject<Item> ONE_EURO_COIN = CURRENCY_ITEMS.get("one_unit_coin");
    public static final RegistryObject<Item> TWO_EURO_COIN = CURRENCY_ITEMS.get("two_unit_coin");
    public static final RegistryObject<Item> FIVE_EURO_NOTE = CURRENCY_ITEMS.get("five_unit_note");

    // Creates a creative tab for the currency items
    public static final RegistryObject<CreativeModeTab> CURRENCYCRAFT_TAB = CREATIVE_MODE_TABS.register("currencycraft_tab", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> FIVE_EURO_NOTE.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                for (RegistryObject<Item> item : CURRENCY_ITEMS.values()) {
                    output.accept(item.get());
                }
            }).build());

    public CurrencyCraft(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        // modEventBus.addListener(this::addCreative);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }


    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
