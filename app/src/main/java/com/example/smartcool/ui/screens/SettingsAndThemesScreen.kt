package com.example.smartcool.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartcool.model.BatteryTelemetry
import com.example.smartcool.model.CustomLiquidThemeConfig
import com.example.smartcool.model.MemoryInfoSnapshot
import com.example.smartcool.model.SmartCoolSettings
import com.example.smartcool.model.SmartCoolThemePreset
import com.example.smartcool.model.formatTemperature
import com.example.smartcool.ui.components.LiquidGlassCard
import com.example.smartcool.ui.components.StatusBadgePill
import com.example.smartcool.ui.theme.LocalLiquidPalette
import com.example.smartcool.ui.theme.buildLiquidPalette
import com.example.smartcool.util.SystemShortcuts
import kotlin.math.roundToInt

@Composable
fun SettingsAndThemesScreen(
    settings: SmartCoolSettings,
    battery: BatteryTelemetry,
    memory: MemoryInfoSnapshot,
    hasNotificationPermission: Boolean,
    onSelectTheme: (SmartCoolThemePreset) -> Unit,
    onUpdateCustomTheme: (CustomLiquidThemeConfig, Boolean) -> Unit,
    onSetUseFahrenheit: (Boolean) -> Unit,
    onSetHighTempAlert: (Boolean) -> Unit,
    onSetHighTempThreshold: (Float) -> Unit,
    onSetChargingHeatAlert: (Boolean) -> Unit,
    onSetLowBatteryAlert: (Boolean) -> Unit,
    onSetLowBatteryThreshold: (Int) -> Unit,
    onSetMonitorInterval: (Int) -> Unit,
    onSendTestNotification: () -> Unit,
    onClearHistory: () -> Unit
) {
    val palette = LocalLiquidPalette.current
    val context = LocalContext.current

    var previewPreset by remember(settings.selectedTheme) { mutableStateOf(settings.selectedTheme) }
    var customDraft by remember(settings.customTheme) { mutableStateOf(settings.customTheme) }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    val accentSwatches = listOf(
        0xFF0284C7L, // Ice Blue
        0xFF38BDF8L, // Glacial Cyan
        0xFF2563EBL, // Ocean Sapphire
        0xFF059669L, // Emerald Cool
        0xFF6366F1L, // Cobalt Indigo
        0xFFEC4899L, // Rose Quartz
        0xFFF59E0BL, // Solar Amber
        0xFF64748BL  // Anodized Silver
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .testTag("settings_themes_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Theme Studio Header + Live Theme Preview Card
            item {
                val previewPalette = buildLiquidPalette(previewPreset, customDraft)
                LiquidGlassCard(highlighted = true, cornerRadius = 24.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Palette, contentDescription = null, tint = palette.primaryAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "THEME STUDIO (7 LIQUID THEMES)",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap any theme to preview & apply instantly across the entire app. Saved permanently.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Theme Preview Mini-Console
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        previewPalette.backgroundTop,
                                        previewPalette.backgroundBottom
                                    )
                                )
                            )
                            .border(1.2.dp, previewPalette.primaryAccent.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
                            .padding(14.dp)
                            .testTag("live_theme_preview_card")
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "LIVE PREVIEW • ${previewPreset.displayName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = previewPalette.primaryAccent
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(previewPalette.primaryAccent)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (previewPalette.isDark) "DARK" else "LIGHT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (previewPalette.isDark) Color(0xFF041326) else Color.White
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(previewPalette.glassSurface)
                                    .border(1.dp, previewPalette.borderStroke, RoundedCornerShape(14.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${battery.levelPercent}% • ${formatTemperature(battery.temperatureCelsius, settings.useFahrenheit)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = previewPalette.textPrimary
                                    )
                                    Text(
                                        text = "Liquid Glass Surface Preview",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = previewPalette.textSecondary
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(previewPalette.primaryAccent)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // All 7 Theme Rows
                    SmartCoolThemePreset.entries.forEach { preset ->
                        val isActive = settings.selectedTheme == preset
                        val p = buildLiquidPalette(preset, customDraft)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isActive) palette.primaryAccent.copy(alpha = 0.14f) else palette.glassElevated
                                )
                                .border(
                                    width = if (isActive) 1.5.dp else 1.dp,
                                    color = if (isActive) palette.primaryAccent else palette.borderStroke,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    previewPreset = preset
                                    if (preset == SmartCoolThemePreset.CUSTOM) {
                                        onUpdateCustomTheme(customDraft, true)
                                    } else {
                                        onSelectTheme(preset)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                                .testTag("theme_option_${preset.name.lowercase()}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(p.backgroundTop, p.primaryAccent)
                                        )
                                    )
                                    .border(1.dp, palette.specularTopBorder, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = preset.subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary
                                )
                            }
                            if (isActive) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Active theme",
                                    tint = palette.primaryAccent
                                )
                            }
                        }
                    }
                }
            }

            // Custom Theme & Accent Color Picker
            item {
                LiquidGlassCard(highlighted = settings.selectedTheme == SmartCoolThemePreset.CUSTOM) {
                    Text(
                        text = "CUSTOM THEME & ACCENT STUDIO",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.primaryAccent
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dark Glass Appearance",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "Switch Custom Theme between Light Frosted & Dark Obsidian",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                        }
                        Switch(
                            checked = customDraft.isDarkBase,
                            onCheckedChange = { isDark ->
                                previewPreset = SmartCoolThemePreset.CUSTOM
                                customDraft = customDraft.copy(isDarkBase = isDark)
                                onUpdateCustomTheme(customDraft, true)
                            },
                            modifier = Modifier.testTag("custom_theme_dark_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Custom Accent Color",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        accentSwatches.forEachIndexed { idx, hex ->
                            val selected = customDraft.accentColorHex == hex
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(hex))
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color = if (selected) palette.textPrimary else palette.borderStroke,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        previewPreset = SmartCoolThemePreset.CUSTOM
                                        customDraft = customDraft.copy(
                                            accentColorHex = hex,
                                            secondaryAccentHex = hex
                                        )
                                        onUpdateCustomTheme(customDraft, true)
                                    }
                                    .testTag("custom_accent_swatch_$idx")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Glass Card Opacity (${(customDraft.glassOpacity * 100).roundToInt()}%)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textPrimary
                    )
                    Slider(
                        value = customDraft.glassOpacity,
                        onValueChange = {
                            previewPreset = SmartCoolThemePreset.CUSTOM
                            customDraft = customDraft.copy(glassOpacity = it)
                        },
                        onValueChangeFinished = {
                            onUpdateCustomTheme(customDraft, true)
                        },
                        valueRange = 0.50f..0.94f
                    )

                    Text(
                        text = "Liquid Glow Intensity (${(customDraft.glowIntensity * 100).roundToInt()}%)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textPrimary
                    )
                    Slider(
                        value = customDraft.glowIntensity,
                        onValueChange = {
                            previewPreset = SmartCoolThemePreset.CUSTOM
                            customDraft = customDraft.copy(glowIntensity = it)
                        },
                        onValueChangeFinished = {
                            onUpdateCustomTheme(customDraft, true)
                        },
                        valueRange = 0.15f..0.95f
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            previewPreset = SmartCoolThemePreset.CUSTOM
                            onUpdateCustomTheme(customDraft, true)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("apply_custom_theme_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primaryAccent,
                            contentColor = if (palette.isDark) Color(0xFF041326) else Color.White
                        )
                    ) {
                        Text("Apply & Save Custom Theme")
                    }
                }
            }

            // Notification Management
            item {
                LiquidGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = palette.primaryAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "NOTIFICATION MANAGEMENT",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.primaryAccent
                            )
                        }
                        StatusBadgePill(
                            text = if (hasNotificationPermission) "Allowed" else "Permission Needed",
                            dotColor = if (hasNotificationPermission) palette.successEmerald else palette.warningAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    SettingSwitchRow(
                        title = "High-Temperature Alert",
                        subtitle = "Notify at ${formatTemperature(settings.highTempThresholdC, settings.useFahrenheit)}",
                        checked = settings.highTempAlertEnabled,
                        onCheckedChange = onSetHighTempAlert,
                        testTag = "switch_high_temp_alert"
                    )
                    Slider(
                        value = settings.highTempThresholdC,
                        onValueChange = onSetHighTempThreshold,
                        valueRange = 37f..46f
                    )

                    SettingSwitchRow(
                        title = "Charging Heat Warning",
                        subtitle = "Alert when charging exceeds 38.5°C / 101.3°F",
                        checked = settings.chargingHeatAlertEnabled,
                        onCheckedChange = onSetChargingHeatAlert,
                        testTag = "switch_charging_heat_alert"
                    )

                    SettingSwitchRow(
                        title = "Low Battery Alert",
                        subtitle = "Notify below ${settings.lowBatteryThresholdPct}% while discharging",
                        checked = settings.lowBatteryAlertEnabled,
                        onCheckedChange = onSetLowBatteryAlert,
                        testTag = "switch_low_battery_alert"
                    )
                    Slider(
                        value = settings.lowBatteryThresholdPct.toFloat(),
                        onValueChange = { onSetLowBatteryThreshold(it.roundToInt()) },
                        valueRange = 10f..35f
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                            Button(
                                onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("request_notif_perm_btn")
                            ) {
                                Text("Allow Alerts")
                            }
                        }
                        OutlinedButton(
                            onClick = onSendTestNotification,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("send_test_notif_btn")
                        ) {
                            Text("Send Test Alert")
                        }
                    }
                }
            }

            // App Settings
            item {
                LiquidGlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Settings, contentDescription = null, tint = palette.primaryAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "APP SETTINGS",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.primaryAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    SettingSwitchRow(
                        title = "Use Fahrenheit (°F)",
                        subtitle = if (settings.useFahrenheit) "Displaying temperatures in °F" else "Displaying temperatures in °C",
                        checked = settings.useFahrenheit,
                        onCheckedChange = onSetUseFahrenheit,
                        testTag = "switch_fahrenheit"
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Foreground Refresh Interval: ${settings.monitorIntervalSeconds}s",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textPrimary
                    )
                    Slider(
                        value = settings.monitorIntervalSeconds.toFloat(),
                        onValueChange = { onSetMonitorInterval(it.roundToInt()) },
                        valueRange = 2f..15f
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { SystemShortcuts.openUsageAccessSettings(context) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("settings_usage_access_btn")
                        ) {
                            Text("Usage Access")
                        }
                        OutlinedButton(
                            onClick = onClearHistory,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("settings_clear_history_btn")
                        ) {
                            Text("Clear History")
                        }
                    }
                }
            }

            // Privacy & About Section
            item {
                LiquidGlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.PrivacyTip, contentDescription = null, tint = palette.primaryAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PRIVACY & ABOUT",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.primaryAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Privacy Guarantee",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "100% offline processing. Zero ads, zero trackers, zero network transmission, and no root access required. Optional Usage Access data stays strictly inside local device memory.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "About Smart Cool & Battery Saver Pro",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "Version 1.0.0 • Engineered for Vivo X300 & Android 8.0+ (API 26–36) • Active device: ${memory.deviceManufacturer} ${memory.deviceModel}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    val palette = LocalLiquidPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
