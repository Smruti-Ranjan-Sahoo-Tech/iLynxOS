package com.termux.ilynxos.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val WarmModernColorScheme = lightColorScheme(
    primary = OrangePrimary,
    onPrimary = CardSurface,
    primaryContainer = OrangeAccentBg,
    onPrimaryContainer = OrangePrimaryDark,
    secondary = SquircleTerminalIcon,
    onSecondary = CardSurface,
    background = WarmBackground,
    onBackground = TextDarkPrimary,
    surface = CardSurface,
    onSurface = TextDarkPrimary,
    surfaceVariant = WarmBackgroundSubtle,
    onSurfaceVariant = TextDarkSecondary,
    outline = CardBorder
)

private val DarkColorSchemeFallback = darkColorScheme(
    primary = OrangePrimary,
    onPrimary = Slate950,
    primaryContainer = Slate800,
    onPrimaryContainer = OrangeAccentBg,
    secondary = EmeraldGreen,
    onSecondary = Slate950,
    background = Slate950,
    onBackground = Slate200,
    surface = Slate900,
    onSurface = Slate200,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate400,
    outline = Slate700
)

@Composable
fun ILynxOSTheme(
    darkTheme: Boolean = false, // Default to elegant warm cream & orange theme (matching image.png)
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorSchemeFallback else WarmModernColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
