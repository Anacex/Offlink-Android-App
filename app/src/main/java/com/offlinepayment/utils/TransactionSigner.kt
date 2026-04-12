package com.offlinepayment.utils

import android.util.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.PSSParameterSpec
import java.util.TreeMap
import java.util.UUID

/**
 * Offline transaction signing for server sync.
 *
 * Must match [app.core.crypto.CryptoManager.verify_signature]: RSA-PSS, SHA-256, MGF1-SHA-256,
 * salt length 32 bytes, message = UTF-8 [json.dumps(transaction_data, sort_keys=True)].
 *
 * BLE handshake signing uses [com.offlinepayment.ble.TeeEcdsaSigner] separately; this class
 * binds the wallet RSA key the API expects on `/offline-transactions/sync`.
 */
object TransactionSigner {

    private const val PLACEHOLDER_PREFIX = "PLACEHOLDER_SIGNATURE_"

    /**
     * Canonical JSON for sync (sorted keys, same types as FastAPI `tx_for_verify`).
     */
    fun canonicalJsonForSyncSigning(data: Map<String, Any>): String {
        val sorted = TreeMap<String, Any>()
        sorted.putAll(data)
        val o = JSONObject()
        for ((k, v) in sorted) {
            when (v) {
                is Int -> o.put(k, v)
                is Long -> o.put(k, v)
                is String -> o.put(k, v)
                is Boolean -> o.put(k, v)
                else -> o.put(k, v.toString())
            }
        }
        return o.toString()
    }

    /**
     * RSA-PSS-SHA256 with 32-byte salt (matches Python `cryptography` PSS salt_length=32).
     */
    fun signRsaPssSha256(privateKeyPem: String, transactionData: Map<String, Any>): String {
        val canonical = canonicalJsonForSyncSigning(transactionData)
        val message = canonical.toByteArray(StandardCharsets.UTF_8)
        val privateKey = loadPkcs8PrivateKeyFromPem(privateKeyPem)
        val signature = Signature.getInstance("SHA256withRSA/PSS")
        // Match Python cryptography: PSS + MGF1-SHA256 + 32-byte salt; trailer field BC = 1.
        val pssSpec = PSSParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA256,
            32,
            1,
        )
        signature.setParameter(pssSpec)
        signature.initSign(privateKey)
        signature.update(message)
        val der = signature.sign()
        return Base64.encodeToString(der, Base64.NO_WRAP)
    }

    private fun loadPkcs8PrivateKeyFromPem(pem: String): PrivateKey {
        val trimmed = pem.trim()
        val body = trimmed
            .removePrefix("-----BEGIN PRIVATE KEY-----")
            .removePrefix("-----BEGIN RSA PRIVATE KEY-----")
            .removeSuffix("-----END PRIVATE KEY-----")
            .removeSuffix("-----END RSA PRIVATE KEY-----")
            .replace("\\s".toRegex(), "")
        val decoded = Base64.decode(body, Base64.DEFAULT)
        return try {
            KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(decoded))
        } catch (_: Exception) {
            throw IllegalArgumentException("Invalid RSA private key PEM (expected PKCS#8)")
        }
    }

    /**
     * @deprecated Legacy placeholder; use [signRsaPssSha256] for sync.
     */
    @Deprecated("Use signRsaPssSha256 with wallet PEM for server-compatible signatures")
    fun signTransaction(
        transactionData: Map<String, Any>,
        privateKeyPem: String?,
    ): String {
        if (privateKeyPem.isNullOrBlank()) {
            val dataString = transactionData.entries
                .sortedBy { it.key }
                .joinToString("|") { "${it.key}=${it.value}" }
            val hash = calculateSHA256(dataString)
            return "${PLACEHOLDER_PREFIX}${hash.take(16)}"
        }
        return signRsaPssSha256(privateKeyPem, transactionData)
    }

    fun createReceiptData(
        senderWalletId: Int,
        receiverWalletId: Int,
        receiverPublicKey: String,
        amount: String,
        currency: String,
        nonce: String,
        signature: String,
        timestamp: String,
    ): Map<String, Any> {
        return mapOf(
            "sender_wallet_id" to senderWalletId,
            "receiver_wallet_id" to receiverWalletId,
            "receiver_public_key" to receiverPublicKey,
            "amount" to amount,
            "currency" to currency,
            "nonce" to nonce,
            "signature" to signature,
            "timestamp" to timestamp,
        )
    }

    fun calculateReceiptHash(receiptData: Map<String, Any>): String {
        val receiptString = receiptData.entries
            .sortedBy { it.key }
            .joinToString("|") { "${it.key}=${it.value}" }
        return calculateSHA256(receiptString)
    }

    private fun calculateSHA256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun generateNonce(): String = UUID.randomUUID().toString()
}
