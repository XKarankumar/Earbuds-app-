package com.example.smartcool.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartcool.model.BatteryTelemetry
import com.example.smartcool.model.DailyBatteryReport
import com.example.smartcool.model.SmartCoolSettings
import com.example.smartcool.model.SmartCoolThemePreset
import com.example.smartcool.model.SmartMode
import com.example.smartcool.model.formatDurationShort
import com.example.smartcool.model.formatTemperature
import com.example.smartcool.ui.components.LiquidGlassCard
import com.example.smartcool.ui.components.MetricMiniTile
import com.example.smartcool.ui.components.StatusBadgePill
import com.example.smartcool.ui.theme.LocalLiquidPalette
import com.example.smartcool.util.SystemShortcuts

@Composable
fun ModesAndReportsScreen(
    settings: SmartCoolSettings,
    battery: BatteryTelemetry,
    report: DailyBatteryReport,
    onSelectMode: (SmartMode) -> Unit,
    onSelectTheme: (SmartCoolThemePreset) -> Unit
) {
    val palette = LocalLiquidPalette.current
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .testTag("modes_reports_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                LiquidGlassCard(highlighted = true, cornerRadius = 24.dp) {
                    Text(
                        text = "SMART MODES & DAILY REPORT",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Adaptive monitoring profiles, Android power controls & verified daily telemetry.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
            }

            // Normal Mode
            item {
                SmartModeDetailCard(
                    mode = SmartMode.NORMAL,
                    icon = Icons.Filled.Speed,
                    selected = settings.smartMode == SmartMode.NORMAL,
                    details = "Balanced sensor cadence (${settings.monitorIntervalSeconds}s foreground refresh), standard high-temperature and low-battery alerts.",
                    onActivate = { onSelectMode(SmartMode.NORMAL) },
                    testTag = "smart_mode_normal_card"
                )
            }

            // Cooling Mode
            item {
                SmartModeDetailCard(
                    mode = SmartMode.COOLING,
                    icon = Icons.Filled.AcUnit,
                    selected = settings.smartMode == SmartMode.COOLING,
                    details = "Switches foreground sensor polling to 2s intervals, prioritizes charging-heat detection, and surfaces live Android Thermal API headroom.",
                    onActivate = { onSelectMode(SmartMode.COOLING) },
                    testTag = "smart_mode_cooling_card",
                    extraContent = {
                        OutlinedButton(
                            onClick = { SystemShortcuts.openDisplaySettings(context) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("cooling_mode_display_btn")
                        ) {
                            Text("Open Display Brightness & Refresh")
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                )
            }

            // Extreme Battery Saver Mode
            item {
                SmartModeDetailCard(
                    mode = SmartMode.EXTREME_BATTERY_SAVER,
                    icon = Icons.Filled.BatterySaver,
                    selected = settings.smartMode == SmartMode.EXTREME_BATTERY_SAVER,
                    details = "Reduces in-app polling overhead to 8s, links straight to Android Battery Saver & battery optimization controls, and lets you switch to true #000000 Midnight AMOLED.",
                    onActivate = { onSelectMode(SmartMode.EXTREME_BATTERY_SAVER) },
                    testTag = "smart_mode_extreme_card",
                    extraContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Android Battery Saver",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary
                                )
                                StatusBadgePill(
                                    text = if (battery.isPowerSaveMode) "ACTIVE" else "Inactive",
                                    dotColor = if (battery.isPowerSaveMode) palette.successEmerald else palette.textSecondary
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { SystemShortcuts.openBatterySaverSettings(context) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("extreme_open_android_saver_btn"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.primaryAccent,
                                        contentColor = if (palette.isDark) Color(0xFF041326) else Color.White
                                    )
                                ) {
                                    Text("Android Saver")
                                }
                                OutlinedButton(
                                    onClick = { onSelectTheme(SmartCoolThemePreset.MIDNIGHT_AMOLED) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("extreme_amoled_btn")
                                ) {
                                    Icon(Icons.Filled.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("AMOLED Black")
                                }
                            }
                        }
                    }
                )
            }

            // Daily Battery & Screen Usage Report
            item {
                LiquidGlassCard(highlighted = true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Assessment, contentDescription = null, tint = palette.primaryAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DAILY BATTERY & SCREEN REPORT (${report.dateKey})",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.primaryAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    if (report.sampleCount == 0) {
                        Text(
                            text = "Collecting today's verified sensor samples. Leave the dashboard open briefly or check back later today for min/max/average analytics.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricMiniTile(
                                label = "Min Temp",
                                value = report.minTempCelsius?.let { formatTemperature(it, settings.useFahrenheit) } ?: "--",
                                accent = palette.coolCyan,
                                modifier = Modifier.weight(1f)
                            )
                            MetricMiniTile(
                                label = "Avg Temp",
                                value = report.avgTempCelsius?.let { formatTemperature(it, settings.useFahrenheit) } ?: "--",
                                accent = palette.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            MetricMiniTile(
                                label = "Peak Temp",
                                value = report.maxTempCelsius?.let { formatTemperature(it, settings.useFahrenheit) } ?: "--",
                                accent = palette.warningAmber,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricMiniTile(
                                label = "Battery Range",
                                value = "${report.minBatteryPercent ?: battery.levelPercent}% – ${report.maxBatteryPercent ?: battery.levelPercent}%",
                                accent = palette.successEmerald,
                                modifier = Modifier.weight(1f)
                            )
                            MetricMiniTile(
                                label = "Peak Thermal",
                                value = report.peakThermalLabel,
                                accent = palette.primaryAccent,
                                modifier = Modifier.weight(1f)
                            )
                            MetricMiniTile(
                                label = "Samples",
                                value = "${report.sampleCount}",
                                accent = palette.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricMiniTile(
                            label = "System Screen Time",
                            value = if (report.screenTimeTodayMs > 0L) {
                                formatDurationShort(report.screenTimeTodayMs)
                            } else {
                                "Grant Usage Access"
                            },
                            accent = palette.primaryAccent,
                            modifier = Modifier.weight(1f)
                        )
                        MetricMiniTile(
                            label = "Active App Session",
                            value = "${report.sessionScreenOnMinutes} min",
                            accent = palette.secondaryAccent,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartModeDetailCard(
    mode: SmartMode,
    icon: ImageVector,
    selected: Boolean,
    details: String,
    onActivate: () -> Unit,
    testTag: String,
    extraContent: (@Composable () -> Unit)? = null
) {
    val palette = LocalLiquidPalette.current
    LiquidGlassCard(highlighted = selected) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = icon,
                    contentDescription = mode.title,
                    tint = if (selected) palette.primaryAccent else palette.textSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = mode.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary
                )
            }
            StatusBadgePill(
                text = if (selected) "ACTIVE" else "Standby",
                dotColor = if (selected) palette.successEmerald else palette.textSecondary
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = details,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = onActivate,
            enabled = !selected,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag(testTag),
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.primaryAccent,
                contentColor = if (palette.isDark) Color(0xFF041326) else Color.White
            )
        ) {
            Text(if (selected) "Currently Active" else "Activate ${mode.title}")
        }
        if (extraContent != null) {
            Spacer(modifier = Modifier.height(10.dp))
            extraContent()
        }
    }
}
