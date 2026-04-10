package com.offlinepayment.data

import com.squareup.moshi.Json

/**
 * Models for offline transaction synchronization
 */

data class SyncTransactionRequest(
    val transaction_data: Map<String, Any>,
    val signature: String,
    val receipt: Map<String, Any>? = null,
    val device_fingerprint: String? = null,
    val txId: String? = null,
    val transaction_id: String? = null
)

data class SyncRequest(
    val transactions: List<SyncTransactionRequest>
)

data class SyncTransactionResult(
    @Json(name = "transaction_id")
    val transaction_id: Int? = null,
    val reference: String,
    val result: String, // "synced" or "failed"
    @Json(name = "error_reason")
    val error_reason: String? = null
)

data class SyncResponse(
    val message: String,
    val results: List<SyncTransactionResult>,
    @Json(name = "total_synced")
    val total_synced: Int,
    @Json(name = "total_failed")
    val total_failed: Int
)

/**
 * Model for synced offline transactions retrieved from the server
 */
data class SyncedOfflineTransaction(
    val id: Int,
    @Json(name = "sender_wallet_id")
    val senderWalletId: Int,
    @Json(name = "receiver_public_key")
    val receiverPublicKey: String,
    val amount: String, // Decimal as string
    val currency: String,
    @Json(name = "transaction_signature")
    val transactionSignature: String,
    val nonce: String,
    @Json(name = "receipt_hash")
    val receiptHash: String,
    @Json(name = "receipt_data")
    val receiptData: String,
    val status: String, // "pending", "synced", "confirmed", "failed"
    @Json(name = "created_at_device")
    val createdAtDevice: String, // ISO 8601 datetime
    @Json(name = "synced_at")
    val syncedAt: String? = null, // ISO 8601 datetime
    @Json(name = "confirmed_at")
    val confirmedAt: String? = null, // ISO 8601 datetime
    @Json(name = "created_at")
    val createdAt: String // ISO 8601 datetime
)

