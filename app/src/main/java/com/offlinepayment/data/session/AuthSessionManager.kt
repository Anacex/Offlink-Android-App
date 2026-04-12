package com.offlinepayment.data.session

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.offlinepayment.utils.EncryptionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val deviceFingerprint: String,
    val isEmailVerified: Boolean = false,
    val userEmail: String? = null,
    val userId: Int? = null
)

object AuthSessionManager {
    private const val TAG = "AuthSessionManager"
    private const val PREFS_NAME = "auth_session_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_DEVICE_FINGERPRINT = "device_fingerprint"
    private const val KEY_IS_EMAIL_VERIFIED = "is_email_verified"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_ID = "user_id"

    @Volatile
    private var prefs: SharedPreferences? = null
    @Volatile
    private var appContext: Context? = null

    private val sessionFlow = MutableStateFlow<AuthSession?>(null)

    @Synchronized
    fun initialize(context: Context) {
        appContext = context.applicationContext
        if (prefs == null) {
            prefs = appContext!!.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
        if (sessionFlow.value == null) {
            sessionFlow.value = prefs?.let(::loadSession)
        }
    }

    @Synchronized
    fun updateSession(newSession: AuthSession?) {
        sessionFlow.value = newSession
        try {
            persistSession(newSession)
        } catch (e: Exception) {
            // Keep the in-memory session alive even if encrypted persistence fails on a device.
            Log.e(TAG, "Failed to persist auth session securely", e)
        }
    }

    fun currentSession(): AuthSession? {
        val inMemory = sessionFlow.value
        if (inMemory != null) {
            return inMemory
        }
        val loaded = prefs?.let(::loadSession)
        if (loaded != null) {
            sessionFlow.value = loaded
        }
        return loaded
    }

    fun observeSession(): StateFlow<AuthSession?> = sessionFlow

    private fun persistSession(session: AuthSession?) {
        val editor = prefs?.edit() ?: return
        if (session == null) {
            editor
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_DEVICE_FINGERPRINT)
                .remove(KEY_IS_EMAIL_VERIFIED)
                .remove(KEY_USER_EMAIL)
                .remove(KEY_USER_ID)
                .apply()
            return
        }

        val appContext = requireNotNull(appContext) { "AuthSessionManager must be initialized before use" }
        editor
            .putString(KEY_ACCESS_TOKEN, EncryptionHelper.encrypt(appContext, session.accessToken))
            .putString(KEY_REFRESH_TOKEN, EncryptionHelper.encrypt(appContext, session.refreshToken))
            .putString(KEY_DEVICE_FINGERPRINT, EncryptionHelper.encrypt(appContext, session.deviceFingerprint))
            .putBoolean(KEY_IS_EMAIL_VERIFIED, session.isEmailVerified)
            .putString(
                KEY_USER_EMAIL,
                session.userEmail?.let { EncryptionHelper.encrypt(appContext, it) }
            )
            .apply {
                if (session.userId != null) {
                    putInt(KEY_USER_ID, session.userId)
                } else {
                    remove(KEY_USER_ID)
                }
            }
            .apply()
    }

    private fun loadSession(sharedPreferences: SharedPreferences): AuthSession? {
        val appContext = appContext ?: return null
        val deviceFingerprint = try {
            sharedPreferences.getString(KEY_DEVICE_FINGERPRINT, null)
                ?.let { EncryptionHelper.decrypt(appContext, it) }
                ?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt persisted device fingerprint", e)
            null
        } ?: return null

        val accessToken = try {
            sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
                ?.let { EncryptionHelper.decrypt(appContext, it) }
                .orEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt persisted access token", e)
            ""
        }
        val refreshToken = try {
            sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
                ?.let { EncryptionHelper.decrypt(appContext, it) }
                .orEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt persisted refresh token", e)
            ""
        }
        val userId = if (sharedPreferences.contains(KEY_USER_ID)) {
            sharedPreferences.getInt(KEY_USER_ID, 0)
        } else {
            null
        }

        return AuthSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            deviceFingerprint = deviceFingerprint,
            isEmailVerified = sharedPreferences.getBoolean(KEY_IS_EMAIL_VERIFIED, false),
            userEmail = try {
                sharedPreferences.getString(KEY_USER_EMAIL, null)
                    ?.let { EncryptionHelper.decrypt(appContext, it) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decrypt persisted user email", e)
                null
            },
            userId = userId,
        )
    }
}
