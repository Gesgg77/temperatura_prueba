package com.example.temperatura.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Claro = lightColorScheme(
    primary = Color(0xFF006B5C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBCEFE1),
    onPrimaryContainer = Color(0xFF083A32),
    secondaryContainer = Color(0xFFFFE4CE),
    onSecondaryContainer = Color(0xFF542C12),
    tertiaryContainer = Color(0xFFD9E9FF),
    onTertiaryContainer = Color(0xFF153755),
    background = Color(0xFFF3F7F6),
    onBackground = Color(0xFF102823),
    surface = Color.White,
    onSurface = Color(0xFF102823),
    surfaceVariant = Color(0xFFE3EEEA),
    onSurfaceVariant = Color(0xFF425650),
    outline = Color(0xFF687C75),
    outlineVariant = Color(0xFFCBDAD4)
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFF83D7C3),
    onPrimary = Color(0xFF00382E),
    primaryContainer = Color(0xFF154D41),
    onPrimaryContainer = Color(0xFFACF0DD),
    secondaryContainer = Color(0xFF583821),
    onSecondaryContainer = Color(0xFFFFE4CE),
    tertiaryContainer = Color(0xFF203F5B),
    onTertiaryContainer = Color(0xFFD9E9FF),
    background = Color(0xFF0C1916),
    onBackground = Color(0xFFE0EFE9),
    surface = Color(0xFF152521),
    onSurface = Color(0xFFE0EFE9),
    surfaceVariant = Color(0xFF243933),
    onSurfaceVariant = Color(0xFFB5C9C1),
    outline = Color(0xFF839C91),
    outlineVariant = Color(0xFF395049)
)

@Composable
fun TemperaturaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) Oscuro else Claro,
        typography = Typography,
        content = content
    )
}
