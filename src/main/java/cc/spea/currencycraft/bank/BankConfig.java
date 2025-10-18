package cc.spea.currencycraft.bank;

/**
 * Configuration for bank system limits and fees.
 */
public class BankConfig {
    // Transaction limits (in cents)
    public static final long MAX_DEPOSIT_PER_TRANSACTION = 1000000000L; // 10,000,000 units
    public static final long MAX_WITHDRAWAL_PER_TRANSACTION = 1000000000L; // 10,000,000 units
    public static final long MIN_WITHDRAWAL = 0L; // 0 cent minimum

    // Transaction fees (in cents) - can be set to 0 to disable
    public static final long DEPOSIT_FEE = 0L; // No deposit fee
    public static final long WITHDRAWAL_FEE = 0L; // No withdrawal fee
    public static final long CARD_SETUP_FEE = 0L; // No fee to set up a card

    /**
     * Calculates the amount that will be deposited after fees.
     */
    public static long calculateDepositAmount(long amount) {
        if (amount <= DEPOSIT_FEE) {
            return 0L;
        }
        return amount - DEPOSIT_FEE;
    }

    /**
     * Calculates the total cost to withdraw an amount (including fees).
     */
    public static long calculateWithdrawalCost(long amount) {
        return amount + WITHDRAWAL_FEE;
    }

    /**
     * Validates if a deposit amount is within limits.
     */
    public static boolean isValidDepositAmount(long amount) {
        return amount > 0 && amount <= MAX_DEPOSIT_PER_TRANSACTION;
    }

    /**
     * Validates if a withdrawal amount is within limits.
     */
    public static boolean isValidWithdrawalAmount(long amount) {
        return amount >= MIN_WITHDRAWAL && amount <= MAX_WITHDRAWAL_PER_TRANSACTION;
    }
}
