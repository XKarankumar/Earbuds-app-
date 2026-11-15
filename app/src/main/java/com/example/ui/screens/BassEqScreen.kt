package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hometheatre.core.AudioTelemetrySnapshot
import com.example.hometheatre.core.DefaultEqCatalog
import com.example.hometheatre.core.DeviceCompatibilityReport
import com.example.hometheatre.core.UserAudioPreferences
import com.example.ui.components.GlassConsoleCard
import com.example.ui.components.SpectrumVisualizerCanvas
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.viewmodel.TheatreViewModel
import kotlin.math.roundToInt

@Composable
fun BassEqScreen(
    viewModel: TheatreViewModel,
    prefs: UserAudioPreferences,
    telemetry: AudioTelemetrySnapshot,
    report: DeviceCompatibilityReport
) {
    val palette = LocalStudioPalette.current
    var showSaveDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }

    val subSupported = report.primaryOutput?.supportsSubwooferChannel == true ||
        report.connectedOutputs.any { it.supportsSubwooferChannel }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            GlassConsoleCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Speaker,
                            contentDescription = null,
                            tint = palette.primaryAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "EXTRA BASS & SUBWOOFER LFE",
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textPrimary
                        )
                    }
                    StatusPill(
                        text = if (subSupported) "Sub Channel Ready" else "DSP Virtual Sub",
                        active = true,
                        activeColor = if (subSupported) palette.successEmerald else palette.primaryAccent
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                SpectrumVisualizerCanvas(telemetry = telemetry)
                Spacer(modifier = Modifier.height(12.dp))

                LabeledGainSlider(
                    title = "Bass Booster",
                    valueText = "${(prefs.bassBoostLevel * 100).roundToInt()}%",
                    value = prefs.bassBoostLevel,
                    range = 0f..1f,
                    onValueChange = viewModel::setBassBoost,
                    testTag = "bass_boost_slider"
                )

                LabeledGainSlider(
                    title = if (subSupported) "Subwoofer LFE Output Gain" else "Sub-Bass LFE Shelf (Software)",
                    valueText = "${if (prefs.subwooferGainDb >= 0) "+" else ""}${"%.1f".format(prefs.subwooferGainDb)} dB",
                    value = prefs.subwooferGainDb,
                    range = -6f..12f,
                    onValueChange = viewModel::setSubwooferGainDb,
                    testTag = "subwoofer_gain_slider"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Deep Bass Mode (40 Hz Sub-Harmonic)", style = MaterialTheme.typography.bodyLarge, color = palette.textPrimary)
                        Text("Adds +4.5 dB sub-octave contour with limiter guard", style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
                    }
                    Switch(
                        checked = prefs.deepBassEnabled,
                        onCheckedChange = viewModel::setDeepBassEnabled,
                        modifier = Modifier.testTag("deep_bass_switch")
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Cinema Bass (58 Hz LFE Punch)", style = MaterialTheme.typography.bodyLarge, color = palette.textPrimary)
                        Text("Tight theatre slam tuned for action soundtracks", style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
                    }
                    Switch(
                        checked = prefs.cinemaBassEnabled,
                        onCheckedChange = viewModel::setCinemaBassEnabled,
                        modifier = Modifier.testTag("cinema_bass_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                LabeledGainSlider(
                    title = "Midrange Presence (1.1 kHz)",
                    valueText = "${if (prefs.midGainDb >= 0) "+" else ""}${"%.1f".format(prefs.midGainDb)} dB",
                    value = prefs.midGainDb,
                    range = -12f..12f,
                    onValueChange = viewModel::setMidGainDb
                )

                LabeledGainSlider(
                    title = "Treble Air & Clarity (7.5 kHz+)",
                    valueText = "${if (prefs.trebleGainDb >= 0) "+" else ""}${"%.1f".format(prefs.trebleGainDb)} dB",
                    value = prefs.trebleGainDb,
                    range = -12f..12f,
                    onValueChange = viewModel::setTrebleGainDb
                )
            }
        }

        // 10-Band Parametric Equalizer & Presets
        item {
            GlassConsoleCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.GraphicEq,
                            contentDescription = null,
                            tint = palette.primaryAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "10-BAND PARAMETRIC EQ",
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textPrimary
                        )
                    }
                    Row {
                        IconButton(onClick = viewModel::resetEqFlat) {
                            Icon(Icons.Filled.RestartAlt, contentDescription = "Reset EQ flat", tint = palette.textSecondary)
                        }
                        OutlinedButton(
                            onClick = { showSaveDialog = true },
                            modifier = Modifier.testTag("save_eq_preset_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                val allPresets = DefaultEqCatalog.builtInPresets + prefs.savedEqPresets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allPresets.forEach { preset ->
                        FilterChip(
                            selected = prefs.selectedEqPresetId == preset.id,
                            onClick = { viewModel.selectEqPreset(preset) },
                            label = { Text(preset.name) },
                            trailingIcon = if (!preset.isBuiltIn) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Delete preset",
                                        modifier = Modifier
                                            .padding(start = 2.dp)
                                            .clickable { viewModel.deleteCustomEqPreset(preset.id) }
                                    )
                                }
                            } else null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DefaultEqCatalog.bandLabels.forEachIndexed { idx, label ->
                        val gain = prefs.customBandGainsDb.getOrElse(idx) { 0f }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(palette.surfaceElevated)
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${label}Hz",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.primaryAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = gain,
                                onValueChange = { viewModel.setEqBandGain(idx, it) },
                                valueRange = -12f..12f,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                                    .testTag("eq_band_$idx"),
                                colors = SliderDefaults.colors(
                                    thumbColor = palette.primaryAccent,
                                    activeTrackColor = palette.primaryAccent
                                )
                            )
                            Text(
                                text = "${if (gain >= 0f) "+" else ""}${"%.1f".format(gain)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (kotlin.math.abs(gain) > 0.2f) palette.textPrimary else palette.textSecondary,
                                modifier = Modifier.width(44.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Custom EQ Preset") },
            text = {
                OutlinedTextField(
                    value = newPresetName,
                    onValueChange = { newPresetName = it },
                    label = { Text("Preset Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCustomEqPreset(newPresetName)
                        newPresetName = ""
                        showSaveDialog = false
                    }
                ) {
                    Text("Save Preset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun LabeledGainSlider(
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    testTag: String? = null
) {
    val palette = LocalStudioPalette.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary)
            Text(text = valueText, style = MaterialTheme.typography.labelMedium, color = palette.primaryAccent)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier,
            colors = SliderDefaults.colors(
                thumbColor = palette.primaryAccent,
                activeTrackColor = palette.primaryAccent
            )
        )
    }
}
