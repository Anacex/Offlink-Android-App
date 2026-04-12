package com.offlinepayment.data.repository

import android.content.Context
import com.offlinepayment.data.SyncRequest
import com.offlinepayment.data.SyncResponse
import com.offlinepayment.data.SyncTransactionRequest
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
            emit(SyncResult.Error("Sync already in progress."))
            return@flow
        }
        
        val session = AuthSessionManager.currentSession()
        val token = session?.accessToken.orEmpty()
        if (token.isBlank() || token.startsWith("offline_token")) {
            emit(SyncResult.Error("Please login online to sync."))
            isSyncing.set(false)
            return@flow
        }

        // Check if online
        if (!NetworkUtils.isOnline(context)) {
            emit(SyncResult.Error("No internet connection. Will retry when online."))
            isSyncing.set(false)
            return@flow
        }
        
        // Get pending transactions
        val pendingTransactions = getPendingTransactions()
        
        if (pendingTransactions.isEmpty()) {
            Log.d("SyncRepository", "No pending transactions to sync")
            emit(SyncResult.Success(0, 0, "No pending transactions to sync"))
            return@flow
        }

        // Pre-validate and drop any rows missing required fields; mark them failed locally.
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
        emit(SyncResult.InProgress(pendingTransactions.size))
        
        try {
            // Convert LocalTransaction to SyncTransactionRequest
            val syncRequests = validTransactions.map { localTx ->
                convertToSyncRequest(localTx)
            }
            
            // Send batch to server
            val syncRequest = SyncRequest(transactions = syncRequests)
            val response: SyncResponse = syncApi.syncTransactions(syncRequest)
            
            Log.d(
                "SyncRepository",
                "Sync response: message=${response.message}, total_synced=${response.total_synced}, total_failed=${response.total_failed}"
            )
            
            // Process results and update local database
            var syncedCount = 0
            var failedCount = 0
            
            response.results.forEach { result ->
                // Treat duplicate nonce errors as already-synced to avoid flipping status to failed on retries.
                val isDuplicateNonce = result.error_reason?.contains("duplicate key value", ignoreCase = true) == true ||
                    result.error_reason?.contains("ix_offline_transactions_nonce", ignoreCase = true) == true ||
                    result.error_reason?.contains("uq_offline_recv_user_nonce", ignoreCase = true) == true ||
                    result.error_reason?.contains("offline_receiver_syncs", ignoreCase = true) == true
                val normalizedResult = if (isDuplicateNonce) {
                    result.copy(result = "synced", error_reason = null)
                } else {
                    result
                }

                // Find the corresponding local transaction by reference (nonce or txId)
                val localTx = pendingTransactions.find { tx ->
                    tx.nonce == normalizedResult.reference || tx.txId == normalizedResult.reference
                }
                
                if (localTx != null) {
                    val syncedAt = if (normalizedResult.result == "synced") System.currentTimeMillis() else null
                    
                    // Update transaction status in local DB
                    transactionDao.updateTransactionStatus(
                        txId = localTx.txId,
                        status = normalizedResult.result,
                        syncedAt = syncedAt,
                        errorReason = normalizedResult.error_reason
                    )
                    
                    if (normalizedResult.result == "synced") {
                        syncedCount++
                    } else {
                        Log.w(
                            "SyncRepository",
                            "Sync failed for reference=${normalizedResult.reference} reason=${normalizedResult.error_reason}"
                        )
                        failedCount++
                    }
                }
            }
            
            emit(SyncResult.Success(
                syncedCount = syncedCount,
                failedCount = failedCount,
                message = response.message
            ))
            
        } catch (e: Exception) {
            // Handle network errors or server errors
            val errorMessage = when {
                e.message?.contains("Unable to resolve host") == true -> 
                    "Server unreachable. Will retry later."
                e.message?.contains("timeout") == true -> 
                    "Connection timeout. Will retry later."
                else -> 
                    "Sync failed: ${e.message ?: "Unknown error"}"
            }
            
            emit(SyncResult.Error(errorMessage))
        } finally {
            isSyncing.set(false)
        }
    }
    
    /**
     * Convert LocalTransaction to SyncTransactionRequest format expected by API.
     */
    private fun convertToSyncRequest(localTx: LocalTransaction): SyncTransactionRequest {
        val receipt = buildReceiptMapForSync(localTx)

        val isReceived = localTx.direction?.equals("RECEIVED", ignoreCase = true) == true
        val transactionData: Map<String, Any> = if (isReceived) {
            mapOf(
                "direction" to "RECEIVED",
                "receiver_wallet_id" to localTx.senderWalletId,
                "amount" to localTx.amount,
                "currency" to localTx.currency,
                "nonce" to localTx.nonce,
                "timestamp" to java.time.Instant.ofEpochMilli(localTx.createdAtDevice).toString(),
                "payer_id" to (localTx.payerId ?: ""),
                "payee_id" to (localTx.payeeId ?: ""),
                "tx_id" to localTx.txId,
            )
        } else {
            mapOf(
                "sender_wallet_id" to localTx.senderWalletId,
                "receiver_public_key" to localTx.receiverPublicKey,
                "amount" to localTx.amount,
                "currency" to localTx.currency,
                "nonce" to localTx.nonce,
                "timestamp" to java.time.Instant.ofEpochMilli(localTx.createdAtDevice)
                    .toString(),
            )
        }
        
        val hasChain = !localTx.ledgerEntryHash.isNullOrBlank()
        val integrityJson = if (hasChain) {
            OfflineLedgerChain.buildIntegrityCanonicalJson(localTx)
        } else {
            null
        }

        return SyncTransactionRequest(
            transaction_data = transactionData,
            signature = localTx.transactionSignature,
            receipt = receipt,
            device_fingerprint = localTx.deviceFingerprint ?: DeviceFingerprintProvider.getFingerprint(),
            txId = localTx.txId,
            ledger_prev_hash = localTx.ledgerPrevHash,
            ledger_entry_hash = localTx.ledgerEntryHash,
            ledger_sequence = if (hasChain) localTx.ledgerSequence else null,
            integrity_canonical_json = integrityJson,
        )
    }

    /**
     * Receipt map for the API: always includes [LocalTransaction.receiptHash]; if [LocalTransaction.receiptData]
     * is JSON it is merged, otherwise it is sent as ciphertext (at-rest AES-GCM blob) for audit storage.
     */
    private fun buildReceiptMapForSync(localTx: LocalTransaction): Map<String, Any>? {
        val rd = localTx.receiptData
        val m = linkedMapOf<String, Any>()
        m["receipt_hash"] = localTx.receiptHash
        if (rd.isNotBlank()) {
            if (rd.trimStart().startsWith("{")) {
                try {
                    val json = JSONObject(rd)
                    val inner = json.toMap()
                    inner.forEach { (k, v) -> m[k] = v }
                } catch (_: Exception) {
                    m["receipt_ciphertext_b64"] = rd
                }
            } else {
                m["receipt_ciphertext_b64"] = rd
            }
        }
        return m
    }

    /**
     * Helper to convert JSONObject to Map<String, Any>
     */
    private fun JSONObject.toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        val keys = this.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = this.get(key)
            map[key] = when (value) {
                is JSONObject -> value.toMap()
                is org.json.JSONArray -> value.toList()
                else -> value
            }
        }
        return map
    }
    
    /**
     * Helper to convert JSONArray to List
     */
    private fun org.json.JSONArray.toList(): List<Any> {
        val list = mutableListOf<Any>()
        for (i in 0 until this.length()) {
            val value = this.get(i)
            list.add(when (value) {
                is JSONObject -> value.toMap()
                is org.json.JSONArray -> value.toList()
                else -> value
            })
        }
        return list
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

