package com.offlinepayment.ui.qr

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

/**
 * Payee identity QR (Step 1) — same payload as legacy "My QR Code".
 * Used from Receive payment (BLE) flow only; there is no separate dashboard entry for this QR.
 */
@Composable
fun PayeeIdentityQrPanel(
    userId: Int,
    userName: String,
    balance: Double,
    modifier: Modifier = Modifier,
    /** Smaller QR for nested receive screen */
    qrSizeDp: Int = 220,
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

    val currentBalanceBD = remember(balance) {
        BigDecimal(balance.toString())
    }
    val isBalanceAtMax = remember(balance) {
        currentBalanceBD >= WalletLimits.MAX_OFFLINE_WALLET_BALANCE_BD
    }

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
        qrDataJson?.let { QRCodeHelper.generateQRCodeBitmap(it, 320, 320) }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isBalanceAtMax) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Wallet at maximum",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Balance ${CurrencyUtils.formatPkr(balance)} — limit ${CurrencyUtils.formatPkr(WalletLimits.MAX_OFFLINE_WALLET_BALANCE)}. You cannot receive until you spend or transfer.",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = userName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF374151),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = CurrencyUtils.formatPkr(balance),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF059669),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    qrBitmap?.let { bitmap ->
                        Card(
                            modifier = Modifier
                                .size(qrSizeDp.dp + 32.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Payee identity QR",
                                    modifier = Modifier.size(qrSizeDp.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Payee ID: $userId",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
