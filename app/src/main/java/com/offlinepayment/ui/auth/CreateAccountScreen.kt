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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Validates password complexity to match server requirements:
 * - At least 10 characters
 * - Contains uppercase letter
 * - Contains lowercase letter
 * - Contains a digit
 * - Contains a special character
 */
private fun isPasswordValid(password: String): Boolean {
    if (password.length < 10) return false
    val hasUpperCase = password.any { it.isUpperCase() }
    val hasLowerCase = password.any { it.isLowerCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSpecialChar = password.any { !it.isLetterOrDigit() && !it.isWhitespace() }
    return hasUpperCase && hasLowerCase && hasDigit && hasSpecialChar
}

/**
 * Returns detailed password requirement status
 */
private fun getPasswordRequirements(password: String): List<Pair<String, Boolean>> {
    return listOf(
        "At least 10 characters" to (password.length >= 10),
        "Contains uppercase letter" to password.any { it.isUpperCase() },
        "Contains lowercase letter" to password.any { it.isLowerCase() },
        "Contains a number" to password.any { it.isDigit() },
        "Contains special character" to password.any { !it.isLetterOrDigit() && !it.isWhitespace() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAccountScreen(
    uiState: AuthUiState,
    onSignup: (name: String, email: String, password: String, phone: String) -> Unit,
    onVerifyEmail: (email: String, otp: String) -> Unit,
    onBackToLogin: () -> Unit
) {
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var verificationOtp by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var showPasswordRequirements by rememberSaveable { mutableStateOf(false) }
    
    val otpFocusRequester = remember { FocusRequester() }
    
    // Auto-focus OTP input when signup succeeds
    LaunchedEffect(uiState.isSignupOtpStep) {
        if (uiState.isSignupOtpStep) {
            // Use the email from signup response
            email = uiState.signupEmail ?: email
            // Clear any previous OTP - user must manually enter it for security
            // Note: We intentionally do NOT auto-fill OTP even if server provides otp_demo
            // OTPs should only be received via email/SMS, not through API responses
            verificationOtp = ""
            // Focus the OTP field
            otpFocusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF059669),
                        Color(0xFF10B981)
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
            // Header
            Card(
                modifier = Modifier
                    .size(100.dp)
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
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669)
                        )
                        Text(
                            text = "PAY",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Create Account",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Registration Form or OTP Verification
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                if (uiState.isSignupOtpStep) {
                    // Show OTP verification screen after successful signup
                    OtpVerificationSection(
                        email = uiState.signupEmail ?: email,
                        otp = verificationOtp,
                        onOtpChange = { verificationOtp = it },
                        onVerify = { onVerifyEmail(uiState.signupEmail ?: email, verificationOtp.trim()) },
                        isLoading = uiState.isLoading,
                        errorMessage = uiState.errorMessage,
                        successMessage = uiState.successMessage,
                        otpFocusRequester = otpFocusRequester
                    )
                } else {
                    // Show registration form
                    RegistrationFormSection(
                        fullName = fullName,
                        onFullNameChange = { fullName = it },
                        email = email,
                        onEmailChange = { email = it },
                        phoneNumber = phoneNumber,
                        onPhoneNumberChange = { phoneNumber = it },
                        password = password,
                        onPasswordChange = { 
                            password = it
                            showPasswordRequirements = it.isNotEmpty() && !isPasswordValid(it)
                        },
                        confirmPassword = confirmPassword,
                        onConfirmPasswordChange = { confirmPassword = it },
                        passwordVisible = passwordVisible,
                        onPasswordVisibleToggle = { passwordVisible = !passwordVisible },
                        confirmPasswordVisible = confirmPasswordVisible,
                        onConfirmPasswordVisibleToggle = { confirmPasswordVisible = !confirmPasswordVisible },
                        showPasswordRequirements = showPasswordRequirements,
                        errorMessage = errorMessage.ifEmpty { uiState.errorMessage.orEmpty() },
                        successMessage = uiState.successMessage,
                        isLoading = uiState.isLoading,
                        onSignup = {
                            when {
                                fullName.isEmpty() || email.isEmpty() || phoneNumber.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() -> {
                                    errorMessage = "Please fill all fields"
                                    showPasswordRequirements = false
                                }
                                password != confirmPassword -> {
                                    errorMessage = "Passwords do not match"
                                    showPasswordRequirements = false
                                }
                                !isPasswordValid(password) -> {
                                    errorMessage = "Password does not meet complexity requirements"
                                    showPasswordRequirements = true
                                }
                                else -> {
                                    errorMessage = ""
                                    showPasswordRequirements = false
                                    onSignup(
                                        fullName.trim(),
                                        email.trim(),
                                        password,
                                        phoneNumber.trim()
                                    )
                                }
                            }
                        },
                        onBackToLogin = onBackToLogin
                    )
                }
            }
        }
    }
}

@Composable
private fun OtpVerificationSection(
    email: String,
    otp: String,
    onOtpChange: (String) -> Unit,
    onVerify: () -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    successMessage: String?,
    otpFocusRequester: FocusRequester
) {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
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
        Text(
            text = email,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF059669)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Please check your email for the verification code",
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(otpFocusRequester),
            value = otp,
            onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) onOtpChange(it) },
            label = { Text("Enter OTP", color = Color(0xFF374151)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF10B981),
                focusedLabelColor = Color(0xFF10B981),
                unfocusedBorderColor = Color(0xFF9CA3AF),
                unfocusedLabelColor = Color(0xFF6B7280),
                focusedTextColor = Color(0xFF111827), // Dark text when focused
                unfocusedTextColor = Color(0xFF111827) // Dark text when unfocused
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
        
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onVerify,
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
                Text("Verify Email", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RegistrationFormSection(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibleToggle: () -> Unit,
    confirmPasswordVisible: Boolean,
    onConfirmPasswordVisibleToggle: () -> Unit,
    showPasswordRequirements: Boolean,
    errorMessage: String,
    successMessage: String?,
    isLoading: Boolean,
    onSignup: () -> Unit,
    onBackToLogin: () -> Unit
) {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = fullName,
            onValueChange = onFullNameChange,
            label = { Text("Full Name", color = Color(0xFF374151)) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF10B981),
                focusedLabelColor = Color(0xFF10B981),
                unfocusedBorderColor = Color(0xFF9CA3AF),
                unfocusedLabelColor = Color(0xFF6B7280),
                focusedTextColor = Color(0xFF111827), // Dark text when focused
                unfocusedTextColor = Color(0xFF111827) // Dark text when unfocused
            )
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email", color = Color(0xFF374151)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF10B981),
                focusedLabelColor = Color(0xFF10B981),
                unfocusedBorderColor = Color(0xFF9CA3AF),
                unfocusedLabelColor = Color(0xFF6B7280),
                focusedTextColor = Color(0xFF111827), // Dark text when focused
                unfocusedTextColor = Color(0xFF111827) // Dark text when unfocused
            )
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = phoneNumber,
            onValueChange = onPhoneNumberChange,
            label = { Text("Phone Number", color = Color(0xFF374151)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF10B981),
                focusedLabelColor = Color(0xFF10B981),
                unfocusedBorderColor = Color(0xFF9CA3AF),
                unfocusedLabelColor = Color(0xFF6B7280),
                focusedTextColor = Color(0xFF111827), // Dark text when focused
                unfocusedTextColor = Color(0xFF111827) // Dark text when unfocused
            )
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password", color = Color(0xFF374151)) },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = onPasswordVisibleToggle) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF10B981),
                focusedLabelColor = Color(0xFF10B981),
                unfocusedBorderColor = Color(0xFF9CA3AF),
                unfocusedLabelColor = Color(0xFF6B7280),
                focusedTextColor = Color(0xFF111827), // Dark text when focused
                unfocusedTextColor = Color(0xFF111827) // Dark text when unfocused
            )
        )
        if (showPasswordRequirements) {
            Column(modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
                Text(
                    text = "Password requirements:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(4.dp))
                getPasswordRequirements(password).forEach { (requirement, met) ->
                    Text(
                        text = "${if (met) "✓" else "✗"} $requirement",
                        fontSize = 11.sp,
                        color = if (met) Color(0xFF059669) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                    )
                }
            }
        } else {
            Text(
                text = "Must be 10+ chars with uppercase, lowercase, number, and special char",
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = { Text("Confirm Password", color = Color(0xFF374151)) },
            singleLine = true,
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = onConfirmPasswordVisibleToggle) {
                    Icon(
                        imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF10B981),
                focusedLabelColor = Color(0xFF10B981),
                unfocusedBorderColor = Color(0xFF9CA3AF),
                unfocusedLabelColor = Color(0xFF6B7280),
                focusedTextColor = Color(0xFF111827), // Dark text when focused
                unfocusedTextColor = Color(0xFF111827) // Dark text when unfocused
            )
        )

        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp
            )
        }
        successMessage?.let { message ->
            Text(
                text = message,
                color = Color(0xFF059669),
                fontSize = 14.sp
            )
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onSignup,
            enabled = !isLoading,
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
                Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Already have an account? ",
                color = Color.Gray
            )
            TextButton(onClick = onBackToLogin) {
                Text(
                    text = "Login",
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
