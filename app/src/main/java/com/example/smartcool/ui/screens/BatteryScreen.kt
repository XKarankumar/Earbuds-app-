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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.OpenInNew
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartcool.model.BatteryHealthState
import com.example.smartcool.model.BatteryTelemetry
import com.example.smartcool.model.SmartCoolSettings
import com.example.smartcool.model.TelemetrySamplePoint
import com.example.smartcool.model.formatTemperature
import com.example.smartcool.ui.components.BatteryHistoryChart
import com.example.smartcool.ui.components.LiquidGlassCard
import com.example.smartcool.ui.components.LiquidRingGauge
import com.example.smartcool.ui.components.MetricMiniTile
import com.example.smartcool.ui.components.StatusBadgePill
import com.example.smartcool.ui.theme.LocalLiquidPalette
import com.example.smartcool.util.SystemShortcuts

@Composable
fun BatteryScreen(
    settings: SmartCoolSettings,
    battery: BatteryTelemetry,
    history: List<TelemetrySamplePoint>
) {
    val palette = LocalLiquidPalette.current
    val context = LocalContext.current

    val healthColor = when (battery.healthState) {
        BatteryHealthState.GOOD -> palette.successEmerald
        BatteryHealthState.OVERHEAT, BatteryHealthState.DEAD, BatteryHealthState.OVER_VOLTAGE -> palette.dangerRed
        else -> palette.warningAmber
    }

    val batteryAccent = when {
        battery.isCharging -> palette.successEmerald
        battery.levelPercent <= settings.lowBatteryThresholdPct -> palette.dangerRed
        else -> palette.primaryAccent
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .testTag("battery_screen_list"),
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
                                text = "BATTERY MANAGEMENT",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "Live Android BatteryManager diagnostics & power controls",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                        }
                        StatusBadgePill(
                            text = if (battery.isPowerSaveMode) "Saver On" else battery.statusLabel,
                            dotColor = batteryAccent
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        LiquidRingGauge(
                            progress = battery.levelPercent / 100f,
                            primaryValue = "${battery.levelPercent}%",
                            label = battery.statusLabel,
                            subLabel = battery.plugTypeLabel,
                            accentColor = batteryAccent
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricMiniTile(
                                label = "Voltage",
                                value = "${"%.3f".format(battery.voltageVolts)} V (${battery.voltageMv} mV)",
                                accent = palette.textPrimary
                            )
                            MetricMiniTile(
                                label = "Battery Temp",
                                value = formatTemperature(battery.temperatureCelsius, settings.useFahrenheit),
                                accent = if (battery.temperatureCelsius >= settings.highTempThresholdC) palette.dangerRed else palette.coolCyan
                            )
                            MetricMiniTile(
                                label = "Reported Health",
                                value = "${battery.healthState.label} • ${battery.technology}",
                                accent = healthColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricMiniTile(
                            label = "Current Flow",
                            value = battery.currentMilliamps?.let { "${it} mA" } ?: "Not reported",
                            accent = palette.primaryAccent,
                            modifier = Modifier.weight(1f)
                        )
                        MetricMiniTile(
                            label = "Charge Counter",
                            value = battery.chargeCounterMah?.let { "${it} mAh" } ?: "Not reported",
                            accent = palette.secondaryAccent,
                            modifier = Modifier.weight(1f)
                        )
                        MetricMiniTile(
                            label = "Cycle Count",
                            value = battery.cycleCount?.toString() ?: "API 34+ / HAL",
                            accent = palette.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Android Battery Saver & System Usage Shortcuts
            item {
                LiquidGlassCard {
                    Text(
                        text = "SYSTEM POWER CONTROLS",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.primaryAccent
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Open native Android Battery Saver and per-app battery drain telemetry.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { SystemShortcuts.openBatterySaverSettings(context) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("battery_saver_shortcut_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primaryAccent,
                                contentColor = if (palette.isDark) Color(0xFF041326) else Color.White
                            )
                        ) {
                            Icon(Icons.Filled.BatterySaver, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Battery Saver")
                        }
                        OutlinedButton(
                            onClick = { SystemShortcuts.openBatteryUsageSettings(context) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("battery_usage_shortcut_btn")
                        ) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Battery Usage")
                        }
                    }
                }
            }

            // Battery Usage History Chart
            item {
                LiquidGlassCard {
                    Text(
                        text = "RECORDED BATTERY LEVEL HISTORY",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.primaryAccent
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Real battery percentage samples logged during app sessions (${history.size} points saved).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    BatteryHistoryChart(samples = history)
                }
            }

            // Dynamic Charging Guidance
            item {
                val guidanceTitle: String
                val guidanceBody: String
                when {
                    battery.isFull -> {
                        guidanceTitle = "Battery Full — Safe to Unplug"
                        guidanceBody = "Your battery reached 100%. Unplugging avoids prolonged trickle-top-up heat and preserves long-term lithium-ion capacity."
                    }
                    battery.isCharging && battery.temperatureCelsius >= 38.5f -> {
                        guidanceTitle = "Warm Fast-Charging Detected"
                        guidanceBody = "Pause heavy workloads and ensure airflow around the back glass while charging to keep cell temperature below 39°C."
                    }
                    battery.isCharging && battery.levelPercent >= 80 -> {
                        guidanceTitle = "Near 80% Longevity Window"
                        guidanceBody = "Stopping routine top-ups around 80–85% reduces high-voltage cathode stress on lithium-ion batteries."
                    }
                    battery.isCharging -> {
                        guidanceTitle = "Healthy Charging Active (${battery.plugTypeLabel})"
                        guidanceBody = "Charging current and temperature are within normal operating bounds. Use certified PD/FlashCharge cables."
                    }
                    battery.levelPercent <= settings.lowBatteryThresholdPct -> {
                        guidanceTitle = "Low Battery Threshold Reached"
                        guidanceBody = "Enable Android Battery Saver or Extreme Battery Saver to restrict background wakeups and protect against deep discharge."
                    }
                    else -> {
                        guidanceTitle = "Optimal Lithium-Ion Range (20% – 80%)"
                        guidanceBody = "Keeping daily discharge between 20% and 80% minimizes chemical aging and reduces total charge cycle wear."
                    }
                }

                LiquidGlassCard(highlighted = battery.isCharging || battery.levelPercent <= settings.lowBatteryThresholdPct) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (battery.isCharging) Icons.Filled.BatteryChargingFull else Icons.Filled.HealthAndSafety,
                            contentDescription = "Charging guidance",
                            tint = palette.primaryAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CHARGING GUIDANCE",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.primaryAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = guidanceTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = guidanceBody,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
            }
        }
    }
}
