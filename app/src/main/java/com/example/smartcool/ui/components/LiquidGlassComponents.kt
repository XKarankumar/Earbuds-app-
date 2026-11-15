package com.example.smartcool.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.smartcool.model.TelemetrySamplePoint
import com.example.smartcool.model.formatTemperature
import com.example.smartcool.ui.theme.LiquidPalette
import com.example.smartcool.ui.theme.LocalLiquidPalette

@Composable
fun LiquidGlassBackdrop(
    modifier: Modifier = Modifier,
    palette: LiquidPalette = LocalLiquidPalette.current,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        palette.backgroundTop,
                        palette.backgroundMid,
                        palette.backgroundBottom
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.primaryAccent.copy(alpha = palette.orbAlpha * 0.45f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.14f, size.height * 0.10f),
                    radius = size.maxDimension * 0.55f
                ),
                center = Offset(size.width * 0.14f, size.height * 0.10f),
                radius = size.maxDimension * 0.55f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.secondaryAccent.copy(alpha = palette.orbAlpha * 0.35f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.88f, size.height * 0.65f),
                    radius = size.maxDimension * 0.52f
                ),
                center = Offset(size.width * 0.88f, size.height * 0.65f),
                radius = size.maxDimension * 0.52f
            )
        }
        content()
    }
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    accentOverride: Color? = null,
    cornerRadius: Dp = 22.dp,
    palette: LiquidPalette = LocalLiquidPalette.current,
    content: @Composable ColumnScope.() -> Unit
) {
    val activeBorder = accentOverride ?: palette.primaryAccent
    val borderBrush = if (highlighted) {
        Brush.verticalGradient(
            colors = listOf(
                activeBorder.copy(alpha = 0.85f),
                palette.borderStroke
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                palette.specularTopBorder,
                palette.borderStroke
            )
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        color = palette.glassSurface,
        tonalElevation = if (palette.isDark) 0.dp else 1.dp,
        border = BorderStroke(if (highlighted) 1.4.dp else 1.dp, borderBrush)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (palette.isDark) 0.05f else 0.25f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
fun StatusBadgePill(
    text: String,
    dotColor: Color,
    modifier: Modifier = Modifier,
    palette: LiquidPalette = LocalLiquidPalette.current
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(palette.glassElevated)
            .border(1.dp, dotColor.copy(alpha = 0.45f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun LiquidRingGauge(
    progress: Float,
    primaryValue: String,
    label: String,
    subLabel: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    palette: LiquidPalette = LocalLiquidPalette.current
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 550),
        label = "ringProgress"
    )

    Box(
        modifier = modifier.size(132.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 11.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = palette.silverChrome.copy(alpha = if (palette.isDark) 0.18f else 0.45f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.65f),
                        accentColor,
                        palette.secondaryAccent
                    )
                ),
                startAngle = 135f,
                sweepAngle = 270f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = primaryValue,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = accentColor
            )
            Text(
                text = subLabel,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MetricMiniTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
    palette: LiquidPalette = LocalLiquidPalette.current
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(palette.glassElevated)
            .border(1.dp, palette.borderStroke, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TemperatureHistoryChart(
    samples: List<TelemetrySamplePoint>,
    useFahrenheit: Boolean,
    modifier: Modifier = Modifier,
    palette: LiquidPalette = LocalLiquidPalette.current
) {
    if (samples.size < 2) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(palette.glassElevated)
                .border(1.dp, palette.borderStroke, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Recording Real Sensor History...",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Live BatteryManager samples appear automatically as readings change.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )
            }
        }
        return
    }

    val recent = samples.takeLast(48)
    val minTemp = (recent.minOf { it.tempCelsius } - 1.5f).coerceAtLeast(15f)
    val maxTemp = (recent.maxOf { it.tempCelsius } + 1.5f).coerceAtLeast(minTemp + 4f)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Low ${formatTemperature(recent.minOf { it.tempCelsius }, useFahrenheit)}",
                style = MaterialTheme.typography.labelMedium,
                color = palette.coolCyan
            )
            Text(
                text = "${recent.size} real samples",
                style = MaterialTheme.typography.labelMedium,
                color = palette.textSecondary
            )
            Text(
                text = "Peak ${formatTemperature(recent.maxOf { it.tempCelsius }, useFahrenheit)}",
                style = MaterialTheme.typography.labelMedium,
                color = palette.warningAmber
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(palette.glassElevated)
                .border(1.dp, palette.borderStroke, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            val stepX = size.width / (recent.size - 1).coerceAtLeast(1)
            val span = (maxTemp - minTemp).coerceAtLeast(1f)

            val linePath = Path()
            val fillPath = Path()

            recent.forEachIndexed { idx, pt ->
                val x = idx * stepX
                val norm = ((pt.tempCelsius - minTemp) / span).coerceIn(0f, 1f)
                val y = size.height - norm * size.height
                if (idx == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, size.height)
                    fillPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
                if (idx == recent.lastIndex) {
                    fillPath.lineTo(x, size.height)
                    fillPath.close()
                }
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        palette.primaryAccent.copy(alpha = 0.35f),
                        Color.Transparent
                    )
                )
            )
            drawPath(
                path = linePath,
                color = palette.primaryAccent,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            val last = recent.last()
            val lastNorm = ((last.tempCelsius - minTemp) / span).coerceIn(0f, 1f)
            val lastOffset = Offset(size.width, size.height - lastNorm * size.height)
            drawCircle(
                color = palette.primaryAccent.copy(alpha = 0.3f),
                radius = 8.dp.toPx(),
                center = lastOffset
            )
            drawCircle(
                color = palette.primaryAccent,
                radius = 4.dp.toPx(),
                center = lastOffset
            )
        }
    }
}

@Composable
fun BatteryHistoryChart(
    samples: List<TelemetrySamplePoint>,
    modifier: Modifier = Modifier,
    palette: LiquidPalette = LocalLiquidPalette.current
) {
    if (samples.size < 2) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(palette.glassElevated)
                .border(1.dp, palette.borderStroke, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Collecting real battery charge & drain curve...",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary
            )
        }
        return
    }

    val recent = samples.takeLast(48)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(palette.glassElevated)
            .border(1.dp, palette.borderStroke, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        val stepX = size.width / (recent.size - 1).coerceAtLeast(1)
        val linePath = Path()
        val fillPath = Path()

        recent.forEachIndexed { idx, pt ->
            val x = idx * stepX
            val norm = (pt.batteryPercent / 100f).coerceIn(0f, 1f)
            val y = size.height - norm * size.height
            if (idx == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            if (idx == recent.lastIndex) {
                fillPath.lineTo(x, size.height)
                fillPath.close()
            }
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    palette.successEmerald.copy(alpha = 0.34f),
                    Color.Transparent
                )
            )
        )
        drawPath(
            path = linePath,
            color = palette.successEmerald,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
