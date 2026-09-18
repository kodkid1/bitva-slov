package com.bitvaslov.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object AppColors {
    val Background = Color(0xFF080A18)
    val BackgroundSecondary = Color(0xFF111633)
    val BackgroundGradientEnd = Color(0xFF1B1240)
    val Surface = Color(0xFF171C38)
    val SurfaceElevated = Color(0xFF20264A)
    val Border = Color(0xFF30365F)
    val Primary = Color(0xFF00E5A0)
    val PrimaryDark = Color(0xFF00C98D)
    val Secondary = Color(0xFF7C5CFF)
    val Warning = Color(0xFFFFB547)
    val Danger = Color(0xFFFF4D6D)
    val TextPrimary = Color(0xFFF5F7FF)
    val TextSecondary = Color(0xFF8F97B8)
    val TextDisabled = Color(0xFF555D80)
    val ButtonDisabled = Color(0xFF292E49)
    val ButtonDisabledText = Color(0xFF646B88)
    val ContentDark = Color(0xFF07131A)
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