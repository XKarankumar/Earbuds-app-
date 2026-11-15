package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.hometheatre.core.CustomThemeConfig
import com.example.hometheatre.core.ThemePreset

@Immutable
data class StudioPalette(
    val preset: ThemePreset,
    val isDark: Boolean,
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val surfaceGlass: Color,
    val surfaceElevated: Color,
    val borderStroke: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val warningAmber: Color,
    val successEmerald: Color,
    val dangerRed: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val navSurface: Color,
    val glowAlpha: Float
)

val LocalStudioPalette = staticCompositionLocalOf {
    buildStudioPalette(ThemePreset.PREMIUM_DARK, CustomThemeConfig(), null)
}

fun buildStudioPalette(
    preset: ThemePreset,
    custom: CustomThemeConfig,
    liveRgbAccent: Color?
): StudioPalette {
    return when (preset) {
        ThemePreset.PREMIUM_DARK -> StudioPalette(
            preset = preset,
            isDark = true,
            backgroundTop = Color(0xFF07090F),
            backgroundBottom = Color(0xFF0D121E),
            surfaceGlass = Color(0xCC121826),
            surfaceElevated = Color(0xE6192234),
            borderStroke = Color(0x3300E5FF),
            primaryAccent = Color(0xFF00E5FF),
            secondaryAccent = Color(0xFF8B5CF6),
            warningAmber = Color(0xFFF59E0B),
            successEmerald = Color(0xFF10B981),
            dangerRed = Color(0xFFEF4444),
            textPrimary = Color(0xFFF8FAFC),
            textSecondary = Color(0xFF94A3B8),
            navSurface = Color(0xF00B0F19),
            glowAlpha = 0.35f
        )
        ThemePreset.MONOCHROME -> StudioPalette(
            preset = preset,
            isDark = true,
            backgroundTop = Color(0xFF000000),
            backgroundBottom = Color(0xFF09090B),
            surfaceGlass = Color(0xD9111114),
            surfaceElevated = Color(0xEB1C1C21),
            borderStroke = Color(0x40FFFFFF),
            primaryAccent = Color(0xFFFFFFFF),
            secondaryAccent = Color(0xFFA1A1AA),
            warningAmber = Color(0xFFE4E4E7),
            successEmerald = Color(0xFFF4F4F5),
            dangerRed = Color(0xFFDC2626),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFA1A1AA),
            navSurface = Color(0xF5050505),
            glowAlpha = 0.18f
        )
        ThemePreset.SILVER_GLASS -> StudioPalette(
            preset = preset,
            isDark = false,
            backgroundTop = Color(0xFFE2E8F0),
            backgroundBottom = Color(0xFFCBD5E1),
            surfaceGlass = Color(0xBFFFFFFF),
            surfaceElevated = Color(0xE6F8FAFC),
            borderStroke = Color(0x9994A3B8),
            primaryAccent = Color(0xFF0284C7),
            secondaryAccent = Color(0xFF475569),
            warningAmber = Color(0xFFD97706),
            successEmerald = Color(0xFF059669),
            dangerRed = Color(0xFFDC2626),
            textPrimary = Color(0xFF0F172A),
            textSecondary = Color(0xFF475569),
            navSurface = Color(0xEBEEF2F6),
            glowAlpha = 0.22f
        )
        ThemePreset.RGB_NEON -> {
            val dynamicPrimary = liveRgbAccent ?: Color(0xFF00F5D4)
            StudioPalette(
                preset = preset,
                isDark = true,
                backgroundTop = Color(0xFF070512),
                backgroundBottom = Color(0xFF130924),
                surfaceGlass = Color(0xCC16102B),
                surfaceElevated = Color(0xEB1F163D),
                borderStroke = dynamicPrimary.copy(alpha = 0.45f),
                primaryAccent = dynamicPrimary,
                secondaryAccent = Color(0xFFFF2A85),
                warningAmber = Color(0xFFFBBF24),
                successEmerald = Color(0xFF22D3EE),
                dangerRed = Color(0xFFFF3366),
                textPrimary = Color(0xFFFDF4FF),
                textSecondary = Color(0xFFC4B5FD),
                navSurface = Color(0xF20D081D),
                glowAlpha = 0.55f
            )
        }
        ThemePreset.CUSTOM -> {
            val bg = Color(custom.backgroundHex)
            val surface = Color(custom.surfaceHex).copy(alpha = custom.glassOpacity.coerceIn(0.45f, 0.96f))
            val primary = Color(custom.primaryAccentHex)
            val secondary = Color(custom.secondaryAccentHex)
            val text = Color(custom.textPrimaryHex)
            val isDarkBg = (0.299f * bg.red + 0.587f * bg.green + 0.114f * bg.blue) < 0.5f
            StudioPalette(
                preset = preset,
                isDark = isDarkBg,
                backgroundTop = bg,
                backgroundBottom = bg.copy(
                    red = (bg.red * 0.85f).coerceIn(0f, 1f),
                    green = (bg.green * 0.85f).coerceIn(0f, 1f),
                    blue = (bg.blue * 0.85f).coerceIn(0f, 1f)
                ),
                surfaceGlass = surface,
                surfaceElevated = Color(custom.surfaceHex),
                borderStroke = primary.copy(alpha = 0.35f),
                primaryAccent = primary,
                secondaryAccent = secondary,
                warningAmber = Color(0xFFF59E0B),
                successEmerald = Color(0xFF10B981),
                dangerRed = Color(0xFFEF4444),
                textPrimary = text,
                textSecondary = text.copy(alpha = 0.68f),
                navSurface = Color(custom.surfaceHex).copy(alpha = 0.94f),
                glowAlpha = custom.glowIntensity.coerceIn(0.1f, 0.8f) * 0.5f
            )
        }
    }
}

