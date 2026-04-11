package com.offlinepayment.utils

import org.json.JSONObject

object AccountBlockedParser {
    /**
     * Parses FastAPI body `{"detail":{"code":"ACCOUNT_BLOCKED","message":"..."}}`.
     * Returns the user-facing message if blocked; otherwise null.
     */
    fun extractBlockedUserMessage(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            val root = JSONObject(errorBody)
            val detail = root.optJSONObject("detail") ?: return null
            if (detail.optString("code") != "ACCOUNT_BLOCKED") return null
            detail.optString("message").ifBlank {
                "Your account is suspended pending manual review."
            }
        } catch (_: Exception) {
            null
        }
    }
}
