package com.offlinepayment.data

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
    val nonce_demo: String,
    val otp_demo: String
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
    val token_type: String
)
