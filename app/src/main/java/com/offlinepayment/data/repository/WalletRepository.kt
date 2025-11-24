package com.offlinepayment.data.repository

import com.offlinepayment.data.ApiErrorResponse
import com.offlinepayment.data.WalletCreateRequest
import com.offlinepayment.data.WalletDto
import com.offlinepayment.data.WalletTransferRequest
import com.offlinepayment.data.WalletTransferResponse
import com.offlinepayment.data.network.WalletApi
import com.offlinepayment.data.session.AuthSessionManager
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class WalletRepository(
    private val api: WalletApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun ensureSessionOrThrow() {
        requireNotNull(AuthSessionManager.currentSession()) {
            "Auth session required for wallet operations"
        }
    }

    suspend fun createWallet(request: WalletCreateRequest): Result<WalletDto> = safeCall {
        ensureSessionOrThrow()
        api.createWallet(request)
    }

    suspend fun listWallets(): Result<List<WalletDto>> = safeCall {
        ensureSessionOrThrow()
        api.listWallets()
    }

    suspend fun transfer(request: WalletTransferRequest): Result<WalletTransferResponse> = safeCall {
        ensureSessionOrThrow()
        api.transfer(request)
    }

    suspend fun getWallet(id: Int): Result<WalletDto> = safeCall {
        ensureSessionOrThrow()
        api.getWallet(id)
    }

    private suspend fun <T> safeCall(block: suspend () -> T): Result<T> =
        withContext(ioDispatcher) {
            try {
                Result.success(block())
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                val errorMessage = try {
                    val moshi = Moshi.Builder().build()
                    val adapter = moshi.adapter(ApiErrorResponse::class.java)
                    adapter.fromJson(errorBody ?: "")?.detail ?: "HTTP ${e.code()}: ${e.message()}"
                } catch (parseException: Exception) {
                    errorBody ?: "HTTP ${e.code()}: ${e.message()}"
                }
                Result.failure(Exception(errorMessage))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}

