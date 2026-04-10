package com.offlinepayment.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.offlinepayment.data.WalletDto
import com.offlinepayment.data.WalletTransferRequest
import com.offlinepayment.data.SyncedOfflineTransaction
import com.offlinepayment.data.network.ApiClient
import com.offlinepayment.data.repository.WalletRepository
import com.offlinepayment.data.session.AuthSessionManager
import com.offlinepayment.utils.ErrorUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class WalletUiState(
    val isLoading: Boolean = false,
    val wallets: List<WalletDto> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val lastTransferReference: String? = null,
    val transferHistory: List<com.offlinepayment.data.WalletTransferResponse> = emptyList(),
    val isLoadingHistory: Boolean = false,
    val syncedTransactions: List<SyncedOfflineTransaction> = emptyList(),
    val isLoadingSyncedTransactions: Boolean = false,
    val isCreatingWallet: Boolean = false,
    val walletCreationOtp: String? = null,
    val walletCreationBankAccount: String? = null
)

class WalletViewModel(
    private val repository: WalletRepository,
    private val application: android.app.Application? = null
) : ViewModel() {
    
    init {
        // Start periodic refresh of cached wallet data when online
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(300000) // Refresh every 5 minutes
                try {
                    repository.refreshCachedWallets()
                } catch (e: Exception) {
                    // Silently fail - will retry on next cycle
                }
            }
        }
        
        // Refresh wallets after sync completes (if sync ViewModel is available)
        // This will be called from MainActivity when sync completes
    }
    
    /**
     * Called after sync completes to refresh wallet balances and synced transactions.
     */
    fun onSyncCompleted() {
        refreshWallets()
        getSyncedTransactions() // Refresh synced transactions after sync
    }

    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    private var sessionJob: Job? = null

    init {
        sessionJob = viewModelScope.launch {
            AuthSessionManager.observeSession().collectLatest { session ->
                if (session != null) {
                    refreshWallets()
                } else {
                    _uiState.value = WalletUiState()
                }
            }
        }
    }

    fun refreshWallets() {
        if (AuthSessionManager.currentSession() == null) {
            _uiState.value = WalletUiState()
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.listWallets()
            _uiState.update {
                result.fold(
                    onSuccess = { wallets ->
                        // User can have only one wallet - no auto-creation needed
                        it.copy(isLoading = false, wallets = wallets, errorMessage = null)
                    },
                    onFailure = { error ->
                        it.copy(isLoading = false, errorMessage = ErrorUtils.cleanErrorMessageForDisplay(error.message))
                    }
                )
            }
        }
    }
    
    private fun createOfflineWalletInternal() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingWallet = true, errorMessage = null) }
            val result = repository.createWallet(
                com.offlinepayment.data.WalletCreateRequest(
                    wallet_type = "offline",
                    currency = "PKR",
                    bank_account_number = "" // This method is deprecated, use initiateWalletCreation instead
                )
            )
            _uiState.update { it.copy(isCreatingWallet = false) }
            result.fold(
                onSuccess = { wallet ->
                    // Refresh wallets after creation to show the new wallet
                    val refreshResult = repository.listWallets()
                    refreshResult.fold(
                        onSuccess = { wallets ->
                            _uiState.update { it.copy(wallets = wallets, errorMessage = null) }
                        },
                        onFailure = { error ->
                            _uiState.update { it.copy(errorMessage = ErrorUtils.cleanErrorMessageForDisplay(error.message)) }
                        }
                    )
                },
                onFailure = { error ->
                    _uiState.update { it.copy(errorMessage = "Failed to create wallet: ${ErrorUtils.cleanErrorMessageForDisplay(error.message) ?: "Unknown error"}") }
                }
            )
        }
    }
    
    fun initiateWalletCreation(bankAccountNumber: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingWallet = true, errorMessage = null) }
            val result = repository.initiateWalletCreation(
                com.offlinepayment.data.WalletCreateRequest(
                    wallet_type = "offline",  // All wallets are offline type
                    currency = "PKR",
                    bank_account_number = bankAccountNumber
                )
            )
            _uiState.update { it.copy(isCreatingWallet = false) }
            result.fold(
                onSuccess = { response ->
                    _uiState.update { 
                        it.copy(
                            walletCreationOtp = response.otp_demo,
                            walletCreationBankAccount = bankAccountNumber,
                            errorMessage = null
                        ) 
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(errorMessage = "Failed to initiate wallet creation: ${ErrorUtils.cleanErrorMessageForDisplay(error.message) ?: "Unknown error"}") }
                }
            )
        }
    }
    
    fun verifyAndCreateWallet(otp: String) {
        val bankAccount = _uiState.value.walletCreationBankAccount
        if (bankAccount == null) {
            _uiState.update { it.copy(errorMessage = "Bank account information missing. Please start over.") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingWallet = true, errorMessage = null) }
            val result = repository.verifyAndCreateWallet(
                com.offlinepayment.data.WalletCreateVerifyRequest(
                    wallet_type = "offline",  // All wallets are offline type
                    currency = "PKR",
                    bank_account_number = bankAccount,
                    otp = otp
                )
            )
            _uiState.update { it.copy(isCreatingWallet = false) }
            result.fold(
                onSuccess = { wallet ->
                    // Refresh wallets after creation
                    val refreshResult = repository.listWallets()
                    refreshResult.fold(
                        onSuccess = { wallets ->
                            _uiState.update { 
                                it.copy(
                                    wallets = wallets, 
                                    errorMessage = null,
                                    walletCreationOtp = null,
                                    walletCreationBankAccount = null,
                                    successMessage = "Wallet created successfully!"
                                ) 
                            }
                            // Clear success message after 5 seconds
                            viewModelScope.launch {
                                kotlinx.coroutines.delay(5000)
                                _uiState.update { it.copy(successMessage = null) }
                            }
                        },
                        onFailure = { error ->
                            _uiState.update { it.copy(errorMessage = ErrorUtils.cleanErrorMessageForDisplay(error.message)) }
                        }
                    )
                },
                onFailure = { error ->
                    _uiState.update { it.copy(errorMessage = "Failed to create wallet: ${ErrorUtils.cleanErrorMessageForDisplay(error.message) ?: "Unknown error"}") }
                }
            )
        }
    }
    
    fun createOfflineWallet() {
        // This is kept for backward compatibility but should not be used
        // Wallet creation now requires bank account and OTP
        createOfflineWalletInternal()
    }

    fun transfer(fromWalletId: Int, toWalletId: Int, amount: BigDecimal) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val request = WalletTransferRequest(
                from_wallet_id = fromWalletId,
                to_wallet_id = toWalletId,
                amount = amount
            )
            val result = repository.transfer(request)
            _uiState.update {
                result.fold(
                    onSuccess = { response ->
                        // Refresh balances after a successful transfer.
                        refreshWallets()
                        // Refresh transfer history
                        getTransferHistory()
                        it.copy(
                            isLoading = false,
                            lastTransferReference = response.reference,
                            errorMessage = null
                        )
                    },
                    onFailure = { error ->
                        it.copy(isLoading = false, errorMessage = ErrorUtils.cleanErrorMessageForDisplay(error.message))
                    }
                )
            }
        }
    }

    fun getTransferHistory() {
        if (AuthSessionManager.currentSession() == null) {
            _uiState.update { it.copy(transferHistory = emptyList()) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHistory = true) }
            val result = repository.getTransferHistory()
            _uiState.update {
                result.fold(
                    onSuccess = { history ->
                        it.copy(
                            isLoadingHistory = false,
                            transferHistory = history,
                            errorMessage = null
                        )
                    },
                    onFailure = { error ->
                        it.copy(
                            isLoadingHistory = false,
                            errorMessage = error.message
                        )
                    }
                )
            }
        }
    }
    
    fun getSyncedTransactions() {
        if (AuthSessionManager.currentSession() == null) {
            _uiState.update { it.copy(syncedTransactions = emptyList()) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSyncedTransactions = true) }
            val result = repository.getSyncedTransactions(statusFilter = "synced", limit = 100)
            _uiState.update {
                result.fold(
                    onSuccess = { transactions ->
                        it.copy(
                            isLoadingSyncedTransactions = false,
                            syncedTransactions = transactions,
                            errorMessage = null
                        )
                    },
                    onFailure = { error ->
                        it.copy(
                            isLoadingSyncedTransactions = false,
                            syncedTransactions = emptyList(),
                            errorMessage = error.message
                        )
                    }
                )
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                // Note: Context should be passed from UI layer
                // For now, using API-only repository (will be updated in MainActivity)
                val repository = WalletRepository(ApiClient.walletApi)
                return WalletViewModel(repository) as T
            }
        }
        
        fun createFactory(context: android.content.Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return WalletViewModel(
                        WalletRepository(ApiClient.walletApi, context),
                        context.applicationContext as? android.app.Application
                    ) as T
                }
            }
        }
    }
}

