package com.offlinepayment.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class OfflineTransaction(
	val id: String,
	val date: String,
	val recipient: String,
	val amount: String
)

@Composable
fun TransactionListScreen(
	transactions: List<OfflineTransaction>,
	onTransactionClick: (OfflineTransaction) -> Unit = {}
) {
	LazyColumn(
		modifier = Modifier.fillMaxSize(),
		contentPadding = PaddingValues(16.dp),
		verticalArrangement = Arrangement.spacedBy(12.dp)
	) {
		items(transactions, key = { it.id }) { tx ->
			Card(
				modifier = Modifier
					.fillMaxWidth()
					.clickable { onTransactionClick(tx) },
				colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
				elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
			) {
				Column(modifier = Modifier.padding(16.dp)) {
					Row(modifier = Modifier.fillMaxWidth()) {
						Text(
							text = tx.date,
							style = MaterialTheme.typography.labelMedium,
							color = MaterialTheme.colorScheme.onSurfaceVariant
						)
					}
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.padding(top = 6.dp),
						horizontalArrangement = Arrangement.SpaceBetween
					) {
						Text(
							text = tx.recipient,
							style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
							color = MaterialTheme.colorScheme.onSurface
						)
						Text(
							text = if (tx.amount.startsWith("Rs")) tx.amount else "Rs ${tx.amount}",
							style = MaterialTheme.typography.titleMedium,
							color = MaterialTheme.colorScheme.onSurface
						)
					}
				}
			}
		}
	}
}
