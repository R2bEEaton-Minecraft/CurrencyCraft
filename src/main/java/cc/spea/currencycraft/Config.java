package cc.spea.currencycraft;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = CurrencyCraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // Bank Transaction Limits
    private static final ForgeConfigSpec.LongValue MAX_DEPOSIT_PER_TRANSACTION = BUILDER
            .comment("Maximum amount that can be deposited in a single transaction (in cents)")
            .defineInRange("maxDepositPerTransaction", 1000000000L, 1L, Long.MAX_VALUE);

    private static final ForgeConfigSpec.LongValue MAX_WITHDRAWAL_PER_TRANSACTION = BUILDER
            .comment("Maximum amount that can be withdrawn in a single transaction (in cents)")
            .defineInRange("maxWithdrawalPerTransaction", 1000000000L, 1L, Long.MAX_VALUE);

    private static final ForgeConfigSpec.LongValue MIN_WITHDRAWAL = BUILDER
            .comment("Minimum amount that can be withdrawn (in cents)")
            .defineInRange("minWithdrawal", 0L, 0L, Long.MAX_VALUE);

    // Bank Transaction Fees
    private static final ForgeConfigSpec.LongValue DEPOSIT_FEE = BUILDER
            .comment("Fee charged for deposits (in cents). Set to 0 to disable.")
            .defineInRange("depositFee", 0L, 0L, Long.MAX_VALUE);

    private static final ForgeConfigSpec.LongValue WITHDRAWAL_FEE = BUILDER
            .comment("Fee charged for withdrawals (in cents). Set to 0 to disable.")
            .defineInRange("withdrawalFee", 0L, 0L, Long.MAX_VALUE);

    private static final ForgeConfigSpec.LongValue CARD_SETUP_FEE = BUILDER
            .comment("Fee charged to set up a new debit card (in cents). Set to 0 to disable.")
            .defineInRange("cardSetupFee", 0L, 0L, Long.MAX_VALUE);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static long maxDepositPerTransaction;
    public static long maxWithdrawalPerTransaction;
    public static long minWithdrawal;
    public static long depositFee;
    public static long withdrawalFee;
    public static long cardSetupFee;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        maxDepositPerTransaction = MAX_DEPOSIT_PER_TRANSACTION.get();
        maxWithdrawalPerTransaction = MAX_WITHDRAWAL_PER_TRANSACTION.get();
        minWithdrawal = MIN_WITHDRAWAL.get();
        depositFee = DEPOSIT_FEE.get();
        withdrawalFee = WITHDRAWAL_FEE.get();
        cardSetupFee = CARD_SETUP_FEE.get();
    }
}
