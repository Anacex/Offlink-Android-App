package com.offlinepayment.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.offlinepayment.data.WalletDto
import com.offlinepayment.data.WalletTransferRequest
import com.offlinepayment.data.network.ApiClient
import com.offlinepayment.data.repository.WalletRepository
import com.offlinepayment.data.session.AuthSessionManager
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
    val lastTransferReference: String? = null
)

class WalletViewModel(
    private val repository: WalletRepository
) : ViewModel() {

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
                        it.copy(isLoading = false, wallets = wallets, errorMessage = null)
                    },
                    onFailure = { error ->
                        it.copy(isLoading = false, errorMessage = error.message)
                    }
                )
            }
        }
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
                        it.copy(
                            isLoading = false,
                            lastTransferReference = response.reference,
                            errorMessage = null
                        )
                    },
                    onFailure = { error ->
                        it.copy(isLoading = false, errorMessage = error.message)
                    }
                )
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repository = WalletRepository(ApiClient.walletApi)
                return WalletViewModel(repository) as T
            }
        }
    }
}

