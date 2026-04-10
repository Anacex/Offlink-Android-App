package com.offlinepayment.data

import com.squareup.moshi.Json
import java.math.BigDecimal

data class TopUpRequest(
    val wallet_id: Int,
    val amount: BigDecimal,
    val password: String,
    val bank_account_number: String
)

data class TopUpResponse(
    @Json(name = "msg")
    val msg: String,
    @Json(name = "otp_demo")
    val otp_demo: String? = null // For development/testing
)

data class TopUpVerifyRequest(
    @Json(name = "wallet_id")
    val wallet_id: Int,
    @Json(name = "otp")
    val otp: String
)

data class TopUpVerifyResponse(
    @Json(name = "msg")
    val msg: String,
    @Json(name = "wallet")
    val wallet: WalletDto
)

