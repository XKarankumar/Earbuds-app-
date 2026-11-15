package com.example.smartcool.ui.theme

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
import com.example.smartcool.model.CustomLiquidThemeConfig
import com.example.smartcool.model.SmartCoolThemePreset
import com.example.ui.theme.TheatreTypography

@Immutable
data class LiquidPalette(
    val preset: SmartCoolThemePreset,
    val isDark: Boolean,
    val backgroundTop: Color,
    val backgroundMid: Color,
    val backgroundBottom: Color,
    val glassSurface: Color,
    val glassElevated: Color,
    val specularTopBorder: Color,
    val borderStroke: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val coolCyan: Color,
    val silverChrome: Color,
    val successEmerald: Color,
    val warningAmber: Color,
    val dangerRed: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val navGlassSurface: Color,
    val orbAlpha: Float
)

val LocalLiquidPalette = staticCompositionLocalOf {
    buildLiquidPalette(SmartCoolThemePreset.IOS_LIQUID_GLASS, CustomLiquidThemeConfig())
}

fun buildLiquidPalette(
    preset: SmartCoolThemePreset,
    custom: CustomLiquidThemeConfig
): LiquidPalette {
    return when (preset) {
        SmartCoolThemePreset.IOS_LIQUID_GLASS -> LiquidPalette(
            preset = preset,
            isDark = false,
            backgroundTop = Color(0xFFF3F8FF),
            backgroundMid = Color(0xFFE7F0FA),
            backgroundBottom = Color(0xFFDCE7F5),
            glassSurface = Color(0xB8FFFFFF),
            glassElevated = Color(0xE0FFFFFF),
            specularTopBorder = Color(0xFFFFFFFF),
            borderStroke = Color(0x88BFD7ED),
            primaryAccent = Color(0xFF0284C7),
            secondaryAccent = Color(0xFF38BDF8),
            coolCyan = Color(0xFF0EA5E9),
            silverChrome = Color(0xFFCBD5E1),
            successEmerald = Color(0xFF059669),
            warningAmber = Color(0xFFD97706),
            dangerRed = Color(0xFFDC2626),
            textPrimary = Color(0xFF0F172A),
            textSecondary = Color(0xFF475569),
            navGlassSurface = Color(0xD9F8FBFF),
            orbAlpha = 0.42f
        )

        SmartCoolThemePreset.MIDNIGHT_AMOLED -> LiquidPalette(
            preset = preset,
            isDark = true,
            backgroundTop = Color(0xFF000000),
            backgroundMid = Color(0xFF030508),
            backgroundBottom = Color(0xFF070B12),
            glassSurface = Color(0xCC0D1119),
            glassElevated = Color(0xE6141A26),
            specularTopBorder = Color(0x4D94A3B8),
            borderStroke = Color(0x3838BDF8),
            primaryAccent = Color(0xFF38BDF8),
            secondaryAccent = Color(0xFF94A3B8),
            coolCyan = Color(0xFF0EA5E9),
            silverChrome = Color(0xFF64748B),
            successEmerald = Color(0xFF10B981),
            warningAmber = Color(0xFFF59E0B),
            dangerRed = Color(0xFFEF4444),
            textPrimary = Color(0xFFF8FAFC),
            textSecondary = Color(0xFF94A3B8),
            navGlassSurface = Color(0xEB05070B),
            orbAlpha = 0.24f
        )

        SmartCoolThemePreset.PEARL_LIGHT -> LiquidPalette(
            preset = preset,
            isDark = false,
            backgroundTop = Color(0xFFFCFCFD),
            backgroundMid = Color(0xFFF4F6FA),
            backgroundBottom = Color(0xFFEBEFF5),
            glassSurface = Color(0xCCFFFFFF),
            glassElevated = Color(0xF0FFFFFF),
            specularTopBorder = Color(0xFFFFFFFF),
            borderStroke = Color(0x99CBD5E1),
            primaryAccent = Color(0xFF2563EB),
            secondaryAccent = Color(0xFF60A5FA),
            coolCyan = Color(0xFF0EA5E9),
            silverChrome = Color(0xFFE2E8F0),
            successEmerald = Color(0xFF059669),
            warningAmber = Color(0xFFD97706),
            dangerRed = Color(0xFFDC2626),
            textPrimary = Color(0xFF111827),
            textSecondary = Color(0xFF4B5563),
            navGlassSurface = Color(0xE6FFFFFF),
            orbAlpha = 0.28f
        )

        SmartCoolThemePreset.ICE_BLUE -> LiquidPalette(
            preset = preset,
            isDark = false,
            backgroundTop = Color(0xFFE0F2FE),
            backgroundMid = Color(0xFFEDF8FF),
            backgroundBottom = Color(0xFFCDE9FD),
            glassSurface = Color(0xBFFFFFFF),
            glassElevated = Color(0xE6F4FBFF),
            specularTopBorder = Color(0xFFFFFFFF),
            borderStroke = Color(0x997DD3FC),
            primaryAccent = Color(0xFF0369A1),
            secondaryAccent = Color(0xFF0EA5E9),
            coolCyan = Color(0xFF06B6D4),
            silverChrome = Color(0xFFBAE6FD),
            successEmerald = Color(0xFF059669),
            warningAmber = Color(0xFFD97706),
            dangerRed = Color(0xFFDC2626),
            textPrimary = Color(0xFF082F49),
            textSecondary = Color(0xFF0C4A6E),
            navGlassSurface = Color(0xE0ECFAFF),
            orbAlpha = 0.50f
        )

        SmartCoolThemePreset.SILVER_GLASS -> LiquidPalette(
            preset = preset,
            isDark = false,
            backgroundTop = Color(0xFFE5EAF0),
            backgroundMid = Color(0xFFD6DDE6),
            backgroundBottom = Color(0xFFC5CFD9),
            glassSurface = Color(0xB8F8FAFC),
            glassElevated = Color(0xE6FFFFFF),
            specularTopBorder = Color(0xFFFFFFFF),
            borderStroke = Color(0xA694A3B8),
            primaryAccent = Color(0xFF1E40AF),
            secondaryAccent = Color(0xFF475569),
            coolCyan = Color(0xFF0284C7),
            silverChrome = Color(0xFFCBD5E1),
            successEmerald = Color(0xFF059669),
            warningAmber = Color(0xFFB45309),
            dangerRed = Color(0xFFDC2626),
            textPrimary = Color(0xFF0F172A),
            textSecondary = Color(0xFF334155),
            navGlassSurface = Color(0xE6EDF2F7),
            orbAlpha = 0.30f
        )

        SmartCoolThemePreset.OCEAN_BLUE -> LiquidPalette(
            preset = preset,
            isDark = true,
            backgroundTop = Color(0xFF041326),
            backgroundMid = Color(0xFF08203E),
            backgroundBottom = Color(0xFF0A2E57),
            glassSurface = Color(0xBD0D2B4E),
            glassElevated = Color(0xE0123863),
            specularTopBorder = Color(0x667DD3FC),
            borderStroke = Color(0x5538BDF8),
            primaryAccent = Color(0xFF38BDF8),
            secondaryAccent = Color(0xFF67E8F9),
            coolCyan = Color(0xFF22D3EE),
            silverChrome = Color(0xFF7DD3FC),
            successEmerald = Color(0xFF34D399),
            warningAmber = Color(0xFFFBBF24),
            dangerRed = Color(0xFFF87171),
            textPrimary = Color(0xFFF0F9FF),
            textSecondary = Color(0xFFBAE6FD),
            navGlassSurface = Color(0xEB061A33),
            orbAlpha = 0.45f
        )

        SmartCoolThemePreset.CUSTOM -> {
            val accent = Color(custom.accentColorHex)
            val secondary = Color(custom.secondaryAccentHex)
            val opacity = custom.glassOpacity.coerceIn(0.45f, 0.94f)
            val glow = custom.glowIntensity.coerceIn(0.10f, 0.95f)
            if (custom.isDarkBase) {
                LiquidPalette(
                    preset = preset,
                    isDark = true,
                    backgroundTop = Color(0xFF050A14),
                    backgroundMid = Color(0xFF0B1424),
                    backgroundBottom = Color(0xFF0F1B30),
                    glassSurface = Color(0xFF121C2E).copy(alpha = opacity),
                    glassElevated = Color(0xFF18243A).copy(alpha = (opacity + 0.12f).coerceAtMost(0.98f)),
                    specularTopBorder = accent.copy(alpha = 0.45f),
                    borderStroke = accent.copy(alpha = 0.38f),
                    primaryAccent = accent,
                    secondaryAccent = secondary,
                    coolCyan = accent,
                    silverChrome = Color(0xFF94A3B8),
                    successEmerald = Color(0xFF10B981),
                    warningAmber = Color(0xFFF59E0B),
                    dangerRed = Color(0xFFEF4444),
                    textPrimary = Color(0xFFF8FAFC),
                    textSecondary = Color(0xFFCBD5E1),
                    navGlassSurface = Color(0xEB09101D),
                    orbAlpha = glow * 0.55f
                )
            } else {
                LiquidPalette(
                    preset = preset,
                    isDark = false,
                    backgroundTop = Color(0xFFF3F7FC),
                    backgroundMid = Color(0xFFE8F0F8),
                    backgroundBottom = Color(0xFFDCE6F2),
                    glassSurface = Color.White.copy(alpha = opacity),
                    glassElevated = Color.White.copy(alpha = (opacity + 0.14f).coerceAtMost(0.98f)),
                    specularTopBorder = Color.White,
                    borderStroke = accent.copy(alpha = 0.42f),
                    primaryAccent = accent,
                    secondaryAccent = secondary,
                    coolCyan = accent,
                    silverChrome = Color(0xFFCBD5E1),
                    successEmerald = Color(0xFF059669),
                    warningAmber = Color(0xFFD97706),
                    dangerRed = Color(0xFFDC2626),
                    textPrimary = Color(0xFF0F172A),
                    textSecondary = Color(0xFF475569),
                    navGlassSurface = Color(0xE6F8FBFF),
                    orbAlpha = glow * 0.45f
                )
            }
        }
    }
}

