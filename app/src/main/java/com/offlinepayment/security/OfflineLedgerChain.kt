package com.offlinepayment.security

import android.content.Context
import com.offlinepayment.data.local.LocalTransaction
import com.offlinepayment.data.local.LocalTransactionDao
import com.offlinepayment.utils.EncryptionHelper
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.TreeMap

/**
 * Hash-chained offline ledger: each new local row commits to the previous entry hash
 * and a deterministic canonical JSON of immutable fields. Tampering any stored field
 * breaks verification on sync (server) or local audit.
 *
 * Sensitive JSON (`rawPayload`, `receiptData`) is AES-GCM encrypted at rest when they
 * look like plaintext JSON; ciphertext is what gets hashed and synced.
 */
object OfflineLedgerChain {

    /** Genesis "previous hash" for the first chained entry on a device. */
    const val GENESIS_PREV_HASH: String = "0000000000000000000000000000000000000000000000000000000000000000"

    private fun looksLikePlaintextJson(s: String): Boolean {
        val t = s.trimStart()
        return t.startsWith("{") || t.startsWith("[")
    }

    /**
     * Encrypts [rawPayload] / [receiptData] when they appear to be plaintext JSON.
     */
    fun encryptAtRest(context: Context, tx: LocalTransaction): LocalTransaction {
        var out = tx
        val raw = tx.rawPayload
        if (!raw.isNullOrBlank() && looksLikePlaintextJson(raw)) {
            out = out.copy(rawPayload = EncryptionHelper.encrypt(context, raw))
        }
        if (tx.receiptData.isNotBlank() && looksLikePlaintextJson(tx.receiptData)) {
            out = out.copy(receiptData = EncryptionHelper.encrypt(context, tx.receiptData))
        }
        return out
    }

    /**
     * Deterministic JSON string (sorted keys, UTF-8) over fields that must match between
     * device and sync payload. Excludes mutable columns (status, syncedAt, errorReason)
     * and ledger linkage fields.
     */
    fun buildIntegrityCanonicalJson(tx: LocalTransaction): String {
        val isoTimestamp = java.time.Instant.ofEpochMilli(tx.createdAtDevice).toString()
        val sorted = TreeMap<String, String>()
        sorted["amount"] = tx.amount
        sorted["created_at_device"] = tx.createdAtDevice.toString()
        sorted["currency"] = tx.currency
        sorted["device_fingerprint"] = tx.deviceFingerprint.orEmpty()
        sorted["direction"] = tx.direction.orEmpty()
        sorted["nonce"] = tx.nonce
        sorted["payee_id"] = tx.payeeId.orEmpty()
        sorted["payer_id"] = tx.payerId.orEmpty()
        sorted["raw_payload"] = tx.rawPayload.orEmpty()
        sorted["receipt_data"] = tx.receiptData
        sorted["receipt_hash"] = tx.receiptHash
        sorted["receiver_public_key"] = tx.receiverPublicKey
        sorted["sender_wallet_id"] = tx.senderWalletId.toString()
        sorted["timestamp"] = isoTimestamp
        sorted["transaction_signature"] = tx.transactionSignature
        sorted["tx_id"] = tx.txId
        val o = JSONObject()
        sorted.forEach { (k, v) -> o.put(k, v) }
        return o.toString()
    }

    fun entryHash(prevHashHex: String, canonicalJson: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val input = "$prevHashHex|$canonicalJson".toByteArray(StandardCharsets.UTF_8)
        return md.digest(input).joinToString("") { b -> "%02x".format(b) }
    }

    /**
     * Prepares row for insert: encrypt at rest, then append hash-chain tail.
     */
    suspend fun appendEncryptedAndChained(
        dao: LocalTransactionDao,
        context: Context,
        tx: LocalTransaction,
    ): LocalTransaction {
        val enc = encryptAtRest(context, tx)
        val tail = dao.getLatestChainedTail()
        val prev = tail?.ledgerEntryHash?.takeIf { it.isNotBlank() } ?: GENESIS_PREV_HASH
        val seq = (tail?.ledgerSequence ?: 0L) + 1L
        val canonical = buildIntegrityCanonicalJson(enc)
        val entry = entryHash(prev, canonical)
        return enc.copy(
            ledgerPrevHash = prev,
            ledgerEntryHash = entry,
            ledgerSequence = seq,
        )
    }

    /**
     * Verifies all chained rows in order; returns first txId where recomputed hash mismatches, or null if OK.
     */
    suspend fun findFirstTamperedTxId(dao: LocalTransactionDao): String? {
        val chained = dao.getAllChainedOrderedBySequence()
        var expectedPrev = GENESIS_PREV_HASH
        for (tx in chained) {
            val canon = buildIntegrityCanonicalJson(tx)
            val expectedEntry = entryHash(expectedPrev, canon)
            val stored = tx.ledgerEntryHash ?: return tx.txId
            if (!stored.equals(expectedEntry, ignoreCase = true)) {
                return tx.txId
            }
            expectedPrev = stored
        }
        return null
    }
}
