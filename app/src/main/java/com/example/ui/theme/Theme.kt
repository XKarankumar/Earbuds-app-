package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.model.ThemeMode

val LocalGlassPalette = staticCompositionLocalOf {
    FitnessThemes.paletteFor(ThemeMode.AMOLED_DARK)
}

val FitnessShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun FitnessHubTheme(
    mode: ThemeMode = ThemeMode.AMOLED_DARK,
    content: @Composable () -> Unit
) {
    val palette = FitnessThemes.paletteFor(mode)
    val colorScheme = FitnessThemes.colorSchemeFor(mode)
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                @Suppress("DEPRECATION")
                window.statusBarColor = palette.background.toArgb()
                @Suppress("DEPRECATION")
                window.navigationBarColor = palette.background.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !palette.isDark
                controller.isAppearanceLightNavigationBars = !palette.isDark
            }
        }
    }

    CompositionLocalProvider(LocalGlassPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = FitnessShapes,
            content = content
        )
    }
}
