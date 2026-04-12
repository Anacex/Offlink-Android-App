package com.offlinepayment.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * Transaction Intent for QR-based payments.
 * Contains all necessary information for a payment request.
 */
data class TransactionIntent(
    @Json(name = "userID")
    val userID: Int,
    
    @Json(name = "walletID")
    val walletID: Int,
    
    @Json(name = "timestamp")
    val timestamp: Long,
    
    @Json(name = "nonce")
    val nonce: String,
    
    @Json(name = "amount")
    val amount: BigDecimal,
    
    @Json(name = "transactionIntentType")
    val transactionIntentType: String = "PAY_REQUEST",
    
    @Json(name = "signature")
    val signature: String? = null // Placeholder for signature (not required this semester)
)

