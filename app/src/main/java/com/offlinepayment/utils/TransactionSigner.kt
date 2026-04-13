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
        // IMPORTANT: Must byte-match server canonicalization:
        // Python: json.dumps(transaction_data, sort_keys=True, separators=(",", ":"))
        // Using JSONObject() is NOT safe because key ordering is not guaranteed.
        val sorted = TreeMap<String, Any>()
        sorted.putAll(data)
        val sb = StringBuilder()
        sb.append('{')
        var first = true
        for ((k, vRaw) in sorted) {
            if (!first) sb.append(',')
            first = false
            sb.append('"').append(escapeJsonString(k)).append('"').append(':')
            val v = when (vRaw) {
                is Int, is Long, is Boolean -> vRaw
                is String -> vRaw
                else -> vRaw.toString()
            }
            when (v) {
                is Int -> sb.append(v)
                is Long -> sb.append(v)
                is Boolean -> sb.append(if (v) "true" else "false")
                is String -> sb.append('"').append(escapeJsonString(v)).append('"')
                else -> sb.append('"').append(escapeJsonString(v.toString())).append('"')
            }
        }
        sb.append('}')
        return sb.toString()
    }

    private fun escapeJsonString(s: String): String {
        val out = StringBuilder(s.length + 8)
        for (ch in s) {
            when (ch) {
                '\\' -> out.append("\\\\")
                '"' -> out.append("\\\"")
                '\b' -> out.append("\\b")
                '\u000C' -> out.append("\\f")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")
                else -> {
                    if (ch.code < 0x20) {
                        out.append(String.format("\\u%04x", ch.code))
                    } else {
                        out.append(ch)
                    }
                }
            }
        }
        return out.toString()
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
