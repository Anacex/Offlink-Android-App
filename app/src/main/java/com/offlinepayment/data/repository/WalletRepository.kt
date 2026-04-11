package com.offlinepayment.data.repository

import android.content.Context
import com.offlinepayment.data.ApiErrorResponse
import com.offlinepayment.data.TopUpRequest
import com.offlinepayment.data.TopUpResponse
import com.offlinepayment.data.TopUpVerifyRequest
import com.offlinepayment.data.TopUpVerifyResponse
import com.offlinepayment.data.WalletCreateRequest
import com.offlinepayment.data.WalletCreateResponse
import com.offlinepayment.data.WalletCreateVerifyRequest
import com.offlinepayment.data.WalletDto
import com.offlinepayment.data.WalletTransferRequest
import com.offlinepayment.data.WalletTransferResponse
import com.offlinepayment.data.SyncedOfflineTransaction
import com.offlinepayment.data.local.AppDatabase
import com.offlinepayment.data.local.LocalTransaction
import com.offlinepayment.data.local.OfflineWallet
import com.offlinepayment.data.network.WalletApi
import com.offlinepayment.data.network.ApiClient
import android.content.SharedPreferences
import com.offlinepayment.data.session.AuthSessionManager
import com.offlinepayment.security.OfflineLedgerChain
import com.offlinepayment.utils.EncryptionHelper
import com.offlinepayment.utils.ErrorUtils
import com.offlinepayment.utils.NetworkUtils
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.math.BigDecimal