@Composable
fun SmartCoolProTheme(
    preset: SmartCoolThemePreset,
    customConfig: CustomLiquidThemeConfig,
    content: @Composable () -> Unit
) {
    val raw = buildLiquidPalette(preset, customConfig)
    val animSpec = tween<Color>(durationMillis = 260)

    val bgTop by animateColorAsState(raw.backgroundTop, animSpec, label = "bgTop")
    val bgMid by animateColorAsState(raw.backgroundMid, animSpec, label = "bgMid")
    val bgBottom by animateColorAsState(raw.backgroundBottom, animSpec, label = "bgBottom")
    val glassSurface by animateColorAsState(raw.glassSurface, animSpec, label = "glassSurface")
    val glassElevated by animateColorAsState(raw.glassElevated, animSpec, label = "glassElevated")
    val borderStroke by animateColorAsState(raw.borderStroke, animSpec, label = "borderStroke")
    val primaryAccent by animateColorAsState(raw.primaryAccent, animSpec, label = "primaryAccent")
    val secondaryAccent by animateColorAsState(raw.secondaryAccent, animSpec, label = "secondaryAccent")
    val textPrimary by animateColorAsState(raw.textPrimary, animSpec, label = "textPrimary")
    val textSecondary by animateColorAsState(raw.textSecondary, animSpec, label = "textSecondary")
    val navGlass by animateColorAsState(raw.navGlassSurface, animSpec, label = "navGlass")

    val animated = raw.copy(
        backgroundTop = bgTop,
        backgroundMid = bgMid,
        backgroundBottom = bgBottom,
        glassSurface = glassSurface,
        glassElevated = glassElevated,
        borderStroke = borderStroke,
        primaryAccent = primaryAccent,
        secondaryAccent = secondaryAccent,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        navGlassSurface = navGlass
    )

    val colorScheme = if (animated.isDark) {
        darkColorScheme(
            primary = animated.primaryAccent,
            onPrimary = Color(0xFF041326),
            secondary = animated.secondaryAccent,
            background = animated.backgroundTop,
            onBackground = animated.textPrimary,
            surface = animated.glassElevated,
            onSurface = animated.textPrimary,
            surfaceVariant = animated.glassSurface,
            onSurfaceVariant = animated.textSecondary,
            outline = animated.borderStroke
        )
    } else {
        lightColorScheme(
            primary = animated.primaryAccent,
            onPrimary = Color.White,
            secondary = animated.secondaryAccent,
            background = animated.backgroundTop,
            onBackground = animated.textPrimary,
            surface = animated.glassElevated,
            onSurface = animated.textPrimary,
            surfaceVariant = animated.glassSurface,
            onSurfaceVariant = animated.textSecondary,
            outline = animated.borderStroke
        )
    }

    CompositionLocalProvider(LocalLiquidPalette provides animated) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TheatreTypography,
            content = content
        )
    }
}
