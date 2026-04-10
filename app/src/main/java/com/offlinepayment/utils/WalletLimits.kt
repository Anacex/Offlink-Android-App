package com.offlinepayment.utils

import java.math.BigDecimal

/**
 * Wallet security limits and constraints
 */
object WalletLimits {
    /**
     * Maximum balance allowed in offline wallet (PKR)
     */
    const val MAX_OFFLINE_WALLET_BALANCE = 5000.0
    
    /**
     * Maximum amount per transaction (PKR)
     */
    const val MAX_TRANSACTION_AMOUNT = 500.0
    
    /**
     * Maximum balance as BigDecimal
     */
    val MAX_OFFLINE_WALLET_BALANCE_BD = BigDecimal("5000")
    
    /**
     * Maximum transaction amount as BigDecimal
     */
    val MAX_TRANSACTION_AMOUNT_BD = BigDecimal("500")
    
    /**
     * Validates if a transaction amount is within limits
     */
    fun isTransactionAmountValid(amount: BigDecimal): Boolean {
        return amount > BigDecimal.ZERO && amount <= MAX_TRANSACTION_AMOUNT_BD
    }
    
    /**
     * Validates if new balance after top-up would be within limits
     */
    fun isBalanceWithinLimit(currentBalance: BigDecimal, topUpAmount: BigDecimal): Boolean {
        return (currentBalance + topUpAmount) <= MAX_OFFLINE_WALLET_BALANCE_BD
    }
    
    /**
     * Calculates the maximum transaction amount a receiver can accept based on their current balance.
     * Rules:
     * - If balance < 4500 PKR: max limit is 500 PKR
     * - If balance >= 4500 PKR: max limit = 5000 - current_balance (to not exceed 5000 PKR total)
     * 
     * @param currentBalance Receiver's current wallet balance in PKR
     * @return Maximum transaction amount receiver can accept in PKR
     */
    fun calculateMaxTransactionLimit(currentBalance: BigDecimal): BigDecimal {
        val balance4500 = BigDecimal("4500")
        
        return if (currentBalance < balance4500) {
            // If balance < 4500, max limit is 500 PKR
            MAX_TRANSACTION_AMOUNT_BD
        } else {
            // If balance >= 4500, calculate available cap: 5000 - current_balance
            val availableCap = MAX_OFFLINE_WALLET_BALANCE_BD - currentBalance
            // Ensure it's not negative and not more than 500
            availableCap.max(BigDecimal.ZERO).min(MAX_TRANSACTION_AMOUNT_BD)
        }
    }
}

