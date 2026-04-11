package com.offlinepayment.ui.ble

import android.bluetooth.le.ScanResult
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offlinepayment.ble.BleGattClientManager
import com.offlinepayment.ble.BleOfflinkAssessment
import com.offlinepayment.ble.BleOfflinkEligibility
import com.offlinepayment.ble.BlePaymentLink
import com.offlinepayment.ble.BlePaymentWire
import com.offlinepayment.ble.BlePermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleSenderScanScreen(
    onBack: () -> Unit,
    onLinked: () -> Unit,
) {
    val context = LocalContext.current
    val results = remember { mutableStateListOf<ScanResult>() }
    val scanning = remember { mutableStateOf(false) }
    val linking = remember { mutableStateOf(false) }
    val error = remember { mutableStateOf<String?>(null) }

    val client = remember {
        BleGattClientManager(context).also { BlePaymentLink.client = it }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        if (granted.values.all { it }) {
            when (val a = BleOfflinkEligibility.assessSender(context)) {
                is BleOfflinkAssessment.Blocked -> {
                    error.value = a.userMessage
                    scanning.value = false
                }
                is BleOfflinkAssessment.Ok -> {
                    error.value = null
                    scanning.value = true
                    client.startScan()
                }
            }
        } else {
            error.value = "Bluetooth permissions are required for this step."
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (!BlePaymentLink.senderBleSessionActive) {
                BlePaymentLink.client?.disconnect()
                BlePaymentLink.client?.stopScan()
                BlePaymentLink.client = null
            }
        }
    }

    LaunchedEffect(Unit) {
        client.onConnected = {
            BlePaymentLink.senderBleSessionActive = true
            linking.value = false
            onLinked()
        }
        client.onScanResult = { sr ->
            if (results.none { it.device.address == sr.device.address }) {
                results.add(sr)
            }
        }
        client.onReceiverAckPayload = { bytes ->
            BlePaymentWire.decodeReceiverAck(bytes)?.let { BlePaymentLink.publishReceiverAck(it) }
        }
        client.onDisconnected = {
            linking.value = false
            BlePaymentLink.onSenderGattDisconnected()
        }
        when (val a = BleOfflinkEligibility.assessSender(context)) {
            is BleOfflinkAssessment.Blocked -> {
                error.value = a.userMessage
                scanning.value = false
            }
            is BleOfflinkAssessment.Ok -> {
                error.value = null
                if (BlePermissionHelper.hasAll(context)) {
                    scanning.value = true
                    client.startScan()
                } else {
                    permLauncher.launch(BlePermissionHelper.requiredPermissions())
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0FDF4)),
    ) {
        TopAppBar(
            title = { Text("Connect to receiver (BLE)") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF059669),
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White,
            ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Text(
                text = "Scan for the receiver phone that tapped \"Receive payment (BLE)\".",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            error.value?.let { e ->
                Text(text = e, color = Color(0xFFB91C1C))
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (scanning.value && results.isEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.height(24.dp), strokeWidth = 2.dp)
                    Text("Scanning…")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(results, key = { it.device.address }) { sr ->
                    val label = sr.device.name ?: sr.device.address
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !linking.value) {
                                linking.value = true
                                error.value = null
                                client.stopScan()
                                scanning.value = false
                                client.connect(sr.device)
                            },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(16.dp),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Cancel")
            }
        }
    }
}
