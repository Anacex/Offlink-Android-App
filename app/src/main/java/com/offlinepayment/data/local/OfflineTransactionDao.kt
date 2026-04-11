package com.offlinepayment.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalTransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: LocalTransaction)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<LocalTransaction>)
    
    @Update
    suspend fun updateTransaction(transaction: LocalTransaction)
    
    @Query("SELECT * FROM local_transactions WHERE txId = :txId LIMIT 1")
    suspend fun getTransactionByTxId(txId: String): LocalTransaction?
    
    @Query("SELECT * FROM local_transactions WHERE nonce = :nonce LIMIT 1")
    suspend fun getTransactionByNonce(nonce: String): LocalTransaction?
    
    // Get all pending transactions for sync (chain order then time)
    @Query("SELECT * FROM local_transactions WHERE status = 'pending' ORDER BY ledgerSequence ASC, createdAtDevice ASC")
    suspend fun getPendingTransactions(): List<LocalTransaction>

    @Query(
        "SELECT * FROM local_transactions WHERE ledgerEntryHash IS NOT NULL AND ledgerSequence > 0 " +
            "ORDER BY ledgerSequence DESC LIMIT 1",
    )
    suspend fun getLatestChainedTail(): LocalTransaction?

    @Query(
        "SELECT * FROM local_transactions WHERE ledgerEntryHash IS NOT NULL AND ledgerSequence > 0 " +
            "ORDER BY ledgerSequence ASC",
    )
    suspend fun getAllChainedOrderedBySequence(): List<LocalTransaction>
    
    // Get transactions by status
    @Query("SELECT * FROM local_transactions WHERE status = :status ORDER BY createdAtDevice DESC")
    suspend fun getTransactionsByStatus(status: String): List<LocalTransaction>
    
    // Get all transactions for a wallet
    @Query("SELECT * FROM local_transactions WHERE senderWalletId = :walletId ORDER BY createdAtDevice DESC")
    suspend fun getTransactionsByWallet(walletId: Int): List<LocalTransaction>
    
    @Query("SELECT * FROM local_transactions WHERE senderWalletId = :walletId ORDER BY createdAtDevice DESC")
    fun observeTransactionsByWallet(walletId: Int): Flow<List<LocalTransaction>>
    
    // Update transaction status after sync
    @Query("UPDATE local_transactions SET status = :status, syncedAt = :syncedAt, errorReason = :errorReason WHERE txId = :txId")
    suspend fun updateTransactionStatus(txId: String, status: String, syncedAt: Long?, errorReason: String?)
    
    // Legacy queries (for backward compatibility)
    @Query("SELECT * FROM local_transactions WHERE payerId = :userId ORDER BY createdAtDevice DESC")
    suspend fun getSentTransactions(userId: String): List<LocalTransaction>
    
    @Query("SELECT * FROM local_transactions WHERE payeeId = :userId ORDER BY createdAtDevice DESC")
    suspend fun getReceivedTransactions(userId: String): List<LocalTransaction>
    
    @Query("SELECT * FROM local_transactions WHERE payerId = :userId OR payeeId = :userId ORDER BY createdAtDevice DESC")
    suspend fun getAllTransactionsForUser(userId: String): List<LocalTransaction>
    
    @Query("SELECT * FROM local_transactions WHERE payerId = :userId OR payeeId = :userId ORDER BY createdAtDevice DESC")
    fun observeTransactionsForUser(userId: String): Flow<List<LocalTransaction>>
    
    @Query("SELECT * FROM local_transactions WHERE direction = :direction ORDER BY createdAtDevice DESC")
    suspend fun getTransactionsByDirection(direction: String): List<LocalTransaction>
    
    @Query("DELETE FROM local_transactions WHERE txId = :txId")
    suspend fun deleteTransaction(txId: String)
    
    @Query("DELETE FROM local_transactions WHERE payerId = :userId OR payeeId = :userId")
    suspend fun deleteTransactionsByUserId(userId: String)
}

