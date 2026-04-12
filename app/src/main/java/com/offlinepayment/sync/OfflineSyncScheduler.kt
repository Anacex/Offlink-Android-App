package com.offlinepayment.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

object OfflineSyncScheduler {
    private const val UNIQUE_SYNC_WORK = "offline_pending_sync"

    fun schedulePendingSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<OfflineSyncWorker>()
            .setConstraints(constraints)
            .addTag(UNIQUE_SYNC_WORK)
            .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(UNIQUE_SYNC_WORK, ExistingWorkPolicy.KEEP, request)
    }
}
