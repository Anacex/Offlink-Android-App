package com.offlinepayment.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * Entity for storing wallet data locally for offline QR generation.
 * Sensitive data (private key) is encrypted before storage.
 */
@Entity(tableName = "offline_wallets")
data class OfflineWallet(
    @PrimaryKey
    val walletId: Int,
    val userId: Int,
    val walletType: String,
    val currency: String,
    val balance: String, // Stored as String to preserve BigDecimal precision
    val publicKey: String?,
    val privateKeyEncrypted: String?, // Encrypted private key
    val bankAccountNumber: String?,
    val isActive: Boolean,
    val lastSyncedAt: Long = System.currentTimeMillis()
)

