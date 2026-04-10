package com.offlinepayment.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * Complete offline transaction payload for QR code transfer.
 * Contains all necessary data for offline transaction processing.
 * This is what the sender generates and the receiver scans.
 */
data class OfflineTransactionPayload(
    // Version and type
    @Json(name = "version")
    val version: String = "1.0",
    
    @Json(name = "type")
    val type: String = "offline_transaction",
    
    // Sender information
    @Json(name = "sender_user_id")
    val sender_user_id: Int,
    
    @Json(name = "sender_wallet_id")
    val sender_wallet_id: Int,
    
    @Json(name = "sender_public_key")
    val sender_public_key: String, // For receiver to verify sender identity
    
    @Json(name = "sender_name")
    val sender_name: String, // Sender's name for display
    
    @Json(name = "sender_account")
    val sender_account: String, // Sender's bank account number
    
    // Receiver information (from receiver's QR)
    @Json(name = "receiver_user_id")
    val receiver_user_id: Int,
    
    @Json(name = "receiver_wallet_id")
    val receiver_wallet_id: Int,
    
    @Json(name = "receiver_public_key")
    val receiver_public_key: String,
    
    // Transaction details
    @Json(name = "amount")
    val amount: BigDecimal,
    
    @Json(name = "currency")
    val currency: String = "PKR",
    
    @Json(name = "nonce")
    val nonce: String, // Unique nonce to prevent replay attacks
    
    @Json(name = "timestamp")
    val timestamp: String, // ISO format timestamp
    
    // Cryptographic proof
    @Json(name = "transaction_signature")
    val transaction_signature: String, // Digital signature of transaction data
    
    @Json(name = "receipt_hash")
    val receipt_hash: String, // SHA-256 hash of receipt data
    
    @Json(name = "receipt_data")
    val receipt_data: String, // JSON string of receipt data
    
    // Metadata
    @Json(name = "device_fingerprint")
    val device_fingerprint: String? = null,
    
    @Json(name = "created_at_device")
    val created_at_device: String // ISO format timestamp when created on device
)

