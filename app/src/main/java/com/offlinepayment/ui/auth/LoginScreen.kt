package com.offlinepayment.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    uiState: AuthUiState,
    onLogin: (email: String, password: String) -> Unit,
    onOtpConfirm: (otp: String) -> Unit,
    onCreateAccount: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var otp by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E3A8A),
                        Color(0xFF3B82F6)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo/Icon
            Card(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "PK",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3A8A)
                        )
                        Text(
                            text = "PAY",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3A8A)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App Title
            Text(
                text = "Offline Payment",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Secure • Fast • Offline",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Login Form
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Welcome Back",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A8A)
                    )

                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            focusedLabelColor = Color(0xFF3B82F6)
                        )
                    )

                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            focusedLabelColor = Color(0xFF3B82F6)
                        )
                    )

                    if (uiState.isOtpStep) {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = otp,
                            onValueChange = { otp = it },
                            label = { Text("OTP (check email)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3B82F6),
                                focusedLabelColor = Color(0xFF3B82F6)
                            )
                        )
                        uiState.demoOtpFromServer?.let { demoOtp ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7))
                            ) {
                                Text(
                                    text = "Demo OTP from server: $demoOtp\nTODO: remove once email channel is live.",
                                    color = Color(0xFF92400E),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    uiState.errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                    }
                    uiState.successMessage?.let { message ->
                        Text(
                            text = message,
                            color = Color(0xFF059669),
                            fontSize = 14.sp
                        )
                    }

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onLogin(email.trim(), password) },
                        enabled = !uiState.isLoading && !uiState.isOtpStep && email.isNotBlank() && password.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E3A8A)
                        )
                    ) {
                        if (uiState.isLoading && !uiState.isOtpStep) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White
                            )
                        } else {
                            Text("Login", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (uiState.isOtpStep) {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onOtpConfirm(otp.trim()) },
                            enabled = !uiState.isLoading && otp.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0F766E)
                            )
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White
                                )
                            } else {
                                Text("Confirm OTP", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Don't have an account? ",
                            color = Color.Gray
                        )
                        TextButton(onClick = onCreateAccount) {
                            Text(
                                text = "Create Account",
                                color = Color(0xFF3B82F6),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Demo Credentials
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Demo Credentials",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Username: demo",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Password: demo123",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
