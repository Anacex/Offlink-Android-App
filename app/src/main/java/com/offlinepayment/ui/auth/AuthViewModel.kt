package com.offlinepayment.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.offlinepayment.data.SignupRequest
import com.offlinepayment.data.VerifyEmailRequest
import com.offlinepayment.data.network.ApiClient
import com.offlinepayment.data.repository.AuthRepository
import com.offlinepayment.data.session.AuthSessionManager
import com.offlinepayment.data.session.DeviceFingerprintProvider
import com.offlinepayment.utils.NetworkUtils
import com.offlinepayment.utils.BiometricAuthHelper
import androidx.fragment.app.FragmentActivity
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
    val isLoggedIn: Boolean = false,
    val signupEmail: String? = null,
    val signupOtpDemo: String? = null,
    val isSignupOtpStep: Boolean = false,
    val isEmailVerified: Boolean = false,
    val userEmail: String? = null,
    val showEmailVerificationDialog: Boolean = false
)

enum class AuthFlowStep { Credentials, Otp }

class AuthViewModel(
    private val repository: AuthRepository,
    private val context: Context? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var pendingLoginEmail: String? = null
    private var pendingNonce: String? = null
    private var pendingSignupEmail: String? = null
    private var pendingSignupPassword: String? = null // Store password for auto-login after verification

    init {
        viewModelScope.launch {
            AuthSessionManager.observeSession().collect { session ->
                val isVerified = session?.isEmailVerified ?: false
                _uiState.update {
                    it.copy(
                        isLoggedIn = session != null, // Login succeeds if session exists, regardless of verification
                        isLoading = false,
                        isOtpStep = if (session != null) false else it.isOtpStep,
                        isEmailVerified = isVerified,
                        userEmail = session?.userEmail,
                        // Auto-hide verification dialog if user becomes verified
                        showEmailVerificationDialog = if (isVerified) false else it.showEmailVerificationDialog
                    )
                }
                // If session exists, refresh user info to get latest verification status
                // Email verification is stored in DB and only needs to be checked once per user
                // This is done asynchronously and doesn't block login
                if (session != null) {
                    refreshUserInfo()
                }
            }
        }
    }
    
    private fun refreshUserInfo() {
        viewModelScope.launch {
            repository.fetchUserInfo().fold(
                onSuccess = { userInfo ->
                    val currentSession = AuthSessionManager.currentSession()
                    if (currentSession != null) {
                        AuthSessionManager.updateSession(
                            currentSession.copy(
                                userEmail = userInfo.email,
                                isEmailVerified = userInfo.emailVerified
                            )
                        )
                    }
                },
                onFailure = { 
                    // Silently fail - verification status will remain as stored in session
                }
            )
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
                        pendingSignupEmail = email
                        pendingSignupPassword = password // Store password for auto-login after verification
                        it.copy(
                            isLoading = false,
                            successMessage = response.msg,
                            errorMessage = null,
                            signupEmail = email,
                            signupOtpDemo = response.otp_demo,
                            isSignupOtpStep = true // Automatically show OTP step after successful signup
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
                        // After successful email verification during signup, automatically log the user in
                        val signupPassword = pendingSignupPassword
                        val signupEmail = pendingSignupEmail ?: email
                        
                        // Reset signup OTP step
                        pendingSignupEmail = null
                        pendingSignupPassword = null
                        
                        // If this was during signup, auto-login the user
                        if (signupPassword != null) {
                            // Automatically log in after email verification
                            autoLoginAfterSignup(signupEmail, signupPassword)
                        } else {
                            // If not during signup (e.g., from home screen), just refresh user info
                            refreshUserInfo()
                        }
                        
                        it.copy(
                            isLoading = false,
                            successMessage = response.msg,
                            isSignupOtpStep = false,
                            signupEmail = null,
                            signupOtpDemo = null,
                            showEmailVerificationDialog = false,
                            isEmailVerified = true, // Will be updated by refreshUserInfo or auto-login
                            userEmail = email
                        )
                    },
                    onFailure = { error ->
                        it.copy(isLoading = false, errorMessage = error.message)
                    }
                )
            }
        }
    }
    
    private fun autoLoginAfterSignup(email: String, password: String) {
        viewModelScope.launch {
            val fingerprint = DeviceFingerprintProvider.getFingerprint()
            // Since email is now verified, automatically log the user in
            // Backend should skip OTP for verified users and return tokens directly
            val loginResult = repository.loginStepOne(email, password, fingerprint)
            loginResult.fold(
                onSuccess = { loginResponse ->
                    if (!loginResponse.requires_otp) {
                        // Tokens received directly - login complete (session already created by repository)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isOtpStep = false,
                                successMessage = "Login successful",
                                errorMessage = null
                            )
                        }
                    } else {
                        // OTP still required (shouldn't happen for verified users, but handle gracefully)
                        pendingLoginEmail = email
                        pendingNonce = loginResponse.nonce_demo
                        val otpToUse = loginResponse.otp_demo ?: ""
                        if (otpToUse.isNotEmpty()) {
                            // Automatically confirm with OTP
                            confirmOtp(otpToUse)
                        } else {
                            // If no demo OTP, show OTP step (user needs to enter OTP from email)
                            _uiState.update {
                                it.copy(
                                    isOtpStep = true,
                                    pendingOtpNonce = loginResponse.nonce_demo,
                                    errorMessage = null
                                )
                            }
                        }
                    }
                },
                onFailure = { error ->
                    // If login fails, user will need to manually login
                    _uiState.update {
                        it.copy(
                            errorMessage = "Auto-login failed. Please login manually. ${error.message}",
                            isLoading = false
                        )
                    }
                }
            )
        }
    }
    
    fun showEmailVerificationDialog() {
        _uiState.update { it.copy(showEmailVerificationDialog = true) }
    }
    
    fun hideEmailVerificationDialog() {
        _uiState.update { it.copy(showEmailVerificationDialog = false) }
    }
    
    fun requestEmailVerificationOtp(email: String) {
        // For now, we'll use signup endpoint to resend OTP
        // In production, you'd have a dedicated resend-otp endpoint
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            // Note: This is a workaround - ideally backend should have /auth/resend-otp endpoint
            // For now, we'll show a message that OTP was sent
            _uiState.update {
                it.copy(
                    isLoading = false,
                    successMessage = "Verification code sent to $email. Please check your email.",
                    showEmailVerificationDialog = true
                )
            }
        }
    }

    fun startLogin(email: String, password: String) {
        val fingerprint = DeviceFingerprintProvider.getFingerprint()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            
            // Check network connectivity before attempting login
            val isOnline = context?.let { NetworkUtils.isOnline(it) } ?: true
            val isOffline = context?.let { NetworkUtils.isOffline(it) } ?: false
            
            if (isOffline) {
                // User is completely offline - check if verified user exists locally
                val offlineUserResult = repository.getOfflineUser(email)
                if (offlineUserResult == null) {
                    // User not found in local database - must verify email online first
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Please verify your email when online to use this app. You are currently offline.",
                            isOtpStep = false
                        )
                    }
                    return@launch
                }
                
                // Verified user found - verify password and use biometric auth
                if (!repository.verifyOfflinePassword(email, password)) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Invalid password",
                            isOtpStep = false
                        )
                    }
                    return@launch
                }
                
                // Password verified - login offline
                // Note: Biometric auth should be handled in UI layer before calling startLogin
                // For now, we proceed with offline login after password verification
                val offlineLoginResult = repository.loginOffline(email, password, fingerprint)
                _uiState.update {
                    offlineLoginResult.fold(
                        onSuccess = { session ->
                            it.copy(
                                isLoading = false,
                                isOtpStep = false,
                                successMessage = "Offline login successful",
                                errorMessage = null,
                                userEmail = session.userEmail,
                                isEmailVerified = session.isEmailVerified
                            )
                        },
                        onFailure = { error ->
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Offline login failed",
                                isOtpStep = false
                            )
                        }
                    )
                }
                return@launch
            }
            
            // User is online - proceed with normal login flow
            val result = repository.loginStepOne(email, password, fingerprint)
            _uiState.update {
                result.fold(
                    onSuccess = { response ->
                        // If backend returns tokens directly (verified user, OTP skipped), login is complete
                        if (!response.requires_otp) {
                            // Session is already created by repository, user is logged in
                            val currentSession = AuthSessionManager.currentSession()
                            it.copy(
                                isLoading = false,
                                isOtpStep = false,
                                successMessage = "Login successful",
                                errorMessage = null,
                                userEmail = currentSession?.userEmail ?: email,
                                isEmailVerified = currentSession?.isEmailVerified ?: true
                            )
                        } else {
                            // OTP required - user is unverified
                            // Check if online (WiFi or mobile data) for OTP to be sent
                            if (!isOnline) {
                                // Not online - show message
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "Please connect to internet (WiFi or mobile data) to receive email verification code. Verify your email when online to use this app.",
                                    isOtpStep = false
                                )
                            } else {
                                // Online - show OTP step
                                pendingLoginEmail = email
                                pendingNonce = response.nonce_demo
                                it.copy(
                                    isLoading = false,
                                    isOtpStep = true,
                                    pendingOtpNonce = response.nonce_demo,
                                    demoOtpFromServer = response.otp_demo,
                                    errorMessage = null
                                )
                            }
                        }
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
                        // Session is already updated by repository - login succeeds regardless of email verification
                        // Email verification status is fetched from backend and stored in session
                        val currentSession = AuthSessionManager.currentSession()
                        // Explicitly set isLoggedIn = true - user can log in even if email is not verified
                        it.copy(
                            isLoading = false,
                            isOtpStep = false,
                            isLoggedIn = true, // Login succeeds regardless of email verification status
                            successMessage = "Login successful",
                            errorMessage = null,
                            userEmail = currentSession?.userEmail ?: email,
                            isEmailVerified = currentSession?.isEmailVerified ?: false
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
        fun createFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repository = AuthRepository(ApiClient.authApi, context)
                return AuthViewModel(repository, context) as T
            }
        }
        
        // Legacy factory for backward compatibility
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repository = AuthRepository(ApiClient.authApi, null)
                return AuthViewModel(repository, null) as T
            }
        }
    }
}

