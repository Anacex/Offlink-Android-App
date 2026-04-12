package com.offlinepayment.data.session

import android.content.Context
import java.util.UUID

/**
 * Generates and persists a pseudo device fingerprint for API calls.
 * NOTE: Replace with hardware-backed fingerprinting before production rollout.
 */
object DeviceFingerprintProvider {
    private const val PREFS_NAME = "device_fingerprint_store"
    private const val KEY_FINGERPRINT = "fingerprint"
    @Volatile private var cachedFingerprint: String? = null

    fun initialize(context: Context) {
        if (cachedFingerprint == null) {
            synchronized(this) {
                if (cachedFingerprint == null) {
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val stored = prefs.getString(KEY_FINGERPRINT, null)
                    if (stored.isNullOrBlank()) {
                        val generated = "android-${UUID.randomUUID()}"
                        prefs.edit().putString(KEY_FINGERPRINT, generated).apply()
                        cachedFingerprint = generated
                    } else {
                        cachedFingerprint = stored
                    }
                }
            }
        }
    }

    fun getFingerprint(): String {
        return requireNotNull(cachedFingerprint) {
            "DeviceFingerprintProvider has not been initialized. Call initialize() from Application."
        }
    }
}

