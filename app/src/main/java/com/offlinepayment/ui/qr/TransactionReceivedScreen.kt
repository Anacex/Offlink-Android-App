package com.offlinepayment.ui.qr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlinepayment.data.TransactionPayloadQR
import com.offlinepayment.utils.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Shown after the receiver scans the sender's payment QR and the BLE handshake
 * (ack + sender OK) completes. The credit is already persisted to the encrypted ledger.
 */
@Composable
fun TransactionReceivedScreen(
    transactionPayload: TransactionPayloadQR,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        // Success Icon
        Card(
            modifier = Modifier.size(80.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
            shape = RoundedCornerShape(40.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Transaction Received",
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFF059669)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Title
        Text(
            text = "Payment received",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Review the payment details below",
            fontSize = 14.sp,
            color = Color(0xFF6B7280)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Transaction Details Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Amount (Prominent)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Amount",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF374151)
                    )
                           Text(
                               text = CurrencyUtils.formatPkr(transactionPayload.amount / 100.0), // Convert from paisa to PKR
                               fontSize = 28.sp,
                               fontWeight = FontWeight.Bold,
                               color = Color(0xFF059669)
                           )
                }
                
                Divider(modifier = Modifier.padding(vertical = 16.dp))
                
                // Payer Name
                DetailRow(
                    icon = Icons.Default.Person,
                    label = "Payer Name",
                    value = transactionPayload.payerName,
                    iconColor = Color(0xFF3B82F6)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Payer ID
                DetailRow(
                    label = "Payer ID",
                    value = transactionPayload.payerId
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Transaction ID
                DetailRow(
                    label = "Transaction ID",
                    value = transactionPayload.txId.take(16) + "..."
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Timestamp
                DetailRow(
                    label = "Timestamp",
                    value = formatTimestamp(transactionPayload.timestamp)
                )
                
                transactionPayload.note?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    DetailRow(
                        label = "Note",
                        value = it
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Security Note
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🔒 Secure Transaction",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E40AF)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Payment Received (Offline). This transaction will be synchronized when online.",
                    fontSize = 12.sp,
                    color = Color(0xFF475569)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onReject,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFDC2626)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Reject", fontWeight = FontWeight.Bold)
            }
            
            Button(
                modifier = Modifier.weight(1f),
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF059669)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Accept Payment", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    label: String,
    value: String,
    iconColor: Color = Color(0xFF6B7280)
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = iconColor
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
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

private fun formatTimestamp(timestamp: Long): String {
    return try {
        val date = java.util.Date(timestamp)
        SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(date)
    } catch (e: Exception) {
        timestamp.toString()
    }
}
