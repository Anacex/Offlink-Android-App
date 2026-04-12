package com.offlinepayment.data

import android.os.Parcelable
import com.squareup.moshi.Json
import kotlinx.parcelize.Parcelize

/**
 * Data model for Payee QR code payload (Step 1).
 * This is what payees (receivers) generate to receive payments.
 * Format: { payeeId, payeeName, deviceId, nonce, maxTransactionLimit }
 */
@Parcelize
data class PayeeQRPayload(
    @Json(name = "payeeId")
    val payeeId: String,
    
    @Json(name = "payeeName")
    val payeeName: String,
    
    @Json(name = "deviceId")
    val deviceId: String,
    
    @Json(name = "nonce")
    val nonce: String,
    
    @Json(name = "maxTransactionLimit")
    val maxTransactionLimit: Double? = null, // Max amount receiver can accept (in PKR)

    // Added so sender can acquire the receiver offline public key for syncing
    @Json(name = "public_key")
    val public_key: String? = null,

    // Optional receiver wallet id (offline wallet)
    @Json(name = "wallet_id")
    val wallet_id: Int? = null
) : Parcelable

/**
 * Legacy ReceiverQRPayload for backward compatibility.
 * @deprecated Use PayeeQRPayload instead
 */
@Deprecated("Use PayeeQRPayload instead")
data class ReceiverQRPayload(
    @Json(name = "version")
    val version: String,
    
    @Json(name = "type")
    val type: String,
    
    @Json(name = "public_key")
    val public_key: String,
    
    @Json(name = "user_id")
    val user_id: Int,
    
    @Json(name = "wallet_id")
    val wallet_id: Int,
    
    @Json(name = "timestamp")
    val timestamp: String,
    
    @Json(name = "nonce")
    val nonce: String
)

