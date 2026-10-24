package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RinAiDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF031620),
    primaryContainer = Color(0xFF0A2B3C),
    onPrimaryContainer = NeonCyanLight,
    secondary = ElectricPurple,
    onSecondary = Color(0xFF1E0A36),
    secondaryContainer = Color(0xFF381466),
    onSecondaryContainer = ElectricPurpleLight,
    tertiary = FuturisticPink,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    outlineVariant = Color(0xFF162035)
)

private val RinAiLightColorScheme = lightColorScheme(
    primary = Color(0xFF007799),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD0F4FF),
    onPrimaryContainer = Color(0xFF00202B),
    secondary = Color(0xFF7E22CE),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E8FF),
    onSecondaryContainer = Color(0xFF2E0854),
    tertiary = FuturisticPink,
    onTertiary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun RinAiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Default to futuristic dark aesthetic as requested in prompt, but support light mode
    val colorScheme = if (darkTheme) RinAiDarkColorScheme else RinAiLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
