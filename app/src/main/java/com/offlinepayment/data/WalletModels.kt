package com.offlinepayment.data

import com.squareup.moshi.Json
import java.math.BigDecimal

data class WalletDto(
    @Json(name = "id")
    val id: Int,
    @Json(name = "wallet_type")
    val wallet_type: String,
    @Json(name = "currency")
    val currency: String,
    @Json(name = "balance")
    val balance: BigDecimal,
    @Json(name = "is_active")
    val is_active: Boolean
)

data class WalletCreateRequest(
    val wallet_type: String,
    val currency: String = "PKR"
)

data class WalletTransferRequest(
    val from_wallet_id: Int,
    val to_wallet_id: Int,
    val amount: BigDecimal,
    val currency: String = "PKR"
)

data class WalletTransferResponse(
    val reference: String,
    val status: String,
    val amount: BigDecimal,
    val currency: String
)

