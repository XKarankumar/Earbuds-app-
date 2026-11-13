package com.example.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.model.AppThemeId

val LocalGlassPalette = staticCompositionLocalOf {
    AppThemes.glassPaletteFor(AppThemeId.DARK_3D_GLASS)
}

val LocalAppThemeId = staticCompositionLocalOf {
    AppThemeId.DARK_3D_GLASS
}

val LocalGlassIntensity = staticCompositionLocalOf { 0.88f }

@Composable
fun KaranFinderTheme(
    themeId: AppThemeId = AppThemeId.DARK_3D_GLASS,
    glassIntensity: Float = 0.88f,
    content: @Composable () -> Unit
) {
    val targetScheme = AppThemes.colorSchemeFor(themeId)
    val glassPalette = AppThemes.glassPaletteFor(themeId)

    val primary by animateColorAsState(targetScheme.primary, tween(280), label = "primary")
    val background by animateColorAsState(targetScheme.background, tween(280), label = "background")
    val surface by animateColorAsState(targetScheme.surface, tween(280), label = "surface")

    val animatedScheme = targetScheme.copy(
        primary = primary,
        background = background,
        surface = surface
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !themeId.isDark
                    isAppearanceLightNavigationBars = !themeId.isDark
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalGlassPalette provides glassPalette,
        LocalAppThemeId provides themeId,
        LocalGlassIntensity provides glassIntensity
    ) {
        MaterialTheme(
            colorScheme = animatedScheme,
            typography = Typography,
            content = content
        )
    }
}
