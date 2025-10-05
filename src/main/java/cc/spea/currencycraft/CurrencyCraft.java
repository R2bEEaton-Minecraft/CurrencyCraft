package cc.spea.currencycraft;

import com.mojang.logging.LogUtils;

import cc.spea.currencycraft.blocks.ATMBlock;
import cc.spea.currencycraft.blocks.CashRegister.CashRegisterBlock;
import cc.spea.currencycraft.blocks.CashRegister.CashRegisterBlockEntity;
import cc.spea.currencycraft.blocks.VendingMachine.VendingMachineBlock;
import cc.spea.currencycraft.blocks.VendingMachine.VendingMachineBlockEntity;
import cc.spea.currencycraft.blocks.VendingMachine.VendingMachineRenderer;
import cc.spea.currencycraft.gui.CashRegister.CashRegisterMenu;
import cc.spea.currencycraft.gui.CashRegister.CashRegisterScreen;
import cc.spea.currencycraft.gui.VendingMachine.VendingMachinePurchaseMenu;
import cc.spea.currencycraft.gui.VendingMachine.VendingMachinePurchaseScreen;
import cc.spea.currencycraft.gui.VendingMachine.VendingMachineRestockMenu;
import cc.spea.currencycraft.gui.VendingMachine.VendingMachineRestockScreen;
import cc.spea.currencycraft.gui.Wallet.WalletMenu;
import cc.spea.currencycraft.gui.Wallet.WalletScreen;
import cc.spea.currencycraft.items.DebitCardItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
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
import cc.spea.currencycraft.items.Wallet.WalletItem;
import cc.spea.currencycraft.network.ModMessages;
import cc.spea.currencycraft.villager.CurrencyCraftVillagers;

