package com.bitvaslov.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Единый набор цветов. Раньше тут был зелёный (#00E5A0), а весь интерфейс
// рисовался фиолетовым — из-за этого AppColors.* и код расходились.
object AppColors {
    val Background = Color(0xFF000000)
    val BackgroundSecondary = Color(0xFF161618)
    val BackgroundGradientEnd = Color(0xFF0A0A0A)
    val Surface = Color(0xFF151517)
    val SurfaceElevated = Color(0xFF1C1C1E)
    val Border = Color(0xFF2A2A2D)
    val Primary = Color(0xFF6B33D6)
    val PrimaryDark = Color(0xFF4B22A0)
    val Secondary = Color(0xFFB794F6)
    val Warning = Color(0xFFFFB547)
    val Danger = Color(0xFFFF4D6D)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF8E8E93)
    val TextDisabled = Color(0xFF48484A)
    val ButtonDisabled = Color(0xFF1C1C1E)
    val ButtonDisabledText = Color(0xFF636366)
    val ContentDark = Color(0xFF0A0A0A)
}

val AppTypography = androidx.compose.material3.Typography(
    displaySmall = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold),
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    bodySmall = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold),
)

private val DarkColorScheme = darkColorScheme(
    primary = AppColors.Primary,
    onPrimary = AppColors.ContentDark,
    primaryContainer = AppColors.SurfaceElevated,
    onPrimaryContainer = AppColors.TextPrimary,
    secondary = AppColors.Secondary,
    onSecondary = Color.White,
    secondaryContainer = AppColors.SurfaceElevated,
    onSecondaryContainer = AppColors.TextPrimary,
    background = AppColors.Background,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.Surface,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.SurfaceElevated,
    onSurfaceVariant = AppColors.TextSecondary,
    error = AppColors.Danger,
    onError = Color.White,
    outline = AppColors.Border,
    outlineVariant = AppColors.Border,
    surfaceTint = AppColors.Primary,
)

@Composable
fun BitvaSlovTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content,
    )
}