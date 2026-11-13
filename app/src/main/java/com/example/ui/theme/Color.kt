package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.example.model.OceanEmeraldMode

object OceanEmeraldColors {
    val DeepOceanBlue = Color(0xFF071B33)
    val OceanBlue = Color(0xFF0878D1)
    val EmeraldGreen = Color(0xFF00A878)
    val MintGreen = Color(0xFF69E6B4)
    val GlassWhite = Color(0xFFF5FFFC)

    // Supporting tonal variants for Blue-to-Emerald harmony
    val DeepTeal = Color(0xFF062C38)
    val MidnightOcean = Color(0xFF0B2747)
    val SoftSeafoam = Color(0xFFE1F7F0)
    val SoftSkyTint = Color(0xFFDCEEFF)
    val CoralAlert = Color(0xFFFF6B81)
    val AmberHighlight = Color(0xFFFBBF24)
}

data class OceanEmeraldPalette(
    val isDark: Boolean,
    val bgTop: Color,
    val bgMiddle: Color,
    val bgBottom: Color,
    val ambientOrbOcean: Color,
    val ambientOrbMint: Color,
    val glassCardFill: Color,
    val glassCardElevatedFill: Color,
    val glassBorderHighlight: Color,
    val glassBorderAccent: Color,
    val glowShadowColor: Color,
    val numberKeyFill: Color,
    val numberKeyBorder: Color,
    val numberKeyText: Color,
    val operatorKeyFill: Color,
    val operatorKeyBorder: Color,
    val operatorKeyText: Color,
    val utilityKeyFill: Color,
    val utilityKeyText: Color,
    val equalsStart: Color,
    val equalsEnd: Color,
    val jamaAccent: Color,
    val udhaarAccent: Color,
    val warningAccent: Color,
    val dangerAccent: Color,
    val navBarSurface: Color
)

object OceanEmeraldThemes {

    fun colorSchemeFor(mode: OceanEmeraldMode): ColorScheme = when (mode) {
        OceanEmeraldMode.DARK -> darkColorScheme(
            primary = OceanEmeraldColors.OceanBlue,
            onPrimary = OceanEmeraldColors.GlassWhite,
            primaryContainer = Color(0xFF0D3B66),
            onPrimaryContainer = Color(0xFFD6ECFF),
            secondary = OceanEmeraldColors.EmeraldGreen,
            onSecondary = OceanEmeraldColors.GlassWhite,
            secondaryContainer = Color(0xFF054A39),
            onSecondaryContainer = OceanEmeraldColors.MintGreen,
            tertiary = OceanEmeraldColors.MintGreen,
            onTertiary = OceanEmeraldColors.DeepOceanBlue,
            background = OceanEmeraldColors.DeepOceanBlue,
            onBackground = OceanEmeraldColors.GlassWhite,
            surface = Color(0xFF0B2542),
            onSurface = OceanEmeraldColors.GlassWhite,
            surfaceVariant = Color(0xFF103252),
            onSurfaceVariant = Color(0xFFAAD6D0),
            outline = Color(0xFF28587B),
            error = OceanEmeraldColors.CoralAlert,
            onError = Color(0xFF3E0412)
        )

        OceanEmeraldMode.LIGHT -> lightColorScheme(
            primary = OceanEmeraldColors.OceanBlue,
            onPrimary = OceanEmeraldColors.GlassWhite,
            primaryContainer = Color(0xFFD6ECFF),
            onPrimaryContainer = OceanEmeraldColors.DeepOceanBlue,
            secondary = OceanEmeraldColors.EmeraldGreen,
            onSecondary = OceanEmeraldColors.GlassWhite,
            secondaryContainer = Color(0xFFD3F8EA),
            onSecondaryContainer = Color(0xFF023D2C),
            tertiary = Color(0xFF0B8A6A),
            onTertiary = OceanEmeraldColors.GlassWhite,
            background = OceanEmeraldColors.GlassWhite,
            onBackground = OceanEmeraldColors.DeepOceanBlue,
            surface = OceanEmeraldColors.GlassWhite,
            onSurface = OceanEmeraldColors.DeepOceanBlue,
            surfaceVariant = Color(0xFFE0F3EE),
            onSurfaceVariant = Color(0xFF2F546E),
            outline = Color(0xFF9AC9C6),
            error = Color(0xFFD92D53),
            onError = Color.White
        )
    }

