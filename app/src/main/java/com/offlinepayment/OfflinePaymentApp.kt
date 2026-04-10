package com.offlinepayment

import android.app.Application
import com.offlinepayment.data.session.DeviceFingerprintProvider

class OfflinePaymentApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DeviceFingerprintProvider.initialize(this)
    }
}

