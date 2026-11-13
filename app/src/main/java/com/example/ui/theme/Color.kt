package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.example.model.AppThemeId

data class GlassPalette(
    val backgroundStart: Color,
    val backgroundEnd: Color,
    val ambientOrbPrimary: Color,
    val ambientOrbSecondary: Color,
    val glassSurface: Color,
    val glassSurfaceElevated: Color,
    val glassBorderTopLeft: Color,
    val glassBorderBottomRight: Color,
    val safeAccent: Color,
    val warningAccent: Color,
    val dangerAccent: Color,
    val infoAccent: Color,
    val swatchColors: List<Color>
)

object AppThemes {

    fun colorSchemeFor(themeId: AppThemeId): ColorScheme = when (themeId) {
        AppThemeId.DARK_3D_GLASS -> darkColorScheme(
            primary = Color(0xFF38BDF8),
            onPrimary = Color(0xFF042438),
            primaryContainer = Color(0xFF0C4A6E),
            onPrimaryContainer = Color(0xFFBAE6FD),
            secondary = Color(0xFF10B981),
            onSecondary = Color(0xFF032D1E),
            secondaryContainer = Color(0xFF064E3B),
            onSecondaryContainer = Color(0xFFA7F3D0),
            tertiary = Color(0xFFF59E0B),
            background = Color(0xFF060A14),
            onBackground = Color(0xFFF1F5F9),
            surface = Color(0xFF0D1526),
            onSurface = Color(0xFFF1F5F9),
            surfaceVariant = Color(0xFF16213A),
            onSurfaceVariant = Color(0xFF94A3B8),
            outline = Color(0xFF334155),
            error = Color(0xFFFB7185),
            onError = Color(0xFF4C0519)
        )

        AppThemeId.WHITE_GLASS -> lightColorScheme(
            primary = Color(0xFF0284C7),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFE0F2FE),
            onPrimaryContainer = Color(0xFF0C4A6E),
            secondary = Color(0xFF059669),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFD1FAE5),
            onSecondaryContainer = Color(0xFF064E3B),
            tertiary = Color(0xFFD97706),
            background = Color(0xFFF2F6FC),
            onBackground = Color(0xFF0F172A),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFFE2E8F0),
            onSurfaceVariant = Color(0xFF475569),
            outline = Color(0xFFCBD5E1),
            error = Color(0xFFE11D48),
            onError = Color(0xFFFFFFFF)
        )

        AppThemeId.CYBER_AURORA -> darkColorScheme(
            primary = Color(0xFF818CF8),
            onPrimary = Color(0xFF1E1B4B),
            primaryContainer = Color(0xFF312E81),
            onPrimaryContainer = Color(0xFFE0E7FF),
            secondary = Color(0xFF22D3EE),
            onSecondary = Color(0xFF042F38),
            secondaryContainer = Color(0xFF164E63),
            onSecondaryContainer = Color(0xFFCFFAFE),
            tertiary = Color(0xFF34D399),
            background = Color(0xFF080A1A),
            onBackground = Color(0xFFEEF2FF),
            surface = Color(0xFF101531),
            onSurface = Color(0xFFEEF2FF),
            surfaceVariant = Color(0xFF1A2247),
            onSurfaceVariant = Color(0xFFA5B4FC),
            outline = Color(0xFF3730A3),
            error = Color(0xFFFB7185),
            onError = Color(0xFF4C0519)
        )
    }

    fun glassPaletteFor(themeId: AppThemeId): GlassPalette = when (themeId) {
        AppThemeId.DARK_3D_GLASS -> GlassPalette(
            backgroundStart = Color(0xFF060A14),
            backgroundEnd = Color(0xFF0B162C),
            ambientOrbPrimary = Color(0x3D38BDF8),
            ambientOrbSecondary = Color(0x2E10B981),
            glassSurface = Color(0xB30F1A30),
            glassSurfaceElevated = Color(0xD4162645),
            glassBorderTopLeft = Color(0x5CE0F2FE),
            glassBorderBottomRight = Color(0x1F10B981),
            safeAccent = Color(0xFF34D399),
            warningAccent = Color(0xFFFBBF24),
            dangerAccent = Color(0xFFFB7185),
            infoAccent = Color(0xFF38BDF8),
            swatchColors = listOf(Color(0xFF060A14), Color(0xFF38BDF8), Color(0xFF10B981))
        )

        AppThemeId.WHITE_GLASS -> GlassPalette(
            backgroundStart = Color(0xFFF1F5FB),
            backgroundEnd = Color(0xFFE3ECF8),
            ambientOrbPrimary = Color(0x300284C7),
            ambientOrbSecondary = Color(0x28059669),
            glassSurface = Color(0xCCFFFFFF),
            glassSurfaceElevated = Color(0xEEFFFFFF),
            glassBorderTopLeft = Color(0xFFFFFFFF),
            glassBorderBottomRight = Color(0x360284C7),
            safeAccent = Color(0xFF059669),
            warningAccent = Color(0xFFD97706),
            dangerAccent = Color(0xFFE11D48),
            infoAccent = Color(0xFF0284C7),
            swatchColors = listOf(Color(0xFFF4F7FB), Color(0xFF0284C7), Color(0xFF059669))
        )

        AppThemeId.CYBER_AURORA -> GlassPalette(
            backgroundStart = Color(0xFF080A1A),
            backgroundEnd = Color(0xFF14183C),
            ambientOrbPrimary = Color(0x3D818CF8),
            ambientOrbSecondary = Color(0x2B22D3EE),
            glassSurface = Color(0xB312183B),
            glassSurfaceElevated = Color(0xD41B2454),
            glassBorderTopLeft = Color(0x52E0E7FF),
            glassBorderBottomRight = Color(0x2222D3EE),
            safeAccent = Color(0xFF34D399),
            warningAccent = Color(0xFFFBBF24),
            dangerAccent = Color(0xFFFB7185),
            infoAccent = Color(0xFF22D3EE),
            swatchColors = listOf(Color(0xFF080A1A), Color(0xFF818CF8), Color(0xFF22D3EE))
        )
    }
}
