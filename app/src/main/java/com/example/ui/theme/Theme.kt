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
import com.example.model.AnimationPreset
import com.example.model.OceanEmeraldMode

val LocalOceanPalette = staticCompositionLocalOf {
    OceanEmeraldThemes.paletteFor(OceanEmeraldMode.DARK)
}

val LocalOceanMode = staticCompositionLocalOf {
    OceanEmeraldMode.DARK
}

val LocalAnimationPreset = staticCompositionLocalOf {
    AnimationPreset.FULL_SPRING
}

val LocalGlassIntensity = staticCompositionLocalOf { 0.88f }

@Composable
fun OceanEmeraldTheme(
    mode: OceanEmeraldMode = OceanEmeraldMode.DARK,
    animationPreset: AnimationPreset = AnimationPreset.FULL_SPRING,
    glassIntensity: Float = 0.88f,
    content: @Composable () -> Unit
) {
    val targetScheme = OceanEmeraldThemes.colorSchemeFor(mode)
    val targetPalette = OceanEmeraldThemes.paletteFor(mode)

    val durationMs = (320 * animationPreset.durationScale).toInt().coerceAtLeast(1)

    val primary by animateColorAsState(targetScheme.primary, tween(durationMs), label = "primary")
    val secondary by animateColorAsState(targetScheme.secondary, tween(durationMs), label = "secondary")
    val background by animateColorAsState(targetScheme.background, tween(durationMs), label = "background")
    val surface by animateColorAsState(targetScheme.surface, tween(durationMs), label = "surface")
    val onBackground by animateColorAsState(targetScheme.onBackground, tween(durationMs), label = "onBackground")
    val onSurface by animateColorAsState(targetScheme.onSurface, tween(durationMs), label = "onSurface")

    val animatedScheme = targetScheme.copy(
        primary = primary,
        secondary = secondary,
        background = background,
        surface = surface,
        onBackground = onBackground,
        onSurface = onSurface
    )

    val bgTop by animateColorAsState(targetPalette.bgTop, tween(durationMs), label = "bgTop")
    val bgMiddle by animateColorAsState(targetPalette.bgMiddle, tween(durationMs), label = "bgMiddle")
    val bgBottom by animateColorAsState(targetPalette.bgBottom, tween(durationMs), label = "bgBottom")
    val cardFill by animateColorAsState(targetPalette.glassCardFill, tween(durationMs), label = "cardFill")
    val cardElevated by animateColorAsState(targetPalette.glassCardElevatedFill, tween(durationMs), label = "cardElev")

    val animatedPalette = targetPalette.copy(
        bgTop = bgTop,
        bgMiddle = bgMiddle,
        bgBottom = bgBottom,
        glassCardFill = cardFill,
        glassCardElevatedFill = cardElevated
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !mode.isDark
                    isAppearanceLightNavigationBars = !mode.isDark
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalOceanPalette provides animatedPalette,
        LocalOceanMode provides mode,
        LocalAnimationPreset provides animationPreset,
        LocalGlassIntensity provides glassIntensity
    ) {
        MaterialTheme(
            colorScheme = animatedScheme,
            typography = Typography,
            content = content
        )
    }
}
