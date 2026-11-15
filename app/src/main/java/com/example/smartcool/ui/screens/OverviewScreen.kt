package com.example.smartcool.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.smartcool.model.BatteryTelemetry
import com.example.smartcool.model.MemoryInfoSnapshot
import com.example.smartcool.model.SmartCoolSettings
import com.example.smartcool.model.SmartMode
import com.example.smartcool.model.TelemetrySamplePoint
import com.example.smartcool.model.ThermalSeverity
import com.example.smartcool.model.ThermalTelemetry
import com.example.smartcool.model.formatTemperature
import com.example.smartcool.ui.components.LiquidGlassCard
import com.example.smartcool.ui.components.LiquidRingGauge
import com.example.smartcool.ui.components.MetricMiniTile
import com.example.smartcool.ui.components.StatusBadgePill
import com.example.smartcool.ui.components.TemperatureHistoryChart
import com.example.smartcool.ui.theme.LocalLiquidPalette
import com.example.smartcool.util.SystemShortcuts

@Composable
fun OverviewScreen(
    settings: SmartCoolSettings,
    battery: BatteryTelemetry,
    thermal: ThermalTelemetry,
    memory: MemoryInfoSnapshot,
    history: List<TelemetrySamplePoint>,
    onSelectMode: (SmartMode) -> Unit,
    onRefresh: () -> Unit,
    onNavigateCooling: () -> Unit,
    onNavigateBattery: () -> Unit,
    onNavigatePerformance: () -> Unit,
    onNavigateModes: () -> Unit,
    onNavigateThemes: () -> Unit
) {
    val palette = LocalLiquidPalette.current
    val context = LocalContext.current

    val tempColor = when {
        thermal.highTempExceeded || thermal.thermalSeverity.rank >= ThermalSeverity.SEVERE.rank -> palette.dangerRed
        thermal.chargingHeatWarning || battery.temperatureCelsius >= 37f -> palette.warningAmber
        else -> palette.coolCyan
    }
    val batteryColor = when {
        battery.levelPercent <= settings.lowBatteryThresholdPct && !battery.isCharging -> palette.dangerRed
        battery.isCharging -> palette.successEmerald
        else -> palette.primaryAccent
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .testTag("overview_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Header & Liquid Glass Hero Banner
            item {
                LiquidGlassCard(highlighted = true, cornerRadius = 26.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SMART COOL & BATTERY SAVER PRO",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.primaryAccent
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${memory.deviceManufacturer} ${memory.deviceModel}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                            Text(
                                text = memory.androidVersionLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onNavigateThemes,
                                modifier = Modifier.testTag("overview_theme_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Palette,
                                    contentDescription = stringResource(id = R.string.cd_open_themes),
                                    tint = palette.primaryAccent
                                )
                            }
                            IconButton(
                                onClick = onRefresh,
                                modifier = Modifier.testTag("overview_refresh_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = stringResource(id = R.string.cd_refresh_telemetry),
                                    tint = palette.textPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(116.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.dp, palette.specularTopBorder, RoundedCornerShape(18.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_liquid_glass_hero_1794747357585),
                            contentDescription = stringResource(id = R.string.cd_hero_glass),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            palette.backgroundTop.copy(alpha = 0.85f),
                                            palette.backgroundTop.copy(alpha = 0.35f)
                                        )
                                    )
                                )
                                .padding(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.align(Alignment.CenterStart),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                StatusBadgePill(
                                    text = settings.smartMode.title,
                                    dotColor = palette.primaryAccent
                                )
                                Text(
                                    text = if (battery.isPowerSaveMode) {
                                        "Android Battery Saver: ACTIVE"
                                    } else {
                                        "Theme: ${settings.selectedTheme.displayName}"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = memory.deviceProfileNote,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }

            // Dynamic Charging Heat or High Temperature Banner
            item {
                AnimatedVisibility(visible = thermal.chargingHeatWarning || thermal.highTempExceeded) {
                    LiquidGlassCard(
                        highlighted = true,
                        accentOverride = palette.dangerRed
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WarningAmber,
                                contentDescription = "Thermal advisory",
                                tint = palette.dangerRed,
                                modifier = Modifier.size(26.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (thermal.chargingHeatWarning) {
                                        "Charging Heat Warning (${formatTemperature(battery.temperatureCelsius, settings.useFahrenheit)})"
                                    } else {
                                        "High Temperature Advisory (${formatTemperature(battery.temperatureCelsius, settings.useFahrenheit)})"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.dangerRed
                                )
                                Text(
                                    text = "Battery thermistor exceeds safe comfort range. Tap Cooling for safe thermal guidance.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onNavigateCooling,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("overview_open_cooling_alert_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.dangerRed,
                                contentColor = Color.White
                            )
                        ) {
                            Text("View Safe Cooling Actions")
                        }
                    }
                }
            }

            // Live Twin Gauges: Battery % & Battery Temp
            item {
                LiquidGlassCard {
                    Text(
                        text = "LIVE SENSOR TELEMETRY",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.primaryAccent
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LiquidRingGauge(
                            progress = battery.levelPercent / 100f,
                            primaryValue = "${battery.levelPercent}%",
                            label = battery.statusLabel,
                            subLabel = battery.plugTypeLabel,
                            accentColor = batteryColor,
                            modifier = Modifier
                                .clickable(onClick = onNavigateBattery)
                                .testTag("overview_battery_gauge")
                        )
                        LiquidRingGauge(
                            progress = ((battery.temperatureCelsius - 15f) / 35f).coerceIn(0.05f, 1f),
                            primaryValue = formatTemperature(battery.temperatureCelsius, settings.useFahrenheit),
                            label = "Battery Temp",
                            subLabel = thermal.thermalSeverity.label,
                            accentColor = tempColor,
                            modifier = Modifier
                                .clickable(onClick = onNavigateCooling)
                                .testTag("overview_temp_gauge")
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricMiniTile(
                            label = "Voltage",
                            value = "${"%.2f".format(battery.voltageVolts)} V",
                            accent = palette.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricMiniTile(
                            label = "Health",
                            value = battery.healthState.label,
                            accent = palette.successEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        MetricMiniTile(
                            label = "Free RAM",
                            value = "${memory.availableMb} MB",
                            accent = palette.primaryAccent,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Smart Modes Switcher
            item {
                LiquidGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SMART MODES",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.primaryAccent
                        )
                        Text(
                            text = "Modes & Daily Report",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.primaryAccent,
                            modifier = Modifier
                                .clickable(onClick = onNavigateModes)
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .testTag("overview_open_modes_btn")
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = settings.smartMode.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SmartModeChip(
                            label = "Normal",
                            icon = Icons.Filled.Speed,
                            selected = settings.smartMode == SmartMode.NORMAL,
                            onClick = { onSelectMode(SmartMode.NORMAL) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mode_chip_normal")
                        )
                        SmartModeChip(
                            label = "Cooling",
                            icon = Icons.Filled.AcUnit,
                            selected = settings.smartMode == SmartMode.COOLING,
                            onClick = { onSelectMode(SmartMode.COOLING) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mode_chip_cooling")
                        )
                        SmartModeChip(
                            label = "Extreme",
                            icon = Icons.Filled.BatterySaver,
                            selected = settings.smartMode == SmartMode.EXTREME_BATTERY_SAVER,
                            onClick = { onSelectMode(SmartMode.EXTREME_BATTERY_SAVER) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mode_chip_extreme")
                        )
                    }
                }
            }

            // Live Temperature Curve Preview
            item {
                LiquidGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "THERMAL TIMELINE",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.primaryAccent
                        )
                        Text(
                            text = "Details",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.primaryAccent,
                            modifier = Modifier
                                .clickable(onClick = onNavigateCooling)
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    TemperatureHistoryChart(
                        samples = history,
                        useFahrenheit = settings.useFahrenheit
                    )
                }
            }

            // Quick Actions
            item {
                LiquidGlassCard {
                    Text(
                        text = "QUICK CONTROLS",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.primaryAccent
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { SystemShortcuts.openBatterySaverSettings(context) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("quick_battery_saver_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primaryAccent,
                                contentColor = if (palette.isDark) Color(0xFF041326) else Color.White
                            )
                        ) {
                            Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Battery Saver")
                        }
                        OutlinedButton(
                            onClick = onNavigatePerformance,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("quick_performance_btn")
                        ) {
                            Icon(Icons.Filled.Thermostat, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RAM & Apps")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { SystemShortcuts.openBatteryUsageSettings(context) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("quick_system_battery_btn")
                        ) {
                            Icon(Icons.Filled.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("System Usage")
                        }
                        OutlinedButton(
                            onClick = onNavigateThemes,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("quick_themes_btn")
                        ) {
                            Icon(Icons.Filled.Palette, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("7 Themes")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartModeChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalLiquidPalette.current
    val bg = if (selected) palette.primaryAccent else palette.glassElevated
    val fg = if (selected) {
        if (palette.isDark) Color(0xFF041326) else Color.White
    } else {
        palette.textPrimary
    }

    Column(
        modifier = modifier
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(
                width = 1.dp,
                color = if (selected) palette.primaryAccent else palette.borderStroke,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = fg, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = fg,
            maxLines = 1
        )
    }
}
