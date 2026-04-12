package com.offlinepayment.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlinepayment.utils.meetsSignupPasswordPolicy
import com.offlinepayment.utils.signupPasswordRequirements

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    uiState: AuthUiState,
    onRequestReset: (email: String) -> Unit,
    onConfirmReset: (otp: String, newPassword: String, confirmPassword: String) -> Unit,
    onBack: () -> Unit,
    onConsumeCompleteMessage: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var otp by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmVisible by rememberSaveable { mutableStateOf(false) }

    val doneMsg = uiState.passwordResetCompleteMessage

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6)),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Reset password",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = "We will email you a code if this address is registered.",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        if (doneMsg != null) {
                            Text(
                                text = doneMsg,
                                color = Color(0xFF059669),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    onConsumeCompleteMessage()
                                    onBack()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                            ) {
                                Text("Back to login", fontWeight = FontWeight.Bold)
                            }
                        } else {

                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            singleLine = true,
                            enabled = !uiState.forgotPasswordAwaitingCode,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3B82F6),
                                focusedLabelColor = Color(0xFF3B82F6),
                                focusedTextColor = Color(0xFF111827),
                                unfocusedTextColor = Color(0xFF111827),
                            ),
                        )

                        uiState.forgotPasswordInfo?.let { info ->
                            Text(
                                text = info,
                                color = Color(0xFF374151),
                                fontSize = 13.sp,
                            )
                        }

                        if (uiState.forgotPasswordAwaitingCode) {
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = otp,
                                onValueChange = { otp = it },
                                label = { Text("Code from email") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF3B82F6),
                                    focusedLabelColor = Color(0xFF3B82F6),
                                    focusedTextColor = Color(0xFF111827),
                                    unfocusedTextColor = Color(0xFF111827),
                                ),
                            )
                            uiState.forgotPasswordOtpDemo?.let { demo ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                                ) {
                                    Text(
                                        text = "Demo code from server: $demo",
                                        color = Color(0xFF92400E),
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(12.dp),
                                    )
                                }
                            }
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                label = { Text("New password") },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) {
                                                Icons.Default.VisibilityOff
                                            } else {
                                                Icons.Default.Visibility
                                            },
                                            contentDescription = null,
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF3B82F6),
                                    focusedLabelColor = Color(0xFF3B82F6),
                                    focusedTextColor = Color(0xFF111827),
                                    unfocusedTextColor = Color(0xFF111827),
                                ),
                            )
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("Confirm new password") },
                                singleLine = true,
                                visualTransformation = if (confirmVisible) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(onClick = { confirmVisible = !confirmVisible }) {
                                        Icon(
                                            imageVector = if (confirmVisible) {
                                                Icons.Default.VisibilityOff
                                            } else {
                                                Icons.Default.Visibility
                                            },
                                            contentDescription = null,
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF3B82F6),
                                    focusedLabelColor = Color(0xFF3B82F6),
                                    focusedTextColor = Color(0xFF111827),
                                    unfocusedTextColor = Color(0xFF111827),
                                ),
                            )
                            Text(
                                text = "Same rules as signup: 10+ chars, upper, lower, number, special character.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B7280),
                            )
                            signupPasswordRequirements(newPassword).forEach { (reqText, ok) ->
                                Text(
                                    text = "${if (ok) "✓" else "○"} $reqText",
                                    fontSize = 12.sp,
                                    color = if (ok) Color(0xFF059669) else Color(0xFF9CA3AF),
                                )
                            }
                        }

                        uiState.forgotPasswordError?.let { err ->
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp,
                            )
                        }

                        if (uiState.forgotPasswordAwaitingCode) {
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    onConfirmReset(otp.trim(), newPassword, confirmPassword)
                                },
                                enabled = !uiState.forgotPasswordLoading &&
                                    otp.isNotBlank() &&
                                    meetsSignupPasswordPolicy(newPassword) &&
                                    newPassword == confirmPassword,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                            ) {
                                if (uiState.forgotPasswordLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                    )
                                } else {
                                    Text("Update password", fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onRequestReset(email) },
                                enabled = !uiState.forgotPasswordLoading && email.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                            ) {
                                if (uiState.forgotPasswordLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                    )
                                } else {
                                    Text("Send reset code", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        TextButton(
                            onClick = onBack,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            Text("Cancel", color = Color(0xFF3B82F6))
                        }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
