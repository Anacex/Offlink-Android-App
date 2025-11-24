package com.offlinepayment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offlinepayment.ui.auth.AuthViewModel
import com.offlinepayment.ui.auth.CreateAccountScreen
import com.offlinepayment.ui.auth.LoginScreen
import com.offlinepayment.ui.profile.ProfileScreen
import com.offlinepayment.ui.qr.QRCodeScreen
import com.offlinepayment.ui.SendPaymentScreen
import com.offlinepayment.ui.TransactionListScreen
import com.offlinepayment.ui.WalletScreen
import com.offlinepayment.ui.theme.OfflinePaymentTheme
import com.offlinepayment.ui.wallet.WalletUiState
import com.offlinepayment.ui.wallet.WalletViewModel
import com.offlinepayment.utils.CurrencyUtils
import kotlinx.coroutines.launch
import java.math.BigDecimal

class MainActivity : ComponentActivity() {
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
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
    val walletViewModel: WalletViewModel = viewModel(factory = WalletViewModel.Factory)
    val authState by authViewModel.uiState.collectAsState()
    val walletState by walletViewModel.uiState.collectAsState()

    var showCreateAccount by remember { mutableStateOf(false) }

    LaunchedEffect(authState.isLoggedIn) {
        if (authState.isLoggedIn) {
            walletViewModel.refreshWallets()
        }
    }

    if (!authState.isLoggedIn) {
        if (showCreateAccount) {
            CreateAccountScreen(
                uiState = authState,
                onSignup = authViewModel::signup,
                onVerifyEmail = authViewModel::verifyEmail,
                onBackToLogin = {
                    showCreateAccount = false
                }
            )
        } else {
            LoginScreen(
                uiState = authState,
                onLogin = authViewModel::startLogin,
                onOtpConfirm = authViewModel::confirmOtp,
                onCreateAccount = {
                    showCreateAccount = true
                }
            )
        }
    } else {
        MainAppContent(
            walletUiState = walletState,
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
    onLogout: () -> Unit,
    onRefreshWallets: () -> Unit,
    onTransfer: (Int, Int, BigDecimal) -> Unit
) {
    val userId = 1 // TODO: Replace with actual authenticated user id once endpoint is wired
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Mock user data
    val userName = "John Doe"
    val userEmail = "john.doe@example.com"
    val userPhone = "+92 300 1234567"
    val userBalance = 1250.0
    val qrCodeId = "QR123456789"

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
                onLogout = onLogout
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Offline Payment",
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
                    WalletScreen(
                        uiState = walletUiState,
                        onRefresh = onRefreshWallets,
                        onTransfer = onTransfer,
                        onSendClick = { navController.navigate("send") },
                        onViewTransactionsClick = { navController.navigate("transactions") },
                        onTopUpClick = {
                            onRefreshWallets()
                            navController.navigate("topup")
                        }
                    )
                }
                composable("send") {
                    SendPaymentScreen { _, _ ->
                        navController.popBackStack()
                    }
                }
                composable("transactions") {
                    val txs = listOf(
                        com.offlinepayment.ui.OfflineTransaction("1", "2025-01-17 10:21", "Alice", "Rs 25.00"),
                        com.offlinepayment.ui.OfflineTransaction("2", "2025-01-16 19:05", "Bob", "Rs 13.50"),
                        com.offlinepayment.ui.OfflineTransaction("3", "2025-01-15 14:30", "Charlie", "Rs 100.00")
                    )
                    TransactionListScreen(transactions = txs) { _ -> }
                }
                composable("topup") {
                    com.offlinepayment.ui.TopUpScreen { _ ->
                        navController.popBackStack()
                    }
                }
                composable("profile") {
                    ProfileScreen(
                        userName = userName,
                        email = userEmail,
                        phoneNumber = userPhone,
                        balance = userBalance,
                        qrCodeId = qrCodeId,
                        onEditProfile = { /* TODO */ },
                        onViewQRCode = { navController.navigate("qr") },
                        onSettings = { /* TODO */ },
                        onLogout = onLogout
                    )
                }
                composable("qr") {
                    QRCodeScreen(
                        userId = userId,
                        qrCodeId = qrCodeId,
                        userName = userName,
                        balance = userBalance,
                        onScanQR = { /* TODO: Implement QR Scanner */ }
                    )
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
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // User Info Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = userName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = userEmail,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = CurrencyUtils.formatPkr(userBalance),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Navigation Items
        DrawerMenuItem(
            icon = Icons.Default.Home,
            title = "Wallet",
            onClick = { onNavigate("wallet") }
        )
        DrawerMenuItem(
            icon = Icons.Default.Send,
            title = "Send Payment",
            onClick = { onNavigate("send") }
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

        Spacer(modifier = Modifier.weight(1f))

        // Logout Button
        DrawerMenuItem(
            icon = Icons.Default.ExitToApp,
            title = "Logout",
            onClick = onLogout,
            textColor = Color(0xFFDC2626)
        )
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
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        onClick = onClick
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
