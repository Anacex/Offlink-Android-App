package com.offlinepayment.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.offlinepayment.data.repository.AuthRepository
import com.offlinepayment.data.network.ApiClient
import com.offlinepayment.data.session.AuthSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.offlinepayment.utils.NetworkUtils
import com.offlinepayment.utils.ErrorUtils

data class ProfileUiState(
    val isLoading: Boolean = false,
    val userName: String = "",
    val email: String = "",
    val phone: String? = null,
    val totalBalance: Double = 0.0,
    val offlineBalance: Double = 0.0,
    val errorMessage: String? = null
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val context: Context? = null
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    private var refreshJob: kotlinx.coroutines.Job? = null
    private var lastRefreshTime: Long = 0
    private val REFRESH_INTERVAL_MS = 30_000L // Refresh every 30 seconds when online
    
    init {
        loadUserProfile()
        startAutoRefresh()
    }
    
    private fun startAutoRefresh() {
        refreshJob = viewModelScope.launch {
            while (true) {
                delay(REFRESH_INTERVAL_MS)
                if (isOnline() && AuthSessionManager.currentSession() != null) {
                    // Only refresh if we're online and have a session
                    refreshProfileSilently()
                }
            }
        }
    }
    
    private fun isOnline(): Boolean {
        return context?.let { NetworkUtils.isOnline(it) } ?: false
    }
    
    private suspend fun refreshProfileSilently() {
        val session = AuthSessionManager.currentSession() ?: return
        val userEmail = session.userEmail ?: return
        
        // Only refresh if enough time has passed since last refresh
        val now = System.currentTimeMillis()
        if (now - lastRefreshTime < REFRESH_INTERVAL_MS) return
        
        try {
            val result = authRepository.fetchUserInfo()
            result.fold(
                onSuccess = { userInfo ->
                    // Update cache with fresh data
                    authRepository.updateCachedUserProfile(userInfo)
                    
                    // Update UI state if data changed
                    val currentState = _uiState.value
                    if (currentState.userName != userInfo.name ||
                        currentState.totalBalance != userInfo.totalBalance ||
                        currentState.offlineBalance != userInfo.offlineBalance) {
                        _uiState.update {
                            it.copy(
                                userName = userInfo.name,
                                email = userInfo.email,
                                phone = userInfo.phone,
                                totalBalance = userInfo.totalBalance,
                                offlineBalance = userInfo.offlineBalance
                            )
                        }
                    }
                    lastRefreshTime = now
                },
                onFailure = { 
                    // Silently fail - keep showing cached data
                }
            )
        } catch (e: Exception) {
            // Silently handle errors
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }
    
    fun loadUserProfile() {
        viewModelScope.launch {
            val session = AuthSessionManager.currentSession()
            if (session == null) {
                _uiState.update { 
                    it.copy(
                        errorMessage = "Not logged in",
                        isLoading = false
                    ) 
                }
                return@launch
            }
            
            val userEmail = session.userEmail ?: return@launch
            
            // Try to load from cache first (for offline access)
            val cachedUser = authRepository.getCachedUserProfile(userEmail)
            if (cachedUser != null) {
                _uiState.update {
                    it.copy(
                        userName = cachedUser.name,
                        email = cachedUser.email,
                        phone = cachedUser.phone,
                        totalBalance = cachedUser.totalBalance,
                        offlineBalance = cachedUser.offlineBalance,
                        isLoading = false
                    )
                }
            }
            
            // Fetch fresh data from API
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.fetchUserInfo()
            
            _uiState.update {
                result.fold(
                    onSuccess = { userInfo ->
                        // Update cache with fresh data
                        authRepository.updateCachedUserProfile(userInfo)
                        
                        it.copy(
                            userName = userInfo.name,
                            email = userInfo.email,
                            phone = userInfo.phone,
                            totalBalance = userInfo.totalBalance,
                            offlineBalance = userInfo.offlineBalance,
                            isLoading = false,
                            errorMessage = null
                        )
                    },
                    onFailure = { error ->
                        // If API call fails but we have cached data, keep showing cached data
                        if (cachedUser != null) {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Using cached data as you are in offline mode"
                            )
                        } else {
                            it.copy(
                                isLoading = false,
                                errorMessage = ErrorUtils.cleanErrorMessageForDisplay(error.message) ?: "Failed to load profile"
                            )
                        }
                    }
                )
            }
        }
    }
    
    fun refreshProfile() {
        lastRefreshTime = 0 // Force refresh
        loadUserProfile()
    }
    
    /**
     * Called when app comes to foreground or network becomes available
     */
    fun onAppResumed() {
        viewModelScope.launch {
            if (isOnline() && AuthSessionManager.currentSession() != null) {
                lastRefreshTime = 0 // Force refresh
                refreshProfileSilently()
            }
        }
    }
    
    companion object {
        fun createFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val authRepository = AuthRepository(ApiClient.authApi, context)
                    return ProfileViewModel(authRepository, context) as T
                }
            }
        }
    }
}

