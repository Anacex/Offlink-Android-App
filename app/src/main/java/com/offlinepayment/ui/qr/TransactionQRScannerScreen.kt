package com.offlinepayment.ui.qr

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.offlinepayment.ble.BleHandshake
import com.offlinepayment.ble.BlePaymentLink
import com.offlinepayment.data.TransactionPayloadQR
import com.offlinepayment.data.repository.WalletRepository
import com.offlinepayment.data.network.ApiClient
import com.offlinepayment.utils.QRCodeHelper
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import android.widget.Toast
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

/** ML Kit success runs here so the main thread is not flooded on every camera frame. */
private val transactionQrBarcodeExecutor: Executor by lazy {
    Executors.newSingleThreadExecutor { r ->
        Thread(r, "tx-qr-barcode-ml").apply { isDaemon = true }
    }
}

/** Only one BLE receive handshake at a time (ML Kit fires many times per second on a stable QR). */
private object TransactionQrBleReceiveGuard {
    private val inFlight = AtomicBoolean(false)
    fun tryAcquire(): Boolean = inFlight.compareAndSet(false, true)
    fun release() {
        inFlight.set(false)
    }
}

/**
 * After a BLE error/disconnect, ML Kit still sees the same QR every frame and would otherwise
 * restart the handshake immediately in a tight loop (Toast spam + Logcat flood).
 */
private object TransactionQrBleReceiveCooldown {
    private val lastFailureAtMs = AtomicLong(0L)
    private const val COOLDOWN_MS = 5_000L

    fun isInCooldown(): Boolean {
        val t = lastFailureAtMs.get()
        if (t == 0L) return false
        return System.currentTimeMillis() - t < COOLDOWN_MS
    }

    fun recordFailure() {
        lastFailureAtMs.set(System.currentTimeMillis())
    }

    fun reset() {
        lastFailureAtMs.set(0L)
    }
}

private val lastBleScannerToastAtMs = AtomicLong(0L)
private const val TOAST_DEBOUNCE_MS = 3_500L

private fun showBleScannerToastOnce(context: android.content.Context, message: String) {
    val now = System.currentTimeMillis()
    while (true) {
        val prev = lastBleScannerToastAtMs.get()
        if (now - prev < TOAST_DEBOUNCE_MS) return
        if (lastBleScannerToastAtMs.compareAndSet(prev, now)) break
    }
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}

/**
 * Scanner screen for receiving transaction payload QR codes.
 * This is used by the receiver to scan the sender's transaction QR code.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionQRScannerScreen(
    currentPayeeId: String, // Current user's ID to validate payeeId in QR
    bleReceiverMode: Boolean = false,
    onScanResult: (TransactionPayloadQR) -> Unit,
    onBack: () -> Unit,
    onOfflineLedgerUpdated: () -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val repository = remember {
        WalletRepository(ApiClient.walletApi, context)
    }
    val scope = rememberCoroutineScope()
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    var scannedTransaction by remember { mutableStateOf<TransactionPayloadQR?>(null) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }
    
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top bar
        TopAppBar(
            title = { Text("Scan Transaction QR Code", color = Color.White) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Black
            )
        )
        
        if (!hasPermission) {
            // Permission denied
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Camera permission required",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                        Text("Grant Permission")
                    }
                }
            }
        } else {
            // Camera preview
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(
                                    Executors.newSingleThreadExecutor()
                                ) { imageProxy ->
                                    processImageProxyForTransaction(
                                        imageProxy = imageProxy,
                                        context = ctx,
                                        currentPayeeId = currentPayeeId,
                                        repository = repository,
                                        scope = scope,
                                        bleReceiverMode = bleReceiverMode,
                                        onOfflineLedgerUpdated = onOfflineLedgerUpdated,
                                    ) { transaction ->
                                        scannedTransaction = transaction
                                        onScanResult(transaction)
                                    }
                                }
                            }
                        
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    
                    previewView
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            
            // Instructions
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFF059669)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Point camera at sender's transaction QR code",
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

private fun processImageProxyForTransaction(
    imageProxy: ImageProxy,
    context: android.content.Context,
    currentPayeeId: String,
    repository: WalletRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    bleReceiverMode: Boolean,
    onOfflineLedgerUpdated: () -> Unit,
    onResult: (TransactionPayloadQR) -> Unit,
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )
        
        val scanner = BarcodeScanning.getClient()
        scanner.process(image)
            .addOnSuccessListener(transactionQrBarcodeExecutor) { barcodes ->
                for (barcode in barcodes) {
                    when (barcode.valueType) {
                        Barcode.TYPE_TEXT, Barcode.TYPE_URL -> {
                            val qrData = barcode.rawValue ?: ""
                            val transactionPayload = QRCodeHelper.parseTransactionPayloadQR(qrData)
                            if (transactionPayload != null) {
                                // Validate transaction payload (amount > 0, timestamp ±2 min, payerId present, payeeId matches)
                                val validation = QRCodeHelper.validateTransactionPayload(
                                    payload = transactionPayload,
                                    currentPayeeId = currentPayeeId
                                )
                                if (validation.isValid) {
                                    val useBle = bleReceiverMode && BlePaymentLink.server != null
                                    if (useBle) {
                                        if (TransactionQrBleReceiveCooldown.isInCooldown()) {
                                            return@addOnSuccessListener
                                        }
                                        if (!TransactionQrBleReceiveGuard.tryAcquire()) {
                                            return@addOnSuccessListener
                                        }
                                        scope.launch {
                                            try {
                                                val msg = coroutineScope {
                                                    val abort = async {
                                                        BlePaymentLink.sessionAbortFlow.first()
                                                    }
                                                    val work = async {
                                                        BleHandshake.receiverSendAckAndAwaitOk(
                                                            context = context,
                                                            repository = repository,
                                                            payload = transactionPayload,
                                                            currentPayeeId = currentPayeeId,
                                                        )
                                                    }
                                                    select<String?> {
                                                        abort.onAwait { reason ->
                                                            work.cancel()
                                                            reason
                                                        }
                                                        work.onAwait { res ->
                                                            abort.cancel()
                                                            if (res.isSuccess) {
                                                                TransactionQrBleReceiveCooldown.reset()
                                                                onOfflineLedgerUpdated()
                                                                onResult(transactionPayload)
                                                                null
                                                            } else {
                                                                res.exceptionOrNull()?.message
                                                                    ?: "Bluetooth payment could not be completed"
                                                            }
                                                        }
                                                    }
                                                }
                                                msg?.let { m ->
                                                    TransactionQrBleReceiveCooldown.recordFailure()
                                                    android.util.Log.e("TransactionQRScanner", "BLE receive flow: $m")
                                                    showBleScannerToastOnce(
                                                        context,
                                                        "$m\n\nYou can scan the same payment QR again to retry. " +
                                                            "Credits apply only once per transaction id.",
                                                    )
                                                }
                                            } finally {
                                                TransactionQrBleReceiveGuard.release()
                                            }
                                        }
                                    } else {
                                        scope.launch {
                                            Toast.makeText(
                                                context,
                                                "Offlink requires an active Bluetooth session before a payment can be recorded. Open Receive payment, stay connected, then scan again.",
                                                Toast.LENGTH_LONG,
                                            ).show()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

