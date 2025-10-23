package cc.spea.currencycraft.bank;

import cc.spea.currencycraft.Config;

/**
 * Configuration for bank system limits and fees.
 * Values are loaded from the config file.
 */
public class BankConfig {
    /**
     * Calculates the amount that will be deposited after fees.
     */
    public static long calculateDepositAmount(long amount) {
        if (amount <= Config.depositFee) {
            return 0L;
        }
        return amount - Config.depositFee;
    }

    /**
     * Calculates the total cost to withdraw an amount (including fees).
     */
    public static long calculateWithdrawalCost(long amount) {
        return amount + Config.withdrawalFee;
    }

    /**
     * Validates if a deposit amount is within limits.
     */
    public static boolean isValidDepositAmount(long amount) {
        return amount > 0 && amount <= Config.maxDepositPerTransaction;
    }

    /**
     * Validates if a withdrawal amount is within limits.
     */
    public static boolean isValidWithdrawalAmount(long amount) {
        return amount >= Config.minWithdrawal && amount <= Config.maxWithdrawalPerTransaction;
    }
}
