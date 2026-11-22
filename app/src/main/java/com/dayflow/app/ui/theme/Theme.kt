package com.dayflow.app.ui.theme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
 primary=Burgundy,onPrimary=Color.White,primaryContainer=SurfaceElevated,onPrimaryContainer=Burgundy,
 secondary=BurgundyLight,onSecondary=Color.White,tertiary=Warning,background=Background,onBackground=TextPrimary,
 surface=Surface,onSurface=TextPrimary,surfaceVariant=SurfaceElevated,onSurfaceVariant=TextSecondary,
 surfaceContainerLowest=Color.White,surfaceContainerLow=Background,surfaceContainer=Surface,
 surfaceContainerHigh=SurfaceElevated,surfaceContainerHighest=SurfaceElevated,outline=Divider,outlineVariant=Divider
)
private val DarkColors = darkColorScheme(
 primary=DarkBurgundy,onPrimary=Color.White,primaryContainer=DarkSurfaceElevated,onPrimaryContainer=DarkTextPrimary,
 secondary=DarkBurgundyLight,onSecondary=Color.White,tertiary=Color(0xFFE1B66C),
 background=DarkBackground,onBackground=DarkTextPrimary,surface=DarkSurface,onSurface=DarkTextPrimary,
 surfaceVariant=DarkSurfaceElevated,onSurfaceVariant=DarkTextSecondary,surfaceContainerLowest=Color(0xFF050405),
 surfaceContainerLow=DarkBackground,surfaceContainer=DarkSurface,surfaceContainerHigh=DarkSurfaceElevated,
 surfaceContainerHighest=Color(0xFF30262C),outline=DarkDivider,outlineVariant=DarkDivider
)
private val DayflowTypography = Typography().let { base ->
 base.copy(
 displayLarge=base.displayLarge.copy(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Bold,letterSpacing=(-0.6).sp),
 displayMedium=base.displayMedium.copy(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Bold,letterSpacing=(-0.4).sp),
 headlineLarge=base.headlineLarge.copy(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Bold,letterSpacing=(-0.3).sp),
 titleSmall=base.titleSmall.copy(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Medium),
 bodyLarge=base.bodyLarge.copy(fontFamily=FontFamily.SansSerif,letterSpacing=(-0.1).sp),
 bodyMedium=base.bodyMedium.copy(fontFamily=FontFamily.SansSerif,letterSpacing=(-0.1).sp),
 bodySmall=base.bodySmall.copy(fontFamily=FontFamily.SansSerif),
 headlineMedium=base.headlineMedium.copy(fontWeight=FontWeight.Bold,letterSpacing=(-0.2).sp),
 headlineSmall=base.headlineSmall.copy(fontWeight=FontWeight.SemiBold),
 titleLarge=base.titleLarge.copy(fontWeight=FontWeight.SemiBold),
 titleMedium=base.titleMedium.copy(fontWeight=FontWeight.Medium),
 labelSmall=base.labelSmall.copy(letterSpacing=0.2.sp),
 labelMedium=base.labelMedium.copy(letterSpacing=0.2.sp,fontWeight=FontWeight.Medium)
 )
}
@Composable fun DayflowTheme(content: @Composable () -> Unit) {
 val dark = isSystemInDarkTheme()
 MaterialTheme(colorScheme=if (dark) DarkColors else LightColors,typography=DayflowTypography,content=content)
}
