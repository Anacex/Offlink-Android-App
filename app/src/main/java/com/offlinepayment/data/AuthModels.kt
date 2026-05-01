package com.offlinepayment.data

import com.squareup.moshi.Json

// Auth-related request/response models

data class SignupRequest(
    val name: String,
    val email: String,
    val password: String,
    val phone: String
)
data class SignupResponse(
    val msg: String,
    val otp_demo: String?
)

data class LoginRequest(
    val email: String,
    val password: String,
    val device_fingerprint: String
)
data class LoginStep1Response(
    val requires_otp: Boolean,
    // OTP fields (when requires_otp is true)
    val nonce_demo: String? = null,
    val otp_demo: String? = null,
    // Token fields (when requires_otp is false)
    val access_token: String? = null,
    val token_type: String? = null,
    val refresh_token: String? = null,
    // Email verification status
    val email_verified: Boolean? = null
)
data class LoginConfirmRequest(
    val email: String,
    val otp: String,
    val nonce: String,
    val device_fingerprint: String
)
data class LoginConfirmResponse(
    val access_token: String,
    val token_type: String,
    val refresh_token: String
)
data class VerifyEmailRequest(
    val email: String,
    val otp: String
)

data class VerifyEmailResponse(
    val msg: String
)

// ... add more as needed

data class ApiErrorResponse(val detail: String)
data class ApiValidationError(val detail: List<ErrorDetail>)
data class ErrorDetail(
    val loc: List<Any>,
    val msg: String,
    val type: String
)

data class RefreshTokenRequest(
    val refresh_token: String,
    val device_fingerprint: String
)

data class TokenRefreshResponse(
    val access_token: String,
    val token_type: String,
    val refresh_token: String? = null
)

data class ForgotPasswordRequest(val email: String)

data class ForgotPasswordRequestResponse(
    val msg: String,
    val nonce_demo: String?,
    val otp_demo: String?
)

data class ForgotPasswordConfirmRequest(
    val email: String,
    val otp: String,
    val nonce: String,
    val new_password: String,
    val confirm_password: String
)

data class ForgotPasswordConfirmResponse(
    val msg: String
)

data class UserInfoResponse(
    val id: Int,
    val email: String,
    val name: String,
    val phone: String?,
    @Json(name = "emailVerified")
    val emailVerified: Boolean,
    @Json(name = "totalBalance")
    val totalBalance: Double = 0.0,
    @Json(name = "offlineBalance")
    val offlineBalance: Double = 0.0,
    @Json(name = "deviceLedgerPrevHash")
    val deviceLedgerPrevHash: String? = null,
    @Json(name = "deviceLedgerNextSequence")
    val deviceLedgerNextSequence: Long? = null,
)
