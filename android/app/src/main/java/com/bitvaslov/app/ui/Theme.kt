package com.bitvaslov.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

val Accent = Color(0xFF00E5A0)
val AccentDark = Color(0xFF00B87F)
val Purple = Color(0xFF7C5CFF)
val PurpleDark = Color(0xFF4A2FD6)
val Danger = Color(0xFFFF5470)
val Ice = Color(0xFFB8C8FF)

private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF003D2B),
    primaryContainer = Color(0xFF0B4A38),
    onPrimaryContainer = Color(0xFF7DFFD0),
    secondary = Purple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2A2270),
    onSecondaryContainer = Color(0xFFC7BFFF),
    tertiary = Ice,
    background = Color(0xFF0A0C20),
    onBackground = Color(0xFFEEF0FF),
    surface = Color(0xFF151A3A),
    onSurface = Color(0xFFEEF0FF),
    surfaceVariant = Color(0xFF232A56),
    onSurfaceVariant = Color(0xFF9AA0D0),
    error = Danger,
    onError = Color.White,
    outline = Color(0xFF3A4278),
    outlineVariant = Color(0xFF232A56),
    surfaceTint = Accent,
    scrim = Color(0xCC0A0C20),
)

@Composable
fun BitvaSlovTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColorScheme, content = content)
}