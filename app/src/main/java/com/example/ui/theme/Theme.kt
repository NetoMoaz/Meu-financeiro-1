package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = GreenDarkPrimary,
    onPrimary = Color(0xFF022C1A),
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = GoldPrimary,
    onSecondary = Color(0xFF451A03),
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = Color(0xFF34D399),
    onTertiary = Color(0xFF064E3B),
    background = GreenDarkBackground,
    onBackground = TextPrimaryDark,
    surface = GreenDarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = GreenDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFD1D5DB),
    error = ExpenseRed,
    onError = Color.White,
    outline = GreenDarkCardBorder
)

private val LightColorScheme = lightColorScheme(
    primary = GreenLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF065F46),
    secondary = GoldDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF92400E),
    tertiary = GreenLightPrimaryVariant,
    onTertiary = Color.White,
    background = GreenLightBackground,
    onBackground = TextPrimaryLight,
    surface = GreenLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = GreenLightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    error = ExpenseRed,
    onError = Color.White,
    outline = GreenLightCardBorder
)

@Composable
fun MeuFinanceiroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = if (darkTheme) colorScheme.surface.toArgb() else HeaderDarkGreen.toArgb()
                window.navigationBarColor = colorScheme.surface.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Kept for backward compatibility with existing tests
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MeuFinanceiroTheme(darkTheme = darkTheme, content = content)
}
