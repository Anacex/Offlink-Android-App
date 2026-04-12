package com.offlinepayment.data.repository

import android.content.Context
import com.offlinepayment.data.SyncResponse
import com.offlinepayment.data.local.AppDatabase
import com.offlinepayment.data.local.LocalTransaction
import com.offlinepayment.data.network.ApiClient
import com.offlinepayment.data.session.AuthSessionManager
import com.offlinepayment.data.session.DeviceFingerprintProvider
import com.offlinepayment.security.OfflineLedgerChain
import com.offlinepayment.utils.NetworkUtils
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Repository for handling offline transaction synchronization.
 * Manages reading pending transactions, syncing to server, and updating local DB.
 */
class SyncRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val transactionDao = database.localTransactionDao()
    private val syncApi = ApiClient.syncApi
    
    companion object {
        // Prevent concurrent sync runs that would resend the same payloads and hit duplicate nonce errors.
        private val isSyncing = AtomicBoolean(false)
    }
    
    /**
     * Get all pending transactions that need to be synced.
     */
    suspend fun getPendingTransactions(): List<LocalTransaction> {
        return transactionDao.getPendingTransactions()
    }
    
    /**
     * Sync pending transactions to the server.
     * Returns a Flow that emits the sync result.
     */
    fun syncPendingTransactions(): Flow<SyncResult> = flow {
        if (!isSyncing.compareAndSet(false, true)) {
            // Another run holds the lock; skip quietly so session/network observers don't surface a bogus error.
            return@flow
        }
        try {
            val session = AuthSessionManager.currentSession()
            val token = session?.accessToken.orEmpty()
            if (token.isBlank() || token.startsWith("offline_token")) {
                emit(SyncResult.Error("Please login online to sync."))
                return@flow
            }

            if (!NetworkUtils.isOnline(context)) {
                emit(SyncResult.Error("No internet connection. Will retry when online."))
                return@flow
            }

            val pendingTransactions = getPendingTransactions()

            if (pendingTransactions.isEmpty()) {
                Log.d("SyncRepository", "No pending transactions to sync")
                emit(SyncResult.Success(0, 0, "No pending transactions to sync"))
                return@flow
            }

            val validTransactions = pendingTransactions.filter { tx ->
                val isReceived = tx.direction?.equals("RECEIVED", ignoreCase = true) == true
                val hasWallet = tx.senderWalletId > 0
                val hasSignature = !tx.transactionSignature.isNullOrBlank()
                val hasChain = !tx.ledgerEntryHash.isNullOrBlank() &&
                    !tx.ledgerPrevHash.isNullOrBlank() &&
                    tx.ledgerSequence > 0L
                val valid = if (isReceived) {
                    val hasPartyIds = !tx.payerId.isNullOrBlank() && !tx.payeeId.isNullOrBlank() && tx.txId.isNotBlank()
                    hasWallet && hasPartyIds && hasSignature && hasChain
                } else {
                    val hasReceiverPk = !tx.receiverPublicKey.isNullOrBlank() && !tx.receiverPublicKey.startsWith("pending_")
                    hasWallet && hasReceiverPk && hasSignature && hasChain
                }
                if (!valid) {
                    val reason = when {
                        !hasWallet -> "Missing wallet id for sync party"
                        isReceived && (tx.payerId.isNullOrBlank() || tx.payeeId.isNullOrBlank() || tx.txId.isBlank()) ->
                            "Missing payer_id, payee_id, or tx_id (receiver sync)"
                        !isReceived && (tx.receiverPublicKey.isNullOrBlank() || tx.receiverPublicKey.startsWith("pending_")) ->
                            "Missing receiver_public_key"
                        !hasSignature -> "Missing signature"
                        else -> "Missing hash-chained ledger fields (sync requires full chain for fraud checks)"
                    }
                    transactionDao.updateTransactionStatus(tx.txId, "failed", null, reason)
                }
                valid
            }

            if (validTransactions.isEmpty()) {
                Log.d("SyncRepository", "No valid transactions to sync after local validation")
                emit(SyncResult.Success(0, 0, "No valid transactions to sync"))
                return@flow
            }

            Log.d("SyncRepository", "Syncing ${validTransactions.size} pending transaction(s) (SENT and/or RECEIVED)")
            emit(SyncResult.InProgress(validTransactions.size))

            val json = buildSyncRequestJson(validTransactions)
            val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
            val response: SyncResponse = syncApi.syncTransactions(body)

            Log.d(
                "SyncRepository",
                "Sync response: message=${response.message}, total_synced=${response.total_synced}, total_failed=${response.total_failed}",
            )

            var syncedCount = 0
            var failedCount = 0

            response.results.forEach { result ->
                val isDuplicateNonce = result.error_reason?.contains("duplicate key value", ignoreCase = true) == true ||
                    result.error_reason?.contains("ix_offline_transactions_nonce", ignoreCase = true) == true ||
                    result.error_reason?.contains("uq_offline_recv_user_nonce", ignoreCase = true) == true ||
                    result.error_reason?.contains("offline_receiver_syncs", ignoreCase = true) == true
                val normalizedResult = if (isDuplicateNonce) {
                    result.copy(result = "synced", error_reason = null)
                } else {
                    result
                }

                val localTx = validTransactions.find { tx ->
                    tx.nonce == normalizedResult.reference || tx.txId == normalizedResult.reference
                }

                if (localTx != null) {
                    val syncedAt = if (normalizedResult.result == "synced") System.currentTimeMillis() else null
                    transactionDao.updateTransactionStatus(
                        txId = localTx.txId,
                        status = normalizedResult.result,
                        syncedAt = syncedAt,
                        errorReason = normalizedResult.error_reason,
                    )
                    if (normalizedResult.result == "synced") {
                        syncedCount++
                    } else {
                        Log.w(
                            "SyncRepository",
                            "Sync failed for reference=${normalizedResult.reference} reason=${normalizedResult.error_reason}",
                        )
                        failedCount++
                    }
                } else {
                    Log.w(
                        "SyncRepository",
                        "No local row for server result reference=${normalizedResult.reference} result=${normalizedResult.result}",
                    )
                }
            }

            emit(
                SyncResult.Success(
                    syncedCount = syncedCount,
                    failedCount = failedCount,
                    message = response.message,
                ),
            )
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("Unable to resolve host") == true ->
                    "Server unreachable. Will retry later."
                e.message?.contains("timeout", ignoreCase = true) == true ->
                    "Connection timeout. Will retry later."
                else ->
                    "Sync failed: ${e.message ?: "Unknown error"}"
            }
            emit(SyncResult.Error(errorMessage))
        } finally {
            isSyncing.set(false)
        }
    }

    private fun buildSyncRequestJson(validTransactions: List<LocalTransaction>): String {
        val root = JSONObject()
        val arr = JSONArray()
        validTransactions.forEach { arr.put(syncTransactionJsonObject(it)) }
        root.put("transactions", arr)
        return root.toString()
    }

    private fun syncTransactionJsonObject(localTx: LocalTransaction): JSONObject {
        val tx = JSONObject()
        tx.put("transaction_data", transactionDataJsonObject(localTx))
        tx.put("signature", localTx.transactionSignature)
        tx.put("receipt", receiptJsonObject(localTx))
        tx.put("device_fingerprint", localTx.deviceFingerprint ?: DeviceFingerprintProvider.getFingerprint())
        tx.put("txId", localTx.txId)
        val hasChain = !localTx.ledgerEntryHash.isNullOrBlank()
        if (hasChain) {
            tx.put("ledger_prev_hash", localTx.ledgerPrevHash)
            tx.put("ledger_entry_hash", localTx.ledgerEntryHash)
            tx.put("ledger_sequence", localTx.ledgerSequence)
            tx.put("integrity_canonical_json", OfflineLedgerChain.buildIntegrityCanonicalJson(localTx))
        }
        return tx
    }

    private fun transactionDataJsonObject(localTx: LocalTransaction): JSONObject {
        val td = JSONObject()
        val isReceived = localTx.direction?.equals("RECEIVED", ignoreCase = true) == true
        val ts = java.time.Instant.ofEpochMilli(localTx.createdAtDevice).toString()
        if (isReceived) {
            td.put("direction", "RECEIVED")
            td.put("receiver_wallet_id", localTx.senderWalletId)
            td.put("amount", localTx.amount)
            td.put("currency", localTx.currency)
            td.put("nonce", localTx.nonce)
            td.put("timestamp", ts)
            td.put("payer_id", localTx.payerId ?: "")
            td.put("payee_id", localTx.payeeId ?: "")
            td.put("tx_id", localTx.txId)
        } else {
            td.put("sender_wallet_id", localTx.senderWalletId)
            td.put("receiver_public_key", localTx.receiverPublicKey)
            td.put("amount", localTx.amount)
            td.put("currency", localTx.currency)
            td.put("nonce", localTx.nonce)
            td.put("timestamp", ts)
        }
        return td
    }

    /**
     * Receipt object: [receipt_hash] plus merged JSON or ciphertext (matches server expectations).
     */
    private fun receiptJsonObject(localTx: LocalTransaction): JSONObject {
        val o = JSONObject()
        o.put("receipt_hash", localTx.receiptHash)
        val rd = localTx.receiptData
        if (rd.isNotBlank()) {
            if (rd.trimStart().startsWith("{")) {
                try {
                    val inner = JSONObject(rd)
                    val keys = inner.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        o.put(k, inner.get(k))
                    }
                } catch (_: Exception) {
                    o.put("receipt_ciphertext_b64", rd)
                }
            } else {
                o.put("receipt_ciphertext_b64", rd)
            }
        }
        return o
    }
    
    /**
     * Get transactions by status (for UI display).
     */
    suspend fun getTransactionsByStatus(status: String): List<LocalTransaction> {
        return transactionDao.getTransactionsByStatus(status)
    }
    
    /**
     * Observe transactions for a wallet (for UI updates).
     */
    fun observeTransactionsByWallet(walletId: Int): Flow<List<LocalTransaction>> {
        return transactionDao.observeTransactionsByWallet(walletId)
    }
}

/**
 * Result of sync operation.
 */
sealed class SyncResult {
    data class InProgress(val totalTransactions: Int) : SyncResult()
    data class Success(
        val syncedCount: Int,
        val failedCount: Int,
        val message: String
    ) : SyncResult()
    data class Error(val message: String) : SyncResult()
}

