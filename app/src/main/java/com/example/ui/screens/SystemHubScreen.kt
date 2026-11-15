package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SpeakerGroup
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.hometheatre.core.DeviceCompatibilityReport
import com.example.hometheatre.core.FutureModuleRegistry
import com.example.hometheatre.core.SpatialMode
import com.example.hometheatre.core.ThemePreset
import com.example.hometheatre.core.UserAudioPreferences
import com.example.ui.components.GlassConsoleCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.viewmodel.TheatreViewModel

@Composable
fun SystemHubScreen(
    viewModel: TheatreViewModel,
    prefs: UserAudioPreferences,
    report: DeviceCompatibilityReport,
    onOpenThemeEditor: () -> Unit
) {
    val palette = LocalStudioPalette.current
    val btConnectLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshHardwareCapabilities()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("system_hub_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Headphones & ANC Hardware Inspection
        item {
            GlassConsoleCard {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(136.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_acoustic_headphones),
                        contentDescription = stringResource(id = R.string.cd_headphones_preview),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Headphones, contentDescription = null, tint = palette.primaryAccent)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ANC HEADPHONE INSPECTOR", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            }
                            StatusPill(
                                text = if (report.ancStatus.hardwareAncDetected) "ANC Ready" else "Unavailable",
                                active = report.ancStatus.hardwareAncDetected
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                DiagnosticRow("Connected Headset", report.ancStatus.connectedHeadphoneName ?: "None detected")
                DiagnosticRow("Connection Bus", report.ancStatus.connectionType?.label ?: "N/A")
                DiagnosticRow("Hardware ANC", if (report.ancStatus.hardwareAncDetected) "Supported" else "Unavailable (OEM SDK required)")
                DiagnosticRow("Transparency Mode", if (report.ancStatus.transparencySupported) "Supported" else "Unavailable")
                DiagnosticRow("Adaptive ANC", if (report.ancStatus.adaptiveAncSupported) "Supported" else "Unavailable")

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = report.ancStatus.diagnosticNote,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )

                if (!report.bluetoothPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { btConnectLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Bluetooth, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Grant Bluetooth Permission for Headset Detection")
                    }
                }
            }
        }

        // 2. Spatial Audio (Android Spatializer API + DSP Virtualizer)
        item {
            GlassConsoleCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.SurroundSound, contentDescription = null, tint = palette.primaryAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SPATIAL AUDIO & 3D SURROUND", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                    }
                    StatusPill(
                        text = if (report.spatialStatus.isAvailable) "Spatializer Ready" else "DSP Virtualizer",
                        active = report.spatialStatus.isAvailable || report.spatialStatus.fallbackVirtualizerActive
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                DiagnosticRow("Android Spatializer API", if (report.spatialStatus.apiSupported) "API 32+ Present" else "API < 32 (Fallback)")
                DiagnosticRow("Immersive Level", report.spatialStatus.immersiveLevelLabel)
                DiagnosticRow("Head Tracking", if (report.spatialStatus.headTrackerAvailable) "Hardware Supported" else "Unavailable")

                Spacer(modifier = Modifier.height(8.dp))
                Text(report.spatialStatus.diagnosticNote, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SpatialMode.entries.forEach { mode ->
                        val selected = prefs.spatialMode == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) palette.primaryAccent.copy(alpha = 0.16f) else palette.surfaceElevated)
                                .border(1.dp, if (selected) palette.primaryAccent else palette.borderStroke, RoundedCornerShape(12.dp))
                                .clickable { viewModel.setSpatialMode(mode) }
                                .padding(12.dp)
                                .testTag("spatial_mode_${mode.name.lowercase()}"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(mode.label, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                                Text(mode.subtitle, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
                            }
                            if (selected) {
                                Text("ACTIVE", style = MaterialTheme.typography.labelMedium, color = palette.primaryAccent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 3. Honest Dolby Atmos & HDMI ARC/eARC Integration
        item {
            val dolby = report.dolbyStatus
            GlassConsoleCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("DOLBY ATMOS CERTIFICATION", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                    StatusPill(
                        text = if (dolby.activeRouteSupportsDirectAtmos) "Atmos Ready" else "Not Certified / Route N/A",
                        active = dolby.activeRouteSupportsDirectAtmos,
                        activeColor = if (dolby.activeRouteSupportsDirectAtmos) palette.successEmerald else palette.warningAmber
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(dolby.honestyStatusBanner, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                DiagnosticRow("E-AC-3 JOC (Atmos) Decoder", if (dolby.dolbyAtmosDecoderPresent) "Detected" else "Not Found")
                DiagnosticRow("Dolby AC-3 / E-AC-3", if (dolby.dolbyAc3DecoderPresent || dolby.dolbyEac3DecoderPresent) "Platform Decoder Present" else "Unavailable")
                DiagnosticRow("Dolby AC-4 Decoder", if (dolby.dolbyAc4DecoderPresent) "Detected" else "Unavailable")
                DiagnosticRow("HDMI ARC / eARC Route", if (dolby.hdmiArcOrEarcDetected) "Connected" else "Not Connected")
                DiagnosticRow("Direct Passthrough", if (dolby.activeRouteSupportsDirectAtmos) "Supported" else "Unavailable")
                if (dolby.detectedCodecs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Platform Decoders: ${dolby.detectedCodecs.joinToString()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.primaryAccent
                    )
                }
            }
        }

        // 4. Connectivity & Device Compatibility Report
        item {
            GlassConsoleCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.SpeakerGroup, contentDescription = null, tint = palette.primaryAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CONNECTIVITY & COMPATIBILITY REPORT", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                    }
                    IconButton(onClick = viewModel::refreshHardwareCapabilities) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh audio routes", tint = palette.primaryAccent)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                DiagnosticRow("Device Model", report.deviceModel)
                DiagnosticRow("Android OS", "Android ${report.androidVersionRelease} (API ${report.androidSdkInt})")
                DiagnosticRow("Low-Latency Audio", if (report.hasLowLatencyFeature) "Hardware Supported" else "Standard Buffer")
                DiagnosticRow("Pro Audio Flag", if (report.hasProAudioFeature) "Present" else "Not Advertised")
                DiagnosticRow("DynamicsProcessing API", if (report.hasDynamicsProcessingApi) "API 28+ Active" else "API 26-27 Biquad Fallback")

                Spacer(modifier = Modifier.height(10.dp))
                Text("Detected Audio Output Routes (${report.connectedOutputs.size})", style = MaterialTheme.typography.labelMedium, color = palette.primaryAccent)
                Spacer(modifier = Modifier.height(6.dp))
                report.connectedOutputs.forEach { dev ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.surfaceElevated)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(dev.name, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                            Text(dev.connectionType.label, style = MaterialTheme.typography.labelMedium, color = palette.primaryAccent)
                        }
                        Text(
                            text = "Channels: ${dev.channelCounts.joinToString("/")}ch • Rates: ${dev.sampleRates.take(4).joinToString("/")} Hz",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                        Text(
                            text = "Encodings: ${dev.encodings.joinToString(", ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                }
            }
        }

        // 5. Themes & Custom Theme Editor
        item {
            GlassConsoleCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Palette, contentDescription = null, tint = palette.primaryAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("THEMES & APPEARANCE", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                    }
                    OutlinedButton(onClick = onOpenThemeEditor, border = BorderStroke(1.dp, palette.primaryAccent)) {
                        Text("Customize")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ThemePreset.entries.forEach { preset ->
                        FilterChip(
                            selected = prefs.themePreset == preset,
                            onClick = { viewModel.selectTheme(preset) },
                            label = { Text("${preset.displayName} — ${preset.description}") }
                        )
                    }
                }
            }
        }

        // 6. Future Expansion Architecture Modules
        item {
            GlassConsoleCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.DeveloperBoard, contentDescription = null, tint = palette.secondaryAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("FUTURE MODULE ARCHITECTURE", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                FutureModuleRegistry.plannedModules.forEach { mod ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.surfaceElevated)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(mod.title, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                            Text(mod.category, style = MaterialTheme.typography.labelSmall, color = palette.secondaryAccent)
                        }
                        Text(mod.description, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
                        Text(mod.architectureStatus, style = MaterialTheme.typography.labelSmall, color = palette.successEmerald)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    val palette = LocalStudioPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
        Text(value, style = MaterialTheme.typography.labelMedium, color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
    }
}
