package org.nagrikvoice.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = CivicNavy,
    onPrimary = CardBackground,
    secondary = CivicSaffron,
    onSecondary = CivicNavy,
    tertiary = CivicGreen,
    background = BackgroundLight,
    surface = CardBackground,
    onSurface = TextPrimary,
    error = AlertRed
)

private val DarkColorScheme = darkColorScheme(
    primary = CivicSaffron,
    onPrimary = CivicNavy,
    secondary = CivicGreen,
    onSecondary = CivicNavy,
    background = CivicNavy,
    surface = CivicNavyLight,
    onSurface = CardBackground,
    error = AlertRed
)

@Composable
fun NagrikVoiceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
