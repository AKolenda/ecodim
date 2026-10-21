package com.ecodim.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Amber = Color(0xFFF2B35B)
val Ink = Color(0xFF0E0D0B)
val Card = Color(0xFF1A1814)
val CardBorder = Color(0xFF2A2620)
val Muted = Color(0xFF9C9387)

private val colors = darkColorScheme(
    primary = Amber,
    onPrimary = Color(0xFF2A1A00),
    secondary = Amber,
    background = Ink,
    onBackground = Color(0xFFEDE6DA),
    surface = Card,
    onSurface = Color(0xFFEDE6DA),
    surfaceVariant = CardBorder,
    onSurfaceVariant = Muted,
    outline = Color(0xFF4A443B),
    error = Color(0xFFFF8A73),
)

@Composable
fun EcoDimTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
