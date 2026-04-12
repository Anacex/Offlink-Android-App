package com.offlinepayment.ui

import android.app.Activity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.offlinepayment.data.TopUpVerifyRequest
import com.offlinepayment.data.network.ApiClient
import com.offlinepayment.data.repository.WalletRepository
import com.offlinepayment.data.session.AuthSessionManager
import com.offlinepayment.ui.auth.EmailVerificationDialog
import com.offlinepayment.utils.BiometricAuthHelper
import com.offlinepayment.utils.CurrencyUtils
import com.offlinepayment.utils.ErrorUtils
import com.offlinepayment.utils.NetworkUtils
import com.offlinepayment.utils.WalletLimits
import kotlinx.coroutines.launch
import java.math.BigDecimal

enum class TopUpStep {
    FORM, // Enter amount, password, biometric
    OTP_VERIFICATION // Enter OTP
}

@Composable
fun TopUpScreen(
    walletId: Int,
    currentBalance: BigDecimal,
    isBiometricAuthenticated: Boolean = false, // Pre-authenticated state
    onTopUpComplete: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val repository = remember { WalletRepository(ApiClient.walletApi) }
    val scope = rememberCoroutineScope()
    
    var currentStep by remember { mutableStateOf(TopUpStep.FORM) }
	var amount by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var otp by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var otpDemo by remember { mutableStateOf<String?>(null) }
    var biometricAuthenticated by remember { mutableStateOf(isBiometricAuthenticated) } // Initialize with pre-authenticated state
    
    val isOnline = NetworkUtils.isOnline(context)
    val isBiometricAvailable = BiometricAuthHelper.isBiometricAvailable(context)
    
    // Check if online
    LaunchedEffect(Unit) {
        if (!isOnline) {
            errorMessage = "Top-up is only available when online. Please connect to the internet."
        }
    }

	Column(
		modifier = Modifier
			.fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF8B5CF6),
                        Color(0xFF6366F1)
                    )
                )
            )
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "Top Up Wallet",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (!isOnline) {
            // Offline message
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
                        text = "⚠️",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Top-up Unavailable",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Top-up is only available when you are online. Please connect to the internet and try again.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        } else {
            when (currentStep) {
                TopUpStep.FORM -> {
                    // Current Balance Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "Current Balance",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = CurrencyUtils.formatPkr(currentBalance.toDouble()),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Max offline wallet limit: ${CurrencyUtils.formatPkr(WalletLimits.MAX_OFFLINE_WALLET_BALANCE)}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Amount Input
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "Enter Amount",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF374151)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
		OutlinedTextField(
			modifier = Modifier.fillMaxWidth(),
			value = amount,
			onValueChange = { amount = it },
                                label = { Text("Amount (PKR)") },
			singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF8B5CF6),
                                    focusedLabelColor = Color(0xFF8B5CF6),
                                    focusedTextColor = Color(0xFF111827),
                                    unfocusedTextColor = Color(0xFF111827)
                                )
                            )
                            
                            val amountValue = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                            val newBalance = currentBalance + amountValue
                            val exceedsLimit = !WalletLimits.isBalanceWithinLimit(currentBalance, amountValue)
                            
                            if (amountValue > BigDecimal.ZERO) {
                                Spacer(modifier = Modifier.height(12.dp))
                                if (exceedsLimit) {
                                    Text(
                                        text = "⚠️ New balance would exceed limit of ${CurrencyUtils.formatPkr(WalletLimits.MAX_OFFLINE_WALLET_BALANCE)}",
                                        fontSize = 12.sp,
                                        color = Color(0xFFDC2626)
                                    )
                                } else {
                                    Text(
                                        text = "New balance: ${CurrencyUtils.formatPkr(newBalance.toDouble())}",
                                        fontSize = 14.sp,
                                        color = Color(0xFF059669),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Password Input
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "Enter Password",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF374151)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                singleLine = true,
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (showPassword) "Hide password" else "Show password"
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF8B5CF6),
                                    focusedLabelColor = Color(0xFF8B5CF6),
                                    focusedTextColor = Color(0xFF111827),
                                    unfocusedTextColor = Color(0xFF111827)
                                )
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Biometric authentication is already done before showing this screen
                    // No need to show biometric section here
                    
                    // Error/Success Messages
                    errorMessage?.let { error ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Text(
                                text = error,
                                color = Color(0xFFDC2626),
                                modifier = Modifier.padding(16.dp),
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // Submit Button
                    val amountValue = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val canSubmit = amountValue > BigDecimal.ZERO &&
                            password.isNotEmpty() &&
                            (!isBiometricAvailable || biometricAuthenticated) &&
                            WalletLimits.isBalanceWithinLimit(currentBalance, amountValue)

		Button(
			modifier = Modifier
				.fillMaxWidth()
				.height(56.dp),
			onClick = {
                            if (canSubmit && activity != null) {
                                isLoading = true
                                errorMessage = null
                                scope.launch {
                                    val session = AuthSessionManager.currentSession()
                                    if (session == null) {
                                        errorMessage = "Please login first"
                                        isLoading = false
                                        return@launch
                                    }
                                    
                                    val result = repository.topUp(
                                        com.offlinepayment.data.TopUpRequest(
                                            wallet_id = walletId,
                                            amount = amountValue,
                                            password = password,
                                            bank_account_number = "" // Bank account number not required
                                        )
                                    )
                                    
                                    result.fold(
                                        onSuccess = { response ->
                                            otpDemo = response.otp_demo
                                            successMessage = response.msg
                                            currentStep = TopUpStep.OTP_VERIFICATION
                                            isLoading = false
                                        },
                                        onFailure = { error ->
                                            errorMessage = ErrorUtils.cleanErrorMessageForDisplay(error.message) ?: "Top-up request failed"
                                            isLoading = false
                                        }
                                    )
                                }
                            }
                        },
                        enabled = canSubmit && !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canSubmit) Color(0xFF8B5CF6) else Color.Gray
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                        } else {
                            Text(
                                "Request Top Up",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    // Add bottom spacing to ensure button is visible
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                TopUpStep.OTP_VERIFICATION -> {
                    // OTP Verification Screen
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
                                text = "Verify Top Up",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF374151)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Enter the OTP sent to your email",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Show demo OTP if available
                            otpDemo?.let { demo ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Demo OTP (for testing):",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = demo,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF059669)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = otp,
                                onValueChange = { otp = it },
                                label = { Text("Enter OTP") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF8B5CF6),
                                    focusedLabelColor = Color(0xFF8B5CF6),
                                    focusedTextColor = Color(0xFF111827),
                                    unfocusedTextColor = Color(0xFF111827)
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            errorMessage?.let { error ->
                                Text(
                                    text = error,
                                    color = Color(0xFFDC2626),
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        currentStep = TopUpStep.FORM
                                        otp = ""
                                        errorMessage = null
                                        // Keep amount when going back
                                    }
                                ) {
                                    Text("Back")
                                }
                                
                                Button(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (otp.isNotEmpty()) {
                                            isLoading = true
                                            errorMessage = null
                                            scope.launch {
                                                val result = repository.verifyTopUp(
                                                    TopUpVerifyRequest(
                                                        wallet_id = walletId,
                                                        otp = otp
                                                    )
                                                )
                                                
                                                result.fold(
                                                    onSuccess = { response ->
                                                        successMessage = response.msg
                                                        // Refresh wallet balance
                                                        isLoading = false
                                                        onTopUpComplete()
                                                    },
                                                    onFailure = { error ->
                                                        errorMessage = ErrorUtils.cleanErrorMessageForDisplay(error.message) ?: "OTP verification failed"
                                                        isLoading = false
                                                    }
                                                )
                                            }
                                        }
                                    },
                                    enabled = otp.isNotEmpty() && !isLoading,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF8B5CF6)
                                    )
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White
                                        )
                                    } else {
                                        Text("Verify OTP")
                                    }
                                }
                            }
                        }
                    }
                }
            }
		}
	}
}
