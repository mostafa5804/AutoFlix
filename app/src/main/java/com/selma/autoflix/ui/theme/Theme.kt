package com.selma.autoflix.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = SurfaceWhite,
    primaryContainer = ContainerRose,
    onPrimaryContainer = BrandPrimary,
    secondary = BrandSecondary,
    onSecondary = SurfaceWhite,
    secondaryContainer = ContainerIndigo,
    onSecondaryContainer = BrandSecondary,
    tertiary = AccentCoral,
    onTertiary = SurfaceWhite,
    background = BackgroundWarm,
    onBackground = InkDark,
    surface = SurfaceWhite,
    onSurface = InkDark,
    surfaceVariant = SurfaceVariantWarm,
    onSurfaceVariant = MutedSlate,
    outline = BorderLine
)

@Composable
fun AutoFlixTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = SurfaceWhite.toArgb()
                window.navigationBarColor = SurfaceWhite.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = true
                insetsController.isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
