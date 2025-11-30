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
    val lastSyncedAt: Long = System.currentTimeMillis()
)

