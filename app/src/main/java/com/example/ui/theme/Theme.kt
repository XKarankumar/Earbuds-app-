package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.AnimationStyle
import com.example.model.ThemePreference
import com.example.model.UiCustomizationConfig

data class LiquidGlassStyle(
    val isDark: Boolean = false,
    val glassAlpha: Float = 0.80f,
    val blurRadius: Dp = 20.dp,
    val reflectionIntensity: Float = 0.72f,
    val cornerRadius: Dp = 26.dp,
    val animationStyle: AnimationStyle = AnimationStyle.LIQUID_SPRING,
    val animationSpeedMultiplier: Float = 1.0f,
    val enable3dAutoOrbit: Boolean = true,
)

val LocalLiquidGlassStyle = staticCompositionLocalOf { LiquidGlassStyle() }

private val WhiteLuxuryColorScheme = lightColorScheme(
    primary = LuxuryGraphiteInk,
    onPrimary = PureWhite,
    primaryContainer = FrostedSilverSurface,
    onPrimaryContainer = LuxuryGraphiteInk,
    secondary = LuxurySlateMuted,
    onSecondary = PureWhite,
    secondaryContainer = SoftSilverGradientEnd,
    onSecondaryContainer = LuxuryGraphiteInk,
    tertiary = StatusEmerald,
    background = PureWhite,
    onBackground = LuxuryGraphiteInk,
    surface = PureWhite,
    onSurface = LuxuryGraphiteInk,
    surfaceVariant = FrostedSilverSurface,
    onSurfaceVariant = LuxurySlateMuted,
    outline = SilverEdgeRim,
    outlineVariant = SilverDivider,
    error = StatusCoral,
)

private val ObsidianColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = ObsidianBlack,
    primaryContainer = ObsidianSurfaceVariant,
    onPrimaryContainer = PureWhite,
    secondary = DarkMutedInk,
    onSecondary = ObsidianBlack,
    secondaryContainer = ObsidianSurfaceVariant,
    onSecondaryContainer = DarkInk,
    tertiary = StatusEmerald,
    background = ObsidianBlack,
    onBackground = DarkInk,
    surface = ObsidianSurface,
    onSurface = DarkInk,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = DarkMutedInk,
    outline = DarkOutline,
    outlineVariant = DarkOutlineSubtle,
    error = StatusCoral,
)

val StudioShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

@Composable
fun CkMonochromeTheme(
    customization: UiCustomizationConfig = UiCustomizationConfig(),
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (customization.themePreference) {
        ThemePreference.WHITE -> false
        ThemePreference.BLACK -> true
        ThemePreference.AUTO -> systemDark
    }

    val colorScheme = if (useDark) ObsidianColorScheme else WhiteLuxuryColorScheme
    val glassStyle = LiquidGlassStyle(
        isDark = useDark,
        glassAlpha = customization.glassTransparency,
        blurRadius = customization.glassBlurDp.dp,
        reflectionIntensity = customization.silverReflectionIntensity,
        cornerRadius = customization.cardCornerRadiusDp.dp,
        animationStyle = customization.animationStyle,
        animationSpeedMultiplier = customization.animationSpeedMultiplier,
        enable3dAutoOrbit = customization.enable3dAutoOrbit,
    )

    CompositionLocalProvider(LocalLiquidGlassStyle provides glassStyle) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = StudioShapes,
            content = content,
        )
    }
}
