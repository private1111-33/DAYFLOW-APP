package com.dayflow.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DayflowColors = darkColorScheme(
    primary = BurgundyLight,
    onPrimary = TextPrimary,
    secondary = Burgundy,
    onSecondary = TextPrimary,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = Divider
)

@Composable
fun DayflowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DayflowColors,
        content = content
    )
}
