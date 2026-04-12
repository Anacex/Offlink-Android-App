package com.offlinepayment.utils

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Helper for encrypting/decrypting sensitive data (like private keys) for local storage.
 * Uses AES-256-GCM for encryption.
 */
object EncryptionHelper {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_SIZE = 256
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 16

    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS_V2 = "offlink_local_data_key_v2"
    private const val V2_PREFIX = "ks2:"

    private const val PREFS_NAME = "encryption_prefs"
    private const val LEGACY_KEY_ALIAS = "wallet_encryption_key"

    private fun getOrCreateEncryptionKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existing = (keyStore.getEntry(KEY_ALIAS_V2, null) as? KeyStore.SecretKeyEntry)?.secretKey
        if (existing != null) {
            return existing
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS_V2,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE)
            .setRandomizedEncryptionRequired(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setUnlockedDeviceRequired(true)
        }

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    private fun getOrCreateLegacyEncryptionKey(context: Context): SecretKey {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val keyString = prefs.getString(LEGACY_KEY_ALIAS, null)

        return if (keyString != null) {
            val keyBytes = Base64.decode(keyString, Base64.DEFAULT)
            SecretKeySpec(keyBytes, "AES")
        } else {
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(KEY_SIZE)
            val secretKey = keyGenerator.generateKey()

            val keyBytes = secretKey.encoded
            val keyStringEncoded = Base64.encodeToString(keyBytes, Base64.DEFAULT)
            prefs.edit().putString(LEGACY_KEY_ALIAS, keyStringEncoded).apply()

            secretKey
        }
    }

    fun encrypt(context: Context, plaintext: String): String {
        val secretKey = getOrCreateEncryptionKey()
        val cipher = Cipher.getInstance(ALGORITHM)

        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)

        val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        val encrypted = ByteArray(GCM_IV_LENGTH + ciphertext.size)
        System.arraycopy(iv, 0, encrypted, 0, GCM_IV_LENGTH)
        System.arraycopy(ciphertext, 0, encrypted, GCM_IV_LENGTH, ciphertext.size)

        return V2_PREFIX + Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    fun decrypt(context: Context, encrypted: String): String {
        val value = encrypted.trim()
        return if (value.startsWith(V2_PREFIX)) {
            decryptWithKey(getOrCreateEncryptionKey(), value.removePrefix(V2_PREFIX))
        } else {
            try {
                decryptWithKey(getOrCreateLegacyEncryptionKey(context), value)
            } catch (_: Exception) {
                value
            }
        }
    }

    private fun decryptWithKey(secretKey: SecretKey, encodedPayload: String): String {
        val encryptedBytes = Base64.decode(encodedPayload, Base64.DEFAULT)
        val iv = ByteArray(GCM_IV_LENGTH)
        System.arraycopy(encryptedBytes, 0, iv, 0, GCM_IV_LENGTH)

        val ciphertext = ByteArray(encryptedBytes.size - GCM_IV_LENGTH)
        System.arraycopy(encryptedBytes, GCM_IV_LENGTH, ciphertext, 0, ciphertext.size)

        val cipher = Cipher.getInstance(ALGORITHM)
        val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)
        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, Charsets.UTF_8)
    }
}
