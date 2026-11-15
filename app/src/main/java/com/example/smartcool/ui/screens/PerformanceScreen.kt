package com.example.smartcool.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.smartcool.model.MemoryInfoSnapshot
import com.example.smartcool.model.ThermalSeverity
import com.example.smartcool.model.ThermalTelemetry
import com.example.smartcool.model.UsageInsightsSnapshot
import com.example.smartcool.model.formatDurationShort
import com.example.smartcool.ui.components.LiquidGlassCard
import com.example.smartcool.ui.components.MetricMiniTile
import com.example.smartcool.ui.components.StatusBadgePill
import com.example.smartcool.ui.theme.LocalLiquidPalette
import com.example.smartcool.util.SystemShortcuts

@Composable
fun PerformanceScreen(
    memory: MemoryInfoSnapshot,
    thermal: ThermalTelemetry,
    usage: UsageInsightsSnapshot,
    onRefresh: () -> Unit
) {
    val palette = LocalLiquidPalette.current
    val context = LocalContext.current

    val ramColor = when {
        memory.isLowMemory || memory.usedPercent >= 88f -> palette.dangerRed
        memory.usedPercent >= 74f -> palette.warningAmber
        else -> palette.primaryAccent
    }
    val thermalColor = when (thermal.thermalSeverity) {
        ThermalSeverity.NONE -> palette.successEmerald
        ThermalSeverity.LIGHT -> palette.coolCyan
        ThermalSeverity.MODERATE -> palette.warningAmber
        ThermalSeverity.UNSUPPORTED -> palette.textSecondary
        else -> palette.dangerRed
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .testTag("performance_screen_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                LiquidGlassCard(highlighted = true, cornerRadius = 24.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PERFORMANCE MONITOR",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "Live RAM pressure, CPU thermal status & UsageStats insights",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                        }
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.testTag("perf_refresh_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = stringResource(id = R.string.cd_refresh_telemetry),
                                tint = palette.primaryAccent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Memory, contentDescription = null, tint = ramColor)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "System Memory (RAM)",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary
                            )
                        }
                        Text(
                            text = "${"%.1f".format(memory.usedPercent)}% Used",
                            style = MaterialTheme.typography.labelLarge,
                            color = ramColor
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.silverChrome.copy(alpha = if (palette.isDark) 0.18f else 0.45f))
                    ) {
                        val fillWidth = size.width * (memory.usedPercent / 100f).coerceIn(0.02f, 1f)
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(palette.coolCyan, ramColor)
                            ),
                            size = Size(fillWidth, size.height),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricMiniTile(
                            label = "Available RAM",
                            value = "${memory.availableMb} MB",
                            accent = palette.successEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        MetricMiniTile(
                            label = "Used RAM",
                            value = "${memory.usedMb} MB",
                            accent = ramColor,
                            modifier = Modifier.weight(1f)
                        )
                        MetricMiniTile(
                            label = "Total RAM",
                            value = "${"%.1f".format(memory.totalMb / 1024f)} GB",
                            accent = palette.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // CPU Thermal & Hardware Info
            item {
                LiquidGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CPU THERMAL & DEVICE SPEC",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.primaryAccent
                        )
                        StatusBadgePill(
                            text = thermal.thermalSeverity.label,
                            dotColor = thermalColor
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricMiniTile(
                            label = "CPU Cores",
                            value = "${memory.cpuCoreCount} Threads",
                            accent = palette.primaryAccent,
                            modifier = Modifier.weight(1f)
                        )
                        MetricMiniTile(
                            label = "Low Mem Threshold",
                            value = "${memory.thresholdMb} MB",
                            accent = palette.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricMiniTile(
                            label = "Low Mem Flag",
                            value = if (memory.isLowMemory) "YES" else "Normal",
                            accent = if (memory.isLowMemory) palette.dangerRed else palette.successEmerald,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${memory.deviceManufacturer} ${memory.deviceModel} • ${memory.hardwareBoard}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = thermal.thermalStatusDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
            }

            // Honest RAM & Battery Education Card
            item {
                LiquidGlassCard(accentOverride = palette.coolCyan) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "Memory honesty note",
                            tint = palette.coolCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "HONEST MEMORY & BATTERY SCIENCE",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.coolCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(id = R.string.honest_ram_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textPrimary
                    )
                }
            }

            // App Foreground & Background Usage (UsageStatsManager)
            item {
                LiquidGlassCard(highlighted = !usage.hasUsagePermission) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Apps, contentDescription = null, tint = palette.primaryAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "FOREGROUND & BACKGROUND INSIGHTS",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.primaryAccent
                            )
                        }
                        if (usage.hasUsagePermission) {
                            Text(
                                text = formatDurationShort(usage.totalScreenTimeTodayMs),
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.textPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (!usage.hasUsagePermission) {
                        Text(
                            text = stringResource(id = R.string.usage_access_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.usage_access_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { SystemShortcuts.openUsageAccessSettings(context) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("grant_usage_access_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.primaryAccent,
                                    contentColor = if (palette.isDark) Color(0xFF041326) else Color.White
                                )
                            ) {
                                Icon(Icons.Filled.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Grant Access")
                            }
                            OutlinedButton(
                                onClick = onRefresh,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("recheck_usage_access_btn")
                            ) {
                                Text("Recheck")
                            }
                        }
                    } else {
                        Text(
                            text = usage.backgroundInsightSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                        if (usage.topApps.isEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No foreground usage intervals recorded yet today. Tap Refresh after opening apps.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                        }
                    }
                }
            }

            if (usage.hasUsagePermission && usage.topApps.isNotEmpty()) {
                items(usage.topApps, key = { it.packageName }) { app ->
                    LiquidGlassCard(
                        cornerRadius = 18.dp,
                        modifier = Modifier.clickable {
                            SystemShortcuts.openAppDetailsSettings(context, app.packageName)
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${app.packageName} • ${app.standbyBucketLabel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = formatDurationShort(app.foregroundTimeMs),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.primaryAccent
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Filled.OpenInNew,
                                    contentDescription = "Open App Info",
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
