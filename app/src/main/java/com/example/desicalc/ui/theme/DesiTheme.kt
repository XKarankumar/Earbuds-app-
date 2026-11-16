package com.example.desicalc.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.desicalc.model.AppSettingsState
import com.example.desicalc.model.AppThemePreset
import com.example.desicalc.model.CustomBackgroundStyle

val DisplayFontFamily = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Normal),
    Font(R.font.space_grotesk, FontWeight.SemiBold),
    Font(R.font.space_grotesk, FontWeight.Bold)
)

val HeadingFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

val BodyFontFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold)
)

val MonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.Bold)
)

val DesiTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = HeadingFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = HeadingFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = HeadingFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    labelLarge = TextStyle(
        fontFamily = HeadingFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

val DesiShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Immutable
data class DesiExtendedColors(
    val isDark: Boolean,
    val gradientTop: Color,
    val gradientBottom: Color,
    val cardElevated: Color,
    val cardBorder: Color,
    val udharRed: Color,
    val jamaGreen: Color,
    val goldAccent: Color
)

val LocalDesiColors = staticCompositionLocalOf {
    DesiExtendedColors(
        isDark = true,
        gradientTop = Color(0xFF0B1324),
        gradientBottom = Color(0xFF101C33),
        cardElevated = Color(0xFF162238),
        cardBorder = Color(0xFF28395A),
        udharRed = Color(0xFFF87171),
        jamaGreen = Color(0xFF34D399),
        goldAccent = Color(0xFFF59E0B)
    )
}

@Composable
fun DesiCalculatorTheme(
    settings: AppSettingsState,
    content: @Composable () -> Unit
) {
    val (scheme, ext) = resolveThemeColors(settings)

    CompositionLocalProvider(LocalDesiColors provides ext) {
        MaterialTheme(
            colorScheme = scheme,
            typography = DesiTypography,
            shapes = DesiShapes,
            content = content
        )
    }
}

private fun resolveThemeColors(settings: AppSettingsState): Pair<ColorScheme, DesiExtendedColors> {
    return when (settings.themePreset) {
        AppThemePreset.PREMIUM_DARK -> {
            darkColorScheme(
                primary = Color(0xFFF59E0B),
                onPrimary = Color(0xFF1E1300),
                primaryContainer = Color(0xFF3B2B08),
                onPrimaryContainer = Color(0xFFFDE68A),
                secondary = Color(0xFF14B8A6),
                onSecondary = Color(0xFF00201C),
                secondaryContainer = Color(0xFF0D3D38),
                onSecondaryContainer = Color(0xFF99F6E4),
                background = Color(0xFF0B1324),
                onBackground = Color(0xFFF1F5F9),
                surface = Color(0xFF121C31),
                onSurface = Color(0xFFF1F5F9),
                surfaceVariant = Color(0xFF1B2842),
                onSurfaceVariant = Color(0xFF94A3B8),
                outline = Color(0xFF2B3D60),
                error = Color(0xFFF87171)
            ) to DesiExtendedColors(
                isDark = true,
                gradientTop = Color(0xFF091121),
                gradientBottom = Color(0xFF14213D),
                cardElevated = Color(0xFF15223B),
                cardBorder = Color(0xFF293B5F),
                udharRed = Color(0xFFF87171),
                jamaGreen = Color(0xFF34D399),
                goldAccent = Color(0xFFF59E0B)
            )
        }

        AppThemePreset.LIGHT -> {
            lightColorScheme(
                primary = Color(0xFF0F766E),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFCCFBF1),
                onPrimaryContainer = Color(0xFF042F2E),
                secondary = Color(0xFFD97706),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFFEF3C7),
                onSecondaryContainer = Color(0xFF451A03),
                background = Color(0xFFF8FAFC),
                onBackground = Color(0xFF0F172A),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF0F172A),
                surfaceVariant = Color(0xFFE2E8F0),
                onSurfaceVariant = Color(0xFF475569),
                outline = Color(0xFFCBD5E1),
                error = Color(0xFFDC2626)
            ) to DesiExtendedColors(
                isDark = false,
                gradientTop = Color(0xFFF1F5F9),
                gradientBottom = Color(0xFFE2E8F0),
                cardElevated = Color(0xFFFFFFFF),
                cardBorder = Color(0xFFE2E8F0),
                udharRed = Color(0xFFDC2626),
                jamaGreen = Color(0xFF059669),
                goldAccent = Color(0xFFD97706)
            )
        }

        AppThemePreset.BLUE -> {
            darkColorScheme(
                primary = Color(0xFF38BDF8),
                onPrimary = Color(0xFF002438),
                primaryContainer = Color(0xFF0C4A6E),
                onPrimaryContainer = Color(0xFFBAE6FD),
                secondary = Color(0xFF818CF8),
                onSecondary = Color(0xFF0F172A),
                secondaryContainer = Color(0xFF1E3A8A),
                onSecondaryContainer = Color(0xFFE0E7FF),
                background = Color(0xFF07162E),
                onBackground = Color(0xFFF0F9FF),
                surface = Color(0xFF0D2142),
                onSurface = Color(0xFFF0F9FF),
                surfaceVariant = Color(0xFF14305C),
                onSurfaceVariant = Color(0xFF93C5FD),
                outline = Color(0xFF1E447E)
            ) to DesiExtendedColors(
                isDark = true,
                gradientTop = Color(0xFF051226),
                gradientBottom = Color(0xFF0F2C59),
                cardElevated = Color(0xFF10284E),
                cardBorder = Color(0xFF1D437B),
                udharRed = Color(0xFFFB7185),
                jamaGreen = Color(0xFF34D399),
                goldAccent = Color(0xFF38BDF8)
            )
        }

        AppThemePreset.GREEN -> {
            darkColorScheme(
                primary = Color(0xFF10B981),
                onPrimary = Color(0xFF002114),
                primaryContainer = Color(0xFF064E3B),
                onPrimaryContainer = Color(0xFFA7F3D0),
                secondary = Color(0xFFFBBF24),
                onSecondary = Color(0xFF1F1600),
                secondaryContainer = Color(0xFF3F2E05),
                onSecondaryContainer = Color(0xFFFDE68A),
                background = Color(0xFF051B14),
                onBackground = Color(0xFFECFDF5),
                surface = Color(0xFF0A2920),
                onSurface = Color(0xFFECFDF5),
                surfaceVariant = Color(0xFF123B2E),
                onSurfaceVariant = Color(0xFF6EE7B7),
                outline = Color(0xFF1B5240)
            ) to DesiExtendedColors(
                isDark = true,
                gradientTop = Color(0xFF041711),
                gradientBottom = Color(0xFF0B3226),
                cardElevated = Color(0xFF0D2E24),
                cardBorder = Color(0xFF1B5240),
                udharRed = Color(0xFFF87171),
                jamaGreen = Color(0xFF34D399),
                goldAccent = Color(0xFFFBBF24)
            )
        }

        AppThemePreset.PURPLE -> {
            darkColorScheme(
                primary = Color(0xFFC084FC),
                onPrimary = Color(0xFF280547),
                primaryContainer = Color(0xFF4C1D95),
                onPrimaryContainer = Color(0xFFF3E8FF),
                secondary = Color(0xFFF472B6),
                onSecondary = Color(0xFF360620),
                secondaryContainer = Color(0xFF5B1842),
                onSecondaryContainer = Color(0xFFFCE7F3),
                background = Color(0xFF120B24),
                onBackground = Color(0xFFFAF5FF),
                surface = Color(0xFF1D1338),
                onSurface = Color(0xFFFAF5FF),
                surfaceVariant = Color(0xFF2B1C52),
                onSurfaceVariant = Color(0xFFD8B4FE),
                outline = Color(0xFF422C78)
            ) to DesiExtendedColors(
                isDark = true,
                gradientTop = Color(0xFF0F091F),
                gradientBottom = Color(0xFF231546),
                cardElevated = Color(0xFF211640),
                cardBorder = Color(0xFF412B77),
                udharRed = Color(0xFFFB7185),
                jamaGreen = Color(0xFF34D399),
                goldAccent = Color(0xFFC084FC)
            )
        }

        AppThemePreset.BLACK_AMOLED -> {
            darkColorScheme(
                primary = Color(0xFFF59E0B),
                onPrimary = Color.Black,
                primaryContainer = Color(0xFF2B1F04),
                onPrimaryContainer = Color(0xFFFDE68A),
                secondary = Color(0xFF2DD4BF),
                onSecondary = Color.Black,
                secondaryContainer = Color(0xFF082E2A),
                onSecondaryContainer = Color(0xFF99F6E4),
                background = Color(0xFF000000),
                onBackground = Color(0xFFF8FAFC),
                surface = Color(0xFF0B0B0E),
                onSurface = Color(0xFFF8FAFC),
                surfaceVariant = Color(0xFF17171C),
                onSurfaceVariant = Color(0xFFA1A1AA),
                outline = Color(0xFF27272A)
            ) to DesiExtendedColors(
                isDark = true,
                gradientTop = Color(0xFF000000),
                gradientBottom = Color(0xFF09090B),
                cardElevated = Color(0xFF101014),
                cardBorder = Color(0xFF27272A),
                udharRed = Color(0xFFF87171),
                jamaGreen = Color(0xFF34D399),
                goldAccent = Color(0xFFF59E0B)
            )
        }

        AppThemePreset.CUSTOM -> {
            val accent = Color(settings.customAccentColor)
            when (settings.customBackgroundStyle) {
                CustomBackgroundStyle.WARM_IVORY -> {
                    lightColorScheme(
                        primary = accent,
                        onPrimary = Color.White,
                        primaryContainer = accent.copy(alpha = 0.16f),
                        onPrimaryContainer = Color(0xFF1E293B),
                        secondary = Color(0xFF0D9488),
                        onSecondary = Color.White,
                        background = Color(0xFFFFFBEB),
                        onBackground = Color(0xFF1C1917),
                        surface = Color(0xFFFFFFFF),
                        onSurface = Color(0xFF1C1917),
                        surfaceVariant = Color(0xFFF5EFE6),
                        onSurfaceVariant = Color(0xFF57534E),
                        outline = Color(0xFFE7E5E4)
                    ) to DesiExtendedColors(
                        isDark = false,
                        gradientTop = Color(0xFFFFFBEB),
                        gradientBottom = Color(0xFFFEF3C7),
                        cardElevated = Color.White,
                        cardBorder = Color(0xFFE7E5E4),
                        udharRed = Color(0xFFDC2626),
                        jamaGreen = Color(0xFF059669),
                        goldAccent = accent
                    )
                }

                else -> {
                    val (bgTop, bgBot, surf, surfVar) = when (settings.customBackgroundStyle) {
                        CustomBackgroundStyle.DEEP_EMERALD -> listOf(
                            Color(0xFF031A15), Color(0xFF082E26), Color(0xFF0A2B23), Color(0xFF123C32)
                        )
                        CustomBackgroundStyle.SAFFRON_DUSK -> listOf(
                            Color(0xFF1C1018), Color(0xFF2B1622), Color(0xFF261520), Color(0xFF361F2D)
                        )
                        CustomBackgroundStyle.PURE_OBSIDIAN -> listOf(
                            Color(0xFF000000), Color(0xFF09090D), Color(0xFF0E0E13), Color(0xFF191921)
                        )
                        else -> listOf(
                            Color(0xFF0B1324), Color(0xFF15223B), Color(0xFF131E34), Color(0xFF1D2C4A)
                        )
                    }
                    darkColorScheme(
                        primary = accent,
                        onPrimary = Color(0xFF0F172A),
                        primaryContainer = accent.copy(alpha = 0.22f),
                        onPrimaryContainer = Color(0xFFF8FAFC),
                        secondary = Color(0xFF2DD4BF),
                        onSecondary = Color(0xFF042F2E),
                        background = bgTop,
                        onBackground = Color(0xFFF8FAFC),
                        surface = surf,
                        onSurface = Color(0xFFF8FAFC),
                        surfaceVariant = surfVar,
                        onSurfaceVariant = Color(0xFFCBD5E1),
                        outline = accent.copy(alpha = 0.35f)
                    ) to DesiExtendedColors(
                        isDark = true,
                        gradientTop = bgTop,
                        gradientBottom = bgBot,
                        cardElevated = surf,
                        cardBorder = accent.copy(alpha = 0.28f),
                        udharRed = Color(0xFFF87171),
                        jamaGreen = Color(0xFF34D399),
                        goldAccent = accent
                    )
                }
            }
        }
    }
}
