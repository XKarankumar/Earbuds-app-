package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalGlassPalette

@Composable
fun AmoledGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalGlassPalette.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.ambientGlowPrimary, Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.08f),
                    radius = size.width * 0.85f
                ),
                radius = size.width * 0.85f,
                center = Offset(size.width * 0.85f, size.height * 0.08f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.ambientGlowSecondary, Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.78f),
                    radius = size.width * 0.8f
                ),
                radius = size.width * 0.8f,
                center = Offset(size.width * 0.15f, size.height * 0.78f)
            )
        }
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    cornerRadius: Dp = 22.dp,
    accentBorder: Color? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalGlassPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.975f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "glass_press_scale"
    )
    val shape = RoundedCornerShape(cornerRadius)
    val fill = if (elevated) palette.glassElevatedFill else palette.glassFill
    val borderBrush = if (accentBorder != null) {
        Brush.verticalGradient(listOf(accentBorder.copy(alpha = 0.75f), accentBorder.copy(alpha = 0.18f)))
    } else {
        Brush.verticalGradient(listOf(palette.glassBorderTop, palette.glassBorderBottom))
    }

    val clickableMod = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = androidx.compose.material3.ripple(color = palette.accentCyan),
            onClick = onClick
        )
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(fill)
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .then(clickableMod)
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun GlassTopHeader(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val palette = LocalGlassPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("back_button")
                    .clip(CircleShape)
                    .background(palette.glassFill)
                    .border(1.dp, palette.glassBorderTop, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate back",
                    tint = palette.primaryText
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = palette.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = palette.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun MetricGlassTile(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val palette = LocalGlassPalette.current
    GlassCard(
        modifier = modifier,
        elevated = true,
        accentBorder = accent.copy(alpha = 0.55f),
        onClick = onClick,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = palette.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = palette.primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun GlassPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Color? = null,
    filled: Boolean = true,
    enabled: Boolean = true
) {
    val palette = LocalGlassPalette.current
    val resolvedAccent = accent ?: palette.accentCyan
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(24.dp),
        color = if (filled) resolvedAccent else palette.glassElevatedFill,
        contentColor = if (filled) {
            if (palette.isDark) Color.Black else Color.White
        } else {
            palette.primaryText
        },
        border = if (filled) null else BorderStroke(1.dp, palette.glassBorderTop)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun GlassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = palette.secondaryText) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = palette.accentCyan) },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = palette.accentCyan,
            unfocusedBorderColor = palette.glassBorderTop,
            focusedContainerColor = palette.glassFill,
            unfocusedContainerColor = palette.glassFill,
            focusedTextColor = palette.primaryText,
            unfocusedTextColor = palette.primaryText
        )
    )
}

@Composable
fun AnimatedGlassRing(
    progress: Float,
    centerLabel: String,
    subLabel: String,
    accent: Color,
    size: Dp = 112.dp
) {
    val palette = LocalGlassPalette.current
    val animProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "ring_progress"
    )
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 10.dp.toPx()
            drawArc(
                color = palette.glassBorderBottom,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(accent, palette.accentEmerald, accent)),
                startAngle = -90f,
                sweepAngle = 360f * animProgress,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = palette.primaryText
            )
            Text(
                text = subLabel,
                style = MaterialTheme.typography.labelSmall,
                color = palette.secondaryText
            )
        }
    }
}

@Composable
fun WeightTrendChart(
    weights: List<Float>,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    if (weights.size < 2) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Log at least 2 weigh-ins to view progress curve", color = palette.secondaryText, style = MaterialTheme.typography.bodySmall)
        }
        return
    }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .padding(vertical = 8.dp)
    ) {
        val minW = (weights.minOrNull() ?: 60f) - 1.5f
        val maxW = (weights.maxOrNull() ?: 80f) + 1.5f
        val span = (maxW - minW).coerceAtLeast(1f)
        val stepX = size.width / (weights.size - 1).coerceAtLeast(1)

        // Subtle horizontal grid lines
        for (i in 0..3) {
            val y = size.height * (i / 3f)
            drawLine(
                color = palette.glassBorderBottom,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val path = Path()
        val fillPath = Path()
        val pts = weights.mapIndexed { idx, w ->
            val x = idx * stepX
            val y = size.height - ((w - minW) / span) * (size.height - 16.dp.toPx()) - 8.dp.toPx()
            Offset(x, y)
        }

        pts.forEachIndexed { idx, p ->
            if (idx == 0) {
                path.moveTo(p.x, p.y)
                fillPath.moveTo(p.x, size.height)
                fillPath.lineTo(p.x, p.y)
            } else {
                path.lineTo(p.x, p.y)
                fillPath.lineTo(p.x, p.y)
            }
        }
        fillPath.lineTo(pts.last().x, size.height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(listOf(accent.copy(alpha = 0.30f), Color.Transparent))
        )
        drawPath(
            path = path,
            color = accent,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        pts.forEach { p ->
            drawCircle(color = palette.background, radius = 5.dp.toPx(), center = p)
            drawCircle(color = accent, radius = 3.5.dp.toPx(), center = p)
        }
    }
}

@Composable
fun BarProgressChart(
    bars: List<Pair<String, Float>>,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    if (bars.isEmpty()) return
    val maxVal = (bars.maxOfOrNull { it.second } ?: 1f).coerceAtLeast(1f)
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            val slotW = size.width / bars.size
            val barW = (slotW * 0.48f).coerceAtMost(36.dp.toPx())
            bars.forEachIndexed { i, (_, v) ->
                val ratio = (v / maxVal).coerceIn(0.06f, 1f)
                val barH = size.height * ratio
                val left = i * slotW + (slotW - barW) / 2f
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(accent, palette.accentEmerald)),
                    topLeft = Offset(left, size.height - barH),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            bars.forEach { (label, _) ->
                Text(label, style = MaterialTheme.typography.labelSmall, color = palette.secondaryText)
            }
        }
    }
}
