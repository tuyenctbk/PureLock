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
    primary = MinimalDarkOnSurface,
    onPrimary = MinimalDarkSurface,
    primaryContainer = MinimalDarkSurface,
    onPrimaryContainer = MinimalDarkOnSurface,
    secondary = MinimalDarkOnSurface,
    background = MinimalDarkSurface,
    surface = MinimalDarkSurface,
    surfaceVariant = Color(0xFF1E2638),
    onSurface = MinimalDarkOnSurface,
    onSurfaceVariant = MinimalDarkOnSurface.copy(alpha = 0.8f)
)

private val AmoledDarkColorScheme = darkColorScheme(
    primary = Color(0xFF63A0FF),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF10141D),
    onPrimaryContainer = Color(0xFFE2E8F0),
    secondary = Color(0xFF38BDF8),
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceVariant = Color(0xFF121212),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFFA0AEC0)
)

private val LightColorScheme = lightColorScheme(
    primary = MinimalPrimary,
    onPrimary = MinimalOnPrimary,
    primaryContainer = MinimalPrimaryContainer,
    onPrimaryContainer = MinimalOnPrimaryContainer,
    secondary = MinimalPrimary,
    background = MinimalLightBackground,
    surface = MinimalLightSurface,
    surfaceVariant = MinimalLightSurfaceVariant,
    onSurface = MinimalOnSurface,
    onSurfaceVariant = MinimalOnSurfaceVariant,
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

