package com.offlinepayment.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineWalletDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: OfflineWallet)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallets(wallets: List<OfflineWallet>)
    
    @Query("SELECT * FROM offline_wallets WHERE walletId = :walletId LIMIT 1")
    suspend fun getWalletById(walletId: Int): OfflineWallet?
    
    @Query("SELECT * FROM offline_wallets WHERE userId = :userId AND isActive = 1")
    suspend fun getWalletsByUserId(userId: Int): List<OfflineWallet>
    
    @Query("SELECT * FROM offline_wallets WHERE userId = :userId AND walletType = :walletType AND isActive = 1 LIMIT 1")
    suspend fun getWalletByUserIdAndType(userId: Int, walletType: String): OfflineWallet?
    
    @Query("SELECT * FROM offline_wallets WHERE userId = :userId AND isActive = 1")
    fun observeWalletsByUserId(userId: Int): Flow<List<OfflineWallet>>
    
    @Query("DELETE FROM offline_wallets WHERE walletId = :walletId")
    suspend fun deleteWallet(walletId: Int)
    
    @Query("DELETE FROM offline_wallets WHERE userId = :userId")
    suspend fun deleteWalletsByUserId(userId: Int)
    
    @Query("UPDATE offline_wallets SET lastSyncedAt = :timestamp WHERE walletId = :walletId")
    suspend fun updateLastSyncedAt(walletId: Int, timestamp: Long)
    
    @Query("UPDATE offline_wallets SET balance = :newBalance WHERE walletId = :walletId")
    suspend fun updateBalance(walletId: Int, newBalance: String)
}

