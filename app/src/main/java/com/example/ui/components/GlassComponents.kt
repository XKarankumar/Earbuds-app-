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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.permissions.PermissionState
import com.example.ui.theme.LocalGlassIntensity
import com.example.ui.theme.LocalGlassPalette

@Composable
fun GlassAmbientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalGlassPalette.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(glass.backgroundStart, glass.backgroundEnd)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glass.ambientOrbPrimary, Color.Transparent),
                    center = Offset(size.width * 0.16f, size.height * 0.14f),
                    radius = size.maxDimension * 0.56f
                ),
                radius = size.maxDimension * 0.56f,
                center = Offset(size.width * 0.16f, size.height * 0.14f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glass.ambientOrbSecondary, Color.Transparent),
                    center = Offset(size.width * 0.86f, size.height * 0.74f),
                    radius = size.maxDimension * 0.52f
                ),
                radius = size.maxDimension * 0.52f,
                center = Offset(size.width * 0.86f, size.height * 0.74f)
            )
        }
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    shape: Shape = RoundedCornerShape(24.dp),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val glass = LocalGlassPalette.current
    val intensity = LocalGlassIntensity.current
    val baseFill = if (elevated) glass.glassSurfaceElevated else glass.glassSurface
    val adjustedFill = baseFill.copy(alpha = (baseFill.alpha * (0.65f + 0.35f * intensity)).coerceIn(0.48f, 0.96f))

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.975f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "glass_card_scale"
    )

    val cardModifier = modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clip(shape)
        .background(
            Brush.linearGradient(
                colors = listOf(
                    adjustedFill,
                    adjustedFill.copy(alpha = (adjustedFill.alpha * 0.86f).coerceIn(0.42f, 0.94f))
                )
            )
        )
        .border(
            width = 1.1.dp,
            brush = Brush.linearGradient(
                colors = listOf(glass.glassBorderTopLeft, glass.glassBorderBottomRight)
            ),
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
fun RiskBadgePill(
    riskLevel: String,
    score: Int? = null,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlassPalette.current
    val accent = when (riskLevel.lowercase()) {
        "critical", "high" -> glass.dangerAccent
        "medium" -> glass.warningAccent
        else -> glass.safeAccent
    }
    val label = if (score != null) "$riskLevel ($score/100)" else riskLevel
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = accent.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun RiskMeterBar(
    score: Int,
    riskLevel: String,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlassPalette.current
    val accent = when (riskLevel.lowercase()) {
        "critical", "high" -> glass.dangerAccent
        "medium" -> glass.warningAccent
        else -> glass.safeAccent
    }
    val animatedProgress by animateFloatAsState(
        targetValue = (score.coerceIn(0, 100) / 100f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "risk_progress"
    )
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Fraud & Spam Risk Index",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$score / 100",
                style = MaterialTheme.typography.labelLarge,
                color = accent,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50)),
            color = accent,
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        )
    }
}

@Composable
fun AiRadarLoadingCard(
    message: String,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "radar")
    val pulse by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_pulse"
    )
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    GlassCard(modifier = modifier.fillMaxWidth(), elevated = true) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(54.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = primary.copy(alpha = (1f - pulse) * 0.65f),
                        radius = size.minDimension * 0.48f * pulse,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    drawCircle(
                        color = secondary.copy(alpha = 0.4f),
                        radius = size.minDimension * 0.24f
                    )
                    drawCircle(
                        color = primary,
                        radius = 5.dp.toPx()
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI Intelligence Analyzing...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SourceAttributionBanner(
    confidenceLevel: String,
    sourceAttribution: String,
    freshness: String? = null,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlassPalette.current
    val confColor = when (confidenceLevel.lowercase()) {
        "high" -> glass.safeAccent
        "medium" -> glass.infoAccent
        else -> glass.warningAccent
    }
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = "Source attribution",
                tint = confColor,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Confidence: $confidenceLevel" + (if (!freshness.isNullOrBlank()) " • $freshness" else ""),
                    style = MaterialTheme.typography.labelMedium,
                    color = confColor,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Source: $sourceAttribution",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PermissionStatusPill(
    state: PermissionState,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlassPalette.current
    val (bg, fg) = when (state) {
        PermissionState.GRANTED -> glass.safeAccent.copy(alpha = 0.16f) to glass.safeAccent
        PermissionState.NOT_GRANTED -> glass.warningAccent.copy(alpha = 0.18f) to glass.warningAccent
        PermissionState.NOT_REQUIRED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) to
            MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(fg)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = state.label,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SpringCalculatorKey(
    label: String,
    modifier: Modifier = Modifier,
    containerColor: Color,
    contentColor: Color,
    cornerRadius: Dp = 18.dp,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "key_scale"
    )
    val glass = LocalGlassPalette.current

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(cornerRadius))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Button,
                onClick = onClick
            ),
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(glass.glassBorderTopLeft.copy(alpha = 0.45f), glass.glassBorderBottomRight.copy(alpha = 0.18f))
            )
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                color = contentColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
