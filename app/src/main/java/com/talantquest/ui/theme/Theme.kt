package com.talantquest.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
        colorScheme = DarkColorScheme
    ) {
        // Surface 로 감싸야 LocalContentColor 가 onBackground(밝은 색)로 설정된다.
        // 그렇지 않으면 색을 지정하지 않은 Text 가 기본값(검정)으로 렌더링되어
        // 어두운 배경에서 보이지 않는다.
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkColorScheme.background,
            contentColor = DarkColorScheme.onBackground
        ) {
            content()
        }
    }
}
