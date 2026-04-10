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
import com.offlinepayment.data.TransactionPayloadQR
import com.offlinepayment.data.repository.WalletRepository
import com.offlinepayment.data.network.ApiClient
import com.offlinepayment.utils.QRCodeHelper
import java.util.concurrent.Executors
import kotlinx.coroutines.launch

/**
 * Scanner screen for receiving transaction payload QR codes.
 * This is used by the receiver to scan the sender's transaction QR code.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionQRScannerScreen(
    currentPayeeId: String, // Current user's ID to validate payeeId in QR
    onScanResult: (TransactionPayloadQR) -> Unit,
    onBack: () -> Unit
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
                                    processImageProxyForTransaction(imageProxy, currentPayeeId, repository, scope) { transaction ->
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
    currentPayeeId: String,
    repository: WalletRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    onResult: (TransactionPayloadQR) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )
        
        val scanner = BarcodeScanning.getClient()
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
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
                                    // Save transaction to local storage as "RECEIVED" (Step 4)
                                    scope.launch {
                                        val moshi = com.squareup.moshi.Moshi.Builder()
                                            .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                                            .build()
                                        val adapter = moshi.adapter(com.offlinepayment.data.TransactionPayloadQR::class.java)
                                        val rawPayload = adapter.toJson(transactionPayload)
                                        
                                        // Convert amount from paisa (Long) to PKR string format
                                        val amountInPkr = java.math.BigDecimal(transactionPayload.amount.toString())
                                            .divide(java.math.BigDecimal("100"))
                                            .toPlainString()
                                        
                                        // Get receiver wallet info
                                        val payeeUserId = currentPayeeId.toIntOrNull()
                                        val receiverWallet = payeeUserId?.let { 
                                            repository.getOfflineWalletByUserIdAndType(it, "offline")
                                        }
                                        
                                        val localTransaction = com.offlinepayment.data.local.LocalTransaction(
                                            txId = transactionPayload.txId,
                                            senderWalletId = 0, // Sender wallet ID not available in payload, will be updated on sync
                                            receiverPublicKey = receiverWallet?.publicKey ?: "pending_${transactionPayload.payeeId}",
                                            amount = amountInPkr,
                                            currency = "PKR",
                                            transactionSignature = "", // Will be set when transaction is signed
                                            nonce = transactionPayload.nonce,
                                            receiptHash = "", // Will be set when receipt is created
                                            receiptData = "{}", // Will be set when receipt is created
                                            status = "pending",
                                            createdAtDevice = transactionPayload.timestamp,
                                            deviceFingerprint = com.offlinepayment.data.session.DeviceFingerprintProvider.getFingerprint(),
                                            // Legacy fields for backward compatibility
                                            payerId = transactionPayload.payerId,
                                            payeeId = transactionPayload.payeeId,
                                            direction = "RECEIVED",
                                            rawPayload = rawPayload
                                        )
                                        
                                        repository.saveLocalTransaction(localTransaction)
                                        
                                        // Update receiver's wallet balance (add amount)
                                        try {
                                            val payeeUserId = currentPayeeId.toIntOrNull()
                                            if (payeeUserId != null) {
                                                val offlineWallet = repository.getOfflineWalletByUserIdAndType(payeeUserId, "offline")
                                                if (offlineWallet != null) {
                                                    val currentBalance = java.math.BigDecimal(offlineWallet.balance)
                                                    val transactionAmount = java.math.BigDecimal(transactionPayload.amount.toString()).divide(java.math.BigDecimal("100")) // Convert from paisa to PKR
                                                    val newBalance = currentBalance.add(transactionAmount)
                                                    // Ensure balance doesn't exceed max limit
                                                    val maxBalance = java.math.BigDecimal("5000")
                                                    val finalBalance = newBalance.min(maxBalance)
                                                    repository.updateOfflineWalletBalance(offlineWallet.walletId, finalBalance.toPlainString())
                                                }
                                            }
                                        } catch (e: Exception) {
                                            // Log error but don't block transaction
                                            android.util.Log.e("TransactionQRScanner", "Failed to update receiver balance: ${e.message}")
                                        }
                                    }
                                    onResult(transactionPayload)
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

