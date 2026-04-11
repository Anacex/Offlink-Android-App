package com.offlinepayment.ble

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import kotlin.text.Charsets

/**
 * ECDSA P-256 keys in AndroidKeyStore. Uses StrongBox when available (TEE-backed).
 * Used to sign BLE handshake messages; private key never leaves secure hardware when StrongBox/TEE applies.
 */
class TeeEcdsaSigner private constructor() {

    private val keyStore: KeyStore =
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    fun getOrCreateKeyPair(): KeyPair {
        if (keyStore.containsAlias(ALIAS)) {
            val entry = keyStore.getEntry(ALIAS, null) as KeyStore.PrivateKeyEntry
            return KeyPair(entry.certificate.publicKey, entry.privateKey)
        }
        val generator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            "AndroidKeyStore",
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                val strong = KeyGenParameterSpec.Builder(
                    ALIAS,
                    KeyProperties.PURPOSE_SIGN,
                )
                    .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setUserAuthenticationRequired(false)
                    .setIsStrongBoxBacked(true)
                    .apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            setInvalidatedByBiometricEnrollment(false)
                        }
                    }
                    .build()
                generator.initialize(strong)
                return generator.generateKeyPair()
            }
            runCatching { if (keyStore.containsAlias(ALIAS)) keyStore.deleteEntry(ALIAS) }
        }

        val fallback = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_SIGN,
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(false)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    setInvalidatedByBiometricEnrollment(false)
                }
            }
            .build()
        generator.initialize(fallback)
        return generator.generateKeyPair()
    }

    fun getPublicKeySpkiBase64(): String {
        val pair = getOrCreateKeyPair()
        return Base64.encodeToString(pair.public.encoded, Base64.NO_WRAP)
    }

    fun sign(data: ByteArray): ByteArray {
        val entry = keyStore.getEntry(ALIAS, null) as KeyStore.PrivateKeyEntry
        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initSign(entry.privateKey)
        sig.update(data)
        return sig.sign()
    }

    companion object {
        private const val ALIAS = "offlink_ble_tee_ec_p256_v1"

        @Volatile
        private var instance: TeeEcdsaSigner? = null

        fun getInstance(@Suppress("UNUSED_PARAMETER") context: Context): TeeEcdsaSigner {
            return instance ?: synchronized(this) {
                instance ?: TeeEcdsaSigner().also { instance = it }
            }
        }

        /**
         * Ensures keys load, sign, and verify. Fails if the keystore key was invalidated or hardware crypto is unusable.
         */
        fun healthCheck(context: Context): Result<Unit> {
            return runCatching {
                val s = getInstance(context)
                s.getOrCreateKeyPair()
                val pub = s.getPublicKeySpkiBase64()
                val msg = "offlink-tee-health-v1".toByteArray(Charsets.UTF_8)
                val sig = s.sign(msg)
                check(verifySignature(pub, msg, sig)) { "Hardware signing self-check failed" }
            }
        }

        fun verifySignature(publicKeySpkiB64: String, data: ByteArray, signature: ByteArray): Boolean {
            return runCatching {
                val decoded = Base64.decode(publicKeySpkiB64, Base64.NO_WRAP)
                val pub = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(decoded))
                val sig = Signature.getInstance("SHA256withECDSA")
                sig.initVerify(pub)
                sig.update(data)
                sig.verify(signature)
            }.getOrDefault(false)
        }
    }
}
