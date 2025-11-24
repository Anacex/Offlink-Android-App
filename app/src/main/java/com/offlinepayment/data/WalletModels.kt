package com.offlinepayment.data

import java.math.BigDecimal

data class WalletDto(
    val id: Int,
    val wallet_type: String,
    val currency: String,
    val balance: BigDecimal,
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

