package com.offlinepayment.utils

import org.json.JSONObject

object AccountBlockedParser {
    /**
     * Parses FastAPI body `{"detail":{"code":"ACCOUNT_BLOCKED|WALLET_INACTIVE","message":"..."}}`.
     * Returns the user-facing message if access should be blocked; otherwise null.
     */
    fun extractBlockedUserMessage(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            val root = JSONObject(errorBody)
            val detail = root.optJSONObject("detail") ?: return null
            val code = detail.optString("code")
            if (code != "ACCOUNT_BLOCKED" && code != "WALLET_INACTIVE") return null
            detail.optString("message").ifBlank {
                if (code == "WALLET_INACTIVE") {
                    "Your wallet is inactive and payments are blocked."
                } else {
                    "Your account is suspended pending manual review."
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
