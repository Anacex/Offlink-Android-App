package com.offlinepayment.utils

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Helper for encrypting/decrypting sensitive data (like private keys) for local storage.
 * Uses AES-256-GCM for encryption.
 * 
 * NOTE: In production, consider using Android Keystore for key management.
 */
object EncryptionHelper {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_SIZE = 256
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 16
    
    private const val PREFS_NAME = "encryption_prefs"
    private const val KEY_ALIAS = "wallet_encryption_key"
    
    /**
     * Gets or generates an encryption key for this device.
     * In production, this should use Android Keystore.
     */
    private fun getOrCreateEncryptionKey(context: Context): SecretKey {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val keyString = prefs.getString(KEY_ALIAS, null)
        
        return if (keyString != null) {
            // Decode existing key
            val keyBytes = Base64.decode(keyString, Base64.DEFAULT)
            SecretKeySpec(keyBytes, "AES")
        } else {
            // Generate new key
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(KEY_SIZE)
            val secretKey = keyGenerator.generateKey()
            
            // Store key (in production, use Android Keystore)
            val keyBytes = secretKey.encoded
            val keyStringEncoded = Base64.encodeToString(keyBytes, Base64.DEFAULT)
            prefs.edit().putString(KEY_ALIAS, keyStringEncoded).apply()
            
            secretKey
        }
    }
    
    /**
     * Encrypts a string value.
     */
    fun encrypt(context: Context, plaintext: String): String {
        val secretKey = getOrCreateEncryptionKey(context)
        val cipher = Cipher.getInstance(ALGORITHM)
        
        // Generate IV
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)
        
        // Initialize cipher for encryption
        val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)
        
        // Encrypt
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        
        // Combine IV and ciphertext
        val encrypted = ByteArray(GCM_IV_LENGTH + ciphertext.size)
        System.arraycopy(iv, 0, encrypted, 0, GCM_IV_LENGTH)
        System.arraycopy(ciphertext, 0, encrypted, GCM_IV_LENGTH, ciphertext.size)
        
        // Return Base64 encoded
        return Base64.encodeToString(encrypted, Base64.DEFAULT)
    }
    
    /**
     * Decrypts an encrypted string value.
     */
    fun decrypt(context: Context, encrypted: String): String {
        val secretKey = getOrCreateEncryptionKey(context)
        val encryptedBytes = Base64.decode(encrypted, Base64.DEFAULT)
        
        // Extract IV and ciphertext
        val iv = ByteArray(GCM_IV_LENGTH)
        System.arraycopy(encryptedBytes, 0, iv, 0, GCM_IV_LENGTH)
        
        val ciphertext = ByteArray(encryptedBytes.size - GCM_IV_LENGTH)
        System.arraycopy(encryptedBytes, GCM_IV_LENGTH, ciphertext, 0, ciphertext.size)
        
        // Initialize cipher for decryption
        val cipher = Cipher.getInstance(ALGORITHM)
        val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)
        
        // Decrypt
        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, Charsets.UTF_8)
    }
}