class WalletRepository(
    private val api: WalletApi,
    private val context: Context? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val database = context?.let { AppDatabase.getDatabase(it) }
    private val offlineWalletDao = database?.offlineWalletDao()
    private val localTransactionDao = database?.localTransactionDao()
    
    private val sharedPreferences: SharedPreferences? = context?.getSharedPreferences(
        "offline_payment_prefs",
        android.content.Context.MODE_PRIVATE
    )
    
    companion object {
        private const val PREF_LAST_ONLINE_SESSION = "last_online_session_timestamp"
        private const val TWENTY_FOUR_HOURS_MS = 24 * 60 * 60 * 1000L
    }

    suspend fun ensureSessionOrThrow() {
        requireNotNull(AuthSessionManager.currentSession()) {
            "Auth session required for wallet operations"
        }
    }

    suspend fun initiateWalletCreation(request: WalletCreateRequest): Result<WalletCreateResponse> = safeCall {
        ensureSessionOrThrow()
        api.initiateWalletCreation(request)
    }
    
    suspend fun verifyAndCreateWallet(request: WalletCreateVerifyRequest): Result<WalletDto> = safeCall {
        ensureSessionOrThrow()
        api.verifyAndCreateWallet(request)
    }
    
    suspend fun createWallet(request: WalletCreateRequest): Result<WalletDto> = safeCall {
        ensureSessionOrThrow()
        api.createWallet(request)
    }

    suspend fun listWallets(): Result<List<WalletDto>> = withContext(ioDispatcher) {
        ensureSessionOrThrow()
        val isOnline = context?.let { NetworkUtils.isOnline(it) } ?: true
        
        if (isOnline) {
            // Fetch from API and cache locally
            try {
                val wallets = api.listWallets()
                cacheWalletsLocally(wallets)
                Result.success(wallets)
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                val errorMessage = ErrorUtils.extractErrorMessage(
                    errorBody = errorBody,
                    httpCode = e.code(),
                    defaultMessage = "HTTP ${e.code()}: ${e.message()}"
                )
                Result.failure(Exception(errorMessage))
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            // Use cached data when offline
            val userId = AuthSessionManager.currentSession()?.let { session ->
                // Try to get userId from cached user data
                database?.offlineUserDao()?.getUserByEmail(session.userEmail ?: "")?.userId
            }
            
            if (userId != null) {
                val cachedWallets = offlineWalletDao?.getWalletsByUserId(userId) ?: emptyList()
                val walletDtos = cachedWallets.map { it.toWalletDto() }
                Result.success(walletDtos)
            } else {
                Result.failure(Exception("No cached wallet data available. Please connect to internet."))
            }
        }
    }
    
    /**
     * Caches wallet data locally for offline access.
     * Also fetches and encrypts private keys for each wallet.
     */
    private suspend fun cacheWalletsLocally(wallets: List<WalletDto>) {
        if (context == null || offlineWalletDao == null) return
        
        val offlineWallets = wallets.map { wallet ->
            // Fetch private key if available (for offline QR generation)
            val privateKeyEncrypted = try {
                if (wallet.wallet_type == "offline") {
                    val privateKeyResult = getWalletPrivateKey(wallet.id)
                    privateKeyResult.getOrNull()?.let { privateKey ->
                        EncryptionHelper.encrypt(context, privateKey)
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                null // Private key fetch failed, continue without it
            }
            
            OfflineWallet(
                walletId = wallet.id,
                userId = wallet.user_id ?: 0,
                walletType = wallet.wallet_type,
                currency = wallet.currency,
                balance = wallet.balance.toPlainString(),
                publicKey = wallet.public_key,
                privateKeyEncrypted = privateKeyEncrypted,
                bankAccountNumber = wallet.bank_account_number,
                isActive = wallet.is_active,
                lastSyncedAt = System.currentTimeMillis()
            )
        }
        
        offlineWalletDao.insertWallets(offlineWallets)
    }
    
    /**
     * Gets cached wallet data for offline use.
     */
    suspend fun getCachedWallet(walletId: Int): OfflineWallet? {
        return offlineWalletDao?.getWalletById(walletId)
    }
    
    /**
     * Gets decrypted private key from cached wallet.
     */
    suspend fun getCachedPrivateKey(walletId: Int): String? {
        if (context == null) return null
        val wallet = offlineWalletDao?.getWalletById(walletId) ?: return null
        return wallet.privateKeyEncrypted?.let {
            try {
                EncryptionHelper.decrypt(context, it)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Observes cached wallets for a user (for reactive UI updates).
     */
    fun observeCachedWallets(userId: Int): Flow<List<WalletDto>>? {
        return offlineWalletDao?.observeWalletsByUserId(userId)?.map { wallets ->
            wallets.map { it.toWalletDto() }
        }
    }
    
    /**
     * Refreshes cached wallet data from API (when online).
     */
    suspend fun refreshCachedWallets(): Result<Unit> = safeCall {
        ensureSessionOrThrow()
        val isOnline = context?.let { NetworkUtils.isOnline(it) } ?: true
        if (!isOnline) {
            throw Exception("Cannot refresh: device is offline")
        }
        
        val wallets = api.listWallets()
        cacheWalletsLocally(wallets)
    }
    
    /**
     * Extension function to convert OfflineWallet to WalletDto.
     */
    private fun OfflineWallet.toWalletDto(): WalletDto {
        return WalletDto(
            id = walletId,
            user_id = userId,
            wallet_type = walletType,
            currency = currency,
            balance = BigDecimal(balance),
            public_key = publicKey,
            bank_account_number = bankAccountNumber,
            is_active = isActive,
            created_at = null,
            updated_at = null
        )
    }

    suspend fun transfer(request: WalletTransferRequest): Result<WalletTransferResponse> = safeCall {
        ensureSessionOrThrow()
        api.transfer(request)
    }

    suspend fun getWallet(id: Int): Result<WalletDto> = safeCall {
        ensureSessionOrThrow()
        api.getWallet(id)
    }

    suspend fun getTransferHistory(): Result<List<WalletTransferResponse>> = safeCall {
        ensureSessionOrThrow()
        api.transferHistory()
    }

    suspend fun topUp(request: TopUpRequest): Result<TopUpResponse> = safeCall {
        ensureSessionOrThrow()
        api.topUp(request)
    }

    suspend fun verifyTopUp(request: TopUpVerifyRequest): Result<TopUpVerifyResponse> = safeCall {
        ensureSessionOrThrow()
        api.verifyTopUp(request)
    }
    
    suspend fun getWalletPrivateKey(walletId: Int): Result<String> {
        ensureSessionOrThrow()
        val isOnline = context?.let { NetworkUtils.isOnline(it) } ?: true
        
        return if (isOnline) {
            // Fetch from API and cache
            safeCall {
                val response = api.getWalletPrivateKey(walletId)
                val privateKey = response["private_key"] ?: throw Exception("Private key not found in response")
                
                // Cache the encrypted private key
                if (context != null) {
                    val wallet = offlineWalletDao?.getWalletById(walletId)
                    if (wallet != null) {
                        val encrypted = EncryptionHelper.encrypt(context, privateKey)
                        offlineWalletDao?.insertWallet(
                            wallet.copy(
                                privateKeyEncrypted = encrypted,
                                lastSyncedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
                
                privateKey
            }
        } else {
            // Use cached private key when offline
            val cachedKey = getCachedPrivateKey(walletId)
            if (cachedKey != null) {
                Result.success(cachedKey)
            } else {
                Result.failure(Exception("Private key not available offline. Please connect to internet."))
            }
        }
    }

    /**
     * Save a local transaction to the database.
     * Applies AES-GCM encryption to plaintext JSON in [LocalTransaction.rawPayload] / [LocalTransaction.receiptData]
     * when [context] is available, then appends a hash-chain entry for tamper detection at sync.
     */
    suspend fun saveLocalTransaction(transaction: LocalTransaction) {
        withContext(ioDispatcher) {
            val dao = localTransactionDao ?: return@withContext
            val ctx = context
            val toStore = if (ctx != null) {
                OfflineLedgerChain.appendEncryptedAndChained(dao, ctx, transaction)
            } else {
                transaction
            }
            dao.insertTransaction(toStore)
        }
    }
    
    /**
     * Get all local transactions for a user.
     */
    suspend fun getLocalTransactions(userId: String): List<LocalTransaction> {
        return withContext(ioDispatcher) {
            localTransactionDao?.getAllTransactionsForUser(userId) ?: emptyList()
        }
    }

    /**
     * Observe all local transactions (sent/received) for a user.
     * This keeps the history screen updated after sync status changes.
     */
    fun observeLocalTransactions(userId: String): Flow<List<LocalTransaction>> {
        return localTransactionDao?.observeTransactionsForUser(userId) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }
    
    /**
     * Get sent transactions for a user.
     */
    suspend fun getSentTransactions(userId: String): List<LocalTransaction> {
        return withContext(ioDispatcher) {
            localTransactionDao?.getSentTransactions(userId) ?: emptyList()
        }
    }
    
    /**
     * Get received transactions for a user.
     */
    suspend fun getReceivedTransactions(userId: String): List<LocalTransaction> {
        return withContext(ioDispatcher) {
            localTransactionDao?.getReceivedTransactions(userId) ?: emptyList()
        }
    }
    
    /**
     * Update offline wallet balance.
     */
    suspend fun updateOfflineWalletBalance(walletId: Int, newBalance: String) {
        withContext(ioDispatcher) {
            offlineWalletDao?.updateBalance(walletId, newBalance)
        }
    }
    
    /**
     * Get offline wallet by ID.
     */
    suspend fun getOfflineWalletById(walletId: Int): OfflineWallet? {
        return withContext(ioDispatcher) {
            offlineWalletDao?.getWalletById(walletId)
        }
    }
    
    /**
     * Get offline wallet by userId and type.
     */
    suspend fun getOfflineWalletByUserIdAndType(userId: Int, walletType: String): OfflineWallet? {
        return withContext(ioDispatcher) {
            offlineWalletDao?.getWalletByUserIdAndType(userId, walletType)
        }
    }
    
    /**
     * Get synced offline transactions from the server.
     * Returns transactions that have been successfully synced to the backend.
     * Also caches them locally for offline access.
     */
    suspend fun getSyncedTransactions(statusFilter: String? = null, limit: Int = 50): Result<List<SyncedOfflineTransaction>> {
        return withContext(ioDispatcher) {
            ensureSessionOrThrow()
            val isOnline = context?.let { NetworkUtils.isOnline(it) } ?: true
            
            if (!isOnline) {
                // Return cached synced transactions when offline, filtered by 24-hour window
                val cachedTransactions = getCachedSyncedTransactions(statusFilter, limit)
                val lastOnlineSession = getLastOnlineSessionTimestamp()
                if (lastOnlineSession != null) {
                    val filteredCached = filterTransactionsBy24HourWindow(cachedTransactions, lastOnlineSession)
                    return@withContext Result.success(filteredCached)
                } else {
                    // No previous online session, return empty
                    return@withContext Result.success(emptyList())
                }
            }
            
            try {
                val transactions = ApiClient.syncApi.getSyncedTransactions(statusFilter, limit)
                // Cache synced transactions locally for offline access
                cacheSyncedTransactions(transactions)
                
                // Update last online session timestamp to current time
                val currentTime = System.currentTimeMillis()
                updateLastOnlineSessionTimestamp(currentTime)
                
                // Filter to only show transactions settled within last 24 hours of this online session
                val filteredTransactions = filterTransactionsBy24HourWindow(transactions, currentTime)
                Result.success(filteredTransactions)
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                val errorMessage = ErrorUtils.extractErrorMessage(
                    errorBody = errorBody,
                    httpCode = e.code(),
                    defaultMessage = "HTTP ${e.code()}: ${e.message()}"
                )
                // On error, try to return cached data
                val cachedTransactions = getCachedSyncedTransactions(statusFilter, limit)
                val lastOnlineSession = getLastOnlineSessionTimestamp()
                if (lastOnlineSession != null) {
                    val filteredCached = filterTransactionsBy24HourWindow(cachedTransactions, lastOnlineSession)
                    if (filteredCached.isNotEmpty()) {
                        Result.success(filteredCached)
                    } else {
                        Result.failure(Exception(errorMessage))
                    }
                } else {
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                // On error, try to return cached data
                val cachedTransactions = getCachedSyncedTransactions(statusFilter, limit)
                val lastOnlineSession = getLastOnlineSessionTimestamp()
                if (lastOnlineSession != null) {
                    val filteredCached = filterTransactionsBy24HourWindow(cachedTransactions, lastOnlineSession)
                    if (filteredCached.isNotEmpty()) {
                        Result.success(filteredCached)
                    } else {
                        Result.failure(e)
                    }
                } else {
                    Result.failure(e)
                }
            }
        }
    }
    
    /**
     * Cache synced transactions in local database for offline access.
     */
    private suspend fun cacheSyncedTransactions(syncedTransactions: List<SyncedOfflineTransaction>) {
        if (localTransactionDao == null) return
        
        withContext(ioDispatcher) {
            try {
                val localTransactions = syncedTransactions.map { syncedTx ->
                    convertSyncedToLocal(syncedTx)
                }
                // Use REPLACE strategy to update existing transactions or insert new ones
                localTransactionDao.insertTransactions(localTransactions)
            } catch (e: Exception) {
                // Silently fail - caching is best effort
                android.util.Log.e("WalletRepository", "Failed to cache synced transactions: ${e.message}")
            }
        }
    }
    
    /**
     * Convert SyncedOfflineTransaction from server to LocalTransaction for local storage.
     */
    private fun convertSyncedToLocal(syncedTx: SyncedOfflineTransaction): LocalTransaction {
        // Parse ISO datetime strings to millis
        val createdAtDeviceMillis = parseISOToMillis(syncedTx.createdAtDevice)
        val syncedAtMillis = syncedTx.syncedAt?.let { parseISOToMillis(it) }
        
        // Use nonce as txId if we don't have a local txId, or generate one from server id
        val txId = "synced_${syncedTx.id}_${syncedTx.nonce}"
        
        return LocalTransaction(
            txId = txId,
            senderWalletId = syncedTx.senderWalletId,
            receiverPublicKey = syncedTx.receiverPublicKey,
            amount = syncedTx.amount,
            currency = syncedTx.currency,
            transactionSignature = syncedTx.transactionSignature,
            nonce = syncedTx.nonce,
            receiptHash = syncedTx.receiptHash,
            receiptData = syncedTx.receiptData,
            status = syncedTx.status,
            createdAtDevice = createdAtDeviceMillis,
            syncedAt = syncedAtMillis,
            errorReason = null,
            deviceFingerprint = null,
            payerId = null, // Not available from server
            payeeId = null, // Not available from server
            direction = "SENT", // Synced transactions are always sent by current user
            rawPayload = null,
            ledgerPrevHash = null,
            ledgerEntryHash = null,
            ledgerSequence = 0L,
        )
    }
    
    /**
     * Parse ISO 8601 datetime string to milliseconds timestamp.
     */
    private fun parseISOToMillis(isoString: String): Long {
        return try {
            val instant = java.time.Instant.parse(
                isoString.replace(" ", "T").let {
                    if (!it.contains("T")) "${it}T00:00:00Z"
                    else if (!it.contains("Z") && !it.contains("+") && (it.length <= 10 || !it.substring(10).contains("-"))) "${it}Z"
                    else it
                }
            )
            instant.toEpochMilli()
        } catch (e: Exception) {
            try {
                val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                inputFormat.parse(isoString)?.time ?: System.currentTimeMillis()
            } catch (e2: Exception) {
                System.currentTimeMillis()
            }
        }
    }
    
    /**
     * Get cached synced transactions from local database.
     */
    private suspend fun getCachedSyncedTransactions(statusFilter: String?, limit: Int): List<SyncedOfflineTransaction> {
        if (localTransactionDao == null) return emptyList()
        
        return withContext(ioDispatcher) {
            try {
                val localTxs = if (statusFilter != null) {
                    localTransactionDao.getTransactionsByStatus(statusFilter)
                } else {
                    // Get synced and confirmed transactions
                    val synced = localTransactionDao.getTransactionsByStatus("synced")
                    val confirmed = localTransactionDao.getTransactionsByStatus("confirmed")
                    (synced + confirmed).distinctBy { it.nonce }.sortedByDescending { it.createdAtDevice }
                }
                
                // Convert LocalTransaction back to SyncedOfflineTransaction format
                localTxs.take(limit).map { localTx ->
                    SyncedOfflineTransaction(
                        id = localTx.txId.hashCode(), // Generate a pseudo-id from txId
                        senderWalletId = localTx.senderWalletId,
                        receiverPublicKey = localTx.receiverPublicKey,
                        amount = localTx.amount,
                        currency = localTx.currency,
                        transactionSignature = localTx.transactionSignature,
                        nonce = localTx.nonce,
                        receiptHash = localTx.receiptHash,
                        receiptData = localTx.receiptData,
                        status = localTx.status,
                        createdAtDevice = formatMillisToISO(localTx.createdAtDevice),
                        syncedAt = localTx.syncedAt?.let { formatMillisToISO(it) },
                        confirmedAt = null, // Not stored in LocalTransaction
                        createdAt = formatMillisToISO(localTx.createdAtDevice)
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    /**
     * Format milliseconds timestamp to ISO 8601 datetime string.
     */
    private fun formatMillisToISO(millis: Long): String {
        return try {
            val instant = java.time.Instant.ofEpochMilli(millis)
            instant.toString()
        } catch (e: Exception) {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault())
                .format(java.util.Date(millis))
        }
    }
    
    /**
     * Update the last online session timestamp.
     */
    private fun updateLastOnlineSessionTimestamp(timestamp: Long) {
        sharedPreferences?.edit()?.putLong(PREF_LAST_ONLINE_SESSION, timestamp)?.apply()
    }
    
    /**
     * Get the last online session timestamp.
     * Returns null if never online or timestamp not set.
     */
    private fun getLastOnlineSessionTimestamp(): Long? {
        val timestamp = sharedPreferences?.getLong(PREF_LAST_ONLINE_SESSION, -1L)
        return if (timestamp != null && timestamp > 0) timestamp else null
    }
    
    /**
     * Filter transactions to only show those settled (synced) within 24 hours of the reference time.
     * When online: referenceTime is current time, shows transactions synced in last 24 hours.
     * When offline: referenceTime is last online session, shows transactions synced within 24 hours before that.
     */
    private fun filterTransactionsBy24HourWindow(
        transactions: List<SyncedOfflineTransaction>,
        referenceTime: Long
    ): List<SyncedOfflineTransaction> {
        val cutoffTime = referenceTime - TWENTY_FOUR_HOURS_MS
        
        return transactions.filter { transaction ->
            // Only include transactions that have been synced or confirmed
            if (transaction.status != "synced" && transaction.status != "confirmed") {
                return@filter false
            }
            
            // Parse syncedAt timestamp
            val syncedAtMillis = transaction.syncedAt?.let { parseISOToMillis(it) } ?: return@filter false
            
            // Include if synced within 24 hours before the reference time
            syncedAtMillis >= cutoffTime && syncedAtMillis <= referenceTime
        }
    }

    private suspend fun <T> safeCall(block: suspend () -> T): Result<T> =
        withContext(ioDispatcher) {
            try {
                Result.success(block())
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                val errorMessage = ErrorUtils.extractErrorMessage(
                    errorBody = errorBody,
                    httpCode = e.code(),
                    defaultMessage = "HTTP ${e.code()}: ${e.message()}"
                )
                Result.failure(Exception(errorMessage))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}

