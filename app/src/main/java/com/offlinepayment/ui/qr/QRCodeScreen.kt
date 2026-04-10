package com.offlinepayment.ui.qr

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlinepayment.utils.CurrencyUtils
import com.offlinepayment.utils.QRCodeHelper
import com.offlinepayment.utils.WalletLimits
import androidx.compose.ui.platform.LocalContext
import java.math.BigDecimal
import com.offlinepayment.data.repository.WalletRepository
import com.offlinepayment.data.network.ApiClient
import com.offlinepayment.data.local.OfflineWallet

@Composable
fun QRCodeScreen(
    userId: Int,
    qrCodeId: String,
    userName: String,
    balance: Double,
    onScanQR: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { WalletRepository(ApiClient.walletApi, context) }
    var offlineWallet by remember { mutableStateOf<OfflineWallet?>(null) }
    LaunchedEffect(userId) {
        try {
            offlineWallet = repository.getOfflineWalletByUserIdAndType(userId, "offline")
        } catch (_: Exception) {
            offlineWallet = null
        }
    }
    val deviceId = remember {
        try {
            com.offlinepayment.data.session.DeviceFingerprintProvider.getFingerprint()
        } catch (e: Exception) {
            "device-${userId}-${System.currentTimeMillis()}"
        }
    }
    
    // Check if balance is at maximum (5000 PKR) - cannot receive more payments
    val currentBalanceBD = remember(balance) {
        BigDecimal(balance.toString())
    }
    val isBalanceAtMax = remember(balance) {
        currentBalanceBD >= WalletLimits.MAX_OFFLINE_WALLET_BALANCE_BD
    }
    
    // Generate Payee QR (Step 1 - Payee Identity QR) only if balance < 5000
    // Calculate max transaction limit based on current balance
    val payeeQRData = remember(userId, userName, deviceId, balance, offlineWallet?.publicKey, offlineWallet?.walletId) {
        if (!isBalanceAtMax) {
            QRCodeHelper.createPayeeQR(
                payeeId = userId.toString(),
                payeeName = userName,
                deviceId = deviceId,
                currentBalance = currentBalanceBD,
                publicKey = offlineWallet?.publicKey,
                walletId = offlineWallet?.walletId
            )
        } else {
            null
        }
    }
    val qrDataJson = remember(payeeQRData) {
        if (payeeQRData != null) {
            val moshi = com.squareup.moshi.Moshi.Builder()
                .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                .build()
            val adapter = moshi.adapter(com.offlinepayment.data.PayeeQRPayload::class.java)
            adapter.toJson(payeeQRData)
        } else {
            null
        }
    }
    val qrBitmap = remember(qrDataJson) {
        qrDataJson?.let { QRCodeHelper.generateQRCodeBitmap(it, 300, 300) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF6366F1),
                        Color(0xFF8B5CF6)
                    )
                )
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Receive Payment",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6366F1)
                )
                Text(
                    text = "Show this QR code to receive payments offline",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // QR Code Card or Error Message
        if (isBalanceAtMax) {
            // Show error message if balance is at maximum
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⚠️",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Wallet at Maximum Capacity",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your wallet balance is ${CurrencyUtils.formatPkr(balance)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF374151)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "You cannot receive more payments as your wallet has reached the maximum limit of ${CurrencyUtils.formatPkr(WalletLimits.MAX_OFFLINE_WALLET_BALANCE)}.",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please use your wallet or transfer funds before receiving new payments.",
                        fontSize = 13.sp,
                        color = Color(0xFF9CA3AF),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Show QR Code if balance < 5000
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // User Info
                    Text(
                        text = userName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF374151),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = CurrencyUtils.formatPkr(balance),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF059669),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // QR Code
                    qrBitmap?.let { bitmap ->
                        Card(
                            modifier = Modifier
                                .size(250.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "QR Code",
                                    modifier = Modifier.size(220.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Payee ID: $userId",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Button
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onScanQR,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF10B981)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Scan QR",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scan QR")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Instructions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "How to use QR Code",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Show this QR code to the sender\n• They will scan it to identify you as the payee\n• Then they will generate a payment QR for you to scan",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
