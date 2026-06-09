package com.vmm.manager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary          = Color(0xFF1565C0),
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    secondary        = Color(0xFF0277BD),
    tertiary         = Color(0xFF00838F),
    background       = Color(0xFFF8F9FA),
    surface          = Color.White,
    error            = Color(0xFFB71C1C)
)

@Composable
fun VmmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content     = content
    )
}
