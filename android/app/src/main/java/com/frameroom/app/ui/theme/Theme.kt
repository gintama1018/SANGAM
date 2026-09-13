package com.frameroom.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AmberPrimary,
    primaryContainer = AmberContainer,
    onPrimary = OnAmber,
    onPrimaryContainer = OnAmberContainer,
    secondary = IndigoSecondary,
    secondaryContainer = IndigoContainer,
    onSecondary = OnIndigo,
    background = SurfaceOnyx,
    onBackground = OnSurface,
    surface = SurfaceOnyx,
    onSurface = OnSurface,
    surfaceVariant = SurfaceContainer,
    onSurfaceVariant = OnSurfaceVariant,
    outline = OutlineColor,
    error = ErrorColor
)

@Composable
fun FrameRoomTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = SurfaceOnyx.toArgb()
            window.navigationBarColor = SurfaceOnyx.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FrameRoomTypography,
        content = content
    )
}
