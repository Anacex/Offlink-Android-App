package com.offlinepayment.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.offlinepayment.data.TransactionIntent
import com.offlinepayment.data.ReceiverQRPayload
import com.offlinepayment.data.OfflineTransactionPayload
import com.offlinepayment.data.PayeeQRPayload
import com.offlinepayment.data.TransactionPayloadQR
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import android.util.Base64
import java.math.BigDecimal
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object QRCodeHelper {
    
    // Custom BigDecimal adapter for Moshi
    private class BigDecimalAdapter {
        @ToJson
        fun toJson(value: BigDecimal): String = value.toPlainString()
        
        @FromJson
        fun fromJson(value: String): BigDecimal = BigDecimal(value)
    }
    
    private val moshi = Moshi.Builder()
        .add(BigDecimalAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()
    
    private val transactionIntentAdapter = moshi.adapter(TransactionIntent::class.java)
    private val receiverQRAdapter = moshi.adapter(ReceiverQRPayload::class.java)
    private val transactionPayloadAdapter = moshi.adapter(OfflineTransactionPayload::class.java)
    private val payeeQRAdapter = moshi.adapter(PayeeQRPayload::class.java)
    private val transactionPayloadQRAdapter = moshi.adapter(TransactionPayloadQR::class.java)
    
    fun generateQRCodeId(): String {
        return UUID.randomUUID().toString()
    }
    
    fun generateQRCodeBitmap(
        text: String,
        width: Int = 512,
        height: Int = 512
    ): Bitmap {
        val writer = QRCodeWriter()
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 1
        )
        
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height, hints)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(
                    x, y,
                    if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                )
            }
        }
        
        return bitmap
    }
    
    /**
     * Creates a transaction intent QR code.
     * Encodes as: JSON → Base64 → QR Code
     */
    fun createTransactionIntentQR(
        userID: Int,
        walletID: Int,
        amount: java.math.BigDecimal,
        nonce: String = generateQRCodeId()
    ): String {
        val intent = TransactionIntent(
            userID = userID,
            walletID = walletID,
            timestamp = System.currentTimeMillis(),
            nonce = nonce,
            amount = amount,
            transactionIntentType = "PAY_REQUEST",
            signature = null
        )
        
        // Convert to JSON
        val json = transactionIntentAdapter.toJson(intent)
        
        // Encode to Base64
        val base64 = Base64.encodeToString(json.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        
        return base64
    }
    
    /**
     * Parses a transaction intent from QR code.
     * Decodes as: QR Code → Base64 → JSON → TransactionIntent
     */
    fun parseTransactionIntentQR(qrData: String): TransactionIntent? {
        return try {
            // Decode from Base64
            val jsonBytes = Base64.decode(qrData, Base64.NO_WRAP)
            val json = String(jsonBytes, Charsets.UTF_8)
            
            // Parse JSON to TransactionIntent
            transactionIntentAdapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Validates a transaction intent.
     * Checks: timestamp freshness, nonce format, amount > 0, sender identity
     */
    fun validateTransactionIntent(intent: TransactionIntent, maxAgeSeconds: Long = 300): ValidationResult {
        // Check timestamp freshness (default 5 minutes)
        val ageSeconds = (System.currentTimeMillis() - intent.timestamp) / 1000
        if (ageSeconds > maxAgeSeconds) {
            return ValidationResult(false, "Transaction intent expired (older than ${maxAgeSeconds}s)")
        }
        
        if (ageSeconds < 0) {
            return ValidationResult(false, "Invalid timestamp (future time)")
        }
        
        // Check nonce format (should be non-empty)
        if (intent.nonce.isBlank()) {
            return ValidationResult(false, "Invalid nonce format")
        }
        
        // Check amount > 0
        if (intent.amount <= java.math.BigDecimal.ZERO) {
            return ValidationResult(false, "Amount must be greater than zero")
        }
        
        // Check sender identity (userID and walletID should be positive)
        if (intent.userID <= 0 || intent.walletID <= 0) {
            return ValidationResult(false, "Invalid sender identity")
        }
        
        // Check transaction intent type
        if (intent.transactionIntentType != "PAY_REQUEST") {
            return ValidationResult(false, "Invalid transaction intent type")
        }
        
        return ValidationResult(true, "Valid transaction intent")
    }
    
    data class ValidationResult(
        val isValid: Boolean,
        val message: String
    )
    
    // Legacy methods for backward compatibility
    fun createPaymentQRData(userId: Int, qrCodeId: String): String {
        return "OFFLINE_PAYMENT:$userId:$qrCodeId"
    }
    
    fun parsePaymentQRData(qrData: String): Pair<Int, String>? {
        return try {
            val parts = qrData.split(":")
            if (parts.size == 3 && parts[0] == "OFFLINE_PAYMENT") {
                Pair(parts[1].toInt(), parts[2])
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Parses a receiver QR code payload.
     * Receiver QR codes are JSON (not Base64 encoded like transaction intents).
     * Format: Direct JSON string
     */
    fun parseReceiverQR(qrData: String): ReceiverQRPayload? {
        return try {
            // Try parsing as direct JSON first (receiver QR format)
            receiverQRAdapter.fromJson(qrData)
        } catch (e: Exception) {
            // If direct JSON fails, try Base64 decode (for backward compatibility)
            try {
                val decodedBytes = Base64.decode(qrData, Base64.NO_WRAP)
                val json = String(decodedBytes, Charsets.UTF_8)
                receiverQRAdapter.fromJson(json)
            } catch (e2: Exception) {
                null
            }
        }
    }
    
    /**
     * Validates a receiver QR payload.
     */
    fun validateReceiverQR(payload: ReceiverQRPayload): ValidationResult {
        // Check version
        if (payload.version != "1.0") {
            return ValidationResult(false, "Unsupported QR code version")
        }
        
        // Check type
        if (payload.type != "offline_payment_receiver") {
            return ValidationResult(false, "Invalid QR code type")
        }
        
        // Check public key exists
        if (payload.public_key.isBlank()) {
            return ValidationResult(false, "Missing public key")
        }
        
        // Check user_id and wallet_id are valid
        if (payload.user_id <= 0 || payload.wallet_id <= 0) {
            return ValidationResult(false, "Invalid user or wallet ID")
        }
        
        // Check nonce exists
        if (payload.nonce.isBlank()) {
            return ValidationResult(false, "Missing nonce")
        }
        
        return ValidationResult(true, "Valid receiver QR code")
    }
    
    /**
     * Creates a complete offline transaction payload for QR code.
     * This is what the sender generates after scanning receiver's QR.
     * Contains all necessary data for offline transaction processing.
     * 
     * @param senderUserId Sender's user ID
     * @param senderWalletId Sender's wallet ID
     * @param senderPublicKey Sender's public key (for receiver to verify)
     * @param senderName Sender's name for display
     * @param senderAccount Sender's bank account number
     * @param receiverQR Receiver's QR payload (scanned from receiver)
     * @param amount Transaction amount
     * @param currency Currency code (default: PKR)
     * @param deviceFingerprint Device fingerprint (optional)
     * @param privateKeyPem Private key for signing (should be retrieved from secure storage)
     * @return Complete OfflineTransactionPayload ready for QR encoding
     */
    fun createOfflineTransactionPayload(
        senderUserId: Int,
        senderWalletId: Int,
        senderPublicKey: String,
        senderName: String,
        senderAccount: String,
        receiverQR: ReceiverQRPayload,
        amount: java.math.BigDecimal,
        currency: String = "PKR",
        deviceFingerprint: String? = null,
        privateKeyPem: String? = null // For signing - should be retrieved securely
    ): OfflineTransactionPayload {
        val nonce = TransactionSigner.generateNonce()
        val timestamp = System.currentTimeMillis()
        val timestampISO = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
        val createdAtDevice = timestampISO
        
        // Format amount with 2 decimal places (matching backend format)
        val amountString = String.format(Locale.US, "%.2f", amount.toDouble())
        
        // Create transaction data for signing (matches backend format)
        val transactionData = mapOf(
            "sender_wallet_id" to senderWalletId,
            "receiver_public_key" to receiverQR.public_key,
            "receiver_user_id" to receiverQR.user_id,
            "receiver_wallet_id" to receiverQR.wallet_id,
            "amount" to amountString,
            "currency" to currency,
            "nonce" to nonce,
            "timestamp" to timestampISO
        )
        
        // Sign transaction with private key
        val signature = TransactionSigner.signTransaction(transactionData, privateKeyPem)
        
        // Create receipt data
        val receiptData = TransactionSigner.createReceiptData(
            senderWalletId = senderWalletId,
            receiverWalletId = receiverQR.wallet_id,
            receiverPublicKey = receiverQR.public_key,
            amount = amountString,
            currency = currency,
            nonce = nonce,
            signature = signature,
            timestamp = timestampISO
        )
        
        // Calculate receipt hash
        val receiptHash = TransactionSigner.calculateReceiptHash(receiptData)
        
        // Convert receipt data to JSON string
        val receiptJson = moshi.adapter(Map::class.java).toJson(receiptData)
        
        return OfflineTransactionPayload(
            version = "1.0",
            type = "offline_transaction",
            sender_user_id = senderUserId,
            sender_wallet_id = senderWalletId,
            sender_public_key = senderPublicKey,
            sender_name = senderName,
            sender_account = senderAccount,
            receiver_user_id = receiverQR.user_id,
            receiver_wallet_id = receiverQR.wallet_id,
            receiver_public_key = receiverQR.public_key,
            amount = amount,
            currency = currency,
            nonce = nonce,
            timestamp = timestampISO,
            transaction_signature = signature,
            receipt_hash = receiptHash,
            receipt_data = receiptJson,
            device_fingerprint = deviceFingerprint,
            created_at_device = createdAtDevice
        )
    }
    
    /**
     * Creates QR code string from offline transaction payload.
     * Encodes as: JSON → Base64 → QR Code
     */
    fun createTransactionPayloadQR(payload: OfflineTransactionPayload): String {
        val json = transactionPayloadAdapter.toJson(payload)
        return Base64.encodeToString(json.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }
    
    /**
     * Validates offline transaction payload (old format).
     * NOTE: This is for the old OfflineTransactionPayload format. 
     * New code should use validateTransactionPayload(payload: TransactionPayloadQR, currentPayeeId: String)
     */
    fun validateOfflineTransactionPayload(payload: OfflineTransactionPayload): ValidationResult {
        // Check version
        if (payload.version != "1.0") {
            return ValidationResult(false, "Unsupported transaction version")
        }
        
        // Check type
        if (payload.type != "offline_transaction") {
            return ValidationResult(false, "Invalid transaction type")
        }
        
        // Check amount > 0
        if (payload.amount <= java.math.BigDecimal.ZERO) {
            return ValidationResult(false, "Amount must be greater than zero")
        }
        
        // Check IDs are valid
        if (payload.sender_wallet_id <= 0 || payload.receiver_wallet_id <= 0) {
            return ValidationResult(false, "Invalid wallet IDs")
        }
        
        // Check public keys exist
        if (payload.sender_public_key.isBlank() || payload.receiver_public_key.isBlank()) {
            return ValidationResult(false, "Missing public keys")
        }
        
        // Check signature exists
        if (payload.transaction_signature.isBlank()) {
            return ValidationResult(false, "Missing transaction signature")
        }
        
        // Check receipt hash matches receipt data
        try {
            val receiptDataMap = moshi.adapter(Map::class.java).fromJson(payload.receipt_data) as? Map<*, *>
            if (receiptDataMap != null) {
                val receiptData = receiptDataMap.mapKeys { it.key.toString() }.mapValues { it.value.toString() }
                val calculatedHash = TransactionSigner.calculateReceiptHash(receiptData)
                if (calculatedHash != payload.receipt_hash) {
                    return ValidationResult(false, "Receipt hash mismatch")
                }
            }
        } catch (e: Exception) {
            return ValidationResult(false, "Invalid receipt data format")
        }
        
        return ValidationResult(true, "Valid transaction payload")
    }
    
    /**
     * Creates a Payee QR code payload (Step 1).
     * 
     * @param payeeId Payee's user ID
     * @param payeeName Payee's name
     * @param deviceId Payee's device ID
     * @param currentBalance Payee's current wallet balance (used to calculate max transaction limit)
     */
    fun createPayeeQR(
        payeeId: String,
        payeeName: String,
        deviceId: String,
        currentBalance: java.math.BigDecimal? = null,
        publicKey: String? = null,
        walletId: Int? = null
    ): PayeeQRPayload {
        // Calculate max transaction limit based on balance
        val maxTransactionLimit = if (currentBalance != null) {
            WalletLimits.calculateMaxTransactionLimit(currentBalance).toDouble()
        } else {
            null
        }
        
        return PayeeQRPayload(
            payeeId = payeeId,
            payeeName = payeeName,
            deviceId = deviceId,
            nonce = generateQRCodeId(),
            maxTransactionLimit = maxTransactionLimit,
            public_key = publicKey,
            wallet_id = walletId
        )
    }
    
    /**
     * Parses a Payee QR code payload.
     */
    fun parsePayeeQR(qrData: String): PayeeQRPayload? {
        return try {
            payeeQRAdapter.fromJson(qrData)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Creates a Transaction Payload QR (Step 2).
     */
    fun createTransactionPayloadQR(
        txId: String,
        payerId: String,
        payeeId: String,
        amount: Long,
        timestamp: Long,
        nonce: String,
        payerName: String,
        note: String? = null
    ): TransactionPayloadQR {
        return TransactionPayloadQR(
            txId = txId,
            payerId = payerId,
            payeeId = payeeId,
            amount = amount,
            timestamp = timestamp,
            nonce = nonce,
            payerName = payerName,
            note = note
        )
    }
    
    /**
     * Parses a Transaction Payload QR.
     */
    fun parseTransactionPayloadQR(qrData: String): TransactionPayloadQR? {
        return try {
            // Try parsing as direct JSON first
            transactionPayloadQRAdapter.fromJson(qrData)
        } catch (e: Exception) {
            // If direct JSON fails, try Base64 decode
            try {
                val decodedBytes = Base64.decode(qrData, Base64.NO_WRAP)
                val json = String(decodedBytes, Charsets.UTF_8)
                transactionPayloadQRAdapter.fromJson(json)
            } catch (e2: Exception) {
                null
            }
        }
    }
    
    /**
     * Validates a Transaction Payload QR.
     */
    fun validateTransactionPayload(payload: TransactionPayloadQR, currentPayeeId: String): ValidationResult {
        // Amount is positive
        if (payload.amount <= 0) {
            return ValidationResult(false, "Transaction amount must be positive.")
        }
        
        // Timestamp is within reasonable range (e.g., ±2 minutes)
        val currentTime = System.currentTimeMillis()
        val twoMinutesInMillis = 2 * 60 * 1000L
        if (payload.timestamp < currentTime - twoMinutesInMillis || payload.timestamp > currentTime + twoMinutesInMillis) {
            return ValidationResult(false, "Transaction timestamp is outside acceptable range.")
        }
        
        // Payer ID is present
        if (payload.payerId.isBlank()) {
            return ValidationResult(false, "Payer ID is missing.")
        }
        
        // Payee ID matches THIS device's ID
        if (payload.payeeId != currentPayeeId) {
            return ValidationResult(false, "Payee ID in QR does not match this device's ID.")
        }
        
        return ValidationResult(true, "Transaction payload is valid.")
    }
}
