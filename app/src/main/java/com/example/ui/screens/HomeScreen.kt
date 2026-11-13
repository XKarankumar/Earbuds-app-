package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.BudsStudioUiState
import com.example.model.CkRoutes
import com.example.ui.BudsStudioViewModel
import com.example.ui.components.BatteryUiSection
import com.example.ui.components.EqualizerPreviewCard
import com.example.ui.components.HeroEarbudsSection
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.NoiseControlSection
import com.example.ui.components.StudioTopBar

@Composable
fun HomeScreen(
    uiState: BudsStudioUiState,
    viewModel: BudsStudioViewModel,
    isWideLayout: Boolean,
    contentPadding: PaddingValues,
    onPrimaryConnectClick: () -> Unit,
    onToggleBackgroundMonitor: (Boolean) -> Unit,
    onNavigate: (String) -> Unit,
) {
    val deviceTitle = uiState.connectedDeviceName ?: stringResource(R.string.device_model_default)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        StudioTopBar(
            hasAllPermissions = uiState.hasAllBluetoothPermissions,
            isBluetoothEnabled = uiState.isBluetoothEnabled,
            onOpenPermissions = { onNavigate(CkRoutes.PERMISSIONS) },
            onOpenFirmware = { onNavigate(CkRoutes.FIRMWARE_UPDATE) },
            onOpenBluetoothDevices = { onNavigate(CkRoutes.BLUETOOTH_DEVICES) },
            onOpenAbout = { onNavigate(CkRoutes.ABOUT_CK) },
        )

        if (isWideLayout) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    HeroEarbudsSection(
                        deviceTitle = deviceTitle,
                        brand = uiState.connectedBrand,
                        connectionState = uiState.connectionState,
                        statusBanner = uiState.statusBanner,
                        audioRouteInfo = uiState.audioRouteInfo,
                        layoutDensity = uiState.customization.layoutDensity,
                        onPrimaryConnectClick = onPrimaryConnectClick,
                        onOpenBluetoothDevices = { onNavigate(CkRoutes.BLUETOOTH_DEVICES) },
                    )
                    BatteryUiSection(
                        battery = uiState.battery,
                        onOpenBatteryDashboard = { onNavigate(CkRoutes.BATTERY_DASHBOARD) },
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    QuickSettingsGrid(
                        uiState = uiState,
                        onNavigate = onNavigate,
                        onPreviewPopup = { viewModel.previewConnectionPopup(uiState.connectedBrand) },
                    )
                    NoiseControlSection(
                        mode = uiState.noiseControlMode,
                        ancStrength = uiState.ancStrength,
                        personalisedAnc = uiState.personalisedAnc,
                        onSelectMode = viewModel::setNoiseControlMode,
                        onSelectStrength = viewModel::setAncStrength,
                        onTogglePersonalisedAnc = viewModel::setPersonalisedAnc,
                        onOpenNoiseSettings = { onNavigate(CkRoutes.CONTROLS) },
                    )
                    if (uiState.customization.showEqCardOnHome) {
                        EqualizerPreviewCard(
                            preset = uiState.eqPreset,
                            bandsDb = uiState.eqBandsDb,
                            hardwareStatus = uiState.hardwareEqStatus,
                            onOpenEqualizer = { onNavigate(CkRoutes.EQUALIZER) },
                        )
                    }
                    DeviceManagerHubCard(
                        uiState = uiState,
                        onToggleBackgroundMonitor = onToggleBackgroundMonitor,
                        onNavigate = onNavigate,
                    )
                }
            }
        } else {
            HeroEarbudsSection(
                deviceTitle = deviceTitle,
                brand = uiState.connectedBrand,
                connectionState = uiState.connectionState,
                statusBanner = uiState.statusBanner,
                audioRouteInfo = uiState.audioRouteInfo,
                layoutDensity = uiState.customization.layoutDensity,
                onPrimaryConnectClick = onPrimaryConnectClick,
                onOpenBluetoothDevices = { onNavigate(CkRoutes.BLUETOOTH_DEVICES) },
            )

            BatteryUiSection(
                battery = uiState.battery,
                onOpenBatteryDashboard = { onNavigate(CkRoutes.BATTERY_DASHBOARD) },
            )

            QuickSettingsGrid(
                uiState = uiState,
                onNavigate = onNavigate,
                onPreviewPopup = { viewModel.previewConnectionPopup(uiState.connectedBrand) },
            )

            NoiseControlSection(
                mode = uiState.noiseControlMode,
                ancStrength = uiState.ancStrength,
                personalisedAnc = uiState.personalisedAnc,
                onSelectMode = viewModel::setNoiseControlMode,
                onSelectStrength = viewModel::setAncStrength,
                onTogglePersonalisedAnc = viewModel::setPersonalisedAnc,
                onOpenNoiseSettings = { onNavigate(CkRoutes.CONTROLS) },
            )

            if (uiState.customization.showEqCardOnHome) {
                EqualizerPreviewCard(
                    preset = uiState.eqPreset,
                    bandsDb = uiState.eqBandsDb,
                    hardwareStatus = uiState.hardwareEqStatus,
                    onOpenEqualizer = { onNavigate(CkRoutes.EQUALIZER) },
                )
            }

            DeviceManagerHubCard(
                uiState = uiState,
                onToggleBackgroundMonitor = onToggleBackgroundMonitor,
                onNavigate = onNavigate,
            )
        }
    }
}

