package com.offlinepayment.ui.ble

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.offlinepayment.ble.BleGattServerManager
import com.offlinepayment.ble.BleOfflinkAssessment
import com.offlinepayment.ble.BleOfflinkEligibility
import com.offlinepayment.ble.BlePaymentLink
import com.offlinepayment.ble.BlePaymentWire
import com.offlinepayment.ble.BlePermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleReceiverReadyScreen(
    userIdSuffix: String,
    onBack: () -> Unit,
    onReadyContinue: () -> Unit,
) {
    val context = LocalContext.current
    val started = remember { mutableStateOf(false) }
    val err = remember { mutableStateOf<String?>(null) }

    val server = remember {
        BleGattServerManager(context).also {
            BlePaymentLink.server = it
            BlePaymentLink.isReceiverHosting = true
        }
    }

    fun startBleServer() {
        when (val a = BleOfflinkEligibility.assessReceiver(context)) {
            is BleOfflinkAssessment.Blocked -> {
                err.value = a.userMessage
                return
            }
            is BleOfflinkAssessment.Ok -> { }
        }
        if (!server.isBluetoothAvailable()) {
            err.value = "Bluetooth is off. Turn it on and try again."
            return
        }
        server.onCentralDisconnected = {
            BlePaymentLink.onReceiverCentralDisconnected()
        }
        server.onSenderOkPayload = { bytes ->
            BlePaymentWire.decodeSenderOk(bytes)?.let { msg ->
                BlePaymentLink.publishSenderOk(msg)
            }
        }
        runCatching {
            server.startAdvertisingAndServer(userIdSuffix)
            started.value = true
            err.value = null
        }.onFailure { e ->
            err.value = e.message ?: "Could not start BLE receiver mode"
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        if (granted.values.all { it }) {
            startBleServer()
        } else {
            err.value = "Bluetooth permissions are required."
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (!BlePaymentLink.isReceiverHosting) {
                BlePaymentLink.server?.stop()
                BlePaymentLink.server = null
            }
        }
    }

    LaunchedEffect(Unit) {
        if (BlePermissionHelper.hasAll(context)) {
            startBleServer()
        } else {
            permLauncher.launch(BlePermissionHelper.requiredPermissions())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFF6FF)),
    ) {
        TopAppBar(
            title = { Text("Receive payment (BLE)") },
            navigationIcon = {
                IconButton(onClick = {
                    server.stop()
                    BlePaymentLink.server = null
                    BlePaymentLink.isReceiverHosting = false
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF1D4ED8),
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White,
            ),
        )
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Your phone is advertising as a payment receiver. Ask the sender to connect from Send → BLE, then show your payee QR and scan their payment QR as usual.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            err.value?.let {
                Text(text = it, color = Color(0xFFB91C1C))
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (started.value) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "BLE session active — visible as Offlink-* in sender scan.",
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onReadyContinue,
                modifier = Modifier.fillMaxWidth(),
                enabled = started.value,
            ) {
                Text("Continue to scan payment QR")
            }
        }
    }
}
