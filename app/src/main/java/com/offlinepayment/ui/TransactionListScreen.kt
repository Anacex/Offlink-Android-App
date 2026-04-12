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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.offlinepayment.data.WalletTransferResponse
import com.offlinepayment.data.SyncedOfflineTransaction
import com.offlinepayment.data.UnifiedOfflineHistoryItem
import com.offlinepayment.utils.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.time.Instant
import java.math.BigDecimal

data class OfflineTransaction(
	val id: String,
	val date: String,
	val recipient: String,
	val amount: String
)

@Composable
fun TransactionListScreen(
	transfers: List<WalletTransferResponse>,
	localTransactions: List<com.offlinepayment.data.local.LocalTransaction> = emptyList(),
	syncedTransactions: List<SyncedOfflineTransaction> = emptyList(),
	unifiedServerHistory: List<UnifiedOfflineHistoryItem> = emptyList(),
	isLoading: Boolean = false,
	/** When true: online shows last server unified offline payments only; offline shows last 10 local ledger rows. */
	homeHistoryMode: Boolean = false,
	isOnline: Boolean = true,
	onTransactionClick: (WalletTransferResponse) -> Unit = {},
) {
	if (homeHistoryMode) {
		val localLast10 = remember(localTransactions) {
			localTransactions.sortedByDescending { it.createdAtDevice }.take(10)
		}
		when {
			isOnline && isLoading -> {
				Column(
					modifier = Modifier.fillMaxSize(),
					horizontalAlignment = Alignment.CenterHorizontally,
					verticalArrangement = Arrangement.Center
				) {
					CircularProgressIndicator()
				}
			}
			isOnline && unifiedServerHistory.isEmpty() -> {
				Column(
					modifier = Modifier.fillMaxSize(),
					horizontalAlignment = Alignment.CenterHorizontally,
					verticalArrangement = Arrangement.Center
				) {
					Text(
						text = "No recent payments on the server yet",
						style = MaterialTheme.typography.bodyLarge,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
			}
			isOnline -> {
				LazyColumn(
					modifier = Modifier.fillMaxSize(),
					contentPadding = PaddingValues(16.dp),
					verticalArrangement = Arrangement.spacedBy(12.dp)
				) {
					items(unifiedServerHistory.take(10), key = { it.nonce }) { row ->
						HomeUnifiedHistoryCard(row)
					}
				}
			}
			localLast10.isEmpty() -> {
				Column(
					modifier = Modifier.fillMaxSize(),
					horizontalAlignment = Alignment.CenterHorizontally,
					verticalArrangement = Arrangement.Center
				) {
					Text(
						text = "No offline transactions stored on this device",
						style = MaterialTheme.typography.bodyLarge,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
			}
			else -> {
				LazyColumn(
					modifier = Modifier.fillMaxSize(),
					contentPadding = PaddingValues(16.dp),
					verticalArrangement = Arrangement.spacedBy(12.dp)
				) {
					items(localLast10, key = { it.txId }) { localTx ->
						HomeLocalHistoryCard(localTx)
					}
				}
			}
		}
	} else if (isLoading) {
		Column(
			modifier = Modifier.fillMaxSize(),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center
		) {
			CircularProgressIndicator()
		}
	} else if (
		transfers.isEmpty() &&
			localTransactions.isEmpty() &&
			syncedTransactions.isEmpty() &&
			unifiedServerHistory.isEmpty()
	) {
		Column(
			modifier = Modifier.fillMaxSize(),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center
		) {
			Text(
				text = "No transaction history",
				style = MaterialTheme.typography.bodyLarge,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
	} else {
		LazyColumn(
			modifier = Modifier.fillMaxSize(),
			contentPadding = PaddingValues(16.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp)
		) {
			// Show local unsynced transactions first (pending offline transactions)
			items(localTransactions.filter { it.status == "pending" }, key = { it.txId }) { localTx ->
				Card(
					modifier = Modifier.fillMaxWidth(),
					colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
					elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
				) {
					Column(modifier = Modifier.padding(16.dp)) {
						Row(modifier = Modifier.fillMaxWidth()) {
							Text(
								text = formatTimestampFromMillis(localTx.createdAtDevice),
								style = MaterialTheme.typography.labelMedium,
								color = MaterialTheme.colorScheme.onSurfaceVariant
							)
							Text(
								text = " • ${localTx.direction ?: localTx.status}",
								style = MaterialTheme.typography.labelMedium,
								color = when {
									localTx.direction == "SENT" -> Color(0xFFDC2626)
									localTx.direction == "RECEIVED" -> Color(0xFF059669)
									localTx.status == "failed" -> Color(0xFFDC2626)
									else -> Color(0xFF6B7280)
								},
								modifier = Modifier.padding(start = 8.dp)
							)
						}
						Row(
							modifier = Modifier
								.fillMaxWidth()
								.padding(top = 6.dp),
							horizontalArrangement = Arrangement.SpaceBetween
						) {
							Column {
								Text(
									text = if (localTx.direction == "SENT") {
										"To: ${localTx.payeeId ?: "Unknown"}"
									} else {
										"From: ${localTx.payerId ?: "Unknown"}"
									},
									style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
									color = MaterialTheme.colorScheme.onSurface
								)
								Text(
									text = "TxID: ${localTx.txId.take(8)}... • ${localTx.status} (Pending Sync)",
									style = MaterialTheme.typography.bodySmall,
									color = MaterialTheme.colorScheme.onSurfaceVariant,
									modifier = Modifier.padding(top = 4.dp)
								)
							}
							Text(
								text = CurrencyUtils.formatPkr(java.math.BigDecimal(localTx.amount).toDouble()),
								style = MaterialTheme.typography.titleMedium,
								color = if (localTx.direction == "SENT") Color(0xFFDC2626) else Color(0xFF059669)
							)
						}
					}
				}
			}

			items(unifiedServerHistory, key = { it.nonce }) { row ->
				Card(
					modifier = Modifier.fillMaxWidth(),
					colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
					elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
				) {
					Column(modifier = Modifier.padding(16.dp)) {
						Row(modifier = Modifier.fillMaxWidth()) {
							Text(
								text = row.senderSyncedAtServer?.let { formatTimestampFromISO(it) }
									?: row.receiverSyncedAtServer?.let { formatTimestampFromISO(it) }
									?: "—",
								style = MaterialTheme.typography.labelMedium,
								color = MaterialTheme.colorScheme.onSurfaceVariant,
							)
							Text(
								text = when (row.perspective) {
									"sent" -> " • You sent"
									"received" -> " • You received"
									else -> " • ${row.perspective}"
								},
								style = MaterialTheme.typography.labelMedium,
								color = if (row.perspective == "sent") Color(0xFFDC2626) else Color(0xFF059669),
								modifier = Modifier.padding(start = 8.dp),
							)
						}
						Row(
							modifier = Modifier
								.fillMaxWidth()
								.padding(top = 6.dp),
							horizontalArrangement = Arrangement.SpaceBetween,
						) {
							Column(modifier = Modifier.weight(1f)) {
								Text(
									text = "Server offline payment",
									style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
									color = MaterialTheme.colorScheme.onSurface,
								)
								Text(
									text = buildString {
										append("Coverage: ${row.syncCoverage.replace("_", " ")}")
										row.firstSyncParty?.let { append(" • First sync: $it") }
									},
									style = MaterialTheme.typography.bodySmall,
									color = MaterialTheme.colorScheme.onSurfaceVariant,
									modifier = Modifier.padding(top = 4.dp),
								)
								Text(
									text = "Nonce ${row.nonce.take(12)}…${row.txId?.let { " • tx $it" } ?: ""}",
									style = MaterialTheme.typography.bodySmall,
									color = MaterialTheme.colorScheme.onSurfaceVariant,
									modifier = Modifier.padding(top = 2.dp),
								)
							}
							Text(
								text = CurrencyUtils.formatPkr(
									runCatching { BigDecimal(row.amount).toDouble() }.getOrDefault(0.0),
								),
								style = MaterialTheme.typography.titleMedium,
								color = if (row.perspective == "sent") Color(0xFFDC2626) else Color(0xFF059669),
							)
						}
					}
				}
			}
			
			// Legacy: sender-only list from GET /offline-transactions/ (kept for older caches)
			items(syncedTransactions, key = { it.id.toString() }) { syncedTx ->
				Card(
					modifier = Modifier.fillMaxWidth(),
					colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
					elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
				) {
					Column(modifier = Modifier.padding(16.dp)) {
						Row(modifier = Modifier.fillMaxWidth()) {
							Text(
								text = formatTimestampFromISO(syncedTx.createdAtDevice),
								style = MaterialTheme.typography.labelMedium,
								color = MaterialTheme.colorScheme.onSurfaceVariant
							)
							Text(
								text = " • ${syncedTx.status.uppercase()}",
								style = MaterialTheme.typography.labelMedium,
								color = when {
									syncedTx.status == "synced" -> Color(0xFF059669)
									syncedTx.status == "confirmed" -> Color(0xFF059669)
									syncedTx.status == "failed" -> Color(0xFFDC2626)
									else -> Color(0xFF6B7280)
								},
								modifier = Modifier.padding(start = 8.dp)
							)
						}
						Row(
							modifier = Modifier
								.fillMaxWidth()
								.padding(top = 6.dp),
							horizontalArrangement = Arrangement.SpaceBetween
						) {
							Column {
								Text(
									text = "Offline Transaction",
									style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
									color = MaterialTheme.colorScheme.onSurface
								)
								Text(
									text = "Nonce: ${syncedTx.nonce.take(8)}... • Synced",
									style = MaterialTheme.typography.bodySmall,
									color = MaterialTheme.colorScheme.onSurfaceVariant,
									modifier = Modifier.padding(top = 4.dp)
								)
							}
							Text(
								text = CurrencyUtils.formatPkr(java.math.BigDecimal(syncedTx.amount).toDouble()),
								style = MaterialTheme.typography.titleMedium,
								color = Color(0xFFDC2626) // SENT transactions (synced offline transactions are always sent by current user)
							)
						}
					}
				}
			}
			
			// Show server transfers (online transactions)
			items(transfers, key = { it.id }) { transfer ->
				Card(
					modifier = Modifier
						.fillMaxWidth()
						.clickable { onTransactionClick(transfer) },
					colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
					elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
				) {
					Column(modifier = Modifier.padding(16.dp)) {
						Row(modifier = Modifier.fillMaxWidth()) {
							Text(
								text = formatTimestamp(transfer.timestamp),
								style = MaterialTheme.typography.labelMedium,
								color = MaterialTheme.colorScheme.onSurfaceVariant
							)
							Text(
								text = " • ${transfer.status.uppercase()}",
								style = MaterialTheme.typography.labelMedium,
								color = MaterialTheme.colorScheme.onSurfaceVariant,
								modifier = Modifier.padding(start = 8.dp)
							)
						}
						Row(
							modifier = Modifier
								.fillMaxWidth()
								.padding(top = 6.dp),
							horizontalArrangement = Arrangement.SpaceBetween
						) {
							Column {
								Text(
									text = "Wallet ${transfer.from_wallet_id} → Wallet ${transfer.to_wallet_id}",
									style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
									color = MaterialTheme.colorScheme.onSurface
								)
								Text(
									text = "Ref: ${transfer.reference}",
									style = MaterialTheme.typography.bodySmall,
									color = MaterialTheme.colorScheme.onSurfaceVariant,
									modifier = Modifier.padding(top = 4.dp)
								)
							}
							Text(
								text = CurrencyUtils.formatPkr(transfer.amount.toDouble()),
								style = MaterialTheme.typography.titleMedium,
								color = MaterialTheme.colorScheme.onSurface
							)
						}
					}
				}
			}
		}
	}
}

@Composable
private fun HomeUnifiedHistoryCard(row: UnifiedOfflineHistoryItem) {
	Card(
		modifier = Modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
		elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
	) {
		Column(modifier = Modifier.padding(16.dp)) {
			Row(modifier = Modifier.fillMaxWidth()) {
				Text(
					text = row.senderSyncedAtServer?.let { formatTimestampFromISO(it) }
						?: row.receiverSyncedAtServer?.let { formatTimestampFromISO(it) }
						?: "—",
					style = MaterialTheme.typography.labelMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
				Text(
					text = when (row.perspective) {
						"sent" -> " • You sent"
						"received" -> " • You received"
						else -> " • ${row.perspective}"
					},
					style = MaterialTheme.typography.labelMedium,
					color = if (row.perspective == "sent") Color(0xFFDC2626) else Color(0xFF059669),
					modifier = Modifier.padding(start = 8.dp),
				)
			}
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(top = 6.dp),
				horizontalArrangement = Arrangement.SpaceBetween,
			) {
				Column(modifier = Modifier.weight(1f)) {
					Text(
						text = "Server offline payment",
						style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
						color = MaterialTheme.colorScheme.onSurface,
					)
					Text(
						text = buildString {
							append("Coverage: ${row.syncCoverage.replace("_", " ")}")
							row.firstSyncParty?.let { append(" • First sync: $it") }
						},
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						modifier = Modifier.padding(top = 4.dp),
					)
					Text(
						text = "Nonce ${row.nonce.take(12)}…${row.txId?.let { " • tx $it" } ?: ""}",
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						modifier = Modifier.padding(top = 2.dp),
					)
				}
				Text(
					text = CurrencyUtils.formatPkr(
						runCatching { BigDecimal(row.amount).toDouble() }.getOrDefault(0.0),
					),
					style = MaterialTheme.typography.titleMedium,
					color = if (row.perspective == "sent") Color(0xFFDC2626) else Color(0xFF059669),
				)
			}
		}
	}
}

@Composable
private fun HomeLocalHistoryCard(localTx: com.offlinepayment.data.local.LocalTransaction) {
	Card(
		modifier = Modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
		elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
	) {
		Column(modifier = Modifier.padding(16.dp)) {
			Row(modifier = Modifier.fillMaxWidth()) {
				Text(
					text = formatTimestampFromMillis(localTx.createdAtDevice),
					style = MaterialTheme.typography.labelMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
				Text(
					text = " • ${localTx.direction ?: localTx.status}",
					style = MaterialTheme.typography.labelMedium,
					color = when {
						localTx.direction == "SENT" -> Color(0xFFDC2626)
						localTx.direction == "RECEIVED" -> Color(0xFF059669)
						localTx.status == "failed" -> Color(0xFFDC2626)
						else -> Color(0xFF6B7280)
					},
					modifier = Modifier.padding(start = 8.dp)
				)
			}
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(top = 6.dp),
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				Column {
					Text(
						text = if (localTx.direction == "SENT") {
							"To: ${localTx.payeeId ?: "Unknown"}"
						} else {
							"From: ${localTx.payerId ?: "Unknown"}"
						},
						style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
						color = MaterialTheme.colorScheme.onSurface
					)
					val statusNote = when (localTx.status) {
						"pending" -> "Pending sync"
						"synced", "confirmed" -> "Synced"
						else -> localTx.status
					}
					Text(
						text = "TxID: ${localTx.txId.take(8)}... • $statusNote",
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						modifier = Modifier.padding(top = 4.dp)
					)
				}
				Text(
					text = CurrencyUtils.formatPkr(java.math.BigDecimal(localTx.amount).toDouble()),
					style = MaterialTheme.typography.titleMedium,
					color = if (localTx.direction == "SENT") Color(0xFFDC2626) else Color(0xFF059669)
				)
			}
		}
	}
}

private fun formatTimestamp(timestamp: String): String {
	return try {
		// Try parsing ISO 8601 format
		val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
		val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
		val date = inputFormat.parse(timestamp) ?: Date()
		outputFormat.format(date)
	} catch (e: Exception) {
		// If parsing fails, return as-is
		timestamp
	}
}

private fun formatTimestampFromMillis(timestampMillis: Long): String {
	return try {
		val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
		outputFormat.format(Date(timestampMillis))
	} catch (e: Exception) {
		// If formatting fails, return timestamp as string
		timestampMillis.toString()
	}
}

private fun formatTimestampFromISO(isoString: String): String {
	return try {
		// Parse ISO 8601 format (e.g., "2025-12-09T14:49:47.469000+00:00" or "2025-12-09T14:49:47.469Z")
		val instant = Instant.parse(isoString.replace(" ", "T").let { 
			if (!it.contains("T")) "${it}T00:00:00Z"
			else if (!it.contains("Z") && !it.contains("+") && (it.length <= 10 || !it.substring(10).contains("-"))) "${it}Z"
			else it
		})
		val date = Date.from(instant)
		val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
		outputFormat.format(date)
	} catch (e: Exception) {
		// If parsing fails, try simple format
		try {
			val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
			val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
			val date = inputFormat.parse(isoString) ?: Date()
			outputFormat.format(date)
		} catch (e2: Exception) {
			isoString
		}
	}
}
