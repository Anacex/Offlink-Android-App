package com.offlinepayment.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlinepayment.ui.wallet.WalletUiState
import com.offlinepayment.utils.CurrencyUtils
import java.math.BigDecimal
import kotlin.text.toBigDecimalOrNull

@Composable
fun WalletScreen(
    uiState: WalletUiState,
    onRefresh: () -> Unit,
    onTransfer: (Int, Int, BigDecimal) -> Unit,
    onSendClick: () -> Unit,
    onViewTransactionsClick: () -> Unit,
    onTopUpClick: () -> Unit = {}
) {
    val balanceText = uiState.wallets.firstOrNull()?.balance?.toPlainString() ?: "0.00"
    var fromWalletInput by rememberSaveable { mutableStateOf("") }
    var toWalletInput by rememberSaveable { mutableStateOf("") }
    var amountInput by rememberSaveable { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E40AF),
                        Color(0xFF3B82F6)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
            }

            uiState.errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE4E6))
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = Color(0xFFB91C1C)
                    )
                }
            }

            uiState.lastTransferReference?.let { reference ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD1FAE5))
                ) {
                    Text(
                        text = "Last transfer reference: $reference",
                        modifier = Modifier.padding(16.dp),
                        color = Color(0xFF065F46)
                    )
                }
            }

            // Welcome Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Wallet Overview",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E40AF)
                    )
                    Text(
                        text = "Connected to Render backend",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Balance Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Primary Balance",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF374151)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val display = if (balanceText.startsWith("Rs")) {
                        balanceText
                    } else {
                        CurrencyUtils.formatPkr(balanceText.toDoubleOrNull() ?: 0.0)
                    }
                    Text(
                        text = display,
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF059669)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Available for wallet transfers",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRefresh, shape = RoundedCornerShape(12.dp)) {
                        Text("Refresh from API")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Wallet list
            if (uiState.wallets.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Wallets",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E40AF)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        uiState.wallets.forEach { wallet ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Wallet #${wallet.id} (${wallet.wallet_type})",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(text = "Currency: ${wallet.currency}")
                                    Text(
                                        text = "Balance: ${CurrencyUtils.formatPkr(wallet.balance.toDouble())}"
                                    )
                                    Text(text = "Active: ${wallet.is_active}")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Transfer form (online -> offline wallet)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Transfer Between Wallets",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E40AF)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Provide wallet IDs from the list above. This hits POST /api/v1/wallets/transfer.",
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = fromWalletInput,
                        onValueChange = { fromWalletInput = it },
                        label = { Text("From wallet id") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            focusedLabelColor = Color(0xFF3B82F6)
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = toWalletInput,
                        onValueChange = { toWalletInput = it },
                        label = { Text("To wallet id") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            focusedLabelColor = Color(0xFF3B82F6)
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Amount (PKR)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            focusedLabelColor = Color(0xFF3B82F6)
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    val fromId = fromWalletInput.toIntOrNull()
                    val toId = toWalletInput.toIntOrNull()
                    val amount = amountInput.toBigDecimalOrNull()
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = fromId != null && toId != null && amount != null && amount > BigDecimal.ZERO,
                        onClick = {
                            if (fromId != null && toId != null && amount != null) {
                                onTransfer(fromId, toId, amount)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Submit transfer", fontWeight = FontWeight.Bold)
                    }
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
                    onClick = onSendClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Send Payment", fontWeight = FontWeight.Bold)
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onViewTransactionsClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6366F1)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "View Transactions", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onTopUpClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8B5CF6)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Top Up (Online → Offline Wallet)", fontWeight = FontWeight.Bold)
            }
        }
    }
}
