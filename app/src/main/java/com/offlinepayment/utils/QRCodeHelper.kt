package com.offlinepayment.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.util.UUID

object QRCodeHelper {
    
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
}
