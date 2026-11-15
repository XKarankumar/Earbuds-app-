package com.example.ui.components

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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hometheatre.core.AudioTelemetrySnapshot
import com.example.ui.theme.LocalStudioPalette

@Composable
fun StudioBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val palette = LocalStudioPalette.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(palette.backgroundTop, palette.backgroundBottom)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.primaryAccent.copy(alpha = palette.glowAlpha * 0.35f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.15f, size.height * 0.12f),
                    radius = size.maxDimension * 0.55f
                ),
                center = Offset(size.width * 0.15f, size.height * 0.12f),
                radius = size.maxDimension * 0.55f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.secondaryAccent.copy(alpha = palette.glowAlpha * 0.28f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.88f, size.height * 0.72f),
                    radius = size.maxDimension * 0.5f
                ),
                center = Offset(size.width * 0.88f, size.height * 0.72f),
                radius = size.maxDimension * 0.5f
            )
        }
        content()
    }
}

@Composable
fun GlassConsoleCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalStudioPalette.current
    val borderColor = if (highlighted) {
        palette.primaryAccent.copy(alpha = 0.75f)
    } else {
        palette.borderStroke
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = palette.surfaceGlass,
        tonalElevation = 2.dp,
        border = BorderStroke(if (highlighted) 1.25.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun StatusPill(
    text: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color? = null
) {
    val palette = LocalStudioPalette.current
    val dotColor = if (active) (activeColor ?: palette.successEmerald) else palette.textSecondary.copy(alpha = 0.5f)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(palette.surfaceElevated)
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
            color = palette.textPrimary
        )
    }
}

@Composable
fun SpectrumVisualizerCanvas(
    telemetry: AudioTelemetrySnapshot,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    val rmsL by animateFloatAsState(telemetry.rmsLeft, tween(60), label = "rmsL")
    val rmsR by animateFloatAsState(telemetry.rmsRight, tween(60), label = "rmsR")

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "REAL-TIME SPECTRUM // ${telemetry.sampleRateHz / 1000f} kHz",
                style = MaterialTheme.typography.labelMedium,
                color = palette.primaryAccent
            )
            Text(
                text = "PEAK ${"%.1f".format(telemetry.peakDbfs)} dBFS" +
                    if (telemetry.isClippingProtected) " [LIMIT -${"%.1f".format(telemetry.limiterGainReductionDb)}dB]" else "",
                style = MaterialTheme.typography.labelMedium,
                color = if (telemetry.isClippingProtected) palette.warningAmber else palette.textSecondary
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.backgroundTop.copy(alpha = 0.72f))
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            val bins = telemetry.spectrumBins
            val count = bins.size.coerceAtLeast(1)
            val gap = 4.dp.toPx()
            val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(2f)

            for (i in 0 until count) {
                val amp = bins[i].coerceIn(0.03f, 1f)
                val barHeight = amp * size.height
                val x = i * (barWidth + gap)
                val y = size.height - barHeight
                val brush = Brush.verticalGradient(
                    colors = listOf(palette.secondaryAccent, palette.primaryAccent),
                    startY = 0f,
                    endY = size.height
                )
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        StereoMeterBar(label = "L", level = rmsL)
        Spacer(modifier = Modifier.height(4.dp))
        StereoMeterBar(label = "R", level = rmsR)
    }
}

@Composable
private fun StereoMeterBar(label: String, level: Float) {
    val palette = LocalStudioPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = palette.textSecondary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(16.dp)
        )
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(palette.backgroundTop.copy(alpha = 0.8f))
        ) {
            val fillW = (size.width * level.coerceIn(0f, 1f))
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        palette.successEmerald,
                        palette.primaryAccent,
                        palette.warningAmber
                    )
                ),
                size = Size(fillW, size.height)
            )
        }
    }
}

@Composable
fun LedStripPreviewBar(
    colorsArgb: List<Int>,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF06080D))
            .border(1.dp, palette.borderStroke, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        val count = colorsArgb.size.coerceAtLeast(1)
        val step = size.width / count
        val radius = (minOf(step * 0.36f, size.height * 0.42f)).coerceAtLeast(3f)
        colorsArgb.forEachIndexed { idx, argb ->
            val c = Color(argb)
            val cx = step * idx + step * 0.5f
            val cy = size.height * 0.5f
            drawCircle(
                color = c.copy(alpha = 0.32f),
                radius = radius * 1.7f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = c,
                radius = radius,
                center = Offset(cx, cy)
            )
        }
    }
}
