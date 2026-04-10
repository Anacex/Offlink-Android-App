package com.offlinepayment.ui.qr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlinepayment.data.ReceiverQRPayload
import com.offlinepayment.utils.CurrencyUtils
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PaymentConfirmationScreen(
    receiverQR: ReceiverQRPayload,
    amount: BigDecimal,
    receiverName: String? = null, // Will be fetched from API if available
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    // Parse timestamp
    val timestampFormatted = try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
        val date = inputFormat.parse(receiverQR.timestamp) ?: Date()
        outputFormat.format(date)
    } catch (e: Exception) {
        receiverQR.timestamp
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFF10B981)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Confirm Payment",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF374151)
                )
                Text(
                    text = "Review details before sending",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
        
        // Payment Details Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Payment Details",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF374151)
                )
                
                // Amount
                PaymentDetailRow(
                    icon = Icons.Default.AttachMoney,
                    label = "Amount",
                    value = CurrencyUtils.formatPkr(amount.toDouble()),
                    iconColor = Color(0xFF10B981)
                )
                
                Divider()
                
                // Receiver Name
                PaymentDetailRow(
                    icon = Icons.Default.Person,
                    label = "Receiver",
                    value = receiverName ?: "User ID: ${receiverQR.user_id}",
                    iconColor = Color(0xFF6366F1)
                )
                
                Divider()
                
                // Wallet ID
                PaymentDetailRow(
                    icon = Icons.Default.Wallet,
                    label = "Receiver Wallet ID",
                    value = "#${receiverQR.wallet_id}",
                    iconColor = Color(0xFF8B5CF6)
                )
                
                Divider()
                
                // Timestamp
                PaymentDetailRow(
                    icon = Icons.Default.Schedule,
                    label = "QR Generated",
                    value = timestampFormatted,
                    iconColor = Color(0xFFF59E0B)
                )
            }
        }
        
        // QR Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "QR Code Information",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF92400E)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Version: ${receiverQR.version}",
                    fontSize = 12.sp,
                    color = Color(0xFF78350F)
                )
                Text(
                    text = "Type: ${receiverQR.type}",
                    fontSize = 12.sp,
                    color = Color(0xFF78350F)
                )
                Text(
                    text = "Nonce: ${receiverQR.nonce.take(16)}...",
                    fontSize = 12.sp,
                    color = Color(0xFF78350F)
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onCancel,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF374151)
                )
            ) {
                Text("Cancel", fontWeight = FontWeight.Bold)
            }
            
            Button(
                modifier = Modifier.weight(1f),
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Send Payment", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PaymentDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    iconColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = iconColor
            )
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color(0xFF6B7280)
            )
        }
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF111827)
        )
    }
}

