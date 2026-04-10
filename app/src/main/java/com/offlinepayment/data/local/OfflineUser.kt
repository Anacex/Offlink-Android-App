package com.offlinepayment.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing verified users locally for offline login
 */
@Entity(tableName = "offline_users")
data class OfflineUser(
    @PrimaryKey
    val email: String,
    val passwordHash: String, // Format: "salt:hash" for password verification
    val name: String,
    val phone: String?,
    val userId: Int, // Server user ID
    val isEmailVerified: Boolean = true, // Only verified users are stored offline
    val totalBalance: Double = 0.0, // Total balance from all wallets (cached for offline)
    val offlineBalance: Double = 0.0, // Offline wallet balance (cached for offline)
    val lastSyncedAt: Long = System.currentTimeMillis()
)

