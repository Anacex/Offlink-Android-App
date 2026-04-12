package com.offlinepayment.utils

import com.offlinepayment.data.ApiErrorResponse
import com.squareup.moshi.Moshi

object ErrorUtils {
    /**
     * Extracts and cleans error messages from API responses.
     * Handles JSON format like {"detail": "invalid OTP"} and extracts just "Invalid OTP"
     */
    fun extractErrorMessage(errorBody: String?, httpCode: Int, defaultMessage: String): String {
        if (errorBody.isNullOrBlank()) {
            return defaultMessage
        }
        
        // Try to parse as proper JSON first
        try {
            val moshi = Moshi.Builder().build()
            val adapter = moshi.adapter(ApiErrorResponse::class.java)
            val errorResponse = adapter.fromJson(errorBody)
            if (errorResponse?.detail != null) {
                return cleanErrorMessage(errorResponse.detail)
            }
        } catch (e: Exception) {
            // JSON parsing failed, try regex extraction
        }
        
        // Try to extract detail field using regex (handles malformed JSON)
        try {
            // Pattern to match {"detail": "message"} or {"detail":"message"} with double quotes
            // Handles: {"detail": "invalid OTP"} or {"detail":"Invalid Credential"}
            val regex = Regex("""["']detail["']\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            val match = regex.find(errorBody)
            if (match != null && match.groupValues.size > 1) {
                val extracted = match.groupValues[1]
                if (extracted.isNotBlank()) {
                    return cleanErrorMessage(extracted)
                }
            }
            
            // Try pattern without quotes around detail key
            val regex2 = Regex("""detail\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            val match2 = regex2.find(errorBody)
            if (match2 != null && match2.groupValues.size > 1) {
                val extracted = match2.groupValues[1]
                if (extracted.isNotBlank()) {
                    return cleanErrorMessage(extracted)
                }
            }
        } catch (e: Exception) {
            // Regex extraction failed
        }
        
        // If errorBody looks like JSON but we couldn't parse it, return default
        if (errorBody.trim().startsWith("{") && errorBody.contains("detail")) {
            return defaultMessage
        }
        
        // Return cleaned errorBody if it doesn't look like JSON
        return cleanErrorMessage(errorBody)
    }
    
    /**
     * Cleans error messages by capitalizing first letter and trimming
     */
    private fun cleanErrorMessage(message: String): String {
        if (message.isBlank()) {
            return message
        }
        
        val trimmed = message.trim()
        if (trimmed.isEmpty()) {
            return trimmed
        }
        
        // Capitalize first letter
        return trimmed[0].uppercaseChar() + trimmed.substring(1)
    }
    
    /**
     * Cleans error messages that might still be in JSON format.
     * Use this in ViewModels/UI layer as a safety net.
     */
    fun cleanErrorMessageForDisplay(message: String?): String? {
        if (message.isNullOrBlank()) {
            return message
        }
        
        // If message looks like JSON, try to extract detail
        if (message.trim().startsWith("{") && message.contains("detail")) {
            return extractErrorMessage(message, 0, message)
        }
        
        // Otherwise just clean and return
        return cleanErrorMessage(message)
    }
}