import java.util.HashMap;
import java.util.Map;

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
    
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);

    public static final Map<Item, Long> CURRENCY_VALUES = new HashMap<>();

    public static final RegistryObject<Block> ATM_BLOCK = BLOCKS.register("atm", () -> new ATMBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).noOcclusion().strength(5.0f, 6.0f)));
    public static final RegistryObject<Item> ATM_BLOCK_ITEM = ITEMS.register("atm", () -> new BlockItem(ATM_BLOCK.get(), new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Block> VENDING_MACHINE_BLOCK = BLOCKS.register("vending_machine", () -> new VendingMachineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).noOcclusion().strength(50.0f, 1200.0f)));
    public static final RegistryObject<Item> VENDING_MACHINE_BLOCK_ITEM = ITEMS.register("vending_machine", () -> new BlockItem(VENDING_MACHINE_BLOCK.get(), new Item.Properties().stacksTo(1)));
    public static final RegistryObject<BlockEntityType<VendingMachineBlockEntity>> VENDING_MACHINE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("vending_machine",
            () -> BlockEntityType.Builder.of(VendingMachineBlockEntity::new, VENDING_MACHINE_BLOCK.get()).build(null));

    public static final RegistryObject<Block> CASH_REGISTER_BLOCK = BLOCKS.register("cash_register", () -> new CashRegisterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(25.0f, 1200.0f)));
    public static final RegistryObject<Item> CASH_REGISTER_BLOCK_ITEM = ITEMS.register("cash_register", () -> new BlockItem(CASH_REGISTER_BLOCK.get(), new Item.Properties().stacksTo(1)));
    public static final RegistryObject<BlockEntityType<CashRegisterBlockEntity>> CASH_REGISTER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("cash_register",
            () -> BlockEntityType.Builder.of(CashRegisterBlockEntity::new, CASH_REGISTER_BLOCK.get()).build(null));


    public static final RegistryObject<Item> DEBIT_CARD = ITEMS.register("debit_card", () -> new DebitCardItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> WALLET = ITEMS.register("wallet", () -> new WalletItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<MenuType<VendingMachineRestockMenu>> VENDING_MACHINE_RESTOCK_MENU =
    MENUS.register("vending_machine_restock", 
        () -> IForgeMenuType.create((windowId, inv, data) -> {
            BlockPos pos = data.readBlockPos();
            BlockEntity be = inv.player.level().getBlockEntity(pos);
            if (be instanceof VendingMachineBlockEntity vm) {
                return new VendingMachineRestockMenu(windowId, inv, vm);
            }
            return null;
        }));
    public static final RegistryObject<MenuType<VendingMachinePurchaseMenu>> VENDING_MACHINE_PURCHASE_MENU =
    MENUS.register("vending_machine_purchase", 
        () -> IForgeMenuType.create((windowId, inv, data) -> {
            BlockPos pos = data.readBlockPos();
            BlockEntity be = inv.player.level().getBlockEntity(pos);
            if (be instanceof VendingMachineBlockEntity vm) {
                return new VendingMachinePurchaseMenu(windowId, inv, vm);
            }
            return null;
        }));
    public static final RegistryObject<MenuType<CashRegisterMenu>> CASH_REGISTER_MENU =
    MENUS.register("cash_register", 
        () -> IForgeMenuType.create((windowId, inv, data) -> {
            BlockPos pos = data.readBlockPos();
            BlockEntity be = inv.player.level().getBlockEntity(pos);
            if (be instanceof CashRegisterBlockEntity vm) {
                return new CashRegisterMenu(windowId, inv, vm, vm.getContainerData());
            }
            return null;
        }));
    public static final RegistryObject<MenuType<WalletMenu>> WALLET_MENU =
    MENUS.register("wallet", 
        () -> IForgeMenuType.create((windowId, inv, data) -> {
            ItemStack itemStack = data.readItem();
            if (itemStack.is(WALLET.get())) {
                return new WalletMenu(windowId, inv, itemStack);
            }
            return null;
        }));

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
    public static final Map<String, RegistryObject<Item>> CURRENCY_ITEMS = new java.util.LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> CURRENCYCRAFT_ITEMS = new java.util.LinkedHashMap<>();
    static {
        for (String name : CURRENCY_ITEM_NAMES) {
            CURRENCYCRAFT_ITEMS.put(name, registerItem(name));
            CURRENCY_ITEMS.put(name, CURRENCYCRAFT_ITEMS.get(name));
        }
        CURRENCYCRAFT_ITEMS.put("atm", ATM_BLOCK_ITEM);
        CURRENCYCRAFT_ITEMS.put("cash_register", CASH_REGISTER_BLOCK_ITEM);
        CURRENCYCRAFT_ITEMS.put("vending_machine", VENDING_MACHINE_BLOCK_ITEM);
        CURRENCYCRAFT_ITEMS.put("debit_card", DEBIT_CARD);
        CURRENCYCRAFT_ITEMS.put("wallet", WALLET);
    }

    // Example: Access individual items
    public static final RegistryObject<Item> FIVE_EURO_NOTE = CURRENCYCRAFT_ITEMS.get("five_unit_note");

    // Creates a creative tab for the currency items
    public static final RegistryObject<CreativeModeTab> CURRENCYCRAFT_TAB = CREATIVE_MODE_TABS.register("currencycraft_tab", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> FIVE_EURO_NOTE.get().getDefaultInstance())
            .title(Component.translatable("itemGroup.currencycraft.currencycraft_tab"))
            .displayItems((parameters, output) -> {
                for (RegistryObject<Item> item : CURRENCYCRAFT_ITEMS.values()) {
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

        BLOCK_ENTITY_TYPES.register(modEventBus);

        MENUS.register(modEventBus);

        CurrencyCraftVillagers.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        // modEventBus.addListener(this::addCreative);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private static void registerCurrencyValues() {
        CURRENCY_VALUES.put(CURRENCY_ITEMS.get("one_cent_coin").get(), 1L);
        CURRENCY_VALUES.put(CURRENCY_ITEMS.get("two_cent_coin").get(), 2L);
        CURRENCY_VALUES.put(CURRENCY_ITEMS.get("five_cent_coin").get(), 5L);
        CURRENCY_VALUES.put(CURRENCY_ITEMS.get("ten_cent_coin").get(), 10L);
        CURRENCY_VALUES.put(CURRENCY_ITEMS.get("twenty_cent_coin").get(), 20L);
        CURRENCY_VALUES.put(CURRENCY_ITEMS.get("fifty_cent_coin").get(), 50L);
        CURRENCY_VALUES.put(CURRENCY_ITEMS.get("one_unit_coin").get(), 100L);
        CURRENCY_VALUES.put(CURRENCY_ITEMS.get("two_unit_coin").get(), 200L);
        CURRENCY_VALUES.put(CURRENCY_ITEMS.get("five_unit_note").get(), 500L);
        CURRENCY_VALUES.put(CURRENCY_ITEMS.get("ten_unit_note").get(), 1000L);
        CURRENCY_VALUES.put(CURRENCY_ITEMS.get("twenty_unit_note").get(), 2000L);
        CURRENCY_VALUES.put(CURRENCY_ITEMS.get("fifty_unit_note").get(), 5000L);
        CURRENCY_VALUES.put(CURRENCY_ITEMS.get("one_hundred_unit_note").get(), 10000L);
        CURRENCY_VALUES.put(CURRENCY_ITEMS.get("two_hundred_unit_note").get(), 20000L);
        CURRENCY_VALUES.put(CURRENCY_ITEMS.get("five_hundred_unit_note").get(), 50000L);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));

        registerCurrencyValues();

        event.enqueueWork(() -> {
            ModMessages.register();
        });
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

            event.enqueueWork(() -> {
                MenuScreens.register(CurrencyCraft.VENDING_MACHINE_RESTOCK_MENU.get(),
                        VendingMachineRestockScreen::new);
                MenuScreens.register(CurrencyCraft.VENDING_MACHINE_PURCHASE_MENU.get(),
                        VendingMachinePurchaseScreen::new);
                MenuScreens.register(CurrencyCraft.CASH_REGISTER_MENU.get(),
                        CashRegisterScreen::new);
                MenuScreens.register(CurrencyCraft.WALLET_MENU.get(),
                        WalletScreen::new);
            });
        }

        @SubscribeEvent
        public static void onItemColorRegister(RegisterColorHandlersEvent.Item event) {
            event.register(
                (stack, tintIndex) -> {
                    if (tintIndex == 0) {
                        return ((DyeableLeatherItem) stack.getItem()).getColor(stack);
                    }
                    return 0xFFFFFF;
                },
                WALLET.get()
            );
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(VENDING_MACHINE_BLOCK_ENTITY.get(),
                    VendingMachineRenderer::new);
        }
    }
}
