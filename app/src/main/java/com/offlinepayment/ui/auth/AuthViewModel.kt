package com.offlinepayment.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.offlinepayment.data.SignupRequest
import com.offlinepayment.data.VerifyEmailRequest
import com.offlinepayment.data.network.ApiClient
import com.offlinepayment.data.repository.AuthRepository
import com.offlinepayment.data.session.AuthSessionManager
import com.offlinepayment.data.session.DeviceFingerprintProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val pendingOtpNonce: String? = null,
    val demoOtpFromServer: String? = null,
    val isOtpStep: Boolean = false,
    val isLoggedIn: Boolean = false
)

enum class AuthFlowStep { Credentials, Otp }

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var pendingLoginEmail: String? = null
    private var pendingNonce: String? = null

    init {
        viewModelScope.launch {
            AuthSessionManager.observeSession().collect { session ->
                _uiState.update {
                    it.copy(
                        isLoggedIn = session != null,
                        isLoading = false,
                        isOtpStep = if (session != null) false else it.isOtpStep
                    )
                }
            }
        }
    }

    fun signup(name: String, email: String, password: String, phone: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val result = repository.signup(
                SignupRequest(name = name, email = email, password = password, phone = phone)
            )
            _uiState.update {
                result.fold(
                    onSuccess = { response ->
                        it.copy(
                            isLoading = false,
                            successMessage = response.msg,
                            errorMessage = null
                        )
                    },
                    onFailure = { error ->
                        it.copy(isLoading = false, errorMessage = error.message, successMessage = null)
                    }
                )
            }
        }
    }

    fun verifyEmail(email: String, otp: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val result = repository.verifyEmail(VerifyEmailRequest(email = email, otp = otp))
            _uiState.update {
                result.fold(
                    onSuccess = { response ->
                        it.copy(isLoading = false, successMessage = response.msg)
                    },
                    onFailure = { error ->
                        it.copy(isLoading = false, errorMessage = error.message)
                    }
                )
            }
        }
    }

    fun startLogin(email: String, password: String) {
        val fingerprint = DeviceFingerprintProvider.getFingerprint()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val result = repository.loginStepOne(email, password, fingerprint)
            _uiState.update {
                result.fold(
                    onSuccess = { response ->
                        pendingLoginEmail = email
                        pendingNonce = response.nonce_demo // TODO: replace demo fields when backend hides OTP
                        it.copy(
                            isLoading = false,
                            isOtpStep = true,
                            pendingOtpNonce = response.nonce_demo,
                            demoOtpFromServer = response.otp_demo,
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

    fun confirmOtp(otp: String) {
        val email = pendingLoginEmail
        val nonce = pendingNonce
        val fingerprint = DeviceFingerprintProvider.getFingerprint()
        if (email.isNullOrBlank() || nonce.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "No pending login request") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.confirmLogin(
                email = email,
                otp = otp,
                nonce = nonce,
                deviceFingerprint = fingerprint
            )
            _uiState.update {
                result.fold(
                    onSuccess = { _ ->
                        it.copy(
                            isLoading = false,
                            isOtpStep = false,
                            successMessage = "Login successful",
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

    fun logout() {
        repository.logout()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repository = AuthRepository(ApiClient.authApi)
                return AuthViewModel(repository) as T
            }
        }
    }
}

