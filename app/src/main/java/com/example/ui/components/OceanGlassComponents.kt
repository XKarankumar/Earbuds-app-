package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AnimationPreset
import com.example.ui.theme.LocalAnimationPreset
import com.example.ui.theme.LocalGlassIntensity
import com.example.ui.theme.LocalOceanPalette
import com.example.ui.theme.OceanEmeraldColors

@Composable
fun OceanEmeraldBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalOceanPalette.current
    val animPreset = LocalAnimationPreset.current

    val driftProgress = if (animPreset == AnimationPreset.FULL_SPRING) {
        val infinite = rememberInfiniteTransition(label = "ocean_drift")
        val value by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 7000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "drift"
        )
        value
    } else {
        0.5f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(palette.bgTop, palette.bgMiddle, palette.bgBottom),
                    start = Offset(0f, 0f),
                    end = Offset(1200f, 2200f)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val oceanCenter = Offset(
                x = size.width * (0.14f + 0.08f * driftProgress),
                y = size.height * (0.10f + 0.05f * driftProgress)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.ambientOrbOcean, Color.Transparent),
                    center = oceanCenter,
                    radius = size.maxDimension * 0.56f
                ),
                radius = size.maxDimension * 0.56f,
                center = oceanCenter
            )

            val mintCenter = Offset(
                x = size.width * (0.88f - 0.08f * driftProgress),
                y = size.height * (0.80f - 0.06f * driftProgress)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.ambientOrbMint, Color.Transparent),
                    center = mintCenter,
                    radius = size.maxDimension * 0.52f
                ),
                radius = size.maxDimension * 0.52f,
                center = mintCenter
            )
        }
        content()
    }
}

@Composable
fun OceanGlassCard(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    shape: Shape = RoundedCornerShape(24.dp),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    glowBorderColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalOceanPalette.current
    val intensity = LocalGlassIntensity.current
    val animPreset = LocalAnimationPreset.current

    val baseFill = if (elevated) palette.glassCardElevatedFill else palette.glassCardFill
    val adjustedFill = baseFill.copy(
        alpha = (baseFill.alpha * (0.62f + 0.38f * intensity)).coerceIn(0.45f, 0.96f)
    )

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null && animPreset != AnimationPreset.REDUCED_MOTION) 0.975f else 1f,
        animationSpec = if (animPreset == AnimationPreset.FULL_SPRING) {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
        } else {
            tween(durationMillis = (110 * animPreset.durationScale).toInt().coerceAtLeast(1))
        },
        label = "ocean_card_scale"
    )

    val borderStart = glowBorderColor?.copy(alpha = 0.7f) ?: palette.glassBorderHighlight
    val borderEnd = glowBorderColor?.copy(alpha = 0.28f) ?: palette.glassBorderAccent

    val cardModifier = modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .shadow(
            elevation = if (elevated) 10.dp else 4.dp,
            shape = shape,
            ambientColor = palette.glowShadowColor,
            spotColor = palette.glowShadowColor
        )
        .clip(shape)
        .background(
            Brush.linearGradient(
                colors = listOf(
                    adjustedFill,
                    adjustedFill.copy(alpha = (adjustedFill.alpha * 0.88f).coerceIn(0.4f, 0.95f))
                )
            )
        )
        .border(
            width = 1.15.dp,
            brush = Brush.linearGradient(listOf(borderStart, borderEnd)),
            shape = shape
        )
        .let { base ->
            if (onClick != null) {
                base.clickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    role = Role.Button,
                    onClick = onClick
                )
            } else base
        }

    Column(
        modifier = cardModifier.padding(contentPadding),
        content = content
    )
}

@Composable
fun OceanTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    backTestTag: String = "btn_back",
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag(backTestTag)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(Modifier.width(4.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun OceanPillBadge(
    text: String,
    accent: Color,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = accent.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.48f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(5.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

enum class CalculatorKeyStyle {
    NUMBER,
    OPERATOR,
    UTILITY,
    EQUALS
}

@Composable
fun OceanCalculatorKey(
    label: String,
    style: CalculatorKeyStyle,
    hapticEnabled: Boolean,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 22.sp,
    isCircular: Boolean = true,
    onClick: () -> Unit
) {
    val palette = LocalOceanPalette.current
    val animPreset = LocalAnimationPreset.current
    val haptic = LocalHapticFeedback.current

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            animPreset == AnimationPreset.REDUCED_MOTION -> 1f
            pressed -> 0.90f
            else -> 1f
        },
        animationSpec = if (animPreset == AnimationPreset.FULL_SPRING) {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        } else {
            tween(durationMillis = (90 * animPreset.durationScale).toInt().coerceAtLeast(1))
        },
        label = "calc_key_scale"
    )

    val shape: Shape = if (isCircular) CircleShape else RoundedCornerShape(22.dp)

    val (bgBrush, borderColor, textColor) = when (style) {
        CalculatorKeyStyle.NUMBER -> Triple(
            Brush.linearGradient(
                listOf(palette.numberKeyFill, palette.numberKeyFill.copy(alpha = 0.85f))
            ),
            palette.numberKeyBorder,
            palette.numberKeyText
        )
        CalculatorKeyStyle.OPERATOR -> Triple(
            Brush.linearGradient(
                listOf(palette.operatorKeyFill, palette.operatorKeyFill.copy(alpha = 0.86f))
            ),
            palette.operatorKeyBorder,
            palette.operatorKeyText
        )
        CalculatorKeyStyle.UTILITY -> Triple(
            Brush.linearGradient(
                listOf(palette.utilityKeyFill, palette.utilityKeyFill.copy(alpha = 0.82f))
            ),
            palette.glassBorderHighlight.copy(alpha = 0.55f),
            palette.utilityKeyText
        )
        CalculatorKeyStyle.EQUALS -> Triple(
            Brush.linearGradient(
                listOf(palette.equalsStart, palette.equalsEnd)
            ),
            OceanEmeraldColors.MintGreen.copy(alpha = 0.85f),
            OceanEmeraldColors.GlassWhite
        )
    }

    val shadowColor = when (style) {
        CalculatorKeyStyle.EQUALS -> OceanEmeraldColors.EmeraldGreen.copy(alpha = 0.45f)
        CalculatorKeyStyle.OPERATOR -> OceanEmeraldColors.MintGreen.copy(alpha = 0.25f)
        CalculatorKeyStyle.NUMBER -> OceanEmeraldColors.OceanBlue.copy(alpha = 0.22f)
        CalculatorKeyStyle.UTILITY -> Color.Transparent
    }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (style == CalculatorKeyStyle.EQUALS) 8.dp else 3.dp,
                shape = shape,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(shape)
            .background(bgBrush)
            .border(width = 1.1.dp, color = borderColor, shape = shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Button,
                onClick = {
                    if (hapticEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = fontSize,
            fontWeight = if (style == CalculatorKeyStyle.EQUALS || style == CalculatorKeyStyle.OPERATOR) {
                FontWeight.Bold
            } else {
                FontWeight.SemiBold
            },
            color = textColor,
            maxLines = 1
        )
    }
}
