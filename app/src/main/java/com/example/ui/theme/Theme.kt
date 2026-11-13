package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = StudioCobaltDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF15345E),
    onPrimaryContainer = Color(0xFFD6E6FF),
    secondary = StudioMintSoft,
    onSecondary = StudioMintDeep,
    secondaryContainer = Color(0xFF1E3E3B),
    onSecondaryContainer = StudioMintGlow,
    tertiary = StatusEmerald,
    background = DarkBackground,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMutedInk,
    outline = DarkOutline,
    outlineVariant = Color(0xFF202830),
    error = StatusCoral,
)

private val LightColorScheme = lightColorScheme(
    primary = StudioCobalt,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBE9FF),
    onPrimaryContainer = Color(0xFF0A2E66),
    secondary = StudioMint,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6F3EE),
    onSecondaryContainer = StudioMintDeep,
    tertiary = StatusEmerald,
    background = LightBackground,
    onBackground = LightInk,
    surface = LightSurface,
    onSurface = LightInk,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightMutedInk,
    outline = LightOutline,
    outlineVariant = Color(0xFFE9ECF0),
    error = StatusCoral,
)

val StudioShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun KaranBudsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = StudioShapes,
        content = content,
    )
}
