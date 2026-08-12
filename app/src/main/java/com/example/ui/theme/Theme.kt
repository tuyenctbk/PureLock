package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SecurityBlueLight,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF1E293B),
    onPrimaryContainer = Color(0xFFE2E8F0),
    secondary = SecurityCyan,
    tertiary = SecurityEmerald,
    background = MinimalDarkSurface,
    surface = MinimalDarkSurface,
    surfaceVariant = MinimalDarkSurfaceVariant,
    onSurface = MinimalDarkOnSurface,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155)
)

private val AmoledDarkColorScheme = darkColorScheme(
    primary = SecurityBlueLight,
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF090D16),
    onPrimaryContainer = Color(0xFFE2E8F0),
    secondary = SecurityCyan,
    tertiary = SecurityEmerald,
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceVariant = Color(0xFF111827),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = Color(0xFF1F2937)
)

private val LightColorScheme = lightColorScheme(
    primary = SecurityBluePrimary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = SecurityCyan,
    tertiary = SecurityEmerald,
    background = MinimalLightBackground,
    surface = MinimalLightSurface,
    surfaceVariant = MinimalLightSurfaceVariant,
    onSurface = MinimalOnSurface,
    onSurfaceVariant = Color(0xFF334155),
    surfaceContainer = Color(0xFFEFF6FF),
    surfaceContainerHigh = Color(0xFFE2E8F0),
    outline = MinimalLightOutline
)

@Composable
fun PureLockTheme(
    themeMode: String = "SYSTEM",
    darkTheme: Boolean = when (themeMode) {
        "LIGHT" -> false
        "DARK", "AMOLED" -> true
        else -> isSystemInDarkTheme()
    },
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        themeMode == "AMOLED" -> AmoledDarkColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    PureLockTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
