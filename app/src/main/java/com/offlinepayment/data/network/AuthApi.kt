package com.offlinepayment.data.network

import com.offlinepayment.data.LoginConfirmRequest
import com.offlinepayment.data.LoginConfirmResponse
import com.offlinepayment.data.LoginRequest
import com.offlinepayment.data.LoginStep1Response
import com.offlinepayment.data.RefreshTokenRequest
import com.offlinepayment.data.SignupRequest
import com.offlinepayment.data.SignupResponse
import com.offlinepayment.data.TokenRefreshResponse
import com.offlinepayment.data.UserInfoResponse
import com.offlinepayment.data.VerifyEmailRequest
import com.offlinepayment.data.VerifyEmailResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit contract for FastAPI auth routes.
 * NOTE: Response fields are based on current backend implementation and may change.
 */
interface AuthApi {
    @POST("auth/signup")
    suspend fun signup(@Body request: SignupRequest): SignupResponse

    @POST("auth/verify-email")
    suspend fun verifyEmail(@Body request: VerifyEmailRequest): VerifyEmailResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginStep1Response

    @POST("auth/login/confirm")
    suspend fun confirmLogin(@Body request: LoginConfirmRequest): LoginConfirmResponse

    @POST("auth/token/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): TokenRefreshResponse
    
    @GET("auth/me")
    suspend fun getUserInfo(): UserInfoResponse
}

