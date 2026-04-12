package com.offlinepayment.data.network

import com.offlinepayment.data.WalletCreateRequest
import com.offlinepayment.data.WalletCreateResponse
import com.offlinepayment.data.WalletCreateVerifyRequest
import com.offlinepayment.data.WalletDto
import com.offlinepayment.data.WalletTransferRequest
import com.offlinepayment.data.WalletTransferResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit contract for wallet-related routes.
 * NOTE: Replace placeholder assumptions with generated OpenAPI models if available.
 */
interface WalletApi {
    @POST("api/v1/wallets/create-request")
    suspend fun initiateWalletCreation(@Body request: WalletCreateRequest): WalletCreateResponse
    
    @POST("api/v1/wallets/create-verify")
    suspend fun verifyAndCreateWallet(@Body request: WalletCreateVerifyRequest): WalletDto
    
    @POST("api/v1/wallets/")
    suspend fun createWallet(@Body request: WalletCreateRequest): WalletDto

    @GET("api/v1/wallets/")
    suspend fun listWallets(): List<WalletDto>

    @GET("api/v1/wallets/{id}")
    suspend fun getWallet(@Path("id") walletId: Int): WalletDto

    @POST("api/v1/wallets/transfer")
    suspend fun transfer(@Body request: WalletTransferRequest): WalletTransferResponse

    @GET("api/v1/wallets/transfers/history")
    suspend fun transferHistory(
        @Query("limit") limit: Int = 10
    ): List<WalletTransferResponse>
    
    @POST("api/v1/wallets/topup")
    suspend fun topUp(@Body request: com.offlinepayment.data.TopUpRequest): com.offlinepayment.data.TopUpResponse
    
    @POST("api/v1/wallets/topup/verify")
    suspend fun verifyTopUp(@Body request: com.offlinepayment.data.TopUpVerifyRequest): com.offlinepayment.data.TopUpVerifyResponse
    
    @GET("api/v1/wallets/{id}/private-key")
    suspend fun getWalletPrivateKey(@Path("id") walletId: Int): Map<String, String>
}

/**
 * Retrofit contract for offline transaction synchronization.
 */
interface SyncApi {
    @POST("api/v1/offline-transactions/sync")
    suspend fun syncTransactions(@Body request: com.offlinepayment.data.SyncRequest): com.offlinepayment.data.SyncResponse
    
    @GET("api/v1/offline-transactions/")
    suspend fun getSyncedTransactions(
        @Query("status_filter") statusFilter: String? = null,
        @Query("limit") limit: Int = 10
    ): List<com.offlinepayment.data.SyncedOfflineTransaction>
}
