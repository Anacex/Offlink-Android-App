package com.offlinepayment.utils

import android.app.Activity
import android.content.Context
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object BiometricAuthHelper {
    /**
     * Check if device security is enabled (screen lock is set)
     * Returns true if device has password, PIN, pattern, or biometric lock enabled
     */
    fun isDeviceSecurityEnabled(context: Context): Boolean {
        return try {
            val lockPattern = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.LOCK_PATTERN_ENABLED,
                0
            )
            // Check if any screen lock is enabled
            // This includes password, PIN, pattern, or biometric
            val hasLock = lockPattern == 1 || isBiometricAvailable(context)
            
            // Additional check: verify that device has some form of security
            val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
            val isKeyguardSecure = keyguardManager?.isKeyguardSecure ?: false
            
            hasLock || isKeyguardSecure
        } catch (e: Exception) {
            // Fallback: check if biometric is available
            isBiometricAvailable(context)
        }
    }
    
    /**
     * Check if biometric authentication is available on the device
     * Includes both biometric and device credential (password/PIN) support
     */
    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }
    
    /**
     * Get detailed status of device security
     * Returns a message explaining why security is not available
     */
    fun getDeviceSecurityStatus(context: Context): String {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> "Device security is enabled"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "No biometric hardware available"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Biometric hardware is unavailable"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "No biometric or device security is set up. Please set up a screen lock (password, PIN, pattern, or fingerprint) in your device settings."
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "Security update required"
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> "Biometric authentication is not supported"
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> "Unknown biometric status"
            else -> "Device security is not enabled. Please set up a screen lock (password, PIN, pattern, or fingerprint) in your device settings."
        }
    }
    
    /**
     * Show biometric authentication prompt
     * Returns true if authentication successful, false otherwise
     * Accepts both FragmentActivity and ComponentActivity
     */
    suspend fun authenticate(
        activity: FragmentActivity,
        title: String = "Authenticate",
        subtitle: String = "Use your fingerprint, face, or device password to continue"
    ): Boolean = suspendCancellableCoroutine { continuation ->
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    continuation.resume(true)
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    continuation.resume(false)
                }
                
                override fun onAuthenticationFailed() {
                    continuation.resume(false)
                }
            }
        )
        
        // Build prompt info with device credential fallback support
        // Note: When DEVICE_CREDENTIAL is included, negative button text cannot be set
        // The system automatically provides fallback to device password/PIN if biometric fails
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription("Use your fingerprint, face, or device password/PIN to authenticate")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            // Do not set negative button when DEVICE_CREDENTIAL is allowed
            // System will automatically show device credential option if biometric fails
            .build()
        
        continuation.invokeOnCancellation {
            biometricPrompt.cancelAuthentication()
        }
        
        biometricPrompt.authenticate(promptInfo)
    }
}

