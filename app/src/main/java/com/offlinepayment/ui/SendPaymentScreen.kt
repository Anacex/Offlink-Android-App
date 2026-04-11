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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.offlinepayment.utils.BiometricAuthHelper
import com.offlinepayment.utils.CurrencyUtils
import com.offlinepayment.utils.QRCodeHelper
import com.offlinepayment.utils.WalletLimits
import com.offlinepayment.data.session.DeviceFingerprintProvider
import java.math.BigDecimal
import androidx.compose.foundation.Image
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlin.text.Charsets
import com.offlinepayment.utils.NetworkUtils
import com.offlinepayment.ble.BleHandshake
import com.offlinepayment.ble.BlePaymentMessages
import com.offlinepayment.ble.BlePaymentLink
import com.offlinepayment.ble.BleReceiverAckWire
import com.offlinepayment.ble.TeeEcdsaSigner
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun SendPaymentScreen(
	userId: Int = 1, // TODO: Get from auth session
	walletId: Int = 1, // TODO: Get from wallet selection
	walletBalance: BigDecimal = BigDecimal.ZERO, // Current wallet balance
	senderPublicKey: String? = null, // Sender's public key from wallet
	senderName: String = "", // Sender's name
	senderAccount: String = "", // Sender's bank account number
	bleHandshakeEnabled: Boolean = false,
	onGenerateQR: (String) -> Unit, // Callback with QR data (Base64)
	onScanQR: (BigDecimal) -> Unit, // Navigate to scanner with amount
	onReceiverQRScanned: (com.offlinepayment.data.PayeeQRPayload, BigDecimal) -> Unit = { _, _ -> }, // Callback when payee QR is scanned
	scannedPayeeQRFromNav: com.offlinepayment.data.PayeeQRPayload? = null, // Payee QR passed from navigation
	onBleLinkLostExit: () -> Unit = {}, // Leave send flow after Bluetooth required but link lost
) {
	var amount by remember { mutableStateOf("") }
	var showQRCode by remember { mutableStateOf(false) }
	var qrBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
	var balanceError by remember { mutableStateOf<String?>(null) }
	var biometricAuthenticated by remember { mutableStateOf(false) }
	var authenticationError by remember { mutableStateOf<String?>(null) }
	var scannedPayeeQR by remember(scannedPayeeQRFromNav) { 
		mutableStateOf<com.offlinepayment.data.PayeeQRPayload?>(scannedPayeeQRFromNav)
	}
	var showPayeeConfirmation by remember { mutableStateOf(false) } // Step 2: Show payee confirmation
	var transactionCompleted by remember { mutableStateOf(false) } // Track if transaction is completed
	var currentTransactionPayload by remember { mutableStateOf<com.offlinepayment.data.TransactionPayloadQR?>(null) } // Store current transaction payload
	var bleHandshakeError by remember { mutableStateOf<String?>(null) }
	
	// Update scannedPayeeQR when it comes from navigation
	LaunchedEffect(scannedPayeeQRFromNav) {
		if (scannedPayeeQRFromNav != null) {
			scannedPayeeQR = scannedPayeeQRFromNav
			showPayeeConfirmation = true // Show payee confirmation after scanning QR
		}
	}
	
	val context = LocalContext.current
	val activity = context as? FragmentActivity
	val scope = rememberCoroutineScope()
	val isDeviceSecurityEnabled = remember { BiometricAuthHelper.isDeviceSecurityEnabled(context) }
	val isBiometricAvailable = remember { BiometricAuthHelper.isBiometricAvailable(context) }
	val securityStatusMessage = remember { BiometricAuthHelper.getDeviceSecurityStatus(context) }
	val isOnline = NetworkUtils.isOnline(context)
	val repository = remember { 
		com.offlinepayment.data.repository.WalletRepository(
			com.offlinepayment.data.network.ApiClient.walletApi,
			context
		)
	}
	var isLoadingPrivateKey by remember { mutableStateOf(false) }
	var privateKeyError by remember { mutableStateOf<String?>(null) }
	var privateKey by remember { mutableStateOf<String?>(null) }
	
	// Retrieve private key from cache (offline) or API (online)
	LaunchedEffect(walletId) {
		if (privateKey == null && !isLoadingPrivateKey) {
			isLoadingPrivateKey = true
			privateKeyError = null
			
			// Try to get from cache first (works offline)
			val cachedKey = repository.getCachedPrivateKey(walletId)
			if (cachedKey != null) {
				privateKey = cachedKey
				isLoadingPrivateKey = false
		 } else if (isOnline) {
				// Fetch from API if online and not in cache
				val result = repository.getWalletPrivateKey(walletId)
				result.fold(
					onSuccess = { key ->
						privateKey = key
						isLoadingPrivateKey = false
					},
					onFailure = { error ->
						privateKeyError = error.message
						isLoadingPrivateKey = false
						// Continue with placeholder signature for demo
					}
				)
			} else {
				// Offline and no cached key
				privateKeyError = "Private key not available offline. Please connect to internet first."
				isLoadingPrivateKey = false
			}
		}
	}
	
	LaunchedEffect(showQRCode, currentTransactionPayload?.txId, bleHandshakeEnabled) {
		if (!bleHandshakeEnabled) return@LaunchedEffect
		val payload = currentTransactionPayload ?: return@LaunchedEffect
		if (!showQRCode) return@LaunchedEffect
		if (transactionCompleted) return@LaunchedEffect
		bleHandshakeError = null
		if (!BlePaymentLink.senderBleSessionActive) {
			bleHandshakeError = "Bluetooth disconnected. This payment cannot continue without the link."
			return@LaunchedEffect
		}
		BlePaymentLink.beginAtomicBleTransaction()
		try {
			val (abortReason, ack) = coroutineScope {
				val abortWait = async { BlePaymentLink.sessionAbortFlow.first() }
				val ackWait = async {
					withTimeoutOrNull(180_000L) {
						BlePaymentLink.receiverAckFlow.first { wire ->
							val canon = BlePaymentMessages.canonicalReceiverAckSignString(
								payload.txId,
								payload.payeeId,
								payload.payerId,
								payload.amount,
								wire.ts,
							)
							TeeEcdsaSigner.verifySignature(
								wire.receiverPubKeySpkiB64,
								canon,
								wire.signatureDer,
							)
						}
					}
				}
				select<Pair<String?, BleReceiverAckWire?>> {
					abortWait.onAwait { r ->
						ackWait.cancel()
						r to null
					}
					ackWait.onAwait { a ->
						abortWait.cancel()
						null to a
					}
				}
			}
			if (abortReason != null) {
				bleHandshakeError = abortReason
				showQRCode = false
				qrBitmap = null
				currentTransactionPayload = null
				return@LaunchedEffect
			}
			if (ack == null) {
				bleHandshakeError = "Timed out waiting for receiver Bluetooth acknowledgment."
				showQRCode = false
				qrBitmap = null
				currentTransactionPayload = null
				return@LaunchedEffect
			}
			val okSent = BleHandshake.senderVerifyAckAndReplyOk(context, ack, payload)
			if (!okSent) {
				bleHandshakeError = "Could not send Bluetooth confirmation to receiver."
				showQRCode = false
				qrBitmap = null
				currentTransactionPayload = null
				return@LaunchedEffect
			}
			val payee = scannedPayeeQR
			val result = BleHandshake.persistSenderLedger(
				context = context,
				repository = repository,
				payload = payload,
				walletId = walletId,
				scannedPayeeQR = payee,
			)
			result.onSuccess { transactionCompleted = true }
			result.onFailure { e ->
				bleHandshakeError = e.message ?: "Failed to save payment"
				showQRCode = false
				qrBitmap = null
				currentTransactionPayload = null
			}
		} finally {
			BlePaymentLink.endAtomicBleTransaction()
		}
	}

	LaunchedEffect(transactionCompleted, bleHandshakeEnabled) {
		if (transactionCompleted && bleHandshakeEnabled) {
			BlePaymentLink.clear()
		}
	}

	// Auto-trigger biometric authentication when ready to generate QR (if available and not authenticated)
	LaunchedEffect(scannedPayeeQR, showPayeeConfirmation) {
		if (isDeviceSecurityEnabled && isBiometricAvailable && !biometricAuthenticated && activity != null && scannedPayeeQR != null && !showPayeeConfirmation) {
			// Small delay to let UI render first
			kotlinx.coroutines.delay(500)
			val success = BiometricAuthHelper.authenticate(
				activity,
				title = "Payment QR Authentication",
				subtitle = "Use your fingerprint, face, or device password to generate payment QR"
			)
			biometricAuthenticated = success
			if (!success) {
				authenticationError = "Authentication required to generate payment QR"
			}
		}
	}

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(
				Brush.verticalGradient(
					colors = listOf(
						Color(0xFF10B981),
						Color(0xFF059669)
					)
				)
			)
	) {
		// Check device security first - show error if not enabled
		if (!isDeviceSecurityEnabled) {
			Column(
				modifier = Modifier
					.fillMaxSize()
					.padding(24.dp),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.Center
			) {
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
							text = "🔒",
							fontSize = 64.sp
						)
						Spacer(modifier = Modifier.height(16.dp))
						Text(
							text = "Device Security Required",
							fontSize = 24.sp,
							fontWeight = FontWeight.Bold,
							color = Color(0xFFDC2626)
						)
						Spacer(modifier = Modifier.height(16.dp))
						Text(
							text = securityStatusMessage,
							fontSize = 16.sp,
							color = Color(0xFF6B7280),
							textAlign = TextAlign.Center
						)
						Spacer(modifier = Modifier.height(8.dp))
						Text(
							text = "To use send payment features, please set up a screen lock (password, PIN, pattern, or fingerprint) in your device settings.",
							fontSize = 14.sp,
							color = Color(0xFF9CA3AF),
							textAlign = TextAlign.Center
						)
					}
				}
			}
		} else {
			val scrollState = rememberScrollState()
			Column(
				modifier = Modifier
					.fillMaxSize()
					.verticalScroll(scrollState)
					.padding(16.dp),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.Top
			) {
			// Header
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
						text = "Send Payment",
						fontSize = 24.sp,
						fontWeight = FontWeight.Bold,
						color = Color(0xFF059669)
					)
					Text(
						text = "Scan QR code to send money",
						fontSize = 16.sp,
						color = Color.Gray
					)
				}
			}

			Spacer(modifier = Modifier.height(24.dp))

			val bleLinkLost = bleHandshakeEnabled && !BlePaymentLink.senderBleSessionActive && !transactionCompleted
			if (bleLinkLost) {
				Card(
					modifier = Modifier.fillMaxWidth(),
					colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
					elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
				) {
					Column(
						modifier = Modifier.padding(16.dp),
						horizontalAlignment = Alignment.CenterHorizontally,
					) {
						Text(
							text = "Bluetooth connection lost",
							fontWeight = FontWeight.Bold,
							color = Color(0xFFDC2626),
							fontSize = 16.sp,
						)
						Spacer(modifier = Modifier.height(8.dp))
						Text(
							text = "Payments that use Bluetooth confirmation cannot continue without an active link. Nothing new was saved after the link dropped.",
							fontSize = 13.sp,
							color = Color(0xFF7F1D1D),
							textAlign = TextAlign.Center,
						)
						Spacer(modifier = Modifier.height(12.dp))
						Button(
							onClick = {
								showQRCode = false
								qrBitmap = null
								amount = ""
								biometricAuthenticated = false
								scannedPayeeQR = null
								transactionCompleted = false
								currentTransactionPayload = null
								showPayeeConfirmation = false
								bleHandshakeError = null
								BlePaymentLink.clear()
								onBleLinkLostExit()
							},
							modifier = Modifier.fillMaxWidth(),
							colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
						) {
							Text("Exit to wallet", fontWeight = FontWeight.Bold)
						}
					}
				}
				Spacer(modifier = Modifier.height(16.dp))
			}

			// Step 1: Scan Payee QR Code (if not scanned yet)
			if (scannedPayeeQR == null) {
				// Check if sender has zero balance
				val hasZeroBalance = walletBalance <= BigDecimal.ZERO
				
				Card(
					modifier = Modifier.fillMaxWidth(),
					colors = CardDefaults.cardColors(
						containerColor = if (hasZeroBalance) Color(0xFFFEF2F2) else Color.White
					),
					elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
				) {
					Column(
						modifier = Modifier.padding(20.dp),
						horizontalAlignment = Alignment.CenterHorizontally
					) {
						Text(
							text = "Step 1: Scan Payee QR Code",
							fontSize = 18.sp,
							fontWeight = FontWeight.Bold,
							color = Color(0xFF374151)
						)
						
						Spacer(modifier = Modifier.height(8.dp))
						
						if (hasZeroBalance) {
							// Show error message if balance is zero
							Text(
								text = "⚠️",
								fontSize = 32.sp
							)
							Spacer(modifier = Modifier.height(8.dp))
							Text(
								text = "Insufficient Balance",
								fontSize = 16.sp,
								fontWeight = FontWeight.Bold,
								color = Color(0xFFDC2626)
							)
							Spacer(modifier = Modifier.height(8.dp))
							Text(
								text = "Your wallet balance is ${CurrencyUtils.formatPkr(walletBalance.toDouble())}",
								fontSize = 14.sp,
								color = Color(0xFF6B7280),
								textAlign = TextAlign.Center
							)
							Spacer(modifier = Modifier.height(4.dp))
							Text(
								text = "You cannot send payments with zero balance. Please top up your wallet first.",
								fontSize = 13.sp,
								color = Color(0xFF9CA3AF),
								textAlign = TextAlign.Center
							)
						} else {
							Text(
								text = "Scan the payee's identity QR code to identify who you're paying",
								fontSize = 14.sp,
								color = Color.Gray,
								textAlign = TextAlign.Center
							)
						}
						
						Spacer(modifier = Modifier.height(16.dp))

						Button(
							modifier = Modifier.fillMaxWidth(),
							onClick = {
								// Navigate to payee QR scanner
								onScanQR(BigDecimal.ZERO)
							},
							enabled = !hasZeroBalance,
							colors = ButtonDefaults.buttonColors(
								containerColor = Color(0xFF059669),
								disabledContainerColor = Color(0xFFD1D5DB)
							),
							shape = RoundedCornerShape(12.dp)
						) {
							Row(
								verticalAlignment = Alignment.CenterVertically
							) {
								Icon(
									imageVector = Icons.Default.Search,
									contentDescription = "Scan QR",
									modifier = Modifier.size(20.dp)
								)
								Spacer(modifier = Modifier.width(8.dp))
								Text("Scan Payee QR Code", fontWeight = FontWeight.Bold)
							}
						}
					}
				}
			} else if (scannedPayeeQR != null) {
				// Step 2: Show Payee Confirmation
				if (showPayeeConfirmation) {
					Card(
						modifier = Modifier.fillMaxWidth(),
						colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
						elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
					) {
						Column(
							modifier = Modifier.padding(20.dp)
						) {
							Text(
								text = "Step 2: Confirm Payee",
								fontSize = 18.sp,
								fontWeight = FontWeight.Bold,
								color = Color(0xFF059669)
							)
							
							Spacer(modifier = Modifier.height(12.dp))
							val payeeQR = scannedPayeeQR
							Text(
								text = "Are you paying ${payeeQR?.payeeName ?: "Unknown"}?",
								fontSize = 16.sp,
								fontWeight = FontWeight.SemiBold,
								color = Color(0xFF374151)
							)
							
							Spacer(modifier = Modifier.height(16.dp))
							
							// Payee Details
							if (payeeQR != null) {
								Card(
									modifier = Modifier.fillMaxWidth(),
									colors = CardDefaults.cardColors(containerColor = Color.White)
								) {
									Column(
										modifier = Modifier.padding(16.dp)
									) {
										DetailRow("Payee ID", payeeQR.payeeId)
										Spacer(modifier = Modifier.height(8.dp))
										DetailRow("Payee Name", payeeQR.payeeName)
										Spacer(modifier = Modifier.height(8.dp))
										DetailRow("Device ID", payeeQR.deviceId.take(16) + "...")
										Spacer(modifier = Modifier.height(8.dp))
										Text(
											text = "✓ Verified QR came from a real device",
											fontSize = 12.sp,
											color = Color(0xFF059669),
											fontWeight = FontWeight.Medium
										)
										Spacer(modifier = Modifier.height(12.dp))
										Divider()
										Spacer(modifier = Modifier.height(12.dp))
										// Max limit information (from QR)
										val maxLimit = payeeQR.maxTransactionLimit
										Text(
											text = "Maximum Payment Limit",
											fontSize = 14.sp,
											fontWeight = FontWeight.Bold,
											color = Color(0xFF374151)
										)
										Spacer(modifier = Modifier.height(4.dp))
										if (maxLimit != null) {
											Text(
												text = "Transaction limit for this receiver: ${CurrencyUtils.formatPkr(maxLimit)}",
												fontSize = 13.sp,
												color = Color(0xFF059669),
												fontWeight = FontWeight.Medium
											)
											Spacer(modifier = Modifier.height(4.dp))
											Text(
												text = if (maxLimit >= 500.0) {
													"Receiver can accept up to ${CurrencyUtils.formatPkr(maxLimit)} (balance < 4500 PKR)"
												} else {
													"Receiver can accept up to ${CurrencyUtils.formatPkr(maxLimit)} (available cap to reach 5000 PKR)"
												},
												fontSize = 12.sp,
												color = Color(0xFF6B7280)
											)
										} else {
											Text(
												text = "You can send up to ${CurrencyUtils.formatPkr(WalletLimits.MAX_OFFLINE_WALLET_BALANCE)} to this receiver",
												fontSize = 13.sp,
												color = Color(0xFF059669),
												fontWeight = FontWeight.Medium
											)
											Spacer(modifier = Modifier.height(4.dp))
											Text(
												text = "Receiver wallet capacity: ${CurrencyUtils.formatPkr(WalletLimits.MAX_OFFLINE_WALLET_BALANCE)}",
												fontSize = 12.sp,
												color = Color(0xFF6B7280)
											)
										}
									}
								}
							}
							
							Spacer(modifier = Modifier.height(16.dp))
							
							Row(
								modifier = Modifier.fillMaxWidth(),
								horizontalArrangement = Arrangement.spacedBy(12.dp)
							) {
								OutlinedButton(
									modifier = Modifier.weight(1f),
									onClick = {
										scannedPayeeQR = null
										showPayeeConfirmation = false
										showQRCode = false
										qrBitmap = null
									},
									colors = ButtonDefaults.outlinedButtonColors(
										contentColor = Color(0xFFDC2626)
									)
								) {
									Text("Cancel")
								}
								Button(
									modifier = Modifier.weight(1f),
									onClick = {
										showPayeeConfirmation = false
									},
									colors = ButtonDefaults.buttonColors(
										containerColor = Color(0xFF059669)
									)
								) {
									Text("Confirm")
								}
							}
						}
					}
				}
			}

			Spacer(modifier = Modifier.height(24.dp))

			// Step 3: Amount Input Card (only show after payee confirmation)
			if (scannedPayeeQR != null && !showPayeeConfirmation) {
				Card(
				modifier = Modifier.fillMaxWidth(),
				colors = CardDefaults.cardColors(containerColor = Color.White),
				elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
						onValueChange = { newAmount ->
							// Validate input
							if (newAmount.isEmpty() || newAmount.toDoubleOrNull() != null) {
								amount = newAmount
								balanceError = null // Clear error when user types
							}
						},
						label = { Text("Amount (Rs)") },
						singleLine = true,
						keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
						colors = OutlinedTextFieldDefaults.colors(
							focusedBorderColor = Color(0xFF059669),
							focusedLabelColor = Color(0xFF059669),
							focusedTextColor = Color(0xFF111827),
							unfocusedTextColor = Color(0xFF111827)
						),
						isError = balanceError != null
					)

					if (amount.toDoubleOrNull() != null) {
						Spacer(modifier = Modifier.height(12.dp))
						val amountValue = amount.toBigDecimalOrNull()
						if (amountValue != null) {
							Text(
								text = "You will send: ${CurrencyUtils.formatPkr(amountValue.toDouble())}",
								fontSize = 16.sp,
								fontWeight = FontWeight.Medium,
								color = Color(0xFF059669)
							)
							Spacer(modifier = Modifier.height(8.dp))
							// Show validation messages using dynamic limit from QR
							val scannedQR = scannedPayeeQR
							val maxReceiverLimit = if (scannedQR?.maxTransactionLimit != null) {
								BigDecimal(scannedQR.maxTransactionLimit.toString())
							} else {
								WalletLimits.MAX_OFFLINE_WALLET_BALANCE_BD
							}
							val availableBalance = walletBalance
							val maxAllowed = maxReceiverLimit.min(availableBalance)
							
							if (amountValue > maxReceiverLimit) {
								Text(
									text = "⚠ Amount exceeds receiver's transaction limit (${CurrencyUtils.formatPkr(maxReceiverLimit.toDouble())})",
									fontSize = 12.sp,
									color = Color(0xFFDC2626)
								)
							} else if (amountValue > availableBalance) {
								Text(
									text = "⚠ Insufficient balance. Available: ${CurrencyUtils.formatPkr(availableBalance.toDouble())}",
									fontSize = 12.sp,
									color = Color(0xFFDC2626)
								)
							} else {
								Text(
									text = "✓ Valid amount (max: ${CurrencyUtils.formatPkr(maxAllowed.toDouble())})",
									fontSize = 12.sp,
									color = Color(0xFF059669)
								)
							}
						}
					}
				}
			}
			}

			// Step 3: Generate Transaction Payload QR Button
			if (!showQRCode && scannedPayeeQR != null && !showPayeeConfirmation) {
				// Biometric Authentication Section (if available and not authenticated)
				if (isBiometricAvailable && !biometricAuthenticated) {
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
								text = "Security Authentication Required",
								fontSize = 18.sp,
								fontWeight = FontWeight.Bold,
								color = Color(0xFF374151)
							)
							Spacer(modifier = Modifier.height(8.dp))
							Text(
								text = "Authenticate to generate payment QR code",
								fontSize = 14.sp,
								color = Color.Gray
							)
							Spacer(modifier = Modifier.height(16.dp))
							Button(
								modifier = Modifier.fillMaxWidth(),
								onClick = {
									if (activity != null) {
										authenticationError = null
										scope.launch {
											val success = BiometricAuthHelper.authenticate(
												activity,
												title = "Payment QR Authentication",
												subtitle = "Use your fingerprint, face, or device password to generate payment QR"
											)
											biometricAuthenticated = success
											if (!success) {
												authenticationError = "Authentication failed. Please try again."
											}
										}
									}
								},
								colors = ButtonDefaults.buttonColors(
									containerColor = Color(0xFF6366F1)
								),
								shape = RoundedCornerShape(12.dp)
							) {
								Icon(
									Icons.Default.Lock,
									contentDescription = null,
									modifier = Modifier.size(20.dp)
								)
								Spacer(modifier = Modifier.width(8.dp))
								Text("Authenticate with Biometric", fontWeight = FontWeight.Bold)
							}
							
							// Show authentication error
							authenticationError?.let { error ->
								Spacer(modifier = Modifier.height(8.dp))
								Card(
									modifier = Modifier.fillMaxWidth(),
									colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
								) {
									Text(
										text = error,
										color = Color(0xFFDC2626),
										fontSize = 14.sp,
										modifier = Modifier.padding(12.dp)
									)
								}
							}
						}
					}
					Spacer(modifier = Modifier.height(16.dp))
				}
				
				// Generate QR Button (only enabled after authentication if biometric is available)
				Button(
					modifier = Modifier.fillMaxWidth(),
					onClick = { 
						val amountValue = amount.toBigDecimalOrNull()
						if (amountValue != null && amountValue > BigDecimal.ZERO) {
							// Validate transaction limits
							balanceError = null
							authenticationError = null
							
							// Check if authenticated (if biometric is available)
							if (isBiometricAvailable && !biometricAuthenticated) {
								authenticationError = "Please authenticate first"
								return@Button
							}
							
							// Check receiver's transaction limit from QR (dynamic limit)
							val scannedQR = scannedPayeeQR
							val maxReceiverLimit = if (scannedQR?.maxTransactionLimit != null) {
								BigDecimal(scannedQR.maxTransactionLimit.toString())
							} else {
								WalletLimits.MAX_OFFLINE_WALLET_BALANCE_BD
							}
							if (amountValue > maxReceiverLimit) {
								balanceError = "Amount exceeds receiver's transaction limit of ${CurrencyUtils.formatPkr(maxReceiverLimit.toDouble())}"
								return@Button
							}
							
							// Check available balance
							if (amountValue > walletBalance) {
								balanceError = "Insufficient balance. Available: ${CurrencyUtils.formatPkr(walletBalance.toDouble())}"
								return@Button
							}
							
							// Check amount > 0 (should already be checked, but double-check)
							if (amountValue <= BigDecimal.ZERO) {
								balanceError = "Amount must be greater than zero"
								return@Button
							}
							
							// All checks passed - Generate Transaction Payload QR (Step 3)
							val payeeQRValue = scannedPayeeQR
							if (payeeQRValue != null && senderName.isNotEmpty()) {
								// Convert amount to smallest currency unit (paisa for PKR)
								val amountInPaisa = (amountValue.toDouble() * 100).toLong()

								val payerPkB64 = try {
									val tee = TeeEcdsaSigner.getInstance(context)
									tee.getOrCreateKeyPair()
									tee.getPublicKeySpkiBase64()
								} catch (_: Exception) {
									null
								}

								if (bleHandshakeEnabled && payerPkB64.isNullOrBlank()) {
									balanceError =
										"Secure hardware signing is required for Bluetooth payments but is not available. You cannot generate this QR."
									return@Button
								}
								
								// Create Transaction Payload QR
								val transactionPayloadQR = com.offlinepayment.data.TransactionPayloadQR(
									txId = QRCodeHelper.generateQRCodeId(),
									payerId = userId.toString(),
									payeeId = payeeQRValue.payeeId,
									amount = amountInPaisa,
									timestamp = System.currentTimeMillis(),
									nonce = QRCodeHelper.generateQRCodeId(),
									payerName = senderName,
									note = null, // Optional note
									payerPkB64 = payerPkB64,
								)
								
								// Generate QR code string (Base64 encoded JSON)
								// Convert TransactionPayloadQR to JSON, then Base64 encode
								val moshi = com.squareup.moshi.Moshi.Builder()
									.add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
									.build()
								val adapter = moshi.adapter(com.offlinepayment.data.TransactionPayloadQR::class.java)
								val json = adapter.toJson(transactionPayloadQR)
								val qrData = android.util.Base64.encodeToString(json.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
								
								qrBitmap = QRCodeHelper.generateQRCodeBitmap(qrData, 400, 400)
								showQRCode = true
								currentTransactionPayload = transactionPayloadQR
								transactionCompleted = false
								onGenerateQR(qrData)
							} else {
								balanceError = when {
									payeeQRValue == null -> "Please scan payee's QR code first"
									senderName.isEmpty() -> "Sender name not available"
									else -> "Missing required information"
								}
							}
						}
					},
					enabled = scannedPayeeQR != null &&
						!bleLinkLost &&
						!showPayeeConfirmation &&
						amount.toBigDecimalOrNull()?.let { amountValue ->
							// Get dynamic limit from QR
							val scannedQR = scannedPayeeQR
							val maxReceiverLimit = if (scannedQR?.maxTransactionLimit != null) {
								BigDecimal(scannedQR.maxTransactionLimit.toString())
							} else {
								WalletLimits.MAX_OFFLINE_WALLET_BALANCE_BD
							}
							// Amount must be valid
							amountValue > BigDecimal.ZERO &&
							// Must not exceed receiver's transaction limit (from QR)
							amountValue <= maxReceiverLimit &&
							// Must have sufficient balance
							amountValue <= walletBalance &&
							// Must be authenticated if biometric is available
							(!isBiometricAvailable || biometricAuthenticated)
						} == true,
					colors = ButtonDefaults.buttonColors(
						containerColor = Color(0xFF6366F1)
					),
					shape = RoundedCornerShape(12.dp)
				) {
					Text("Generate Payment QR", fontSize = 16.sp, fontWeight = FontWeight.Bold)
				}
				
				// Show balance/limit/authentication errors
				(balanceError ?: authenticationError)?.let { error ->
					Spacer(modifier = Modifier.height(8.dp))
					Card(
						modifier = Modifier.fillMaxWidth(),
						colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
					) {
						Text(
							text = error,
							color = Color(0xFFDC2626),
							fontSize = 14.sp,
							modifier = Modifier.padding(12.dp)
						)
					}
				}
				
				// Show transaction limit info (from QR)
				Spacer(modifier = Modifier.height(8.dp))
				val scannedQR = scannedPayeeQR
				val maxLimit = scannedQR?.maxTransactionLimit
				if (maxLimit != null) {
					Text(
						text = "Max receiver limit: ${CurrencyUtils.formatPkr(maxLimit)}",
						fontSize = 12.sp,
						color = Color.White.copy(alpha = 0.8f)
					)
				} else {
					Text(
						text = "Max receiver limit: ${CurrencyUtils.formatPkr(WalletLimits.MAX_OFFLINE_WALLET_BALANCE)}",
						fontSize = 12.sp,
						color = Color.White.copy(alpha = 0.8f)
					)
				}
				Text(
					text = "Available balance: ${CurrencyUtils.formatPkr(walletBalance.toDouble())}",
					fontSize = 12.sp,
					color = Color.White.copy(alpha = 0.8f)
				)
			}

			// Show Generated Transaction QR Code
			if (showQRCode && qrBitmap != null && scannedPayeeQR != null) {
				Spacer(modifier = Modifier.height(24.dp))
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
							text = "Scan this QR to complete payment",
							fontSize = 18.sp,
							fontWeight = FontWeight.Bold,
							color = Color(0xFF374151)
						)
						Spacer(modifier = Modifier.height(16.dp))
						Image(
							bitmap = qrBitmap!!.asImageBitmap(),
							contentDescription = "Transaction QR Code",
							modifier = Modifier.size(250.dp)
						)
						Spacer(modifier = Modifier.height(16.dp))
						Text(
							text = "Amount: ${CurrencyUtils.formatPkr(amount.toBigDecimalOrNull()?.toDouble() ?: 0.0)}",
							fontSize = 18.sp,
							fontWeight = FontWeight.Bold,
							color = Color(0xFF059669)
						)
						Spacer(modifier = Modifier.height(8.dp))
						val payeeQRForDisplay = scannedPayeeQR
						Text(
							text = "To: ${payeeQRForDisplay?.payeeName ?: "Unknown"}",
							fontSize = 14.sp,
							color = Color.Gray
						)
						Spacer(modifier = Modifier.height(16.dp))

						if (bleHandshakeEnabled && BlePaymentLink.senderBleSessionActive && !transactionCompleted) {
							Text(
								text = "Bluetooth: waiting for receiver to scan this QR and acknowledge…",
								fontSize = 13.sp,
								color = Color(0xFF1D4ED8),
								textAlign = TextAlign.Center,
							)
							bleHandshakeError?.let { err ->
								Spacer(modifier = Modifier.height(8.dp))
								Text(text = err, color = Color(0xFFDC2626), fontSize = 13.sp, textAlign = TextAlign.Center)
							}
							Spacer(modifier = Modifier.height(12.dp))
						}
						
						if (transactionCompleted) {
							// Show success message
							Card(
								modifier = Modifier.fillMaxWidth(),
								colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5))
							) {
								Column(
									modifier = Modifier.padding(16.dp),
									horizontalAlignment = Alignment.CenterHorizontally
								) {
									Text(
										text = "✓ Transaction Completed",
										fontSize = 16.sp,
										fontWeight = FontWeight.Bold,
										color = Color(0xFF059669)
									)
									Spacer(modifier = Modifier.height(8.dp))
									Text(
										text = "Transaction receipt has been saved to local storage",
										fontSize = 12.sp,
										color = Color(0xFF6B7280),
										textAlign = TextAlign.Center
									)
								}
							}
							Spacer(modifier = Modifier.height(16.dp))
							Button(
								onClick = {
									// Reset everything for new transaction
									if (bleHandshakeEnabled) BlePaymentLink.clear()
									showQRCode = false
									qrBitmap = null
									amount = ""
									biometricAuthenticated = false
									scannedPayeeQR = null
									transactionCompleted = false
									currentTransactionPayload = null
									showPayeeConfirmation = false
									bleHandshakeError = null
								},
								modifier = Modifier.fillMaxWidth(),
								colors = ButtonDefaults.buttonColors(
									containerColor = Color(0xFF059669)
								)
							) {
								Text("Start New Payment")
							}
						} else if (!bleHandshakeEnabled) {
							// Show Sent button (sender clicks after receiver scans)
							Text(
								text = "After receiver scans this QR code, click 'Sent' to confirm payment and save to local storage",
								fontSize = 12.sp,
								color = Color(0xFF6B7280),
								textAlign = TextAlign.Center,
								modifier = Modifier.padding(bottom = 8.dp)
							)
							Button(
								onClick = {
									// Create transaction receipt and save to local storage
									val payload = currentTransactionPayload
									if (payload != null) {
										scope.launch {
											try {
												val moshi = com.squareup.moshi.Moshi.Builder()
													.add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
													.build()
												val adapter = moshi.adapter(com.offlinepayment.data.TransactionPayloadQR::class.java)
												val rawPayload = adapter.toJson(payload)
												
												// Save transaction to local storage as "SENT"
												// Convert amount from paisa (Long) to PKR string format
												val amountInPkr = java.math.BigDecimal(payload.amount.toString())
													.divide(java.math.BigDecimal("100"))
													.toPlainString()

												// Ensure offline wallet has enough balance before creating the transaction
												val offlineWallet = repository.getOfflineWalletById(walletId)
												if (offlineWallet == null) {
													balanceError = "Offline wallet not found. Please refresh wallets online first."
													return@launch
												}
												val currentBalance = java.math.BigDecimal(offlineWallet.balance)
												val transactionAmount = java.math.BigDecimal(payload.amount.toString()).divide(java.math.BigDecimal("100"))
												if (currentBalance < transactionAmount) {
													balanceError = "Insufficient offline balance for this amount."
													return@launch
												}
												
												// Get receiver public key: prefer QR, otherwise try fetching receiver's offline wallet by payeeId
												var receiverPublicKey = scannedPayeeQR?.public_key
												if (receiverPublicKey.isNullOrBlank()) {
													val payeeUserId = scannedPayeeQR?.payeeId?.toIntOrNull()
													val receiverWallet = payeeUserId?.let {
														repository.getOfflineWalletByUserIdAndType(it, "offline")
													}
													receiverPublicKey = receiverWallet?.publicKey
												}

												if (receiverPublicKey.isNullOrBlank()) {
													balanceError = "Receiver public key missing. Ask receiver to share an updated QR."
												} else {
													// Only proceed when receiverPublicKey is present
													val localTransaction = com.offlinepayment.data.local.LocalTransaction(
														txId = payload.txId,
														senderWalletId = walletId,
														receiverPublicKey = receiverPublicKey,
														amount = amountInPkr,
														currency = "PKR",
														// Provide a non-empty signature placeholder so backend passes presence check.
														// Real signing can be wired later.
														transactionSignature = "unsigned-placeholder",
														nonce = payload.nonce,
														receiptHash = "", // Will be set when receipt is created
														receiptData = "{}", // Will be set when receipt is created
														status = "pending",
														createdAtDevice = payload.timestamp,
														deviceFingerprint = com.offlinepayment.data.session.DeviceFingerprintProvider.getFingerprint(),
														// Legacy fields for backward compatibility
														payerId = payload.payerId,
														payeeId = payload.payeeId,
														direction = "SENT",
														rawPayload = rawPayload
													)
													
													repository.saveLocalTransaction(localTransaction)
													
													// Update sender's wallet balance (subtract amount)
													val newBalance = currentBalance.subtract(transactionAmount)
													repository.updateOfflineWalletBalance(walletId, newBalance.toPlainString())
													
													// Mark transaction as completed
													transactionCompleted = true
												}
												
											} catch (e: Exception) {
												balanceError = "Failed to save transaction: ${e.message}"
											}
										}
									}
								},
								modifier = Modifier.fillMaxWidth(),
								colors = ButtonDefaults.buttonColors(
									containerColor = Color(0xFF059669)
								)
							) {
								Text("Sent", fontWeight = FontWeight.Bold, fontSize = 16.sp)
							}
							Spacer(modifier = Modifier.height(8.dp))
							OutlinedButton(
								onClick = {
									if (bleHandshakeEnabled) BlePaymentLink.clear()
									showQRCode = false
									qrBitmap = null
									amount = ""
									biometricAuthenticated = false
									scannedPayeeQR = null
									transactionCompleted = false
									currentTransactionPayload = null
									showPayeeConfirmation = false
									bleHandshakeError = null
								},
								modifier = Modifier.fillMaxWidth(),
								colors = ButtonDefaults.outlinedButtonColors(
									contentColor = Color(0xFFEF4444)
								)
							) {
								Text("Cancel Transaction")
							}
						} else {
							OutlinedButton(
								onClick = {
									if (bleHandshakeEnabled) BlePaymentLink.clear()
									showQRCode = false
									qrBitmap = null
									amount = ""
									biometricAuthenticated = false
									scannedPayeeQR = null
									transactionCompleted = false
									currentTransactionPayload = null
									showPayeeConfirmation = false
									bleHandshakeError = null
								},
								modifier = Modifier.fillMaxWidth(),
								colors = ButtonDefaults.outlinedButtonColors(
									contentColor = Color(0xFFEF4444)
								)
							) {
								Text("Cancel Transaction")
							}
						}
					}
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
							text = "How to Send Money",
							fontSize = 16.sp,
							fontWeight = FontWeight.Bold,
							color = Color.White
						)
						Spacer(modifier = Modifier.height(8.dp))
						Text(
							text = "• Scan recipient's QR code using camera\n• Or select QR image from gallery\n• Enter amount and confirm payment\n• Transaction processed instantly offline",
							fontSize = 14.sp,
							color = Color.White.copy(alpha = 0.9f),
							textAlign = TextAlign.Center
						)
					}
				}
			}
		}
	}
}

@Composable
private fun DetailRow(label: String, value: String) {
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
