package com.offlinepayment.data

import com.squareup.moshi.Json
import java.math.BigDecimal

data class WalletDto(
    @Json(name = "id")
    val id: Int,
    @Json(name = "user_id")
    val user_id: Int? = null,
    @Json(name = "wallet_type")
    val wallet_type: String,
    @Json(name = "currency")
    val currency: String,
    @Json(name = "balance")
    val balance: BigDecimal,
    @Json(name = "public_key")
    val public_key: String? = null,
    @Json(name = "bank_account_number")
    val bank_account_number: String? = null,
    @Json(name = "is_active")
    val is_active: Boolean,
    @Json(name = "created_at")
    val created_at: String? = null,
    @Json(name = "updated_at")
    val updated_at: String? = null
)

data class WalletCreateRequest(
    val wallet_type: String,
    val currency: String = "PKR",
    val bank_account_number: String
)

data class WalletCreateResponse(
    val msg: String,
    val otp_demo: String? = null
)

data class WalletCreateVerifyRequest(
    val wallet_type: String,
    val currency: String = "PKR",
    val bank_account_number: String,
    val otp: String
)

data class WalletTransferRequest(
    val from_wallet_id: Int,
    val to_wallet_id: Int,
    val amount: BigDecimal,
    val currency: String = "PKR"
)

data class WalletTransferResponse(
    val id: Int,
    val user_id: Int,
    val from_wallet_id: Int,
    val to_wallet_id: Int,
    val amount: BigDecimal,
    val currency: String,
    val status: String,
    val reference: String,
    val timestamp: String
)

