package com.offlinepayment.data.repository

import com.offlinepayment.data.ApiErrorResponse
import com.offlinepayment.data.LoginConfirmRequest
import com.offlinepayment.data.LoginConfirmResponse
import com.offlinepayment.data.LoginRequest
import com.offlinepayment.data.LoginStep1Response
import com.offlinepayment.data.RefreshTokenRequest
import com.offlinepayment.data.SignupRequest
import com.offlinepayment.data.SignupResponse
import com.offlinepayment.data.TokenRefreshResponse
import com.offlinepayment.data.VerifyEmailRequest
import com.offlinepayment.data.VerifyEmailResponse
import com.offlinepayment.data.network.AuthApi
import com.offlinepayment.data.session.AuthSession
import com.offlinepayment.data.session.AuthSessionManager
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class AuthRepository(
    private val api: AuthApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun signup(request: SignupRequest): Result<SignupResponse> =
        safeApiCall { api.signup(request) }

    suspend fun verifyEmail(request: VerifyEmailRequest): Result<VerifyEmailResponse> =
        safeApiCall { api.verifyEmail(request) }

    suspend fun loginStepOne(email: String, password: String, deviceFingerprint: String): Result<LoginStep1Response> {
        val payload = LoginRequest(
            email = email,
            password = password,
            device_fingerprint = deviceFingerprint
        )
        return safeApiCall { api.login(payload) }
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
            api.confirmLogin(payload).also { response ->
                AuthSessionManager.updateSession(
                    AuthSession(
                        accessToken = response.access_token,
                        refreshToken = response.refresh_token,
                        deviceFingerprint = deviceFingerprint
                    )
                )
            }
        }
    }

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

    private suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> =
        withContext(ioDispatcher) {
            try {
                Result.success(block())
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                val errorMessage = try {
                    val moshi = Moshi.Builder().build()
                    val adapter = moshi.adapter(ApiErrorResponse::class.java)
                    adapter.fromJson(errorBody ?: "")?.detail ?: "HTTP ${e.code()}: ${e.message()}"
                } catch (parseException: Exception) {
                    errorBody ?: "HTTP ${e.code()}: ${e.message()}"
                }
                Result.failure(Exception(errorMessage))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}

