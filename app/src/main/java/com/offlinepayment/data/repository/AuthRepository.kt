package com.offlinepayment.data.repository

import android.content.Context
import com.offlinepayment.data.AccountBlockedException
import com.offlinepayment.data.ApiErrorResponse
import com.offlinepayment.data.LoginConfirmRequest
import com.offlinepayment.data.LoginConfirmResponse
import com.offlinepayment.data.LoginRequest
import com.offlinepayment.data.LoginStep1Response
import com.offlinepayment.data.RefreshTokenRequest
import com.offlinepayment.data.SignupRequest
import com.offlinepayment.data.SignupResponse
import com.offlinepayment.data.TokenRefreshResponse
import com.offlinepayment.data.UserInfoResponse
import com.offlinepayment.data.ForgotPasswordConfirmRequest
import com.offlinepayment.data.ForgotPasswordConfirmResponse
import com.offlinepayment.data.ForgotPasswordRequest
import com.offlinepayment.data.ForgotPasswordRequestResponse
import com.offlinepayment.data.VerifyEmailRequest
import com.offlinepayment.data.VerifyEmailResponse
import com.offlinepayment.data.local.AppDatabase
import com.offlinepayment.data.local.OfflineUser
import com.offlinepayment.data.network.AuthApi
import com.offlinepayment.data.session.AuthSession
import com.offlinepayment.data.session.AuthSessionManager
import com.offlinepayment.utils.AccountBlockedParser
import com.offlinepayment.utils.PasswordUtils
import com.offlinepayment.utils.ErrorUtils
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class AuthRepository(
    private val api: AuthApi,
    private val context: Context? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val database = context?.let { AppDatabase.getDatabase(it) }
    private val offlineUserDao = database?.offlineUserDao()

    suspend fun signup(request: SignupRequest): Result<SignupResponse> =
        safeApiCall { api.signup(request) }

    suspend fun requestForgotPassword(email: String): Result<ForgotPasswordRequestResponse> =
        safeApiCall { api.forgotPassword(ForgotPasswordRequest(email = email.trim())) }

    suspend fun confirmForgotPassword(
        email: String,
        otp: String,
        nonce: String,
        newPassword: String,
        confirmPassword: String,
    ): Result<ForgotPasswordConfirmResponse> =
        safeApiCall {
            api.forgotPasswordConfirm(
                ForgotPasswordConfirmRequest(
                    email = email.trim(),
                    otp = otp.trim(),
                    nonce = nonce.trim(),
                    new_password = newPassword,
                    confirm_password = confirmPassword,
                )
            )
        }

    suspend fun verifyEmail(request: VerifyEmailRequest): Result<VerifyEmailResponse> =
        safeApiCall { 
            api.verifyEmail(request).also { response ->
                // After successful verification, update session
                val currentSession = AuthSessionManager.currentSession()
                if (currentSession != null) {
                    AuthSessionManager.updateSession(
                        currentSession.copy(isEmailVerified = true)
                    )
                }
            }
        }

    suspend fun loginStepOne(email: String, password: String, deviceFingerprint: String): Result<LoginStep1Response> {
        val payload = LoginRequest(
            email = email,
            password = password,
            device_fingerprint = deviceFingerprint
        )
        return safeApiCall { 
            val response = api.login(payload)
            // If backend returns tokens directly (verified user, OTP skipped), create session immediately
            if (!response.requires_otp && response.access_token != null && response.refresh_token != null) {
                AuthSessionManager.updateSession(
                    AuthSession(
                        accessToken = response.access_token,
                        refreshToken = response.refresh_token,
                        deviceFingerprint = deviceFingerprint,
                        userEmail = email,
                        isEmailVerified = true // Verified users skip OTP
                    )
                )
                // Fetch actual verification status from backend and save to local DB for offline access
                try {
                    fetchUserInfo().fold(
                        onSuccess = { userInfo ->
                            AuthSessionManager.updateSession(
                                AuthSessionManager.currentSession()!!.copy(
                                    userEmail = userInfo.email,
                                    isEmailVerified = userInfo.emailVerified,
                                    userId = userInfo.id
                                )
                            )
                            // Save verified user to local database for offline login
                            if (userInfo.emailVerified) {
                                saveUserForOfflineLogin(email, password, userInfo.email, userInfo.id, userInfo.name, userInfo.phone, userInfo.totalBalance, userInfo.offlineBalance)
                            }
                        },
                        onFailure = { 
                            // Silently fail - verification status already set to true
                            // Still save user for offline login (with default balance)
                            saveUserForOfflineLogin(email, password, email, 0, "", null, 0.0, 0.0)
                        }
                    )
                } catch (e: Exception) {
                    // Silently handle any errors (with default balance)
                    saveUserForOfflineLogin(email, password, email, 0, "", null, 0.0, 0.0)
                }
            } else if (response.requires_otp) {
                // OTP required - store email verification status from response if available
                // This helps track if user is unverified
                val emailVerified = response.email_verified ?: false
                // Note: Session will be created after OTP confirmation
            }
            response
        }
    }

    suspend fun confirmLogin(
        email: String,
        otp: String,
        nonce: String,
        deviceFingerprint: String
    ): Result<LoginConfirmResponse> {
        val payload = LoginConfirmRequest(
            email = email,
            otp = otp,
            nonce = nonce,
            device_fingerprint = deviceFingerprint
        )
        return safeApiCall {
            val response = api.confirmLogin(payload)
            // Backend verifies email automatically when unverified user confirms OTP
            // Create session with tokens - email should now be verified
            AuthSessionManager.updateSession(
                AuthSession(
                    accessToken = response.access_token,
                    refreshToken = response.refresh_token,
                    deviceFingerprint = deviceFingerprint,
                    userEmail = email,
                    isEmailVerified = true // Email is verified after OTP confirmation
                )
            )
            // Fetch actual verification status from backend to confirm
            try {
                    fetchUserInfo().fold(
                        onSuccess = { userInfo ->
                            AuthSessionManager.updateSession(
                                AuthSessionManager.currentSession()!!.copy(
                                    userEmail = userInfo.email,
                                    isEmailVerified = userInfo.emailVerified,
                                    userId = userInfo.id
                                )
                            )
                            // Save verified user to local database for offline login
                            // Note: password not available for OTP logins, so offline login won't be saved
                            // Users who login via OTP can't use offline login anyway
                            // But we can still cache the profile data
                            updateCachedUserProfile(userInfo)
                        },
                        onFailure = { 
                            // If fetching user info fails, silently continue
                            // Password not available for OTP logins, so we can't save for offline login
                        }
                    )
            } catch (e: Exception) {
                // Silently handle any errors - email is already verified by backend
            }
            response
        }
    }
    
    suspend fun fetchUserInfo(): Result<UserInfoResponse> = safeApiCall { api.getUserInfo() }

    suspend fun refreshToken(deviceFingerprint: String): Result<TokenRefreshResponse> {
        val current = AuthSessionManager.currentSession()
            ?: return Result.failure(IllegalStateException("No session to refresh"))
        val payload = RefreshTokenRequest(
            refresh_token = current.refreshToken,
            device_fingerprint = deviceFingerprint
        )
        return safeApiCall {
            api.refreshToken(payload).also { refreshed ->
                AuthSessionManager.updateSession(
                    current.copy(accessToken = refreshed.access_token)
                )
            }
        }
    }

    fun logout() {
        AuthSessionManager.updateSession(null)
    }
    
    /**
     * Save verified user to local database for offline login
     * Note: password is optional - if null, user won't be able to login offline
     */
    private suspend fun saveUserForOfflineLogin(
        email: String, 
        password: String?, 
        verifiedEmail: String,
        userId: Int,
        name: String,
        phone: String?,
        totalBalance: Double = 0.0,
        offlineBalance: Double = 0.0
    ) {
        if (offlineUserDao == null || password == null) return
        
        withContext(ioDispatcher) {
            try {
                val (passwordHash, salt) = PasswordUtils.hashPassword(password)
                
                val offlineUser = OfflineUser(
                    email = verifiedEmail,
                    passwordHash = "$salt:$passwordHash", // Store salt:hash together
                    name = name,
                    phone = phone,
                    userId = userId,
                    isEmailVerified = true,
                    totalBalance = totalBalance,
                    offlineBalance = offlineBalance,
                    lastSyncedAt = System.currentTimeMillis()
                )
                offlineUserDao.insertUser(offlineUser)
            } catch (e: Exception) {
                // Silently fail - offline login won't be available
            }
        }
    }
    
    /**
     * Update cached user profile data (balance, etc.) without password
     */
    suspend fun updateCachedUserProfile(userInfo: UserInfoResponse) {
        if (offlineUserDao == null) return
        
        withContext(ioDispatcher) {
            try {
                val existingUser = offlineUserDao.getUserByEmail(userInfo.email)
                if (existingUser != null) {
                    val updatedUser = existingUser.copy(
                        name = userInfo.name,
                        phone = userInfo.phone,
                        totalBalance = userInfo.totalBalance,
                        offlineBalance = userInfo.offlineBalance,
                        lastSyncedAt = System.currentTimeMillis()
                    )
                    offlineUserDao.insertUser(updatedUser)
                }
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }
    
    /**
     * Get cached user profile from local database
     */
    suspend fun getCachedUserProfile(email: String): OfflineUser? {
        if (offlineUserDao == null) return null
        return withContext(ioDispatcher) {
            try {
                offlineUserDao.getUserByEmail(email)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Check if user exists in local database for offline login
     */
    suspend fun getOfflineUser(email: String): OfflineUser? {
        if (offlineUserDao == null) return null
        return withContext(ioDispatcher) {
            try {
                offlineUserDao.getVerifiedUserByEmail(email)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Verify password for offline login
     */
    suspend fun verifyOfflinePassword(email: String, password: String): Boolean {
        val offlineUser = getOfflineUser(email) ?: return false
        val (salt, hash) = offlineUser.passwordHash.split(":", limit = 2)
        return PasswordUtils.verifyPassword(password, hash, salt)
    }
    
    /**
     * Login offline using local database and password verification
     */
    suspend fun loginOffline(email: String, password: String, deviceFingerprint: String): Result<AuthSession> {
        return withContext(ioDispatcher) {
            try {
                val offlineUser = getOfflineUser(email)
                if (offlineUser == null) {
                    return@withContext Result.failure(Exception("User not found in offline database. Please login online first."))
                }
                
                if (!verifyOfflinePassword(email, password)) {
                    return@withContext Result.failure(Exception("Invalid password"))
                }
                
                // Create offline session (without server tokens)
                val session = AuthSession(
                    accessToken = "", // No server token available in offline mode
                    refreshToken = "", // No refresh token in offline mode
                    deviceFingerprint = deviceFingerprint,
                    userEmail = offlineUser.email,
                    isEmailVerified = true,
                    userId = offlineUser.userId
                )
                
                AuthSessionManager.updateSession(session)
                Result.success(session)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> =
        withContext(ioDispatcher) {
            try {
                Result.success(block())
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                if (e.code() == 403) {
                    AccountBlockedParser.extractBlockedUserMessage(errorBody)?.let { msg ->
                        return@withContext Result.failure(AccountBlockedException(msg))
                    }
                }
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

