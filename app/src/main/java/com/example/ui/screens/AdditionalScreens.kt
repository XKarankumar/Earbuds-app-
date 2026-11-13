package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.BudsStudioUiState
import com.example.model.CkRoutes
import com.example.ui.components.BatteryRingCard
import com.example.ui.components.BrandLogoBadge
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.WhiteGlassScreenHeader
import com.example.ui.sheets.AboutStudioSheetContent
import com.example.ui.sheets.BluetoothManagerSheetContent
import com.example.ui.sheets.FirmwareCenterSheetContent
import com.example.ui.sheets.PermissionHelperSheetContent
import com.example.ui.theme.StatusCoral
import com.example.ui.theme.StatusEmerald
import kotlin.math.roundToInt

@Composable
fun FirmwareUpdateScreen(
    uiState: BudsStudioUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onCheckOfficialFirmware: () -> Unit,
    onOpenOfficialSupport: (String) -> Unit,
    onOpenBluetoothSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        WhiteGlassScreenHeader(
            title = "Firmware Update",
            subtitle = "Official compatibility & safety checks",
            onBack = onBack,
        )
        FirmwareCenterSheetContent(
            uiState = uiState,
            onCheckOfficialFirmware = onCheckOfficialFirmware,
            onOpenOfficialSupport = onOpenOfficialSupport,
            onOpenBluetoothSettings = onOpenBluetoothSettings,
        )
    }
}

@Composable
fun BluetoothDevicesScreen(
    uiState: BudsStudioUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onNavigatePermissions: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnectDevice: (String) -> Unit,
    onDisconnectCurrent: () -> Unit,
    onOpenSystemBluetoothSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        WhiteGlassScreenHeader(
            title = "Bluetooth Devices",
            subtitle = "Paired earbuds, nearby scan & route",
            onBack = onBack,
        )
        BluetoothManagerSheetContent(
            uiState = uiState,
            onRequestPermissions = onNavigatePermissions,
            onEnableBluetooth = onEnableBluetooth,
            onStartScan = onStartScan,
            onStopScan = onStopScan,
            onConnectDevice = onConnectDevice,
            onDisconnectCurrent = onDisconnectCurrent,
            onOpenSystemBluetoothSettings = onOpenSystemBluetoothSettings,
        )
    }
}

@Composable
fun BatteryDashboardScreen(
    uiState: BudsStudioUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onUpdateLowBatteryThreshold: (Int) -> Unit,
    onOpenBluetoothDevices: () -> Unit,
) {
    val battery = uiState.battery

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        WhiteGlassScreenHeader(
            title = "Battery Dashboard",
            subtitle = "Live telemetry & alert thresholds",
            onBack = onBack,
        )

        LiquidGlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandLogoBadge(brand = uiState.connectedBrand, size = 36.dp, showLabel = true)
                Text(
                    text = if (battery.hasAnyLevel) "Telemetry Active" else "Standby",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (battery.hasAnyLevel) StatusEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = uiState.connectedDeviceName ?: "No Earbuds Connected",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = battery.sourceLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = if (battery.isLowBattery) StatusCoral else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val leftLabel = if (battery.isUnifiedSingleReport) "Buds (BT)" else "Left Bud"
            BatteryRingCard(
                label = leftLabel,
                level = battery.leftLevel,
                isCharging = battery.isLeftCharging,
                protocolHint = if (battery.isUnifiedSingleReport) "Unified BT" else null,
                modifier = Modifier.weight(1f),
            )
            BatteryRingCard(
                label = "Right Bud",
                level = battery.rightLevel,
                isCharging = battery.isRightCharging,
                protocolHint = if (battery.isUnifiedSingleReport && battery.rightLevel == null) "OEM BLE" else null,
                modifier = Modifier.weight(1f),
            )
            BatteryRingCard(
                label = "Case",
                level = battery.caseLevel,
                isCharging = battery.isCaseCharging,
                protocolHint = if (battery.isUnifiedSingleReport && battery.caseLevel == null) "OEM BLE" else null,
                modifier = Modifier.weight(1f),
            )
        }

        LiquidGlassCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.BatteryAlert,
                    contentDescription = null,
                    tint = if (battery.isLowBattery) StatusCoral else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "LOW BATTERY ALERT THRESHOLD",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Notify when battery drops below", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "${battery.lowBatteryThreshold}%",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Slider(
                value = battery.lowBatteryThreshold.toFloat(),
                onValueChange = { onUpdateLowBatteryThreshold(it.roundToInt()) },
                valueRange = 10f..35f,
                steps = 4,
                modifier = Modifier.testTag("low_battery_threshold_slider"),
            )
        }

        LiquidGlassCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.BluetoothAudio,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "PROTOCOL TRANSPARENCY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "Standard Android Bluetooth (HFP/A2DP & GATT 0x180F) reports a unified headset level when supported by your earbuds. Individual Left, Right, and Charging Case percentages appear automatically when exposed over standard GATT, or require the brand's proprietary BLE packet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (!uiState.audioRouteInfo.isBluetoothAudioActive) {
            Button(
                onClick = onOpenBluetoothDevices,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                Text("Connect Earbuds in Bluetooth Devices")
            }
        }
    }
}

@Composable
fun PermissionsScreen(
    uiState: BudsStudioUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onRequestBluetoothPermissions: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        WhiteGlassScreenHeader(
            title = "App Permissions",
            subtitle = "Requested only when needed",
            onBack = onBack,
        )
        PermissionHelperSheetContent(
            uiState = uiState,
            onRequestBluetoothPermissions = onRequestBluetoothPermissions,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onEnableBluetooth = onEnableBluetooth,
            onOpenAppSettings = onOpenAppSettings,
            onRefresh = onRefresh,
        )
    }
}

@Composable
fun AboutCkScreen(
    uiState: BudsStudioUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        WhiteGlassScreenHeader(
            title = "About CK",
            subtitle = "Architecture & protocol guide",
            onBack = onBack,
        )
        AboutStudioSheetContent(uiState = uiState)
    }
}
