package com.offlinepayment.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.offlinepayment.utils.CurrencyUtils

@Composable
fun TopUpScreen(
	onConfirmTopUp: (amount: Double) -> Unit
) {
	var amount by remember { mutableStateOf("") }

	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(16.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Top
	) {
		OutlinedTextField(
			modifier = Modifier.fillMaxWidth(),
			value = amount,
			onValueChange = { amount = it },
			label = { Text("Amount (Rs)") },
			singleLine = true,
			keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
		)

		Spacer(modifier = Modifier.height(20.dp))

		Button(
			modifier = Modifier.fillMaxWidth(),
			onClick = {
				val value = amount.toDoubleOrNull() ?: 0.0
				onConfirmTopUp(value)
			}
		) {
			Text("Confirm Top Up")
		}

		if (amount.toDoubleOrNull() != null) {
			Spacer(modifier = Modifier.height(12.dp))
			Text(text = CurrencyUtils.formatPkr(amount.toDouble()))
		}
	}
}
