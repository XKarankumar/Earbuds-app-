package com.example.ui.components

import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.AnimationStyle
import com.example.ui.theme.LocalLiquidGlassStyle
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SilverEdgeRim
import com.example.ui.theme.SilverShadowTint
import com.example.ui.theme.SoftSilverGradientEnd
import com.example.ui.theme.SoftSilverGradientMid

@Composable
fun Modifier.backdropGlassBlur(isBlurred: Boolean): Modifier {
    val style = LocalLiquidGlassStyle.current
    val targetBlur = if (isBlurred) style.blurRadius else 0.dp
    val blurRadius: Dp by animateDpAsState(
        targetValue = targetBlur,
        animationSpec = when (style.animationStyle) {
            AnimationStyle.CRISP_MINIMAL -> tween(120)
            else -> spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
        },
        label = "backdrop_blur_radius",
    )
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurRadius > 0.dp) {
        this.blur(radius = blurRadius, edgeTreatment = BlurredEdgeTreatment.Rectangle)
    } else {
        this
    }
}

@Composable
fun LiquidGlassStageBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val glassStyle = LocalLiquidGlassStyle.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Soft silver luminous radial accents on pure white so frosted glass cards catch light cleanly
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (!glassStyle.isDark) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFEEF1F7).copy(alpha = 0.85f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.16f, size.height * 0.14f),
                        radius = size.maxDimension * 0.45f,
                    ),
                    center = Offset(size.width * 0.16f, size.height * 0.14f),
                    radius = size.maxDimension * 0.45f,
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE9EDF5).copy(alpha = 0.72f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.88f, size.height * 0.56f),
                        radius = size.maxDimension * 0.42f,
                    ),
                    center = Offset(size.width * 0.88f, size.height * 0.56f),
                    radius = size.maxDimension * 0.42f,
                )
            } else {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.22f),
                        radius = size.maxDimension * 0.45f,
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.22f),
                    radius = size.maxDimension * 0.45f,
                )
            }
        }
        content()
    }
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = LocalLiquidGlassStyle.current.cornerRadius,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val glassStyle = LocalLiquidGlassStyle.current
    val shape = RoundedCornerShape(cornerRadius)
    val alpha = glassStyle.glassAlpha.coerceIn(0.45f, 0.96f)
    val reflect = glassStyle.reflectionIntensity.coerceIn(0.15f, 1.0f)

    val borderBrush = if (glassStyle.isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.36f * reflect),
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.22f * reflect),
            ),
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                PureWhite.copy(alpha = 0.96f),
                SilverEdgeRim.copy(alpha = 0.75f),
                PureWhite.copy(alpha = 0.85f),
            ),
        )
    }

    val fillBrush = if (glassStyle.isDark) {
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surface.copy(alpha = alpha),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = (alpha - 0.08f).coerceAtLeast(0.36f)),
            ),
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                PureWhite.copy(alpha = (alpha + 0.08f).coerceAtMost(0.97f)),
                SoftSilverGradientMid.copy(alpha = alpha),
                SoftSilverGradientEnd.copy(alpha = (alpha - 0.12f).coerceAtLeast(0.40f)),
            ),
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (glassStyle.isDark) 10.dp else 14.dp,
                shape = shape,
                ambientColor = if (glassStyle.isDark) Color.Black.copy(alpha = 0.24f) else SilverShadowTint.copy(alpha = 0.28f),
                spotColor = if (glassStyle.isDark) Color.Black.copy(alpha = 0.28f) else SilverShadowTint.copy(alpha = 0.22f),
            )
            .clip(shape)
            .background(fillBrush)
            .drawBehind {
                // Specular top-edge liquid glass reflection band
                val sheenAlpha = if (glassStyle.isDark) 0.07f * reflect else 0.55f * reflect
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = sheenAlpha),
                            Color.Transparent,
                            Color.White.copy(alpha = sheenAlpha * 0.35f),
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width * 0.75f, size.height * 0.55f),
                    ),
                )
            }
            .border(width = 1.15.dp, brush = borderBrush, shape = shape)
            .let { m -> if (onClick != null) m.clickable(onClick = onClick) else m }
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
fun GlassSubCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    LiquidGlassCard(
        modifier = modifier,
        cornerRadius = (LocalLiquidGlassStyle.current.cornerRadius - 4.dp).coerceAtLeast(14.dp),
        onClick = onClick,
        contentPadding = 16.dp,
        content = content,
    )
}

@Composable
fun WhiteGlassScreenHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val glassStyle = LocalLiquidGlassStyle.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Surface(
                shape = CircleShape,
                color = if (glassStyle.isDark) {
                    MaterialTheme.colorScheme.surface.copy(alpha = glassStyle.glassAlpha)
                } else {
                    PureWhite.copy(alpha = 0.90f)
                },
                shadowElevation = 6.dp,
                modifier = Modifier
                    .size(46.dp)
                    .border(
                        1.dp,
                        if (glassStyle.isDark) Color.White.copy(alpha = 0.22f) else SilverEdgeRim,
                        CircleShape,
                    ),
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("screen_back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailingContent()
        }
    }
}
