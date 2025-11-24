package com.offlinepayment.data.network

import com.offlinepayment.data.WalletCreateRequest
import com.offlinepayment.data.WalletDto
import com.offlinepayment.data.WalletTransferRequest
import com.offlinepayment.data.WalletTransferResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit contract for wallet-related routes.
 * NOTE: Replace placeholder assumptions with generated OpenAPI models if available.
 */
interface WalletApi {
    @POST("api/v1/wallets/")
    suspend fun createWallet(@Body request: WalletCreateRequest): WalletDto

    @GET("api/v1/wallets/")
    suspend fun listWallets(): List<WalletDto>

    @GET("api/v1/wallets/{id}")
    suspend fun getWallet(@Path("id") walletId: Int): WalletDto

    @POST("api/v1/wallets/transfer")
    suspend fun transfer(@Body request: WalletTransferRequest): WalletTransferResponse

    @GET("api/v1/wallets/transfers/history")
    suspend fun transferHistory(): List<WalletTransferResponse>
}

