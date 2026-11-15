package com.example.smartcool.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.smartcool.model.BatteryTelemetry
import com.example.smartcool.model.SmartCoolSettings
import com.example.smartcool.model.SmartMode
import com.example.smartcool.model.TelemetrySamplePoint
import com.example.smartcool.model.ThermalSeverity
import com.example.smartcool.model.ThermalTelemetry
import com.example.smartcool.model.formatTemperature
import com.example.smartcool.ui.components.LiquidGlassCard
import com.example.smartcool.ui.components.MetricMiniTile
import com.example.smartcool.ui.components.StatusBadgePill
import com.example.smartcool.ui.components.TemperatureHistoryChart
import com.example.smartcool.ui.theme.LocalLiquidPalette
import com.example.smartcool.util.SystemShortcuts

@Composable
fun CoolingScreen(
    settings: SmartCoolSettings,
    battery: BatteryTelemetry,
    thermal: ThermalTelemetry,
    history: List<TelemetrySamplePoint>,
    onToggleCoolingMode: () -> Unit,
    onClearHistory: () -> Unit
) {
    val palette = LocalLiquidPalette.current
    val context = LocalContext.current

    val severityColor = when (thermal.thermalSeverity) {
        ThermalSeverity.NONE -> palette.successEmerald
        ThermalSeverity.LIGHT -> palette.coolCyan
        ThermalSeverity.MODERATE -> palette.warningAmber
        ThermalSeverity.SEVERE, ThermalSeverity.CRITICAL, ThermalSeverity.EMERGENCY, ThermalSeverity.SHUTDOWN -> palette.dangerRed
        ThermalSeverity.UNSUPPORTED -> palette.textSecondary
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .testTag("cooling_screen_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                LiquidGlassCard(highlighted = true, cornerRadius = 24.dp) {
                    Text(
                        text = "SMART COOLING & THERMAL API",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Real-time BatteryManager thermistor & Android PowerManager thermal throttling monitor.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.dp, palette.specularTopBorder, RoundedCornerShape(18.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_thermal_ice_wave_1794747358270),
                            contentDescription = stringResource(id = R.string.cd_cooling_wave),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            palette.backgroundTop.copy(alpha = 0.88f),
                                            palette.backgroundTop.copy(alpha = 0.30f)
                                        )
                                    )
                                )
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = formatTemperature(battery.temperatureCelsius, settings.useFahrenheit),
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary
                                    )
                                    Text(
                                        text = "Battery Thermistor Sensor",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.primaryAccent
                                    )
                                }
                                StatusBadgePill(
                                    text = thermal.thermalSeverity.label,
                                    dotColor = severityColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricMiniTile(
                            label = "OS Thermal API",
                            value = if (thermal.androidThermalSupported) thermal.thermalSeverity.label else "API 29+ Only",
                            accent = severityColor,
                            modifier = Modifier.weight(1f)
                        )
                        MetricMiniTile(
                            label = "Thermal Headroom",
                            value = thermal.thermalHeadroom?.let { "%.2f".format(it) } ?: "N/A on HAL",
                            accent = palette.primaryAccent,
                            modifier = Modifier.weight(1f)
                        )
                        MetricMiniTile(
                            label = "Alert Threshold",
                            value = formatTemperature(settings.highTempThresholdC, settings.useFahrenheit),
                            accent = palette.warningAmber,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onToggleCoolingMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("toggle_cooling_mode_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (settings.smartMode == SmartMode.COOLING) palette.coolCyan else palette.primaryAccent,
                            contentColor = if (palette.isDark) Color(0xFF041326) else Color.White
                        )
                    ) {
                        Icon(Icons.Filled.AcUnit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (settings.smartMode == SmartMode.COOLING) {
                                "Cooling Mode Active (2s Sensor Refresh)"
                            } else {
                                "Activate Cooling Monitoring Mode"
                            }
                        )
                    }
                }
            }

            // Thermal Status & Charging Heat Advisory
            item {
                LiquidGlassCard(
                    highlighted = thermal.chargingHeatWarning || thermal.highTempExceeded,
                    accentOverride = if (thermal.chargingHeatWarning) palette.warningAmber else palette.primaryAccent
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (thermal.chargingHeatWarning) Icons.Filled.WarningAmber else Icons.Filled.Info,
                            contentDescription = "Thermal status info",
                            tint = if (thermal.chargingHeatWarning) palette.warningAmber else palette.primaryAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (thermal.chargingHeatWarning) {
                                "Charging Heat Warning Active"
                            } else {
                                "OS Thermal Status Analysis"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = thermal.thermalStatusDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
            }

            // Temperature History Graph
            item {
                LiquidGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TEMPERATURE HISTORY GRAPH",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.primaryAccent
                        )
                        OutlinedButton(
                            onClick = onClearHistory,
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .testTag("clear_temp_history_btn")
                        ) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Clear history", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear")
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    TemperatureHistoryChart(
                        samples = history,
                        useFahrenheit = settings.useFahrenheit
                    )
                }
            }

            // Safe Cooling Recommendations
            item {
                Text(
                    text = "SAFE COOLING RECOMMENDATIONS",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.primaryAccent,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            items(thermal.recommendations, key = { it.id }) { rec ->
                LiquidGlassCard(
                    highlighted = rec.priorityHigh,
                    accentOverride = if (rec.priorityHigh) palette.warningAmber else palette.primaryAccent
                ) {
                    Text(
                        text = rec.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = rec.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                    if (rec.actionType != null && rec.actionLabel != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { SystemShortcuts.executeAction(context, rec.actionType) },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("cooling_rec_${rec.id}_btn")
                        ) {
                            Text(rec.actionLabel)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Honest Sensor Disclosure
            item {
                LiquidGlassCard {
                    Text(
                        text = "SENSOR HONESTY & ACCURACY",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(id = R.string.honest_thermal_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
            }
        }
    }
}
