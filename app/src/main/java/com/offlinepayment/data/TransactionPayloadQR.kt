package com.offlinepayment.data

import com.squareup.moshi.Json

/**
 * Data model for Transaction Payload QR (Step 2).
 * This is what payers generate after scanning payee QR.
 * Format: { txId, payerId, payeeId, amount, timestamp, nonce, payerName, note }
 */
data class TransactionPayloadQR(
    @Json(name = "txId")
    val txId: String, // UUID
    
    @Json(name = "payerId")
    val payerId: String,
    
    @Json(name = "payeeId")
    val payeeId: String,
    
    @Json(name = "amount")
    val amount: Long, // Amount in smallest currency unit (e.g., paisa for PKR)
    
    @Json(name = "timestamp")
    val timestamp: Long, // currentTimeMillis
    
    @Json(name = "nonce")
    val nonce: String, // Random string
    
    @Json(name = "payerName")
    val payerName: String,
    
    @Json(name = "note")
    val note: String? = null, // Optional note

    /** Sender TEE/Keystore EC public key (X509 SPKI Base64) for verifying BLE SENDER_OK. */
    @Json(name = "payerPkB64")
    val payerPkB64: String? = null,
)

