package com.offlinepayment.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun EmailVerificationDialog(
    email: String?,
    isLoading: Boolean,
    errorMessage: String?,
    successMessage: String?,
    onOtpSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
    onResendOtp: () -> Unit
) {
    var otp by remember { mutableStateOf("") }
    val otpFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        otpFocusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Verify Your Email",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF059669)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "We've sent a verification code to",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                email?.let {
                    Text(
                        text = it,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF059669)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Please check your email for the verification code",
                    fontSize = 13.sp,
                    color = Color.Gray
                )

                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(otpFocusRequester),
                    value = otp,
                    onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) otp = it },
                    label = { Text("Enter OTP", color = Color(0xFF374151)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF10B981),
                        focusedLabelColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF9CA3AF),
                        unfocusedLabelColor = Color(0xFF6B7280),
                        focusedTextColor = Color(0xFF111827),
                        unfocusedTextColor = Color(0xFF111827)
                    ),
                    placeholder = { Text("000000", color = Color(0xFF9CA3AF)) }
                )

                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }

                successMessage?.let {
                    Text(
                        text = it,
                        color = Color(0xFF059669),
                        fontSize = 14.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss,
                        enabled = !isLoading
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { onOtpSubmit(otp.trim()) },
                        enabled = otp.length == 6 && !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF059669)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White
                            )
                        } else {
                            Text("Verify", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                TextButton(onClick = onResendOtp) {
                    Text(
                        text = "Resend OTP",
                        color = Color(0xFF059669),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

