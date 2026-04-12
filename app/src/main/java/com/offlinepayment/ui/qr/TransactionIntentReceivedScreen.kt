package com.offlinepayment.ui.qr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlinepayment.data.TransactionIntent
import com.offlinepayment.utils.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionIntentReceivedScreen(
    intent: TransactionIntent,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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
                    contentDescription = "Success",
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFF059669)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Title
        Text(
            text = "Transaction Intent Received",
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
                // Amount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Amount",
                        fontSize = 16.sp,
                        color = Color(0xFF6B7280)
                    )
                    Text(
                        text = CurrencyUtils.formatPkr(intent.amount.toDouble()),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF059669)
                    )
                }
                
                Divider(modifier = Modifier.padding(vertical = 16.dp))
                
                // Sender Details
                DetailRow(
                    label = "Sender User ID",
                    value = intent.userID.toString()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                DetailRow(
                    label = "Sender Wallet ID",
                    value = intent.walletID.toString()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                DetailRow(
                    label = "Transaction Type",
                    value = intent.transactionIntentType
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                DetailRow(
                    label = "Timestamp",
                    value = formatTimestamp(intent.timestamp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                DetailRow(
                    label = "Nonce",
                    value = intent.nonce.take(16) + "..."
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
                Text("Accept", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF6B7280)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF111827)
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
    return format.format(date)
}

