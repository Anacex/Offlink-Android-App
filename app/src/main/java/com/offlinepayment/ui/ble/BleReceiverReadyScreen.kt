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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.offlinepayment.ui.qr.PayeeIdentityQrPanel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleReceiverReadyScreen(
    userId: Int,
    userIdSuffix: String,
    userName: String,
    balance: Double,
    canReceivePayments: Boolean,
    onBack: () -> Unit,
    onReadyContinue: () -> Unit,
) {
    val context = LocalContext.current
    val started = remember { mutableStateOf(false) }
    val err = remember { mutableStateOf<String?>(null) }
    val senderConnected = remember { mutableStateOf(false) }

    // Reuse the active manager when returning from the QR scanner so we do not stack GATT servers/advertisers.
    val server = remember {
        (BlePaymentLink.server ?: BleGattServerManager(context).also { BlePaymentLink.server = it }).also {
            BlePaymentLink.isReceiverHosting = true
        }
    }

    fun wireServerCallbacks() {
        server.onCentralConnected = {
            senderConnected.value = true
        }
        server.onCentralDisconnected = {
            senderConnected.value = false
            BlePaymentLink.onReceiverCentralDisconnected()
        }
        server.onSenderOkPayload = { bytes ->
            BlePaymentWire.decodeSenderOk(bytes)?.let { msg ->
                BlePaymentLink.publishSenderOk(msg)
            }
        }
    }

    fun startBleServer() {
        if (started.value && server.isAdvertisingSessionActive()) {
            return
        }
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
        wireServerCallbacks()
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

    /** Only after the user taps Start — avoids advertising just from opening this screen or requesting permissions. */
    fun requestStartAdvertising() {
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
        if (!BlePermissionHelper.hasAll(context)) {
            permLauncher.launch(BlePermissionHelper.requiredPermissions())
            return
        }
        startBleServer()
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
        wireServerCallbacks()
        if (server.isAdvertisingSessionActive()) {
            started.value = true
            senderConnected.value = server.connectedDevice != null
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
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                text = "1. Tap Start Bluetooth advertising when you are ready — your phone will show as Offlink-* so the sender can connect from Send → Bluetooth.\n" +
                    "2. When connected, your payee ID QR appears below — have the sender scan it in Send payment.\n" +
                    "3. After they show the payment QR, tap Continue and scan it.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            err.value?.let {
                Text(text = it, color = Color(0xFFB91C1C))
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (!started.value) {
                Button(
                    onClick = { requestStartAdvertising() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Start Bluetooth advertising")
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (started.value) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = if (senderConnected.value) {
                            "Bluetooth: sender connected — show the payee QR below to their camera."
                        } else {
                            "Bluetooth: advertising — waiting for sender to connect…"
                        },
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            if (started.value && senderConnected.value && canReceivePayments) {
                Spacer(modifier = Modifier.height(16.dp))
                PayeeIdentityQrPanel(
                    userId = userId,
                    userName = userName,
                    balance = balance,
                    qrSizeDp = 200,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onReadyContinue,
                modifier = Modifier.fillMaxWidth(),
                enabled = started.value && senderConnected.value && canReceivePayments,
            ) {
                Text("Continue to scan payment QR")
            }
            if (!canReceivePayments) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your offline wallet is at the maximum balance; you cannot receive until you spend or transfer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB91C1C),
                )
            }
        }
    }
}
