package com.offlinepayment.ui.ble

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.QrCode2
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.offlinepayment.ble.BleGattServerManager
import com.offlinepayment.ble.BleOfflinkAssessment
import com.offlinepayment.ble.BleOfflinkEligibility
import com.offlinepayment.ble.BlePaymentLink
import com.offlinepayment.ble.BlePaymentWire
import com.offlinepayment.ble.BlePermissionHelper
import com.offlinepayment.data.network.ApiClient
import com.offlinepayment.data.repository.WalletRepository
import com.offlinepayment.utils.CurrencyUtils
import com.offlinepayment.utils.QRCodeHelper
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleReceiverReadyScreen(
    userId: Int,
    userName: String,
    offlineBalancePkr: Double,
    userIdSuffix: String,
    onBack: () -> Unit,
    onReadyContinue: () -> Unit,
) {
    val context = LocalContext.current
    val started = remember { mutableStateOf(false) }
    val err = remember { mutableStateOf<String?>(null) }
    var isCentralConnected by remember { mutableStateOf(false) }

    val repository = remember { WalletRepository(ApiClient.walletApi, context) }
    var payeeQrBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

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
        server.onCentralConnected = {
            isCentralConnected = true
        }
        server.onCentralDisconnected = {
            isCentralConnected = false
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
            isCentralConnected = server.connectedDevice != null
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

    LaunchedEffect(userId, userName, offlineBalancePkr) {
        val offlineWallet = runCatching {
            repository.getOfflineWalletByUserIdAndType(userId, "offline")
        }.getOrNull()

        val payload = QRCodeHelper.createPayeeQR(
            payeeId = userId.toString(),
            payeeName = userName,
            deviceId = runCatching { com.offlinepayment.data.session.DeviceFingerprintProvider.getFingerprint() }
                .getOrElse { "device-$userId-${System.currentTimeMillis()}" },
            currentBalance = BigDecimal(offlineBalancePkr.toString()),
            publicKey = offlineWallet?.publicKey,
            walletId = offlineWallet?.walletId,
        )

        val moshi = com.squareup.moshi.Moshi.Builder()
            .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()
        val adapter = moshi.adapter(com.offlinepayment.data.PayeeQRPayload::class.java)
        val json = adapter.toJson(payload)
        payeeQrBitmap = runCatching { QRCodeHelper.generateQRCodeBitmap(json, 700, 700) }.getOrNull()
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
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Receiver flow: advertise over Bluetooth → sender connects → sender scans your ID QR → you scan sender’s Transaction QR → BLE acknowledgements finalize and both devices save the ledger entry.",
                style = MaterialTheme.typography.bodyMedium,
            )

            err.value?.let { e ->
                Text(text = e, color = Color(0xFFB91C1C))
            }

            if (started.value) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Icon(
                            imageVector = Icons.Default.BluetoothConnected,
                            contentDescription = null,
                            tint = if (isCentralConnected) Color(0xFF059669) else Color(0xFF1D4ED8),
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isCentralConnected) "Bluetooth connected" else "Advertising (waiting for sender)…",
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = if (isCentralConnected) {
                                    "Ask the sender to scan your ID QR below."
                                } else {
                                    "Sender should open Send Payment → scan BLE devices → select Offlink-*."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B7280),
                            )
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = null,
                            tint = Color(0xFF1D4ED8),
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Text(
                            text = "Receiver ID QR",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    payeeQrBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Receiver ID QR",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } ?: run {
                        Text(
                            text = "Generating QR…",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280),
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$userName (ID $userId) • Balance ${CurrencyUtils.formatPkr(offlineBalancePkr)}",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF374151),
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onReadyContinue,
                modifier = Modifier.fillMaxWidth(),
                enabled = started.value && isCentralConnected,
            ) {
                Text("Continue to scan sender transaction QR")
            }
            if (started.value && !isCentralConnected) {
                Text(
                    text = "Waiting for sender BLE connection before proceeding.",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280),
                )
            }
        }
    }
}
