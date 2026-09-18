package com.bitvaslov.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF7C5CFF),
    secondary = Color(0xFF00D4A0),
    background = Color(0xFF0F1226),
    surface = Color(0xFF1E2246),
    surfaceVariant = Color(0xFF2E3363),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFEEF0FF),
    onSurface = Color(0xFFEEF0FF),
    onSurfaceVariant = Color(0xFF9AA0D0),
    error = Color(0xFFFF5470),
    outline = Color(0xFF2E3363),
)

@Composable
fun BitvaSlovTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColorScheme, content = content)
}