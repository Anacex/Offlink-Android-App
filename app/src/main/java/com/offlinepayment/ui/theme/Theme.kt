package com.offlinepayment.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
	primary = Color(0xFF006C47),
	onPrimary = Color(0xFFFFFFFF),
	secondary = Color(0xFF476A59),
	onSecondary = Color(0xFFFFFFFF),
	background = Color(0xFFF0F5F2),
	onBackground = Color(0xFF1A1C1A)
)

private val DarkColors = darkColorScheme(
	primary = Color(0xFF6DDBB0),
	onPrimary = Color(0xFF003824),
	secondary = Color(0xFFB2CCBE),
	onSecondary = Color(0xFF193528),
	background = Color(0xFF0D1411),
	onBackground = Color(0xFFE1E3E1)
)

@Composable
fun OfflinePaymentTheme(
	useDarkTheme: Boolean = isSystemInDarkTheme(),
	content: @Composable () -> Unit
) {
	val colorScheme = if (useDarkTheme) DarkColors else LightColors
	MaterialTheme(
		colorScheme = colorScheme,
		content = content
	)
}
