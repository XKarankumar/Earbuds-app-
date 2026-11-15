package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalStudioPalette
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun InteractiveJogWheel(
    deckLabel: String,
    isPlaying: Boolean,
    progress: Float,
    accentColor: Color,
    onScratchDelta: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    var angleRad by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(progress, isPlaying) {
        if (isPlaying) {
            angleRad = (progress * 360f * 16f) * (PI.toFloat() / 180f)
        }
    }

    Box(
        modifier = modifier
            .size(118.dp)
            .clip(CircleShape)
            .background(palette.backgroundTop.copy(alpha = 0.88f))
            .border(2.dp, accentColor.copy(alpha = 0.65f), CircleShape)
            .pointerInput(deckLabel) {
                var lastAngle = 0f
                detectDragGestures(
                    onDragStart = { offset ->
                        val cx = size.width * 0.5f
                        val cy = size.height * 0.5f
                        lastAngle = atan2(offset.y - cy, offset.x - cx)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val cx = size.width * 0.5f
                        val cy = size.height * 0.5f
                        val currentAngle = atan2(change.position.y - cy, change.position.x - cx)
                        var delta = currentAngle - lastAngle
                        if (delta > PI) delta -= (2f * PI.toFloat())
                        if (delta < -PI) delta += (2f * PI.toFloat())
                        lastAngle = currentAngle
                        angleRad += delta
                        onScratchDelta(delta)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(106.dp)) {
            val center = Offset(size.width * 0.5f, size.height * 0.5f)
            val outerR = size.minDimension * 0.48f
            // Vinyl grooves
            for (ring in 1..4) {
                drawCircle(
                    color = palette.textSecondary.copy(alpha = 0.14f),
                    radius = outerR * (0.45f + ring * 0.12f),
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            // Active rotating marker arc
            val deg = (angleRad * 180f / PI.toFloat())
            drawArc(
                color = accentColor,
                startAngle = deg - 25f,
                sweepAngle = 50f,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
            // Needle indicator line
            val markerX = center.x + cos(angleRad) * outerR * 0.82f
            val markerY = center.y + sin(angleRad) * outerR * 0.82f
            drawLine(
                color = accentColor,
                start = center,
                end = Offset(markerX, markerY),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(
                color = palette.surfaceElevated,
                radius = outerR * 0.28f,
                center = center
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = deckLabel,
                style = MaterialTheme.typography.titleMedium,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "SCRATCH",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary
            )
        }
    }
}

@Composable
fun WaveformSeekBar(
    peaks: List<Float>,
    progress: Float,
    cueNormalized: Float,
    hotCues: List<Float?>,
    accentColor: Color,
    onSeekNormalized: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(palette.backgroundTop.copy(alpha = 0.78f))
            .border(1.dp, palette.borderStroke, RoundedCornerShape(10.dp))
            .pointerInput(peaks) {
                detectTapGestures { offset ->
                    val norm = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                    onSeekNormalized(norm)
                }
            }
    ) {
        val count = peaks.size.coerceAtLeast(1)
        val step = size.width / count
        val barW = (step * 0.65f).coerceAtLeast(1.5f)
        val centerY = size.height * 0.5f

        peaks.forEachIndexed { idx, amp ->
            val normPos = idx.toFloat() / count
            val barH = (amp.coerceIn(0.06f, 1f) * size.height * 0.85f)
            val x = idx * step
            val color = if (normPos <= progress) accentColor else palette.textSecondary.copy(alpha = 0.38f)
            drawRoundRect(
                color = color,
                topLeft = Offset(x, centerY - barH * 0.5f),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
        }

        // Cue marker
        val cueX = cueNormalized.coerceIn(0f, 1f) * size.width
        drawLine(
            color = palette.warningAmber,
            start = Offset(cueX, 0f),
            end = Offset(cueX, size.height),
            strokeWidth = 2.dp.toPx()
        )

        // Hot cue markers
        hotCues.forEach { hc ->
            if (hc != null) {
                val hx = hc.coerceIn(0f, 1f) * size.width
                drawLine(
                    color = palette.secondaryAccent,
                    start = Offset(hx, 0f),
                    end = Offset(hx, size.height),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }

        // Active playhead
        val playX = progress.coerceIn(0f, 1f) * size.width
        drawLine(
            color = Color.White,
            start = Offset(playX, 0f),
            end = Offset(playX, size.height),
            strokeWidth = 2.5.dp.toPx()
        )
    }
}

@Composable
fun CompactRotaryIndicator(
    label: String,
    valueText: String,
    normalized: Float,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.size(38.dp)) {
            val stroke = 3.5.dp.toPx()
            drawArc(
                color = palette.textSecondary.copy(alpha = 0.2f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = accentColor,
                startAngle = 135f,
                sweepAngle = 270f * normalized.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
        Text(text = valueText, style = MaterialTheme.typography.labelSmall, color = palette.textPrimary)
    }
}
