package com.talantquest.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFF5C842),
    onPrimary = Color(0xFF1A1040),
    primaryContainer = Color(0xFF3D1A8A),
    onPrimaryContainer = Color(0xFFFFE08A),
    secondary = Color(0xFFAB8EFF),
    onSecondary = Color(0xFF1A1040),
    background = Color(0xFF1A1040),
    onBackground = Color(0xFFEEEEF8),
    surface = Color(0xFF252550),
    onSurface = Color(0xFFEEEEF8),
    surfaceVariant = Color(0xFF2E2E5A),
    onSurfaceVariant = Color(0xFFBBBBDD),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF1A0000),
)

@Composable
fun TalantQuestTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