@Composable
fun HomeTheatreProTheme(
    preset: ThemePreset,
    customConfig: CustomThemeConfig,
    liveRgbAccent: Color? = null,
    content: @Composable () -> Unit
) {
    val raw = buildStudioPalette(preset, customConfig, liveRgbAccent)
    val animSpec = tween<Color>(durationMillis = 280)

    val bgTop by animateColorAsState(raw.backgroundTop, animSpec, label = "bgTop")
    val bgBottom by animateColorAsState(raw.backgroundBottom, animSpec, label = "bgBottom")
    val surfaceGlass by animateColorAsState(raw.surfaceGlass, animSpec, label = "surfaceGlass")
    val surfaceElevated by animateColorAsState(raw.surfaceElevated, animSpec, label = "surfaceElevated")
    val primaryAccent by animateColorAsState(raw.primaryAccent, animSpec, label = "primaryAccent")
    val secondaryAccent by animateColorAsState(raw.secondaryAccent, animSpec, label = "secondaryAccent")
    val textPrimary by animateColorAsState(raw.textPrimary, animSpec, label = "textPrimary")
    val textSecondary by animateColorAsState(raw.textSecondary, animSpec, label = "textSecondary")
    val navSurface by animateColorAsState(raw.navSurface, animSpec, label = "navSurface")

    val animatedPalette = raw.copy(
        backgroundTop = bgTop,
        backgroundBottom = bgBottom,
        surfaceGlass = surfaceGlass,
        surfaceElevated = surfaceElevated,
        primaryAccent = primaryAccent,
        secondaryAccent = secondaryAccent,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        navSurface = navSurface
    )

    val colorScheme = if (animatedPalette.isDark) {
        darkColorScheme(
            primary = animatedPalette.primaryAccent,
            onPrimary = Color.Black,
            secondary = animatedPalette.secondaryAccent,
            background = animatedPalette.backgroundTop,
            onBackground = animatedPalette.textPrimary,
            surface = animatedPalette.surfaceElevated,
            onSurface = animatedPalette.textPrimary,
            surfaceVariant = animatedPalette.surfaceGlass,
            onSurfaceVariant = animatedPalette.textSecondary,
            outline = animatedPalette.borderStroke
        )
    } else {
        lightColorScheme(
            primary = animatedPalette.primaryAccent,
            onPrimary = Color.White,
            secondary = animatedPalette.secondaryAccent,
            background = animatedPalette.backgroundTop,
            onBackground = animatedPalette.textPrimary,
            surface = animatedPalette.surfaceElevated,
            onSurface = animatedPalette.textPrimary,
            surfaceVariant = animatedPalette.surfaceGlass,
            onSurfaceVariant = animatedPalette.textSecondary,
            outline = animatedPalette.borderStroke
        )
    }

    CompositionLocalProvider(LocalStudioPalette provides animatedPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TheatreTypography,
            content = content
        )
    }
}
