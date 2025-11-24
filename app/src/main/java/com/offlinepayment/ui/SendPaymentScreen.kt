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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlinepayment.utils.CurrencyUtils

@Composable
fun SendPaymentScreen(
	onConfirm: (recipientId: String, amount: String) -> Unit
) {
	var amount by remember { mutableStateOf("") }
	var scannedRecipient by remember { mutableStateOf("") }

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
		Column(
			modifier = Modifier
				.fillMaxSize()
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

			// QR Scanner Card
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
						text = "Scan Recipient's QR Code",
						fontSize = 18.sp,
						fontWeight = FontWeight.Bold,
						color = Color(0xFF374151)
					)
					
					Spacer(modifier = Modifier.height(16.dp))

					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.spacedBy(12.dp)
					) {
						Button(
							modifier = Modifier.weight(1f),
							onClick = { 
								// TODO: Open camera scanner
								scannedRecipient = "Alice (QR123456)"
							},
							colors = ButtonDefaults.buttonColors(
								containerColor = Color(0xFF059669)
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
								Text("Camera", fontWeight = FontWeight.Bold)
							}
						}

						OutlinedButton(
							modifier = Modifier.weight(1f),
							onClick = { 
								// TODO: Open gallery picker
								scannedRecipient = "Bob (QR789012)"
							},
							colors = ButtonDefaults.outlinedButtonColors(
								contentColor = Color(0xFF059669)
							),
							shape = RoundedCornerShape(12.dp)
						) {
							Row(
								verticalAlignment = Alignment.CenterVertically
							) {
								Icon(
									imageVector = Icons.Default.PhotoLibrary,
									contentDescription = "Gallery",
									modifier = Modifier.size(20.dp)
								)
								Spacer(modifier = Modifier.width(8.dp))
								Text("Gallery", fontWeight = FontWeight.Bold)
							}
						}
					}

					if (scannedRecipient.isNotEmpty()) {
						Spacer(modifier = Modifier.height(16.dp))
						Card(
							modifier = Modifier.fillMaxWidth(),
							colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4))
						) {
							Column(
								modifier = Modifier.padding(16.dp),
								horizontalAlignment = Alignment.CenterHorizontally
							) {
								Text(
									text = "Recipient Found:",
									fontSize = 14.sp,
									color = Color.Gray
								)
								Text(
									text = scannedRecipient,
									fontSize = 16.sp,
									fontWeight = FontWeight.Bold,
									color = Color(0xFF059669)
								)
							}
						}
					}
				}
			}

			Spacer(modifier = Modifier.height(24.dp))

			// Amount Input Card
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
						onValueChange = { amount = it },
						label = { Text("Amount (Rs)") },
						singleLine = true,
						keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
						colors = OutlinedTextFieldDefaults.colors(
							focusedBorderColor = Color(0xFF059669),
							focusedLabelColor = Color(0xFF059669)
						)
					)

					if (amount.toDoubleOrNull() != null) {
						Spacer(modifier = Modifier.height(12.dp))
						Text(
							text = "You will send: ${CurrencyUtils.formatPkr(amount.toDouble())}",
							fontSize = 16.sp,
							fontWeight = FontWeight.Medium,
							color = Color(0xFF059669)
						)
					}
				}
			}

			Spacer(modifier = Modifier.height(24.dp))

			// Confirm Button
			Button(
				modifier = Modifier.fillMaxWidth(),
				onClick = { 
					if (scannedRecipient.isNotEmpty() && amount.isNotEmpty()) {
						onConfirm(scannedRecipient, amount)
					}
				},
				enabled = scannedRecipient.isNotEmpty() && amount.isNotEmpty(),
				colors = ButtonDefaults.buttonColors(
					containerColor = Color(0xFF059669)
				),
				shape = RoundedCornerShape(12.dp)
			) {
				Text("Confirm Payment", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
