package com.offlinepayment.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.offlinepayment.data.repository.AuthRepository
import com.offlinepayment.data.repository.SyncRepository
import com.offlinepayment.data.repository.SyncResult
import com.offlinepayment.data.session.AuthSessionManager
import com.offlinepayment.data.network.ApiClient
import kotlinx.coroutines.flow.collect

class OfflineSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        AuthSessionManager.initialize(applicationContext)

        val session = AuthSessionManager.currentSession() ?: return Result.failure()
        if (session.accessToken.isBlank() && session.refreshToken.isBlank()) {
            return Result.failure()
        }

        val authRepository = AuthRepository(ApiClient.authApi, applicationContext)
        if (session.refreshToken.isNotBlank()) {
            val refreshed = authRepository.refreshToken(session.deviceFingerprint)
            if (refreshed.isFailure && session.accessToken.isBlank()) {
                return Result.retry()
            }
        }

        val syncRepository = SyncRepository(applicationContext)
        var finalResult: SyncResult = SyncResult.Success(0, 0, "No pending transactions to sync")

        syncRepository.syncPendingTransactions().collect { result ->
            finalResult = result
        }

        return when (finalResult) {
            is SyncResult.Success -> Result.success()
            is SyncResult.InProgress -> Result.retry()
            is SyncResult.Error -> {
                val message = (finalResult as SyncResult.Error).message.lowercase()
                when {
                    "login online" in message -> Result.failure()
                    "account" in message && "suspend" in message -> Result.failure()
                    else -> Result.retry()
                }
            }
        }
    }
}
