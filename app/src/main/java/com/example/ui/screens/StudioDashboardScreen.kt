package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.hometheatre.audio.MasterEngineState
import com.example.hometheatre.audio.ReferenceTestMode
import com.example.hometheatre.core.AudioTelemetrySnapshot
import com.example.hometheatre.core.DeviceCompatibilityReport
import com.example.hometheatre.core.ThemePreset
import com.example.hometheatre.core.UserAudioPreferences
import com.example.hometheatre.rgb.RgbControllerState
import com.example.ui.components.GlassConsoleCard
import com.example.ui.components.LedStripPreviewBar
import com.example.ui.components.SpectrumVisualizerCanvas
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.viewmodel.TheatreViewModel
import kotlin.math.roundToInt

@Composable
fun StudioDashboardScreen(
    viewModel: TheatreViewModel,
    prefs: UserAudioPreferences,
    engineState: MasterEngineState,
    telemetry: AudioTelemetrySnapshot,
    rgbState: RgbControllerState,
    report: DeviceCompatibilityReport,
    safeWarningDismissed: Boolean,
    onOpenThemeEditor: () -> Unit,
    onOpenSystemTab: () -> Unit
) {
    val palette = LocalStudioPalette.current

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadStudioAudioFile(it) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Header Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_home_theatre_hero),
                    contentDescription = stringResource(id = R.string.cd_hero_theatre),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.25f),
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill(
                            text = report.primaryOutput?.connectionType?.label ?: "Built-in Audio",
                            active = true,
                            activeColor = palette.primaryAccent
                        )
                        StatusPill(
                            text = "Android ${report.androidVersionRelease} (API ${report.androidSdkInt})",
                            active = true,
                            activeColor = palette.successEmerald
                        )
                    }
                    Column {
                        Text(
                            text = "ULTIMATE HOME THEATRE PRO",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${report.primaryOutput?.name ?: report.deviceModel} • Spatial: ${prefs.spatialMode.label}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.82f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Safe Volume Warning Banner
        item {
            AnimatedVisibility(visible = engineState.safeVolumeWarningActive && !safeWarningDismissed) {
                GlassConsoleCard(highlighted = true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Safe volume alert",
                            tint = palette.warningAmber,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.safe_volume_warning_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.warningAmber
                            )
                            Text(
                                text = stringResource(id = R.string.safe_volume_warning_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = viewModel::dismissSafeVolumeAlert,
                            modifier = Modifier.testTag("dismiss_safe_volume_button")
                        ) {
                            Text("Acknowledge")
                        }
                    }
                }
            }
        }

        // Master Audio Deck & Real-Time Spectrum
        item {
            GlassConsoleCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "MASTER AUDIO DSP CONSOLE",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.primaryAccent
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (telemetry.isPlaying) telemetry.activeSourceLabel else engineState.loadedMediaTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = viewModel::toggleStudioPlayPause,
                        modifier = Modifier.testTag("studio_play_pause_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primaryAccent,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = if (engineState.isMedia3Playing || engineState.isReferenceSynthPlaying) {
                                Icons.Filled.Pause
                            } else {
                                Icons.Filled.PlayArrow
                            },
                            contentDescription = stringResource(id = R.string.cd_play_pause)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (engineState.isMedia3Playing || engineState.isReferenceSynthPlaying) "Pause" else "Play"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                SpectrumVisualizerCanvas(telemetry = telemetry)

                if (engineState.mediaDurationMs > 0L) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = engineState.mediaPositionMs.toFloat(),
                        onValueChange = { viewModel.seekStudioAudio(it.toLong()) },
                        valueRange = 0f..engineState.mediaDurationMs.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = palette.primaryAccent,
                            activeTrackColor = palette.primaryAccent
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { audioPicker.launch(arrayOf("audio/*")) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_studio_audio_button"),
                        border = BorderStroke(1.dp, palette.primaryAccent.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Filled.AudioFile, contentDescription = stringResource(id = R.string.cd_load_audio))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Load Audio File")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Synthesized Reference Signal Generators",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReferenceTestMode.entries.forEach { mode ->
                        val active = engineState.isReferenceSynthPlaying && engineState.activeReferenceMode == mode
                        FilterChip(
                            selected = active,
                            onClick = { viewModel.toggleReferenceTest(mode) },
                            label = { Text(mode.label) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.GraphicEq,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.primaryAccent.copy(alpha = 0.22f),
                                selectedLabelColor = palette.primaryAccent
                            )
                        )
                    }
                }
            }
        }

        // Extra Volume & Clipping Protection
        item {
            GlassConsoleCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Extra Volume",
                            tint = palette.primaryAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "EXTRA VOLUME & GAIN STAGE",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary
                        )
                    }
                    StatusPill(
                        text = if (prefs.limiterEnabled) "Clip Guard ON" else "Limiter OFF",
                        active = prefs.limiterEnabled
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                val volPercent = ((engineState.deviceVolume.toFloat() / engineState.deviceMaxVolume) * 100).roundToInt()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Android Hardware Volume",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "${engineState.deviceVolume}/${engineState.deviceMaxVolume} ($volPercent%)",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.primaryAccent
                    )
                }
                Slider(
                    value = engineState.deviceVolume.toFloat(),
                    onValueChange = { viewModel.setDeviceVolume(it.roundToInt()) },
                    valueRange = 0f..engineState.deviceMaxVolume.toFloat().coerceAtLeast(1f),
                    steps = (engineState.deviceMaxVolume - 1).coerceAtLeast(0),
                    modifier = Modifier.testTag("hardware_volume_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = palette.primaryAccent,
                        activeTrackColor = palette.primaryAccent
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Software Loudness Enhancement",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "+${"%.1f".format(prefs.loudnessGainDb)} dB",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.secondaryAccent
                    )
                }
                Slider(
                    value = prefs.loudnessGainDb,
                    onValueChange = viewModel::setLoudnessGainDb,
                    valueRange = 0f..12f,
                    modifier = Modifier.testTag("loudness_gain_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = palette.secondaryAccent,
                        activeTrackColor = palette.secondaryAccent
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = palette.successEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Soft-Knee Clipping Protection (-0.3 dBFS)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textPrimary
                        )
                    }
                    Switch(
                        checked = prefs.limiterEnabled,
                        onCheckedChange = viewModel::setLimiterEnabled
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Safe Listening Volume Warning (>= 80%)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                    Switch(
                        checked = prefs.safeVolumeWarningEnabled,
                        onCheckedChange = viewModel::setSafeVolumeWarningEnabled
                    )
                }

                Text(
                    text = "Note: Hardware volume obeys Android device amplifier ceiling; software gain applies digital headroom with anti-clipping protection.",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
            }
        }

        // Live RGB Strip + Quick Hardware Readout
        item {
            GlassConsoleCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AMBIENT RGB STRIP SYNC",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.primaryAccent
                    )
                    StatusPill(
                        text = if (rgbState.isHardwareConnected) rgbState.connectedTargetLabel else "Preview Only",
                        active = rgbState.isHardwareConnected
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                LedStripPreviewBar(colorsArgb = rgbState.ledStripColors)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenSystemTab() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.SurroundSound,
                            contentDescription = null,
                            tint = palette.secondaryAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Spatializer & Dolby Atmos Diagnostics",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary
                            )
                            Text(
                                text = if (report.dolbyStatus.activeRouteSupportsDirectAtmos) {
                                    "Dolby Atmos Passthrough Ready"
                                } else {
                                    "Dolby Atmos: Unavailable on current route • Tap for full report"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "Open System Diagnostics",
                        tint = palette.primaryAccent
                    )
                }
            }
        }

        // Quick Theme Switcher + Theme Editor Entry
        item {
            GlassConsoleCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = null,
                            tint = palette.primaryAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CONSOLE THEME STUDIO",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenThemeEditor,
                        modifier = Modifier.testTag("open_theme_editor_button")
                    ) {
                        Text("Theme Editor")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemePreset.entries.forEach { preset ->
                        FilterChip(
                            selected = prefs.themePreset == preset,
                            onClick = { viewModel.selectTheme(preset) },
                            label = { Text(preset.displayName) },
                            modifier = Modifier.testTag("theme_chip_${preset.name.lowercase()}")
                        )
                    }
                }
            }
        }
    }
}
