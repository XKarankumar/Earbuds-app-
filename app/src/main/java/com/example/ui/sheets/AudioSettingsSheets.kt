package com.example.ui.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AncStrength
import com.example.model.BudsStudioUiState
import com.example.model.EqPreset
import com.example.model.GestureAction
import com.example.model.NoiseControlMode
import com.example.ui.components.FrequencyCurveCanvas
import com.example.ui.components.GlassSubCard
import com.example.ui.theme.StatusEmerald
import java.util.Locale
import kotlin.math.roundToInt

private val BAND_LABELS = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")

@Composable
fun EqualizerSettingsSheetContent(
    uiState: BudsStudioUiState,
    onSelectPreset: (EqPreset) -> Unit,
    onUpdateBand: (Int, Float) -> Unit,
    onUpdateBassBoost: (Float) -> Unit,
) {
    GlassSubCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = uiState.eqPreset.label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Surface(
                shape = CircleShape,
                color = if (uiState.isHardwareEqAttached) {
                    StatusEmerald.copy(alpha = 0.16f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ) {
                Text(
                    text = uiState.hardwareEqStatus,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (uiState.isHardwareEqAttached) StatusEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }

        FrequencyCurveCanvas(
            bandsDb = uiState.eqBandsDb,
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EqPreset.entries.forEach { preset ->
            FilterChip(
                selected = uiState.eqPreset == preset,
                onClick = { onSelectPreset(preset) },
                label = { Text(preset.label) },
                modifier = Modifier.testTag("eq_preset_${preset.name.lowercase()}"),
            )
        }
    }

    GlassSubCard {
        Text(
            text = "5-BAND AUDIOFX EQUALIZER",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        BAND_LABELS.forEachIndexed { index, label ->
            val valueDb = uiState.eqBandsDb.getOrElse(index) { 0f }
            val formattedDb = String.format(Locale.US, "%+.1f dB", valueDb)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = formattedDb,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Slider(
                    value = valueDb,
                    onValueChange = { onUpdateBand(index, it) },
                    valueRange = -12f..12f,
                    modifier = Modifier.testTag("eq_slider_$index"),
                )
            }
        }
    }

    GlassSubCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Sub-Bass Boost",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${(uiState.bassBoostStrength * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = uiState.bassBoostStrength,
            onValueChange = onUpdateBassBoost,
            valueRange = 0f..1f,
            modifier = Modifier.testTag("bass_boost_slider"),
        )
    }

    OutlinedButton(
        onClick = {
            onSelectPreset(EqPreset.BALANCED)
            onUpdateBassBoost(0f)
        },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Reset to Balanced")
    }
}

@Composable
fun NoiseControlSettingsSheetContent(
    uiState: BudsStudioUiState,
    onSelectMode: (NoiseControlMode) -> Unit,
    onSelectStrength: (AncStrength) -> Unit,
    onTogglePersonalisedAnc: (Boolean) -> Unit,
    onToggleWindReduction: (Boolean) -> Unit,
    onToggleTransparencyVoiceFocus: (Boolean) -> Unit,
) {
    GlassSubCard {
        Text(
            text = "NOISE CONTROL PROFILE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Standard Bluetooth A2DP/HFP does not expose vendor ANC registers. Preferences saved here work alongside your earbuds' hardware touch/companion controls.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NoiseControlMode.entries.forEach { mode ->
                val selected = mode == uiState.noiseControlMode
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectMode(mode) },
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                    ) {
                        Text(
                            text = mode.shortLabel,
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

    GlassSubCard {
        Text(
            text = "ANC ATTENUATION TARGET",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AncStrength.entries.forEach { strength ->
            val selected = strength == uiState.ancStrength
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelectStrength(strength) },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${strength.label} Cancellation",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    Text(
                        text = "-${strength.depthDb} dB",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }

    GlassSubCard {
        SettingsSwitchRow(
            title = "Personalised noise cancellation",
            subtitle = "Save seal calibration preference in studio profile",
            checked = uiState.personalisedAnc,
            onCheckedChange = onTogglePersonalisedAnc,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingsSwitchRow(
            title = "Wind noise reduction",
            subtitle = "Prefer outdoor gust suppression profile",
            checked = uiState.windNoiseReduction,
            onCheckedChange = onToggleWindReduction,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingsSwitchRow(
            title = "Transparency voice focus",
            subtitle = "Prefer vocal clarity during ambient pass-through",
            checked = uiState.transparencyVoiceFocus,
            onCheckedChange = onToggleTransparencyVoiceFocus,
        )
    }
}

@Composable
fun EarbudControlsSettingsSheetContent(
    uiState: BudsStudioUiState,
    onSetDoubleTapLeft: (GestureAction) -> Unit,
    onSetDoubleTapRight: (GestureAction) -> Unit,
    onSetTripleTap: (GestureAction) -> Unit,
    onSetPressAndHold: (GestureAction) -> Unit,
) {
    GlassSubCard {
        Text(
            text = "TOUCH GESTURE MAP (PROFILE)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Saved locally as your personal touch-map reference. Hardware gesture flashing requires each brand's proprietary BLE opcode.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    GesturePickerCard(
        title = "Double tap · Left earbud",
        selected = uiState.controls.doubleTapLeft,
        options = listOf(
            GestureAction.NEXT_TRACK,
            GestureAction.PREVIOUS_TRACK,
            GestureAction.PLAY_PAUSE,
            GestureAction.VOICE_ASSISTANT,
        ),
        onSelect = onSetDoubleTapLeft,
    )

    GesturePickerCard(
        title = "Double tap · Right earbud",
        selected = uiState.controls.doubleTapRight,
        options = listOf(
            GestureAction.PLAY_PAUSE,
            GestureAction.NEXT_TRACK,
            GestureAction.PREVIOUS_TRACK,
            GestureAction.VOICE_ASSISTANT,
        ),
        onSelect = onSetDoubleTapRight,
    )

    GesturePickerCard(
        title = "Triple tap · Either earbud",
        selected = uiState.controls.tripleTap,
        options = listOf(
            GestureAction.PREVIOUS_TRACK,
            GestureAction.NEXT_TRACK,
            GestureAction.VOICE_ASSISTANT,
        ),
        onSelect = onSetTripleTap,
    )

    GesturePickerCard(
        title = "Press and hold",
        selected = uiState.controls.pressAndHold,
        options = listOf(
            GestureAction.SWITCH_NOISE_CONTROL,
            GestureAction.VOICE_ASSISTANT,
            GestureAction.VOLUME_CONTROL,
        ),
        onSelect = onSetPressAndHold,
    )
}

@Composable
private fun GesturePickerCard(
    title: String,
    selected: GestureAction,
    options: List<GestureAction>,
    onSelect: (GestureAction) -> Unit,
) {
    GlassSubCard {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { action ->
                FilterChip(
                    selected = action == selected,
                    onClick = { onSelect(action) },
                    label = { Text(action.label) },
                )
            }
        }
    }
}

@Composable
fun DeviceManagementSheetContent(
    uiState: BudsStudioUiState,
    onToggleDualConnection: (Boolean) -> Unit,
    onToggleInEarDetection: (Boolean) -> Unit,
    onToggleLowLatency: (Boolean) -> Unit,
) {
    GlassSubCard {
        SettingsSwitchRow(
            title = "Dual connection",
            subtitle = "Track multi-point preference in your CK profile",
            checked = uiState.dualConnection,
            onCheckedChange = onToggleDualConnection,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingsSwitchRow(
            title = "In-ear wear detection",
            subtitle = "Track auto-pause preference in CK profile",
            checked = uiState.inEarDetection,
            onCheckedChange = onToggleInEarDetection,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingsSwitchRow(
            title = "Low-latency game mode",
            subtitle = "Track reduced-buffer audio preference",
            checked = uiState.lowLatencyMode,
            onCheckedChange = onToggleLowLatency,
        )
    }

    GlassSubCard {
        Text(
            text = "LIVE DEVICE TELEMETRY",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        InfoLine("Detected Brand", uiState.connectedBrand.displayName)
        InfoLine("Device Name", uiState.connectedDeviceName ?: "Not connected")
        InfoLine("MAC Address", uiState.connectedDeviceAddress ?: "—")
        InfoLine("GATT Firmware", uiState.firmware.installedVersion ?: "Not exposed over GATT")
        InfoLine("Audio Transport", uiState.audioRouteInfo.transportType)
    }

    GlassSubCard {
        Text(
            text = "BRAND PROTOCOL NOTE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = uiState.connectedBrand.protocolNote,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun AboutStudioSheetContent(
    uiState: BudsStudioUiState,
) {
    GlassSubCard {
        Text(
            text = "CK · Monochrome Liquid Glass Companion",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Package: com.karan.budsstudio · Designed for Vivo X300 and Android 12+ devices with real Bluetooth A2DP/HFP/GATT telemetry.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    GlassSubCard {
        Text(
            text = "STANDARD BLUETOOTH (REAL-TIME)",
            style = MaterialTheme.typography.labelSmall,
            color = StatusEmerald,
        )
        Text(
            text = "• Automatic connection/disconnection detection, brand identification (OnePlus, Samsung, JBL, realme, OPPO, vivo, boAt, Apple, Sony, Nothing), animated popup & background monitor.\n" +
                "• Unified Bluetooth battery level via HFP/A2DP & GATT 0x180F, AudioRouteInfo codec/sample rate, and GATT Device Information (0x180A) firmware revision.\n" +
                "• Live 5-band Equalizer & BassBoost via Android AudioFX.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "PROPRIETARY OEM PROTOCOLS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "• Per-earbud Left/Right/Case split battery, hardware ANC mode registers, touch gesture flashing, and encrypted firmware OTA packages require each manufacturer's signed protocol.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        InfoLine("Bluetooth Permissions", if (uiState.hasAllBluetoothPermissions) "Granted" else "Pending")
        InfoLine("Notifications", if (uiState.hasNotificationPermission) "Allowed" else "Pending")
        InfoLine("AudioFX Equalizer", uiState.hardwareEqStatus)
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
