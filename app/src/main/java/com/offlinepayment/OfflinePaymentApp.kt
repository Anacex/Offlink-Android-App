package com.offlinepayment

import android.app.Application
import com.offlinepayment.data.session.AuthSessionManager
import com.offlinepayment.data.session.DeviceFingerprintProvider
import com.offlinepayment.sync.OfflineSyncScheduler

class OfflinePaymentApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DeviceFingerprintProvider.initialize(this)
        AuthSessionManager.initialize(this)
        OfflineSyncScheduler.schedulePendingSync(this)
    }
}
