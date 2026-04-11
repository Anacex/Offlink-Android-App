package com.offlinepayment.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for storing offline transactions locally.
 * Matches the backend OfflineTransaction model for sync compatibility.
 */
@Entity(
    tableName = "local_transactions",
    indices = [Index(value = ["nonce"], unique = true), Index(value = ["status"])]
)
data class LocalTransaction(
    @PrimaryKey
    val txId: String, // Transaction ID (UUID) - local primary key
    
    // Transaction details (required for sync)
    val senderWalletId: Int,
    val receiverPublicKey: String,
    val amount: String, // Amount as string to preserve precision (matches backend Decimal)
    val currency: String, // "PKR", "USD", etc.
    
    // Cryptographic proof (required for sync)
    val transactionSignature: String, // Digital signature
    val nonce: String, // Unique nonce for replay attack prevention
    
    // Receipt data
    val receiptHash: String,
    val receiptData: String, // JSON string
    
    // Status tracking
    val status: String, // "pending", "synced", "failed"
    val createdAtDevice: Long, // Timestamp when created on device
    val syncedAt: Long? = null, // Timestamp when synced to server
    val errorReason: String? = null, // Error message if sync failed
    
    // Metadata
    val deviceFingerprint: String? = null,
    
    // Legacy fields (for backward compatibility)
    val payerId: String? = null,
    val payeeId: String? = null,
    val direction: String? = null, // "SENT" or "RECEIVED"
    val rawPayload: String? = null, // Full JSON stored for backward compatibility

    /**
     * Hash-chained offline ledger (tamper-evident). Null / zero = legacy row before chaining.
     * [ledgerEntryHash] = SHA-256 hex of (ledgerPrevHash | integrityCanonicalJson).
     */
    val ledgerPrevHash: String? = null,
    val ledgerEntryHash: String? = null,
    val ledgerSequence: Long = 0L,
)

