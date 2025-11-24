package com.offlinepayment.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlinepayment.utils.CurrencyUtils

@Composable
fun ProfileScreen(
    userName: String,
    email: String,
    phoneNumber: String,
    balance: Double,
    qrCodeId: String,
    onEditProfile: () -> Unit,
    onViewQRCode: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E40AF),
                        Color(0xFF3B82F6)
                    )
                )
            )
            .padding(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Picture
                Card(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.take(1).uppercase(),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = userName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E40AF)
                )

                Text(
                    text = email,
                    fontSize = 16.sp,
                    color = Color.Gray
                )

                Text(
                    text = phoneNumber,
                    fontSize = 16.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Balance Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Current Balance",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = CurrencyUtils.formatPkr(balance),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Menu Items
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                ProfileMenuItem(
                    icon = Icons.Default.Edit,
                    title = "Edit Profile",
                    subtitle = "Update your personal information",
                    onClick = onEditProfile
                )

                ProfileMenuItem(
                    icon = Icons.Default.Settings,
                    title = "My QR Code",
                    subtitle = "View and share your payment QR code",
                    onClick = onViewQRCode
                )

                ProfileMenuItem(
                    icon = Icons.Default.Settings,
                    title = "Settings",
                    subtitle = "App preferences and security",
                    onClick = onSettings
                )

                Divider(modifier = Modifier.padding(horizontal = 16.dp))

                ProfileMenuItem(
                    icon = Icons.Default.ExitToApp,
                    title = "Logout",
                    subtitle = "Sign out of your account",
                    onClick = onLogout,
                    textColor = Color(0xFFDC2626)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Account Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Account Information",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "QR ID: ${qrCodeId.take(12)}...",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = "Account Type: Offline Wallet",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    textColor: Color = Color(0xFF374151)
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
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

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Navigate",
                modifier = Modifier.size(20.dp),
                tint = Color.Gray
            )
        }
    }
}
