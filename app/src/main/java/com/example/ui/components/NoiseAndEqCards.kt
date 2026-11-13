package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.AncStrength
import com.example.model.EqPreset
import com.example.model.NoiseControlMode
import com.example.ui.theme.LocalLiquidGlassStyle
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SilverEdgeRim
import com.example.ui.theme.SilverShadowTint
import com.example.ui.theme.SoftSilverGradientEnd

@Composable
fun NoiseControlSection(
    mode: NoiseControlMode,
    ancStrength: AncStrength,
    personalisedAnc: Boolean,
    onSelectMode: (NoiseControlMode) -> Unit,
    onSelectStrength: (AncStrength) -> Unit,
    onTogglePersonalisedAnc: (Boolean) -> Unit,
    onOpenNoiseSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            NoiseModeTile(
                mode = NoiseControlMode.ANC,
                selected = mode == NoiseControlMode.ANC,
                onClick = { onSelectMode(NoiseControlMode.ANC) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("noise_mode_anc"),
            )
            NoiseModeTile(
                mode = NoiseControlMode.OFF,
                selected = mode == NoiseControlMode.OFF,
                onClick = { onSelectMode(NoiseControlMode.OFF) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("noise_mode_off"),
            )
            NoiseModeTile(
                mode = NoiseControlMode.TRANSPARENCY,
                selected = mode == NoiseControlMode.TRANSPARENCY,
                onClick = { onSelectMode(NoiseControlMode.TRANSPARENCY) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("noise_mode_transparency"),
            )
        }

        LiquidGlassCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenNoiseSettings)
                    .padding(vertical = 4.dp)
                    .testTag("open_noise_settings_row"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Noise control profile",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (mode == NoiseControlMode.ANC) {
                            "${ancStrength.label} · Saved locally (OEM ANC requires vendor BLE)"
                        } else {
                            "Profile: ${mode.label} · Saved locally"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (mode == NoiseControlMode.ANC) ancStrength.label else mode.shortLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "Open noise control settings",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Personalised ANC preference",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Stored in CK profile for companion reference",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = personalisedAnc,
                    onCheckedChange = onTogglePersonalisedAnc,
                    modifier = Modifier.testTag("personalised_anc_switch"),
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "ANC strength preset",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${ancStrength.label} (-${ancStrength.depthDb} dB)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AncStrength.entries.forEach { level ->
                        val selected = level == ancStrength
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                SoftSilverGradientEnd.copy(alpha = 0.65f)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectStrength(level) },
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 10.dp),
                            ) {
                                Text(
                                    text = level.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoiseModeTile(
    mode: NoiseControlMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalLiquidGlassStyle.current.isDark
    val bgColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primary
            isDark -> MaterialTheme.colorScheme.surfaceVariant
            else -> PureWhite.copy(alpha = 0.88f)
        },
        label = "noise_tile_bg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        label = "noise_tile_fg",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.0f else 0.97f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "noise_tile_scale",
    )

    Box(
        modifier = modifier
            .scale(scale)
            .height(92.dp)
            .shadow(
                elevation = if (selected) 10.dp else 6.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = SilverShadowTint.copy(alpha = 0.25f),
            )
            .clip(RoundedCornerShape(22.dp))
            .background(bgColor)
            .border(
                width = 1.dp,
                color = if (selected) Color.Transparent else SilverEdgeRim,
                shape = RoundedCornerShape(22.dp),
            )
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = when (mode) {
                    NoiseControlMode.ANC -> Icons.Rounded.VolumeOff
                    NoiseControlMode.OFF -> Icons.Rounded.RadioButtonUnchecked
                    NoiseControlMode.TRANSPARENCY -> Icons.Rounded.Hearing
                },
                contentDescription = mode.label,
                tint = contentColor,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = mode.label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

@Composable
fun EqualizerPreviewCard(
    preset: EqPreset,
    bandsDb: List<Float>,
    hardwareStatus: String,
    onOpenEqualizer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.testTag("equalizer_preview_card"),
        onClick = onOpenEqualizer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Equalizer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Column {
                    Text(
                        text = "Equalizer Studio",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${preset.label} · ${preset.description}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = "Open Equalizer Studio",
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        FrequencyCurveCanvas(
            bandsDb = bandsDb,
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = hardwareStatus,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Tune 5 bands ›",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun FrequencyCurveCanvas(
    bandsDb: List<Float>,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalLiquidGlassStyle.current.isDark
    val animatedBands = List(5) { idx ->
        val value by animateFloatAsState(
            targetValue = bandsDb.getOrElse(idx) { 0f },
            animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
            label = "eq_band_anim_$idx",
        )
        value
    }
    val gridColor = MaterialTheme.colorScheme.outline
    val lineColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isDark) Color.White.copy(alpha = 0.05f)
                else SoftSilverGradientEnd.copy(alpha = 0.50f),
            ),
    ) {
        val padX = 16.dp.toPx()
        val padY = 14.dp.toPx()
        val usableW = (size.width - padX * 2).coerceAtLeast(1f)
        val usableH = (size.height - padY * 2).coerceAtLeast(1f)
        val midY = padY + usableH / 2f

        drawLine(
            color = gridColor,
            start = Offset(padX, midY),
            end = Offset(size.width - padX, midY),
            strokeWidth = 1.dp.toPx(),
        )

        val points = animatedBands.mapIndexed { idx, db ->
            val x = padX + (usableW * idx / 4f)
            val norm = (-db / 12f).coerceIn(-1f, 1f)
            val y = midY + norm * (usableH / 2f)
            Offset(x, y)
        }

        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 0 until points.lastIndex) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val cx = (p0.x + p1.x) / 2f
                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            }
        }

        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(points.last().x, size.height)
            lineTo(points.first().x, size.height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = 0.16f),
                    lineColor.copy(alpha = 0.03f),
                    Color.Transparent,
                ),
            ),
        )

        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round),
        )

        points.forEach { pt ->
            drawCircle(
                color = if (isDark) Color.Black else PureWhite,
                radius = 5.dp.toPx(),
                center = pt,
            )
            drawCircle(
                color = lineColor,
                radius = 3.2.dp.toPx(),
                center = pt,
            )
        }
    }
}
