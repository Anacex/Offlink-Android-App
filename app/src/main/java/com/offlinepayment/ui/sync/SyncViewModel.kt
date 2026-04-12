package com.offlinepayment.ui.sync

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.offlinepayment.data.repository.SyncRepository
import com.offlinepayment.data.repository.SyncResult
import com.offlinepayment.data.session.AuthSessionManager
import com.offlinepayment.utils.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * ViewModel for managing transaction synchronization.
 * Automatically syncs when network becomes available.
 */
class SyncViewModel(application: Application) : AndroidViewModel(application) {
    private val syncRepository = SyncRepository(application)
    
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
    
    init {
        // Monitor network connectivity
        NetworkMonitor.connectivityFlow(application)
            .onEach { isOnline ->
                _isOnline.value = isOnline

                // Auto-sync when online (retry after errors too — Idle-only would block forever after Error)
                if (isOnline && _syncState.value !is SyncState.InProgress) {
                    syncPendingTransactions()
                }
            }
            .launchIn(viewModelScope)

        // When the user obtains a real (non-offline) session while already on Wi‑Fi, sync immediately
        AuthSessionManager.observeSession()
            .onEach { session ->
                val token = session?.accessToken.orEmpty()
                val canSync = token.isNotBlank() && !token.startsWith("offline_token")
                if (canSync && _isOnline.value && _syncState.value !is SyncState.InProgress) {
                    syncPendingTransactions()
                }
            }
            .launchIn(viewModelScope)
    }
    
    /**
     * Manually trigger sync of pending transactions.
     */
    fun syncPendingTransactions() {
        if (_syncState.value is SyncState.InProgress) {
            return // Already syncing
        }
        
        viewModelScope.launch {
            syncRepository.syncPendingTransactions()
                .collect { result ->
                    _syncState.value = when (result) {
                        is SyncResult.InProgress -> SyncState.InProgress(result.totalTransactions)
                        is SyncResult.Success -> SyncState.Success(
                            syncedCount = result.syncedCount,
                            failedCount = result.failedCount,
                            message = result.message
                        )
                        is SyncResult.Error -> SyncState.Error(result.message)
                    }
                }
        }
    }
    
    /**
     * Reset sync state to idle.
     */
    fun resetSyncState() {
        _syncState.value = SyncState.Idle
    }
}

/**
 * UI state for sync operations.
 */
sealed class SyncState {
    object Idle : SyncState()
    data class InProgress(val totalTransactions: Int) : SyncState()
    data class Success(
        val syncedCount: Int,
        val failedCount: Int,
        val message: String
    ) : SyncState()
    data class Error(val message: String) : SyncState()
}

