package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.example.model.ThemeMode

object FitnessColors {
    val PureAmoledBlack = Color(0xFF000000)
    val DeepObsidian = Color(0xFF08090C)
    val ElectricCyan = Color(0xFF00E5FF)
    val EmeraldPulse = Color(0xFF10B981)
    val AmberAlert = Color(0xFFF59E0B)
    val CrimsonRose = Color(0xFFFF3B5C)
    val PureWhite = Color(0xFFFFFFFF)
    val IceSilver = Color(0xFFE5E7EB)
    val CoolSlate = Color(0xFF94A3B8)
}

data class GlassPalette(
    val isDark: Boolean,
    val background: Color,
    val ambientGlowPrimary: Color,
    val ambientGlowSecondary: Color,
    val glassFill: Color,
    val glassElevatedFill: Color,
    val glassBorderTop: Color,
    val glassBorderBottom: Color,
    val accentCyan: Color,
    val accentEmerald: Color,
    val accentAmber: Color,
    val accentDanger: Color,
    val navSurface: Color,
    val primaryText: Color,
    val secondaryText: Color
)

object FitnessThemes {

    fun colorSchemeFor(mode: ThemeMode): ColorScheme = when (mode) {
        ThemeMode.AMOLED_DARK -> darkColorScheme(
            primary = FitnessColors.ElectricCyan,
            onPrimary = FitnessColors.PureAmoledBlack,
            primaryContainer = Color(0xFF0B2A33),
            onPrimaryContainer = Color(0xFFCFFAFE),
            secondary = FitnessColors.EmeraldPulse,
            onSecondary = FitnessColors.PureAmoledBlack,
            secondaryContainer = Color(0xFF072B20),
            onSecondaryContainer = Color(0xFFA7F3D0),
            tertiary = FitnessColors.PureWhite,
            onTertiary = FitnessColors.PureAmoledBlack,
            background = FitnessColors.PureAmoledBlack,
            onBackground = FitnessColors.PureWhite,
            surface = Color(0xFF0B0D11),
            onSurface = FitnessColors.PureWhite,
            surfaceVariant = Color(0xFF141820),
            onSurfaceVariant = Color(0xFFB6C2D1),
            outline = Color(0x33FFFFFF),
            error = FitnessColors.CrimsonRose,
            onError = FitnessColors.PureWhite
        )

        ThemeMode.LIGHT_GLASS -> lightColorScheme(
            primary = Color(0xFF0284C7),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE0F2FE),
            onPrimaryContainer = Color(0xFF082F49),
            secondary = Color(0xFF059669),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFD1FAE5),
            onSecondaryContainer = Color(0xFF064E3B),
            tertiary = Color(0xFF0F172A),
            onTertiary = Color.White,
            background = Color(0xFFF3F6FA),
            onBackground = Color(0xFF0A0D14),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF0A0D14),
            surfaceVariant = Color(0xFFE8EEF5),
            onSurfaceVariant = Color(0xFF475569),
            outline = Color(0xFFCBD5E1),
            error = Color(0xFFE11D48),
            onError = Color.White
        )
    }

    fun paletteFor(mode: ThemeMode): GlassPalette = when (mode) {
        ThemeMode.AMOLED_DARK -> GlassPalette(
            isDark = true,
            background = FitnessColors.PureAmoledBlack,
            ambientGlowPrimary = Color(0x2800E5FF),
            ambientGlowSecondary = Color(0x2210B981),
            glassFill = Color(0x14FFFFFF),
            glassElevatedFill = Color(0x20FFFFFF),
            glassBorderTop = Color(0x42FFFFFF),
            glassBorderBottom = Color(0x14FFFFFF),
            accentCyan = FitnessColors.ElectricCyan,
            accentEmerald = FitnessColors.EmeraldPulse,
            accentAmber = FitnessColors.AmberAlert,
            accentDanger = FitnessColors.CrimsonRose,
            navSurface = Color(0xE8060709),
            primaryText = FitnessColors.PureWhite,
            secondaryText = Color(0xFFB4C0CF)
        )

        ThemeMode.LIGHT_GLASS -> GlassPalette(
            isDark = false,
            background = Color(0xFFF3F6FA),
            ambientGlowPrimary = Color(0x220284C7),
            ambientGlowSecondary = Color(0x1C10B981),
            glassFill = Color(0xD6FFFFFF),
            glassElevatedFill = Color(0xF5FFFFFF),
            glassBorderTop = Color(0xFFFFFFFF),
            glassBorderBottom = Color(0x220F172A),
            accentCyan = Color(0xFF0284C7),
            accentEmerald = Color(0xFF059669),
            accentAmber = Color(0xFFD97706),
            accentDanger = Color(0xFFE11D48),
            navSurface = Color(0xF0FFFFFF),
            primaryText = Color(0xFF0A0D14),
            secondaryText = Color(0xFF475569)
        )
    }
}
