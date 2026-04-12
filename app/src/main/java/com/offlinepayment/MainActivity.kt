package com.offlinepayment

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offlinepayment.ui.auth.AccountSuspendedScreen
import com.offlinepayment.ui.auth.AuthViewModel
import com.offlinepayment.ui.auth.CreateAccountScreen
import com.offlinepayment.ui.auth.ForgotPasswordScreen
import com.offlinepayment.ui.auth.LoginScreen
import com.offlinepayment.ui.profile.ProfileScreen
import com.offlinepayment.ui.qr.QRCodeScreen
import com.offlinepayment.ui.qr.QRScannerScreen
import com.offlinepayment.ui.qr.TransactionIntentReceivedScreen
import com.offlinepayment.ui.qr.TransactionReceivedScreen
import com.offlinepayment.ui.qr.TransactionQRScannerScreen
import com.offlinepayment.ble.BleOfflinkAssessment
import com.offlinepayment.ble.BleOfflinkEligibility
import com.offlinepayment.ble.BlePaymentLink
import com.offlinepayment.ui.SendPaymentScreen
import com.offlinepayment.ui.ble.BleReceiverReadyScreen
import com.offlinepayment.ui.ble.BleSenderScanScreen
import com.offlinepayment.ui.TopUpScreen
import com.offlinepayment.ui.TransactionListScreen
import com.offlinepayment.ui.WalletScreen
import com.offlinepayment.data.TransactionIntent
import com.offlinepayment.ui.theme.OfflinePaymentTheme
import com.offlinepayment.ui.wallet.WalletUiState
import com.offlinepayment.ui.wallet.WalletViewModel
import com.offlinepayment.utils.CurrencyUtils
import kotlinx.coroutines.launch
import java.math.BigDecimal

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OfflinePaymentTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    OfflinePaymentRoot()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflinePaymentRoot() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.createFactory(context))
    val walletViewModel: WalletViewModel = viewModel(factory = WalletViewModel.createFactory(context))
    val authState by authViewModel.uiState.collectAsState()
    val walletState by walletViewModel.uiState.collectAsState()

    var showCreateAccount by remember { mutableStateOf(false) }
    var showForgotPassword by remember { mutableStateOf(false) }

    LaunchedEffect(authState.isLoggedIn) {
        if (authState.isLoggedIn) {
            walletViewModel.refreshWallets()
        }
    }

    if (authState.accountSuspended || walletState.accountSuspended) {
        AccountSuspendedScreen(
            detail = authState.accountSuspendedDetail ?: walletState.accountSuspendedDetail,
            onBackToSignIn = {
                authViewModel.clearAccountSuspended()
                walletViewModel.clearAccountSuspended()
            },
        )
        return
    }

    if (!authState.isLoggedIn) {
        when {
            showForgotPassword -> {
                ForgotPasswordScreen(
                    uiState = authState,
                    onRequestReset = authViewModel::requestPasswordReset,
                    onConfirmReset = authViewModel::confirmPasswordReset,
                    onBack = {
                        authViewModel.clearForgotPasswordState()
                        showForgotPassword = false
                    },
                    onConsumeCompleteMessage = authViewModel::consumePasswordResetCompleteMessage,
                )
            }
            showCreateAccount -> {
                CreateAccountScreen(
                    uiState = authState,
                    onSignup = authViewModel::signup,
                    onVerifyEmail = authViewModel::verifyEmail,
                    onBackToLogin = {
                        showCreateAccount = false
                    }
                )
            }
            else -> {
                LoginScreen(
                    uiState = authState,
                    onLogin = authViewModel::startLogin,
                    onOtpConfirm = authViewModel::confirmOtp,
                    onCreateAccount = {
                        showCreateAccount = true
                    },
                    onForgotPassword = {
                        authViewModel.clearForgotPasswordState()
                        showForgotPassword = true
                    }
                )
            }
        }
    } else {
        MainAppContent(
            walletUiState = walletState,
            walletViewModel = walletViewModel,
            authUiState = authState,
            authViewModel = authViewModel,
            onLogout = {
                authViewModel.logout()
                showCreateAccount = false
            },
            onRefreshWallets = walletViewModel::refreshWallets,
            onTransfer = walletViewModel::transfer
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    walletUiState: WalletUiState,
    walletViewModel: WalletViewModel,
    authUiState: com.offlinepayment.ui.auth.AuthUiState,
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    onRefreshWallets: () -> Unit,
    onTransfer: (Int, Int, BigDecimal) -> Unit
) {
    // Get actual user ID from session - REQUIRED for all operations
    val context = androidx.compose.ui.platform.LocalContext.current
    val session = com.offlinepayment.data.session.AuthSessionManager.currentSession()
    
    // Try to get userId from session first, fallback to database lookup
    var userIdFromDb by remember { mutableStateOf<Int?>(null) }
    val database = com.offlinepayment.data.local.AppDatabase.getDatabase(context)
    
    // Watch for session changes and update userId from session or database
    LaunchedEffect(session?.userId, session?.userEmail) {
        if (session?.userId != null) {
            userIdFromDb = session.userId
        } else if (session?.userEmail != null && userIdFromDb == null) {
            // Fallback to database lookup if userId not in session yet
            userIdFromDb = session.userEmail?.let { email ->
                database.offlineUserDao().getUserByEmail(email)?.userId
            }
        }
    }
    
    // Use userId from session first, fallback to database
    val finalUserId = session?.userId ?: userIdFromDb
    
    // If userId is still null, user is not properly logged in - show error
    if (finalUserId == null) {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Authentication Error",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Unable to retrieve user information. Please log out and log in again.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onLogout) {
                    Text("Log Out")
                }
            }
        }
        return@MainAppContent
    }
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Shared state for scanned transaction intent
    var scannedTransactionIntent by remember { mutableStateOf<TransactionIntent?>(null) }
    
        // Shared state for scanned payee QR (more reliable than savedStateHandle)
        var scannedPayeeQRShared by remember { mutableStateOf<com.offlinepayment.data.PayeeQRPayload?>(null) }
        
        // Shared state for scanned transaction payload QR (more reliable than savedStateHandle)
        var scannedTransactionPayloadShared by remember { mutableStateOf<com.offlinepayment.data.TransactionPayloadQR?>(null) }

    // Get real user data from ProfileViewModel and WalletViewModel
    val profileViewModel: com.offlinepayment.ui.profile.ProfileViewModel = viewModel(
        factory = com.offlinepayment.ui.profile.ProfileViewModel.createFactory(context)
    )
    val profileState by profileViewModel.uiState.collectAsState()
    
    // Sync ViewModel for transaction synchronization
    val syncViewModel: com.offlinepayment.ui.sync.SyncViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(
            context.applicationContext as android.app.Application
        )
    )
    val syncState by syncViewModel.syncState.collectAsState()
    val isOnline by syncViewModel.isOnline.collectAsState()
    
    // Refresh wallets after successful sync
    LaunchedEffect(syncState) {
        if (syncState is com.offlinepayment.ui.sync.SyncState.Success) {
            val successState = syncState as com.offlinepayment.ui.sync.SyncState.Success
            // Only alert when there is something meaningful to report
            if (successState.syncedCount > 0 || successState.failedCount > 0) {
                val message = "Synced ${successState.syncedCount} (failed ${successState.failedCount})"
                android.widget.Toast
                    .makeText(context, message, android.widget.Toast.LENGTH_SHORT)
                    .show()
                walletViewModel.onSyncCompleted()
            }
            // Reset sync state promptly to avoid repeated alerts on periodic syncs with zero changes
            syncViewModel.resetSyncState()
        }
    }
    
    // Get offline wallet balance
    val offlineWallet = walletUiState.wallets.firstOrNull { it.wallet_type == "offline" }
    val userBalance = offlineWallet?.balance?.toDouble() ?: profileState.offlineBalance
    val userName = profileState.userName.ifEmpty { "User" }
    val userEmail = profileState.email.ifEmpty { authUiState.userEmail ?: "" }
    val userPhone = profileState.phone
    val qrCodeId = offlineWallet?.id?.toString() ?: "QR${finalUserId ?: ""}"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                userName = userName,
                userEmail = userEmail,
                userBalance = userBalance,
                onCloseDrawer = {
                    scope.launch {
                        drawerState.close()
                    }
                },
                onNavigate = { route ->
                    navController.navigate(route)
                    scope.launch {
                        drawerState.close()
                    }
                },
                onSendPaymentFromDrawer = {
                    scope.launch { drawerState.close() }
                    if (!authUiState.isEmailVerified) {
                        authViewModel.showEmailVerificationDialog()
                    } else {
                        when (val a = BleOfflinkEligibility.assessSender(context)) {
                            is BleOfflinkAssessment.Blocked ->
                                Toast.makeText(context, a.userMessage, Toast.LENGTH_LONG).show()
                            is BleOfflinkAssessment.Ok ->
                                navController.navigate("ble-sender-scan")
                        }
                    }
                },
                onReceivePaymentFromDrawer = {
                    scope.launch { drawerState.close() }
                    if (!authUiState.isEmailVerified) {
                        authViewModel.showEmailVerificationDialog()
                    } else {
                        when (val a = BleOfflinkEligibility.assessReceiver(context)) {
                            is BleOfflinkAssessment.Blocked ->
                                Toast.makeText(context, a.userMessage, Toast.LENGTH_LONG).show()
                            is BleOfflinkAssessment.Ok ->
                                navController.navigate("ble-receiver-ready")
                        }
                    }
                },
                onLogout = onLogout
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Offlink",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    drawerState.open()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = "wallet",
                modifier = Modifier.padding(paddingValues)
            ) {
                composable("wallet") {
                    val walletContext = LocalContext.current
                    WalletScreen(
                        uiState = walletUiState,
                        onRefresh = onRefreshWallets,
                        onTransfer = onTransfer,
                        onSendClick = {
                            if (!authUiState.isEmailVerified) {
                                authViewModel.showEmailVerificationDialog()
                            } else {
                                when (val a = BleOfflinkEligibility.assessSender(walletContext)) {
                                    is BleOfflinkAssessment.Blocked ->
                                        Toast.makeText(walletContext, a.userMessage, Toast.LENGTH_LONG).show()
                                    is BleOfflinkAssessment.Ok ->
                                        navController.navigate("ble-sender-scan")
                                }
                            }
                        },
                        onReceivePaymentClick = {
                            if (!authUiState.isEmailVerified) {
                                authViewModel.showEmailVerificationDialog()
                            } else {
                                when (val a = BleOfflinkEligibility.assessReceiver(walletContext)) {
                                    is BleOfflinkAssessment.Blocked ->
                                        Toast.makeText(walletContext, a.userMessage, Toast.LENGTH_LONG).show()
                                    is BleOfflinkAssessment.Ok ->
                                        navController.navigate("ble-receiver-ready")
                                }
                            }
                        },
                        onViewTransactionsClick = { navController.navigate("transactions") },
                        onTopUpClick = {
                            // Biometric authentication will be handled in the topup composable
                            onRefreshWallets()
                            navController.navigate("topup")
                        },
                        onInitiateWalletCreation = { bankAccount ->
                            walletViewModel.initiateWalletCreation(bankAccount)
                        },
                        onVerifyWalletCreation = { otp ->
                            walletViewModel.verifyAndCreateWallet(otp)
                        },
                        isEmailVerified = authUiState.isEmailVerified,
                        userEmail = authUiState.userEmail,
                        onVerifyEmailClick = {
                            authViewModel.showEmailVerificationDialog()
                        },
                        syncState = syncState,
                        isOnline = isOnline,
                        userName = userName
                    )
                    
                    // Show email verification dialog
                    if (authUiState.showEmailVerificationDialog) {
                        com.offlinepayment.ui.auth.EmailVerificationDialog(
                            email = authUiState.userEmail,
                            isLoading = authUiState.isLoading,
                            errorMessage = authUiState.errorMessage,
                            successMessage = authUiState.successMessage,
                            onOtpSubmit = { otp ->
                                authUiState.userEmail?.let { email ->
                                    authViewModel.verifyEmail(email, otp)
                                }
                            },
                            onDismiss = {
                                authViewModel.hideEmailVerificationDialog()
                            },
                            onResendOtp = {
                                authUiState.userEmail?.let { email ->
                                    authViewModel.requestEmailVerificationOtp(email)
                                }
                            }
                        )
                    }
                }
                composable("ble-sender-scan") {
                    BleSenderScanScreen(
                        onBack = { navController.popBackStack() },
                        onLinked = {
                            BlePaymentLink.wasLinkedForBleSend = true
                            navController.navigate("send") {
                                popUpTo("ble-sender-scan") { inclusive = true }
                            }
                        },
                    )
                }
                composable("ble-receiver-ready") {
                    val uid = finalUserId?.toString() ?: "0"
                    BleReceiverReadyScreen(
                        userIdSuffix = uid,
                        onBack = {
                            BlePaymentLink.server?.stop()
                            BlePaymentLink.server = null
                            BlePaymentLink.isReceiverHosting = false
                            navController.popBackStack()
                        },
                        onReadyContinue = {
                            navController.navigate("transaction-qr-scanner/1")
                        },
                    )
                }
                composable("send") {
                    // Get first offline wallet ID if available, otherwise use default
                    val offlineWallet = walletUiState.wallets.firstOrNull { it.wallet_type == "offline" }
                    val walletId = offlineWallet?.id ?: 1
                    val walletBalance = offlineWallet?.balance ?: java.math.BigDecimal.ZERO
                    val senderPublicKey = offlineWallet?.public_key
                    val senderAccount = offlineWallet?.bank_account_number ?: ""
                    
                    // Get sender name from profile
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val profileViewModel: com.offlinepayment.ui.profile.ProfileViewModel = viewModel(
                        factory = com.offlinepayment.ui.profile.ProfileViewModel.createFactory(context)
                    )
                    val profileState by profileViewModel.uiState.collectAsState()
                    val senderName = profileState.userName.ifEmpty { "User" }
                    
                    // Get scanned payee QR from shared state (more reliable than savedStateHandle)
                    var scannedPayeeQRFromNav by remember { mutableStateOf<com.offlinepayment.data.PayeeQRPayload?>(null) }
                    
                    // Method 1: Check immediately on every recomposition (most reliable - runs synchronously)
                    if (scannedPayeeQRShared != null && scannedPayeeQRFromNav == null) {
                        scannedPayeeQRFromNav = scannedPayeeQRShared
                        scannedPayeeQRShared = null
                    }
                    
                    // Method 2: Observe via LaunchedEffect when shared state changes
                    LaunchedEffect(scannedPayeeQRShared) {
                        if (scannedPayeeQRShared != null && scannedPayeeQRFromNav == null) {
                            kotlinx.coroutines.delay(50) // Small delay to ensure state propagation
                            if (scannedPayeeQRShared != null) {
                                scannedPayeeQRFromNav = scannedPayeeQRShared
                                scannedPayeeQRShared = null
                            }
                        }
                    }
                    
                    // Method 3: Observe when back stack entry changes (when returning from scanner)
                    LaunchedEffect(navController.currentBackStackEntry?.id) {
                        kotlinx.coroutines.delay(150) // Delay to ensure we're back on the screen
                        if (scannedPayeeQRShared != null && scannedPayeeQRFromNav == null) {
                            scannedPayeeQRFromNav = scannedPayeeQRShared
                            scannedPayeeQRShared = null
                        }
                    }
                    
                    // Use local variable to avoid smart cast issues
                    val currentUserId = finalUserId
                    if (currentUserId == null) {
                        // Show error if userId is null
                        androidx.compose.material3.Surface {
                            androidx.compose.foundation.layout.Column(
                                modifier = androidx.compose.ui.Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                            ) {
                                androidx.compose.material3.Text(
                                    text = "Unable to retrieve user information",
                                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                                    color = androidx.compose.ui.graphics.Color.Red
                                )
                            }
                        }
                    } else {
                        SendPaymentScreen(
                            userId = currentUserId,
                        walletId = walletId,
                        walletBalance = walletBalance,
                        senderPublicKey = senderPublicKey,
                        senderName = senderName,
                        senderAccount = senderAccount,
                        bleHandshakeEnabled = BlePaymentLink.wasLinkedForBleSend,
                        onGenerateQR = { qrData ->
                            // QR generated, already shown in screen
                        },
                        onScanQR = { amountValue ->
                            // Navigate to payee QR scanner
                            navController.navigate("receiver-qr-scanner")
                        },
                        onReceiverQRScanned = { payeeQR, amount ->
                            // This callback is called when payee QR is scanned
                            // The payee QR is stored in the screen state
                            // No need to navigate, just update the screen
                        },
                        scannedPayeeQRFromNav = scannedPayeeQRFromNav,
                        onBleLinkLostExit = {
                            navController.popBackStack("wallet", inclusive = false)
                        },
                    )
                    }
                }
                composable("qr-scanner") {
                    QRScannerScreen(
                        onScanResult = { intent ->
                            scannedTransactionIntent = intent
                            navController.navigate("transaction-intent-received")
                        },
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("receiver-qr-scanner") {
                    com.offlinepayment.ui.qr.ReceiverQRScannerScreen(
                        onScanResult = { payeeQR ->
                            // Store scanned payee QR in shared state (more reliable than savedStateHandle)
                            // Set state first, then navigate with small delay to ensure state propagation
                            scannedPayeeQRShared = payeeQR
                            scope.launch {
                                kotlinx.coroutines.delay(100) // Ensure state is set before navigation
                                // Navigate back to SendPaymentScreen
                                navController.popBackStack()
                            }
                        },
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("payment-confirmation") {
                    val receiverQR = navController.previousBackStackEntry?.savedStateHandle?.get<com.offlinepayment.data.ReceiverQRPayload>("receiverQR")
                    val amountState = remember { mutableStateOf("") }
                    
                    // Get amount from SendPaymentScreen (we'll need to pass it via savedStateHandle)
                    val amountString = navController.previousBackStackEntry?.savedStateHandle?.get<String>("paymentAmount") ?: "0"
                    val amount = amountString.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO
                    
                    if (receiverQR != null) {
                        com.offlinepayment.ui.qr.PaymentConfirmationScreen(
                            receiverQR = receiverQR,
                            amount = amount,
                            receiverName = null, // TODO: Fetch from API using receiverQR.user_id
                            onConfirm = {
                                // TODO: Process payment
                                navController.popBackStack("send", inclusive = false)
                            },
                            onCancel = {
                                navController.popBackStack()
                            }
                        )
                    } else {
                        // If no receiver QR, go back
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    }
                }
                composable(
                    route = "transaction-qr-scanner/{bleHost}",
                    arguments = listOf(
                        navArgument("bleHost") {
                            type = NavType.StringType
                            defaultValue = "0"
                        },
                    ),
                ) { entry ->
                    val bleReceiverMode = entry.arguments?.getString("bleHost") == "1"
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val session = com.offlinepayment.data.session.AuthSessionManager.currentSession()
                    val database = com.offlinepayment.data.local.AppDatabase.getDatabase(context)
                    // Get userId as Int first, then convert to String for currentPayeeId parameter
                    var currentUserIdInt by remember { mutableStateOf<Int?>(null) }
                    
                    LaunchedEffect(session?.userEmail) {
                        currentUserIdInt = session?.userEmail?.let { email ->
                            database.offlineUserDao().getUserByEmail(email)?.userId
                        }
                    }
                    
                    val currentUserId = currentUserIdInt?.toString()
                    
                    if (currentUserId != null) {
                        com.offlinepayment.ui.qr.TransactionQRScannerScreen(
                            currentPayeeId = currentUserId,
                            bleReceiverMode = bleReceiverMode,
                            onScanResult = { payload ->
                                // Store scanned transaction payload in shared state (more reliable than savedStateHandle)
                                scannedTransactionPayloadShared = payload
                                // Navigate with small delay to ensure state propagation
                                scope.launch {
                                    kotlinx.coroutines.delay(100)
                                    navController.navigate("transaction-received")
                                }
                            },
                            onBack = {
                                navController.popBackStack()
                            }
                        )
                    } else {
                        // User ID not available - show error and navigate back
                        androidx.compose.material3.Surface {
                            androidx.compose.foundation.layout.Column(
                                modifier = androidx.compose.ui.Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                            ) {
                                androidx.compose.material3.Text(
                                    text = "Unable to retrieve user information",
                                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                                    color = androidx.compose.ui.graphics.Color.Red
                                )
                                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
                                androidx.compose.material3.Text(
                                    text = "Please ensure you are logged in and try again.",
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                                )
                                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(24.dp))
                                androidx.compose.material3.Button(
                                    onClick = { navController.popBackStack() }
                                ) {
                                    androidx.compose.material3.Text("Go Back")
                                }
                            }
                        }
                    }
                }
                composable("transaction-received") {
                    // Get scanned transaction payload from shared state (more reliable than savedStateHandle)
                    var transactionPayloadFromNav by remember { mutableStateOf<com.offlinepayment.data.TransactionPayloadQR?>(null) }
                    
                    // Method 1: Check immediately on every recomposition (most reliable - runs synchronously)
                    if (scannedTransactionPayloadShared != null && transactionPayloadFromNav == null) {
                        transactionPayloadFromNav = scannedTransactionPayloadShared
                        scannedTransactionPayloadShared = null
                    }
                    
                    // Method 2: Observe via LaunchedEffect when shared state changes
                    LaunchedEffect(scannedTransactionPayloadShared) {
                        if (scannedTransactionPayloadShared != null && transactionPayloadFromNav == null) {
                            kotlinx.coroutines.delay(50)
                            if (scannedTransactionPayloadShared != null) {
                                transactionPayloadFromNav = scannedTransactionPayloadShared
                                scannedTransactionPayloadShared = null
                            }
                        }
                    }
                    
                    // Method 3: Observe when back stack entry changes (when returning from scanner)
                    LaunchedEffect(navController.currentBackStackEntry?.id) {
                        kotlinx.coroutines.delay(150)
                        if (scannedTransactionPayloadShared != null && transactionPayloadFromNav == null) {
                            transactionPayloadFromNav = scannedTransactionPayloadShared
                            scannedTransactionPayloadShared = null
                        }
                    }
                    
                    transactionPayloadFromNav?.let { payload ->
                        com.offlinepayment.ui.qr.TransactionReceivedScreen(
                            transactionPayload = payload,
                            onAccept = {
                                // Transaction already saved as "RECEIVED" in TransactionQRScannerScreen
                                navController.popBackStack("wallet", inclusive = false)
                            },
                            onReject = {
                                navController.popBackStack()
                            }
                        )
                    } ?: run {
                        // If no transaction payload, go back
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    }
                }
                composable("transaction-intent-received") {
                    scannedTransactionIntent?.let { intent ->
                        TransactionIntentReceivedScreen(
                            intent = intent,
                            onAccept = {
                                // Handle acceptance - could process payment here
                                scannedTransactionIntent = null
                                navController.popBackStack("send", inclusive = false)
                            },
                            onReject = {
                                scannedTransactionIntent = null
                                navController.popBackStack()
                            }
                        )
                    } ?: run {
                        // If no intent, go back
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    }
                }
                composable("transactions") {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val repository = remember { 
                        com.offlinepayment.data.repository.WalletRepository(
                            com.offlinepayment.data.network.ApiClient.walletApi, 
                            context
                        ) 
                    }
                    // Observe local transactions (sent + received) reactively
                    val localTransactions by remember(finalUserId) {
                        if (finalUserId != null) {
                            repository.observeLocalTransactions(finalUserId.toString())
                        } else {
                            kotlinx.coroutines.flow.flowOf(emptyList())
                        }
                    }.collectAsState(initial = emptyList())
                    val isLoadingLocal = false
                    
                    // Load transfer history and synced transactions from server
                    LaunchedEffect(Unit) {
                        walletViewModel.getTransferHistory()
                        walletViewModel.getSyncedTransactions()
                    }
                    
                    TransactionListScreen(
                        transfers = walletUiState.transferHistory,
                        localTransactions = localTransactions,
                        syncedTransactions = walletUiState.syncedTransactions,
                        isLoading = walletUiState.isLoadingHistory || walletUiState.isLoadingSyncedTransactions || isLoadingLocal
                    ) { _ -> }
                }
                composable("topup") {
                    // Get offline wallet for top-up
                    val offlineWallet = walletUiState.wallets.firstOrNull { it.wallet_type == "offline" }
                    if (offlineWallet != null) {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        // Get activity - MainActivity extends AppCompatActivity which extends FragmentActivity
                        // Helper function to get activity from context
                        fun getActivity(context: android.content.Context): androidx.fragment.app.FragmentActivity? {
                            return when (context) {
                                is androidx.fragment.app.FragmentActivity -> context
                                is android.content.ContextWrapper -> getActivity(context.baseContext)
                                else -> null
                            }
                        }
                        val activity = getActivity(context)
                        var biometricAuthenticated by remember { mutableStateOf(false) }
                        var showBiometricPrompt by remember { mutableStateOf(true) }
                        val isDeviceSecurityEnabled = remember { 
                            com.offlinepayment.utils.BiometricAuthHelper.isDeviceSecurityEnabled(context)
                        }
                        val isBiometricAvailable = remember { 
                            com.offlinepayment.utils.BiometricAuthHelper.isBiometricAvailable(context)
                        }
                        val securityStatusMessage = remember {
                            com.offlinepayment.utils.BiometricAuthHelper.getDeviceSecurityStatus(context)
                        }
                        
                        // Check device security first
                        if (!isDeviceSecurityEnabled) {
                            // Show error screen if device security is not enabled
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFF9FAFB)),
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "🔒",
                                            fontSize = 64.sp
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Device Security Required",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFDC2626)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = securityStatusMessage,
                                            fontSize = 16.sp,
                                            color = Color(0xFF6B7280),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "To use top-up and payment features, please set up a screen lock (password, PIN, pattern, or fingerprint) in your device settings.",
                                            fontSize = 14.sp,
                                            color = Color(0xFF9CA3AF),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Button(
                                            onClick = { navController.popBackStack() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF8B5CF6)
                                            )
                                        ) {
                                            Text("Go Back", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        } else {
                            // Device security is enabled, proceed with biometric authentication
                            // Trigger biometric authentication when screen loads
                            LaunchedEffect(showBiometricPrompt) {
                                if (showBiometricPrompt) {
                                    // Log for debugging
                                    android.util.Log.d("TopUp", "LaunchedEffect triggered: isBiometricAvailable=$isBiometricAvailable, activity=${activity != null}")
                                    
                                    if (isBiometricAvailable && activity != null) {
                                        // Small delay to ensure UI is ready
                                        kotlinx.coroutines.delay(300)
                                        android.util.Log.d("TopUp", "Calling BiometricAuthHelper.authenticate")
                                        try {
                                            val success = com.offlinepayment.utils.BiometricAuthHelper.authenticate(
                                                activity,
                                                title = "Top Up Authentication",
                                                subtitle = "Use your fingerprint, face, or device password to access top-up"
                                            )
                                            android.util.Log.d("TopUp", "Biometric authentication result: $success")
                                            biometricAuthenticated = success
                                            showBiometricPrompt = false
                                            if (!success) {
                                                // If authentication fails, go back
                                                navController.popBackStack()
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("TopUp", "Error during biometric authentication", e)
                                            // On error, allow access as fallback
                                            biometricAuthenticated = true
                                            showBiometricPrompt = false
                                        }
                                    } else if (!isBiometricAvailable) {
                                        // If biometric is not available but device security is enabled, allow access
                                        android.util.Log.d("TopUp", "Biometric not available, allowing access")
                                        biometricAuthenticated = true
                                        showBiometricPrompt = false
                                    } else if (activity == null) {
                                        // Activity is null - this shouldn't happen, but allow access as fallback
                                        android.util.Log.e("TopUp", "Activity is null, cannot show biometric prompt")
                                        biometricAuthenticated = true
                                        showBiometricPrompt = false
                                    }
                                }
                            }
                            
                            if (biometricAuthenticated || !isBiometricAvailable) {
                                TopUpScreen(
                                    walletId = offlineWallet.id,
                                    currentBalance = offlineWallet.balance,
                                    isBiometricAuthenticated = true, // Pass authenticated state
                                    onTopUpComplete = {
                                        // Refresh wallets after successful top-up
                                        walletViewModel.refreshWallets()
                                        navController.popBackStack()
                                    },
                                    onBack = {
                                        navController.popBackStack()
                                    }
                                )
                            } else {
                                // Show loading/authentication screen
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        androidx.compose.material3.CircularProgressIndicator()
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Authenticating...",
                                            fontSize = 16.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // No offline wallet - show message
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No offline wallet found",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { navController.popBackStack() }) {
                                Text("Go Back")
                            }
                        }
                    }
                }
                composable("profile") {
                    ProfileScreen(
                        onViewTransactions = { navController.navigate("transactions") },
                        onViewQRCode = { navController.navigate("qr") },
                        onSettings = { /* TODO */ },
                        onLogout = onLogout
                    )
                }
                composable("qr") {
                    // Use local variable to avoid smart cast issues
                    val currentUserIdForQR = finalUserId
                    if (currentUserIdForQR == null) {
                        androidx.compose.material3.Surface {
                            androidx.compose.foundation.layout.Column(
                                modifier = androidx.compose.ui.Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                            ) {
                                androidx.compose.material3.Text(
                                    text = "Unable to retrieve user information",
                                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                                    color = androidx.compose.ui.graphics.Color.Red
                                )
                            }
                        }
                    } else {
                        QRCodeScreen(
                            userId = currentUserIdForQR,
                            qrCodeId = qrCodeId,
                            userName = userName,
                            balance = userBalance,
                            onScanQR = {
                                when (val a = BleOfflinkEligibility.assessReceiver(context)) {
                                    is BleOfflinkAssessment.Blocked ->
                                        Toast.makeText(context, a.userMessage, Toast.LENGTH_LONG).show()
                                    is BleOfflinkAssessment.Ok ->
                                        navController.navigate("ble-receiver-ready")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerContent(
    userName: String,
    userEmail: String,
    userBalance: Double,
    onCloseDrawer: () -> Unit,
    onNavigate: (String) -> Unit,
    onSendPaymentFromDrawer: () -> Unit,
    onReceivePaymentFromDrawer: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // User Info Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = userName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = userEmail,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = CurrencyUtils.formatPkr(userBalance),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }
        
        // Navigation Items
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
        DrawerMenuItem(
            icon = Icons.Default.Home,
            title = "Wallet",
            onClick = { onNavigate("wallet") }
        )
        DrawerMenuItem(
            icon = Icons.Default.Send,
            title = "Send Payment",
            onClick = onSendPaymentFromDrawer
        )
        DrawerMenuItem(
            icon = Icons.Default.QrCodeScanner,
            title = "Receive Payment",
            onClick = onReceivePaymentFromDrawer
        )
        DrawerMenuItem(
            icon = Icons.Default.List,
            title = "Transactions",
            onClick = { onNavigate("transactions") }
        )
        DrawerMenuItem(
            icon = Icons.Default.Settings,
            title = "My QR Code",
            onClick = { onNavigate("qr") }
        )
        DrawerMenuItem(
            icon = Icons.Default.Person,
            title = "Profile",
            onClick = { onNavigate("profile") }
        )

        }
        
        Spacer(modifier = Modifier.weight(1f))

        // Logout Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            DrawerMenuItem(
                icon = Icons.Default.ExitToApp,
                title = "Logout",
                onClick = onLogout,
                textColor = Color(0xFFDC2626)
            )
        }
    }
}

@Composable
fun DrawerMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = textColor
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }
    }
}
