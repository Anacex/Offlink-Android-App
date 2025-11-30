package com.offlinepayment.data.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val deviceFingerprint: String,
    val isEmailVerified: Boolean = false,
    val userEmail: String? = null
)

object AuthSessionManager {
    private val sessionFlow = MutableStateFlow<AuthSession?>(null)

    fun updateSession(newSession: AuthSession?) {
        sessionFlow.value = newSession
    }

    fun currentSession(): AuthSession? = sessionFlow.value

    fun observeSession(): StateFlow<AuthSession?> = sessionFlow
}

