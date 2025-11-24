package com.offlinepayment.ui.qr

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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

@Composable
fun QRCodeScreen(
    userId: Int,
    qrCodeId: String,
    userName: String,
    balance: Double,
    onScanQR: () -> Unit
) {
    val qrData = QRCodeHelper.createPaymentQRData(userId, qrCodeId)
    val qrBitmap = remember(qrData) {
        QRCodeHelper.generateQRCodeBitmap(qrData, 300, 300)
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
                    text = "My QR Code",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6366F1)
                )
                Text(
                    text = "Share this QR code to receive payments",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // QR Code Card
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
                    color = Color(0xFF374151)
                )
                Text(
                    text = CurrencyUtils.formatPkr(balance),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF059669)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // QR Code
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
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.size(220.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "QR Code ID: ${qrCodeId.take(8)}...",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
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

            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { /* TODO: Share QR Code */ },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.horizontalGradient(listOf(Color.White, Color.White))
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Share QR",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share")
            }
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
                    text = "• Show this QR code to others to receive payments\n• Each QR code is unique to your account\n• Payments are processed instantly offline",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
