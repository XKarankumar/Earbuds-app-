package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hometheatre.core.UserAudioPreferences
import com.example.hometheatre.rgb.RgbCategory
import com.example.hometheatre.rgb.RgbControllerState
import com.example.hometheatre.rgb.RgbEffectsCatalog
import com.example.ui.components.GlassConsoleCard
import com.example.ui.components.LedStripPreviewBar
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.viewmodel.TheatreViewModel
import kotlin.math.roundToInt

@Composable
fun RgbStudioScreen(
    viewModel: TheatreViewModel,
    prefs: UserAudioPreferences,
    rgbState: RgbControllerState
) {
    val palette = LocalStudioPalette.current
    var selectedCategory by remember { mutableStateOf<RgbCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var wledIpInput by remember(prefs.wledHostIp) { mutableStateOf(prefs.wledHostIp.ifBlank { "192.168.1.50" }) }
    var esp32IpInput by remember(prefs.esp32HostIp) { mutableStateOf(prefs.esp32HostIp.ifBlank { "192.168.1.60" }) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var presetName by remember { mutableStateOf("") }

    val blePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshHardwareCapabilities()
        viewModel.scanBleRgbControllers()
    }

    val activeEffect = remember(prefs.rgbEffectId) { RgbEffectsCatalog.findById(prefs.rgbEffectId) }
    val filteredEffects = remember(selectedCategory, searchQuery) {
        RgbEffectsCatalog.allEffects.filter { ef ->
            (selectedCategory == null || ef.category == selectedCategory) &&
                (searchQuery.isBlank() || ef.name.contains(searchQuery, ignoreCase = true))
        }
    }

    val colorSwatches = listOf(
        0xFF00E5FF, 0xFF8B5CF6, 0xFFFF2A85, 0xFF10B981,
        0xFFF59E0B, 0xFFEF4444, 0xFF3B82F6, 0xFFFFFFFF
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("rgb_studio_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Live LED Strip Preview & Brightness/Speed/Color Controls
        item {
            GlassConsoleCard(highlighted = rgbState.isHardwareConnected) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Lightbulb,
                            contentDescription = null,
                            tint = palette.primaryAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "RGB LIGHTING STUDIO (${RgbEffectsCatalog.allEffects.size} FX)",
                                style = MaterialTheme.typography.titleLarge,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "#${activeEffect.id} ${activeEffect.name} • ${activeEffect.category.label}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.primaryAccent
                            )
                        }
                    }
                    StatusPill(
                        text = if (rgbState.isHardwareConnected) "HW Live" else "Preview Only",
                        active = rgbState.isHardwareConnected
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                LedStripPreviewBar(colorsArgb = rgbState.ledStripColors)
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = rgbState.lastHardwareDiagnostic,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Brightness", style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary)
                    Text("${(prefs.rgbBrightness * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = palette.primaryAccent)
                }
                Slider(
                    value = prefs.rgbBrightness,
                    onValueChange = viewModel::setRgbBrightness,
                    valueRange = 0.05f..1f,
                    colors = SliderDefaults.colors(thumbColor = palette.primaryAccent, activeTrackColor = palette.primaryAccent)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Animation Speed", style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary)
                    Text("${(prefs.rgbSpeed * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = palette.secondaryAccent)
                }
                Slider(
                    value = prefs.rgbSpeed,
                    onValueChange = viewModel::setRgbSpeed,
                    valueRange = 0.05f..1f,
                    colors = SliderDefaults.colors(thumbColor = palette.secondaryAccent, activeTrackColor = palette.secondaryAccent)
                )

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Custom RGB Color", style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary)
                    OutlinedButton(onClick = { showSavePresetDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Preset")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    colorSwatches.forEach { hex ->
                        val selected = prefs.rgbCustomColorHex == hex
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) Color.White else palette.borderStroke,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.setRgbCustomColor(hex) }
                        )
                    }
                }

                if (prefs.savedRgbPresets.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Saved Lighting Presets", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        prefs.savedRgbPresets.forEach { saved ->
                            FilterChip(
                                selected = prefs.rgbEffectId == saved.effectId,
                                onClick = { viewModel.applyRgbPreset(saved) },
                                label = { Text(saved.name) }
                            )
                        }
                    }
                }
            }
        }

        // Hardware Controllers: WLED, ESP32, Bluetooth LE
        item {
            GlassConsoleCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Wifi, contentDescription = null, tint = palette.primaryAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("WLED / ESP32 / BLE CONTROLLERS", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                    }
                    if (rgbState.isHardwareConnected) {
                        OutlinedButton(onClick = viewModel::disconnectRgbHardware) {
                            Icon(Icons.Filled.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Disconnect")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Hardware RGB output is active only when linked to a reachable WLED (DRGB UDP:21324), ESP32 (UDP:4210), or BLE GATT controller.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = wledIpInput,
                        onValueChange = { wledIpInput = it },
                        label = { Text("WLED IP Address") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { viewModel.connectWled(wledIpInput) },
                        modifier = Modifier.testTag("connect_wled_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primaryAccent, contentColor = Color.Black)
                    ) {
                        Text("WLED")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = esp32IpInput,
                        onValueChange = { esp32IpInput = it },
                        label = { Text("ESP32 IP Address") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = { viewModel.connectEsp32(esp32IpInput) },
                        modifier = Modifier.testTag("connect_esp32_button"),
                        border = BorderStroke(1.dp, palette.secondaryAccent)
                    ) {
                        Text("ESP32")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            blePermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.BLUETOOTH_SCAN,
                                    Manifest.permission.BLUETOOTH_CONNECT
                                )
                            )
                        } else {
                            viewModel.scanBleRgbControllers()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scan_ble_rgb_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.BluetoothSearching, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (rgbState.isScanningBle) "Scanning BLE Controllers..." else "Scan Bluetooth LE RGB Controllers")
                }

                if (rgbState.discoveredBleDevices.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    rgbState.discoveredBleDevices.forEach { dev ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.connectBleRgbController(dev.address) }
                                .padding(vertical = 8.dp, horizontal = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(dev.name, style = MaterialTheme.typography.bodyLarge, color = palette.textPrimary)
                                Text(dev.address, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                            }
                            Text("${dev.rssi} dBm • Bind", style = MaterialTheme.typography.labelMedium, color = palette.primaryAccent)
                        }
                    }
                }
            }
        }

        // Category Filter + Search for 108 Effects
        item {
            GlassConsoleCard {
                Text(
                    text = "EFFECTS LIBRARY (${filteredEffects.size} / ${RgbEffectsCatalog.allEffects.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search 108 lighting effects") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All (108)") }
                    )
                    RgbCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                            label = { Text(cat.label) }
                        )
                    }
                }
            }
        }

        // Grid/Rows of Effects
        items(filteredEffects.chunked(2), key = { row -> row.first().id }) { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { effect ->
                    val selected = prefs.rgbEffectId == effect.id
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (selected) palette.primaryAccent.copy(alpha = 0.22f)
                                else palette.surfaceGlass
                            )
                            .border(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) palette.primaryAccent else palette.borderStroke,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { viewModel.selectRgbEffect(effect.id) }
                            .padding(12.dp)
                            .testTag("rgb_effect_${effect.id}")
                    ) {
                        Column {
                            Text(
                                text = "#${effect.id} • ${effect.category.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) palette.primaryAccent else palette.textSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = effect.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }

    if (showSavePresetDialog) {
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text("Save Custom RGB Preset") },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("Lighting Preset Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveRgbPreset(presetName)
                    presetName = ""
                    showSavePresetDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) { Text("Cancel") }
            }
        )
    }
}
