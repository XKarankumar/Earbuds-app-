package com.example.ui.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.BudsStudioUiState
import com.example.model.DiscoveredBluetoothDevice
import com.example.ui.components.BrandLogoBadge
import com.example.ui.components.GlassSubCard
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald

@Composable
fun BluetoothManagerSheetContent(
    uiState: BudsStudioUiState,
    onRequestPermissions: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnectDevice: (String) -> Unit,
    onDisconnectCurrent: () -> Unit,
    onOpenSystemBluetoothSettings: () -> Unit,
) {
    if (!uiState.hasAllBluetoothPermissions) {
        GlassSubCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = StatusAmber,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bluetooth permissions required",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Grant Nearby Devices permissions so CK can detect earbuds, logos, and GATT battery.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Button(
                onClick = onRequestPermissions,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bt_sheet_grant_perms_button"),
            ) {
                Text("Review Permissions")
            }
        }
    } else if (!uiState.isBluetoothEnabled && uiState.hasBluetoothHardware) {
        GlassSubCard {
            Text(
                text = "Bluetooth is turned off",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Enable Bluetooth to scan nearby earbuds and view live audio route telemetry.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onEnableBluetooth,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bt_sheet_enable_bt_button"),
            ) {
                Text("Turn On Bluetooth")
            }
        }
    }

    GlassSubCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "ACTIVE AUDIO ROUTE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                shape = CircleShape,
                color = if (uiState.audioRouteInfo.isBluetoothAudioActive) {
                    StatusEmerald.copy(alpha = 0.16f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ) {
                Text(
                    text = uiState.audioRouteInfo.transportType,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (uiState.audioRouteInfo.isBluetoothAudioActive) {
                        StatusEmerald
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (uiState.connectedDeviceName != null) {
                BrandLogoBadge(brand = uiState.connectedBrand, size = 34.dp, showLabel = false)
            }
            Text(
                text = uiState.connectedDeviceName
                    ?: uiState.audioRouteInfo.deviceName
                    ?: "No Bluetooth audio device active",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RouteMetricBadge(
                label = "Codec",
                value = uiState.audioRouteInfo.codecLabel,
                modifier = Modifier.weight(1f),
            )
            RouteMetricBadge(
                label = "Sample Rate",
                value = uiState.audioRouteInfo.sampleRateHz?.let { "${it / 1000} kHz" } ?: "—",
                modifier = Modifier.weight(1f),
            )
            RouteMetricBadge(
                label = "Channels",
                value = uiState.audioRouteInfo.channelCount?.let { "${it}ch" } ?: "—",
                modifier = Modifier.weight(1f),
            )
        }

        if (uiState.connectedDeviceAddress != null) {
            OutlinedButton(
                onClick = onDisconnectCurrent,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Close GATT Telemetry Session")
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            onClick = if (uiState.isScanning) onStopScan else onStartScan,
            enabled = uiState.hasAllBluetoothPermissions && uiState.isBluetoothEnabled,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("bt_scan_toggle_button"),
        ) {
            if (uiState.isScanning) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stop Scan")
            } else {
                Icon(
                    imageVector = Icons.Rounded.BluetoothSearching,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan Nearby")
            }
        }

        FilledTonalButton(
            onClick = onOpenSystemBluetoothSettings,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.height(48.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("System BT")
        }
    }

    if (uiState.bondedDevices.isNotEmpty()) {
        Text(
            text = "PAIRED DEVICES (${uiState.bondedDevices.size})",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        uiState.bondedDevices.forEach { device ->
            DeviceRowCard(
                device = device,
                onConnect = { onConnectDevice(device.address) },
            )
        }
    }

    Text(
        text = if (uiState.isScanning) {
            "NEARBY DISCOVERY · SCANNING…"
        } else {
            "AVAILABLE DEVICES (${uiState.scannedDevices.size})"
        },
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )

    if (uiState.scannedDevices.isEmpty()) {
        GlassSubCard {
            Text(
                text = if (uiState.isScanning) {
                    "Listening for OnePlus, Samsung, JBL, realme, OPPO, vivo, boAt & BLE audio beacons…"
                } else {
                    "Tap Scan Nearby to discover nearby earbuds or open System BT for classic A2DP pairing."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        uiState.scannedDevices.forEach { device ->
            DeviceRowCard(
                device = device,
                onConnect = { onConnectDevice(device.address) },
            )
        }
    }
}

@Composable
private fun RouteMetricBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DeviceRowCard(
    device: DiscoveredBluetoothDevice,
    onConnect: () -> Unit,
) {
    GlassSubCard(onClick = onConnect) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                BrandLogoBadge(brand = device.brand, size = 40.dp, showLabel = false)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val rssiText = device.rssi?.let { " · ${it} dBm" } ?: ""
                    Text(
                        text = "${device.deviceTypeLabel}$rssiText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = if (device.isConnected) {
                    StatusEmerald.copy(alpha = 0.16f)
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                },
            ) {
                Text(
                    text = if (device.isConnected) "Active" else if (device.isBonded) "Connect" else "Pair",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (device.isConnected) StatusEmerald else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
fun PermissionHelperSheetContent(
    uiState: BudsStudioUiState,
    onRequestBluetoothPermissions: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onRefresh: () -> Unit,
) {
    val grantedCount = uiState.permissionChecklist.count { it.isGranted }
    val totalCount = uiState.permissionChecklist.size

    GlassSubCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$grantedCount of $totalCount permissions active",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "CK requests each permission only when needed and handles denials gracefully.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalButton(
                onClick = onRefresh,
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Refresh permission status",
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }

    uiState.permissionChecklist.forEach { item ->
        GlassSubCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = if (item.isGranted) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = if (item.isGranted) StatusEmerald else StatusAmber,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(22.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = if (item.isGranted) "Granted" else "Needed",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (item.isGranted) StatusEmerald else StatusAmber,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Why: ${item.whyNeeded}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (!uiState.hasAllBluetoothPermissions) {
        Button(
            onClick = onRequestBluetoothPermissions,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("permission_helper_grant_button"),
        ) {
            Text("Grant Nearby Devices & Bluetooth")
        }
    }

    if (!uiState.hasNotificationPermission) {
        FilledTonalButton(
            onClick = onRequestNotificationPermission,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("permission_helper_notif_button"),
        ) {
            Icon(
                imageVector = Icons.Rounded.NotificationsActive,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Allow Notifications (Alerts & Monitor)")
        }
    }

    if (!uiState.isBluetoothEnabled && uiState.hasBluetoothHardware) {
        FilledTonalButton(
            onClick = onEnableBluetooth,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text("Turn On System Bluetooth")
        }
    }

    OutlinedButton(
        onClick = onOpenAppSettings,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("permission_helper_settings_button"),
    ) {
        Text("Open App Settings (If Permanently Denied)")
    }
}
