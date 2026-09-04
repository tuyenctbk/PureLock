package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SecurityBlueLight,
    onPrimary = Color(0xFF0B1120),
    primaryContainer = Color(0xFF1E293B),
    onPrimaryContainer = Color(0xFFE2E8F0),
    secondary = SecurityCyan,
    onSecondary = Color(0xFF0B1120),
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = SecurityEmerald,
    onTertiary = Color(0xFF0B1120),
    tertiaryContainer = Color(0xFF064E3B),
    onTertiaryContainer = Color(0xFFD1FAE5),
    error = Color(0xFFEF4444),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    background = MinimalDarkSurface,
    onBackground = MinimalDarkOnSurface,
    surface = MinimalDarkSurface,
    surfaceVariant = MinimalDarkSurfaceVariant,
    onSurface = MinimalDarkOnSurface,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF1E293B),
    surfaceContainer = Color(0xFF1E293B),
    surfaceContainerHigh = Color(0xFF334155)
)

private val AmoledDarkColorScheme = darkColorScheme(
    primary = SecurityBlueLight,
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF090D16),
    onPrimaryContainer = Color(0xFFE2E8F0),
    secondary = SecurityCyan,
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF083344),
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = SecurityEmerald,
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFF022C22),
    onTertiaryContainer = Color(0xFFD1FAE5),
    error = Color(0xFFF87171),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFEE2E2),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF000000),
    surfaceVariant = Color(0xFF111827),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = Color(0xFF1F2937),
    outlineVariant = Color(0xFF111827),
    surfaceContainer = Color(0xFF0F172A),
    surfaceContainerHigh = Color(0xFF1E293B)
)

private val LightColorScheme = lightColorScheme(
    primary = SecurityBluePrimary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF0284C7),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    tertiary = Color(0xFF059669),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF065F46),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
    background = MinimalLightBackground,
    onBackground = MinimalOnSurface,
    surface = MinimalLightSurface,
    surfaceVariant = MinimalLightSurfaceVariant,
    onSurface = MinimalOnSurface,
    onSurfaceVariant = Color(0xFF334155),
    surfaceContainer = Color(0xFFEFF6FF),
    surfaceContainerHigh = Color(0xFFE2E8F0),
    outline = MinimalLightOutline,
    outlineVariant = Color(0xFFCBD5E1)
)

private val EmeraldDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color(0xFF042F2E),
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = EmeraldAccent,
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF065F46),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF451A03),
    background = EmeraldSurface,
    onBackground = Color(0xFFECFDF5),
    surface = EmeraldSurface,
    surfaceVariant = EmeraldSurfaceVariant,
    onSurface = Color(0xFFECFDF5),
    onSurfaceVariant = Color(0xFF6EE7B7),
    outline = Color(0xFF065F46),
    outlineVariant = Color(0xFF042F2E)
)

private val SapphireDarkColorScheme = darkColorScheme(
    primary = SapphirePrimary,
    onPrimary = Color(0xFF082F49),
    primaryContainer = Color(0xFF0C4A6E),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = SapphireAccent,
    onSecondary = Color(0xFF082F49),
    secondaryContainer = Color(0xFF0369A1),
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = SecurityCyan,
    onTertiary = Color(0xFF082F49),
    background = SapphireSurface,
    onBackground = Color(0xFFF0F9FF),
    surface = SapphireSurface,
    surfaceVariant = SapphireSurfaceVariant,
    onSurface = Color(0xFFF0F9FF),
    onSurfaceVariant = Color(0xFF7DD3FC),
    outline = Color(0xFF0369A1),
    outlineVariant = Color(0xFF0C4A6E)
)

@Composable
fun PureLockTheme(
    themeMode: String = "SYSTEM",
    darkTheme: Boolean = when (themeMode) {
        "LIGHT" -> false
        "DARK", "AMOLED", "CYBER_MIDNIGHT", "OLED_BLACK", "EMERALD_VAULT", "SAPPHIRE_FROST" -> true
        else -> isSystemInDarkTheme()
    },
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        "AMOLED", "OLED_BLACK" -> AmoledDarkColorScheme
        "EMERALD_VAULT" -> EmeraldDarkColorScheme
        "SAPPHIRE_FROST" -> SapphireDarkColorScheme
        "CYBER_MIDNIGHT" -> DarkColorScheme
        "LIGHT" -> LightColorScheme
        else -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else if (darkTheme) {
                DarkColorScheme
            } else {
                LightColorScheme
            }
        }
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
