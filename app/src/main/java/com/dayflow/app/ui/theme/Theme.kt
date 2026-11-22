package com.dayflow.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DayflowColors = darkColorScheme(
    primary = BurgundyLight,
    onPrimary = TextPrimary,
    primaryContainer = Burgundy,
    onPrimaryContainer = TextPrimary,
    secondary = Burgundy,
    onSecondary = TextPrimary,
    tertiary = Warning,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = Background,
    surfaceContainerLow = Surface,
    surfaceContainer = Surface,
    surfaceContainerHigh = SurfaceElevated,
    surfaceContainerHighest = SurfaceElevated,
    outline = Divider,
    outlineVariant = Divider
)

private val DayflowTypography = Typography().let { base ->
    base.copy(
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Medium),
        labelSmall = base.labelSmall.copy(letterSpacing = 0.4.sp),
        labelMedium = base.labelMedium.copy(letterSpacing = 0.8.sp, fontWeight = FontWeight.Medium)
    )
}

@Composable
fun DayflowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DayflowColors,
        typography = DayflowTypography,
        content = content
    )
}