@Composable
private fun QuickSettingsGrid(
    uiState: BudsStudioUiState,
    onNavigate: (String) -> Unit,
    onPreviewPopup: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "QUICK STUDIO CONTROLS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuickTile(
                icon = Icons.Rounded.SystemUpdate,
                title = "Firmware",
                subtitle = uiState.firmware.installedVersion ?: "Verify OTA",
                onClick = { onNavigate(CkRoutes.FIRMWARE_UPDATE) },
                modifier = Modifier.weight(1f).testTag("tile_firmware"),
            )
            QuickTile(
                icon = Icons.Rounded.Palette,
                title = "White Glass",
                subtitle = "${(uiState.customization.glassTransparency * 100).toInt()}% Frost",
                onClick = { onNavigate(CkRoutes.GLASS_CUSTOMIZATION) },
                modifier = Modifier.weight(1f).testTag("tile_glass"),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuickTile(
                icon = Icons.Rounded.Animation,
                title = "Animations",
                subtitle = uiState.customization.animationStyle.label,
                onClick = { onNavigate(CkRoutes.ANIMATION_SETTINGS) },
                modifier = Modifier.weight(1f).testTag("tile_animations"),
            )
            QuickTile(
                icon = Icons.Rounded.AutoAwesome,
                title = "Popup Card",
                subtitle = "Preview Style",
                onClick = onPreviewPopup,
                modifier = Modifier.weight(1f).testTag("tile_popup_preview"),
            )
        }
    }
}

@Composable
private fun QuickTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier,
        cornerRadius = 22.dp,
        contentPadding = 14.dp,
        onClick = onClick,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DeviceManagerHubCard(
    uiState: BudsStudioUiState,
    onToggleBackgroundMonitor: (Boolean) -> Unit,
    onNavigate: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "DEVICE MANAGER & SCREENS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        LiquidGlassCard(contentPadding = 16.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Background connection monitor",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Event-driven notifications & low-battery alerts",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = uiState.customization.backgroundMonitoringEnabled,
                    onCheckedChange = onToggleBackgroundMonitor,
                    modifier = Modifier.testTag("background_monitor_switch"),
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            ManagerNavRow(
                icon = Icons.Rounded.Bluetooth,
                title = "Bluetooth Devices",
                subtitle = "${uiState.bondedDevices.size} paired · ${uiState.audioRouteInfo.transportType}",
                onClick = { onNavigate(CkRoutes.BLUETOOTH_DEVICES) },
                testTag = "row_bluetooth_devices",
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            ManagerNavRow(
                icon = Icons.Rounded.BatteryChargingFull,
                title = "Battery Dashboard",
                subtitle = uiState.battery.sourceLabel,
                onClick = { onNavigate(CkRoutes.BATTERY_DASHBOARD) },
                testTag = "row_battery_dashboard",
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            ManagerNavRow(
                icon = Icons.Rounded.SystemUpdate,
                title = "Firmware Update",
                subtitle = uiState.firmware.installedVersion?.let { "Installed: $it" }
                    ?: "Verify official compatibility & safety",
                onClick = { onNavigate(CkRoutes.FIRMWARE_UPDATE) },
                testTag = "row_firmware_update",
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            ManagerNavRow(
                icon = Icons.Rounded.TouchApp,
                title = "Touch Controls & Device Protocol",
                subtitle = "Gestures, wear sensor & brand protocol",
                onClick = { onNavigate(CkRoutes.CONTROLS) },
                testTag = "row_controls",
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            ManagerNavRow(
                icon = Icons.Rounded.Security,
                title = "App Permissions",
                subtitle = if (uiState.hasAllBluetoothPermissions) "Bluetooth ready · Review notifications" else "Grant Nearby Devices permissions",
                onClick = { onNavigate(CkRoutes.PERMISSIONS) },
                testTag = "row_app_permissions",
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            ManagerNavRow(
                icon = Icons.Rounded.Info,
                title = "About CK",
                subtitle = "com.karan.budsstudio · Standard vs OEM guide",
                onClick = { onNavigate(CkRoutes.ABOUT_CK) },
                testTag = "row_about_ck",
            )
        }
    }
}

@Composable
private fun ManagerNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
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
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
