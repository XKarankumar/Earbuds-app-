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
    val udhaarAccent: Color,
    val jamaAccent: Color,
    val warningAccent: Color,
    val swatchColors: List<Color>
)

object AppThemes {

    fun colorSchemeFor(themeId: AppThemeId): ColorScheme = when (themeId) {
        AppThemeId.OBSIDIAN_EMERALD -> darkColorScheme(
            primary = Color(0xFF10B981),
            onPrimary = Color(0xFF042F1E),
            primaryContainer = Color(0xFF064E3B),
            onPrimaryContainer = Color(0xFFA7F3D0),
            secondary = Color(0xFF38BDF8),
            onSecondary = Color(0xFF082F49),
            secondaryContainer = Color(0xFF0C4A6E),
            onSecondaryContainer = Color(0xFFBAE6FD),
            tertiary = Color(0xFFF59E0B),
            background = Color(0xFF070B14),
            onBackground = Color(0xFFF1F5F9),
            surface = Color(0xFF0E1526),
            onSurface = Color(0xFFF1F5F9),
            surfaceVariant = Color(0xFF172036),
            onSurfaceVariant = Color(0xFF94A3B8),
            outline = Color(0xFF334155),
            error = Color(0xFFFB7185),
            onError = Color(0xFF4C0519)
        )

        AppThemeId.ALABASTER_IVORY -> lightColorScheme(
            primary = Color(0xFF059669),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFD1FAE5),
            onPrimaryContainer = Color(0xFF064E3B),
            secondary = Color(0xFF0284C7),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFE0F2FE),
            onSecondaryContainer = Color(0xFF0C4A6E),
            tertiary = Color(0xFFD97706),
            background = Color(0xFFF4F7FB),
            onBackground = Color(0xFF0F172A),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFFE2E8F0),
            onSurfaceVariant = Color(0xFF475569),
            outline = Color(0xFFCBD5E1),
            error = Color(0xFFE11D48),
            onError = Color(0xFFFFFFFF)
        )

        AppThemeId.ROYAL_INDIGO -> darkColorScheme(
            primary = Color(0xFF818CF8),
            onPrimary = Color(0xFF1E1B4B),
            primaryContainer = Color(0xFF312E81),
            onPrimaryContainer = Color(0xFFE0E7FF),
            secondary = Color(0xFFF472B6),
            onSecondary = Color(0xFF500724),
            secondaryContainer = Color(0xFF831843),
            onSecondaryContainer = Color(0xFFFCE7F3),
            tertiary = Color(0xFF34D399),
            background = Color(0xFF090D1F),
            onBackground = Color(0xFFEEF2FF),
            surface = Color(0xFF111836),
            onSurface = Color(0xFFEEF2FF),
            surfaceVariant = Color(0xFF1C254D),
            onSurfaceVariant = Color(0xFFA5B4FC),
            outline = Color(0xFF3730A3),
            error = Color(0xFFFB7185),
            onError = Color(0xFF4C0519)
        )

        AppThemeId.SAFFRON_LEDGER -> lightColorScheme(
            primary = Color(0xFFC2410C),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFFEDD5),
            onPrimaryContainer = Color(0xFF7C2D12),
            secondary = Color(0xFF0F766E),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFCCFBF1),
            onSecondaryContainer = Color(0xFF134E4A),
            tertiary = Color(0xFFB45309),
            background = Color(0xFFFDF8F0),
            onBackground = Color(0xFF291B0E),
            surface = Color(0xFFFFFCF7),
            onSurface = Color(0xFF291B0E),
            surfaceVariant = Color(0xFFF3E8D8),
            onSurfaceVariant = Color(0xFF6B4F35),
            outline = Color(0xFFD6C3AA),
            error = Color(0xFFDC2626),
            onError = Color(0xFFFFFFFF)
        )

        AppThemeId.CYBER_SLATE -> darkColorScheme(
            primary = Color(0xFF22D3EE),
            onPrimary = Color(0xFF042F38),
            primaryContainer = Color(0xFF164E63),
            onPrimaryContainer = Color(0xFFCFFAFE),
            secondary = Color(0xFFA3E635),
            onSecondary = Color(0xFF1A2E05),
            secondaryContainer = Color(0xFF365314),
            onSecondaryContainer = Color(0xFFECFCCB),
            tertiary = Color(0xFFFACC15),
            background = Color(0xFF06080A),
            onBackground = Color(0xFFECFEFF),
            surface = Color(0xFF0F1318),
            onSurface = Color(0xFFECFEFF),
            surfaceVariant = Color(0xFF182028),
            onSurfaceVariant = Color(0xFF9CA3AF),
            outline = Color(0xFF2B3744),
            error = Color(0xFFF43F5E),
            onError = Color(0xFF4C0519)
        )
    }

    fun glassPaletteFor(themeId: AppThemeId): GlassPalette = when (themeId) {
        AppThemeId.OBSIDIAN_EMERALD -> GlassPalette(
            backgroundStart = Color(0xFF070B14),
            backgroundEnd = Color(0xFF0C1628),
            ambientOrbPrimary = Color(0x3810B981),
            ambientOrbSecondary = Color(0x2B38BDF8),
            glassSurface = Color(0xAD111B2E),
            glassSurfaceElevated = Color(0xCC172540),
            glassBorderTopLeft = Color(0x4DFFFFFF),
            glassBorderBottomRight = Color(0x1410B981),
            udhaarAccent = Color(0xFFFB7185),
            jamaAccent = Color(0xFF34D399),
            warningAccent = Color(0xFFFBBF24),
            swatchColors = listOf(Color(0xFF070B14), Color(0xFF10B981), Color(0xFF38BDF8))
        )

        AppThemeId.ALABASTER_IVORY -> GlassPalette(
            backgroundStart = Color(0xFFF2F6FC),
            backgroundEnd = Color(0xFFE5EDF8),
            ambientOrbPrimary = Color(0x2E059669),
            ambientOrbSecondary = Color(0x280284C7),
            glassSurface = Color(0xCCFFFFFF),
            glassSurfaceElevated = Color(0xEBFFFFFF),
            glassBorderTopLeft = Color(0xFFFFFFFF),
            glassBorderBottomRight = Color(0x33059669),
            udhaarAccent = Color(0xFFE11D48),
            jamaAccent = Color(0xFF059669),
            warningAccent = Color(0xFFD97706),
            swatchColors = listOf(Color(0xFFF4F7FB), Color(0xFF059669), Color(0xFF0284C7))
        )

        AppThemeId.ROYAL_INDIGO -> GlassPalette(
            backgroundStart = Color(0xFF090D1F),
            backgroundEnd = Color(0xFF14183B),
            ambientOrbPrimary = Color(0x3D818CF8),
            ambientOrbSecondary = Color(0x2DF472B6),
            glassSurface = Color(0xAE141C42),
            glassSurfaceElevated = Color(0xCE1D285A),
            glassBorderTopLeft = Color(0x47E0E7FF),
            glassBorderBottomRight = Color(0x1FF472B6),
            udhaarAccent = Color(0xFFF472B6),
            jamaAccent = Color(0xFF34D399),
            warningAccent = Color(0xFFFBBF24),
            swatchColors = listOf(Color(0xFF090D1F), Color(0xFF818CF8), Color(0xFFF472B6))
        )

        AppThemeId.SAFFRON_LEDGER -> GlassPalette(
            backgroundStart = Color(0xFFFDF8F0),
            backgroundEnd = Color(0xFFF7EBDA),
            ambientOrbPrimary = Color(0x29EA580C),
            ambientOrbSecondary = Color(0x240F766E),
            glassSurface = Color(0xCEFFFDF9),
            glassSurfaceElevated = Color(0xEDFFFFFF),
            glassBorderTopLeft = Color(0xFFFFFFFF),
            glassBorderBottomRight = Color(0x33C2410C),
            udhaarAccent = Color(0xFFDC2626),
            jamaAccent = Color(0xFF0F766E),
            warningAccent = Color(0xFFB45309),
            swatchColors = listOf(Color(0xFFFDF8F0), Color(0xFFC2410C), Color(0xFF0F766E))
        )

        AppThemeId.CYBER_SLATE -> GlassPalette(
            backgroundStart = Color(0xFF05070A),
            backgroundEnd = Color(0xFF0B1117),
            ambientOrbPrimary = Color(0x3322D3EE),
            ambientOrbSecondary = Color(0x26A3E635),
            glassSurface = Color(0xB30F151D),
            glassSurfaceElevated = Color(0xD117202B),
            glassBorderTopLeft = Color(0x4722D3EE),
            glassBorderBottomRight = Color(0x1CA3E635),
            udhaarAccent = Color(0xFFF43F5E),
            jamaAccent = Color(0xFFA3E635),
            warningAccent = Color(0xFFFACC15),
            swatchColors = listOf(Color(0xFF05070A), Color(0xFF22D3EE), Color(0xFFA3E635))
        )
    }
}