    fun paletteFor(mode: OceanEmeraldMode): OceanEmeraldPalette = when (mode) {
        OceanEmeraldMode.DARK -> OceanEmeraldPalette(
            isDark = true,
            bgTop = OceanEmeraldColors.DeepOceanBlue,
            bgMiddle = Color(0xFF07283D),
            bgBottom = Color(0xFF05333A),
            ambientOrbOcean = OceanEmeraldColors.OceanBlue.copy(alpha = 0.34f),
            ambientOrbMint = OceanEmeraldColors.MintGreen.copy(alpha = 0.22f),
            glassCardFill = Color(0xB80B2745),
            glassCardElevatedFill = Color(0xD90E3254),
            glassBorderHighlight = OceanEmeraldColors.MintGreen.copy(alpha = 0.42f),
            glassBorderAccent = OceanEmeraldColors.OceanBlue.copy(alpha = 0.45f),
            glowShadowColor = OceanEmeraldColors.EmeraldGreen.copy(alpha = 0.28f),
            numberKeyFill = OceanEmeraldColors.OceanBlue.copy(alpha = 0.28f),
            numberKeyBorder = OceanEmeraldColors.OceanBlue.copy(alpha = 0.55f),
            numberKeyText = OceanEmeraldColors.GlassWhite,
            operatorKeyFill = OceanEmeraldColors.EmeraldGreen.copy(alpha = 0.30f),
            operatorKeyBorder = OceanEmeraldColors.MintGreen.copy(alpha = 0.60f),
            operatorKeyText = OceanEmeraldColors.MintGreen,
            utilityKeyFill = Color(0x40F5FFFC),
            utilityKeyText = OceanEmeraldColors.GlassWhite,
            equalsStart = OceanEmeraldColors.OceanBlue,
            equalsEnd = OceanEmeraldColors.EmeraldGreen,
            jamaAccent = OceanEmeraldColors.MintGreen,
            udhaarAccent = Color(0xFF56B4FF),
            warningAccent = OceanEmeraldColors.AmberHighlight,
            dangerAccent = OceanEmeraldColors.CoralAlert,
            navBarSurface = Color(0xE0061B31)
        )

        OceanEmeraldMode.LIGHT -> OceanEmeraldPalette(
            isDark = false,
            bgTop = OceanEmeraldColors.GlassWhite,
            bgMiddle = OceanEmeraldColors.SoftSkyTint,
            bgBottom = OceanEmeraldColors.SoftSeafoam,
            ambientOrbOcean = OceanEmeraldColors.OceanBlue.copy(alpha = 0.18f),
            ambientOrbMint = OceanEmeraldColors.EmeraldGreen.copy(alpha = 0.18f),
            glassCardFill = Color(0xD4F5FFFC),
            glassCardElevatedFill = Color(0xF0FFFFFF),
            glassBorderHighlight = Color.White.copy(alpha = 0.95f),
            glassBorderAccent = OceanEmeraldColors.EmeraldGreen.copy(alpha = 0.35f),
            glowShadowColor = OceanEmeraldColors.OceanBlue.copy(alpha = 0.16f),
            numberKeyFill = OceanEmeraldColors.OceanBlue,
            numberKeyBorder = OceanEmeraldColors.GlassWhite.copy(alpha = 0.55f),
            numberKeyText = OceanEmeraldColors.GlassWhite,
            operatorKeyFill = OceanEmeraldColors.EmeraldGreen,
            operatorKeyBorder = OceanEmeraldColors.MintGreen.copy(alpha = 0.75f),
            operatorKeyText = OceanEmeraldColors.GlassWhite,
            utilityKeyFill = Color(0xFFE0F2FE),
            utilityKeyText = OceanEmeraldColors.DeepOceanBlue,
            equalsStart = OceanEmeraldColors.OceanBlue,
            equalsEnd = OceanEmeraldColors.EmeraldGreen,
            jamaAccent = OceanEmeraldColors.EmeraldGreen,
            udhaarAccent = OceanEmeraldColors.OceanBlue,
            warningAccent = Color(0xFFD97706),
            dangerAccent = Color(0xFFD92D53),
            navBarSurface = Color(0xEBF5FFFC)
        )
    }
}
