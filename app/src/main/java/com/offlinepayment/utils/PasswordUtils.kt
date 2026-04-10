package com.offlinepayment.utils

import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64

object PasswordUtils {
    /**
     * Hash password using SHA-256 with salt for local storage
     * Note: This is for local offline verification only
     * Server uses bcrypt which is more secure
     */
    fun hashPassword(password: String, salt: String? = null): Pair<String, String> {
        val actualSalt = salt ?: generateSalt()
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(actualSalt.toByteArray())
        val hash = digest.digest(password.toByteArray())
        val hashString = Base64.encodeToString(hash, Base64.NO_WRAP)
        return Pair(hashString, actualSalt)
    }
    
    /**
     * Verify password against stored hash
     */
    fun verifyPassword(password: String, storedHash: String, salt: String): Boolean {
        val (hash, _) = hashPassword(password, salt)
        return hash == storedHash
    }
    
    private fun generateSalt(): String {
        val random = SecureRandom()
        val saltBytes = ByteArray(16)
        random.nextBytes(saltBytes)
        return Base64.encodeToString(saltBytes, Base64.NO_WRAP)
    }
}

