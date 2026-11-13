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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.model.AnimationStyle
import com.example.model.BluetoothConnectionState
import com.example.model.BudsStudioUiState
import com.example.model.EarbudBrand
import com.example.model.FirmwareCheckStage
import com.example.model.LayoutDensity
import com.example.model.NotificationStyle
import com.example.model.PopupStyle
import com.example.model.ThemePreference
import com.example.ui.components.BrandLogoBadge
import com.example.ui.components.GlassSubCard
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald
import kotlin.math.roundToInt

@Composable
fun FirmwareCenterSheetContent(
    uiState: BudsStudioUiState,
    onCheckOfficialFirmware: () -> Unit,
    onOpenOfficialSupport: (String) -> Unit,
    onOpenBluetoothSettings: () -> Unit,
) {
    val fw = uiState.firmware
    val isConnected = uiState.connectionState == BluetoothConnectionState.CONNECTED

    GlassSubCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BrandLogoBadge(brand = uiState.connectedBrand, size = 36.dp, showLabel = true)
            Surface(
                shape = CircleShape,
                color = if (isConnected) StatusEmerald.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text = if (isConnected) "Connected" else "Offline",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isConnected) StatusEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = fw.statusHeadline,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = fw.statusDetail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (fw.checkStage == FirmwareCheckStage.VERIFYING_GATT_AND_OEM) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape),
            )
        } else if (fw.checkProgress > 0f) {
            LinearProgressIndicator(
                progress = { fw.checkProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape),
            )
        }
    }

    GlassSubCard {
        Text(
            text = "GATT DEVICE INFORMATION (0x180A)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FirmwareInfoRow("Firmware Version", fw.installedVersion ?: "Not exposed over GATT")
        FirmwareInfoRow("Hardware Revision", fw.hardwareRevision ?: "Not exposed")
        FirmwareInfoRow("Software Revision", fw.softwareRevision ?: "Not exposed")
        FirmwareInfoRow("Manufacturer", fw.manufacturerName ?: uiState.connectedBrand.displayName)
        FirmwareInfoRow("Model Number", fw.modelNumber ?: uiState.connectedDeviceName ?: "—")
        FirmwareInfoRow(
            "Standard BLE DFU (0xFE59)",
            if (fw.hasStandardDfuService) "Available" else "Proprietary Vendor OTA",
        )
        if (fw.lastCheckedLabel != null) {
            FirmwareInfoRow("Last Verified", fw.lastCheckedLabel)
        }
    }

    if (fw.verificationSteps.isNotEmpty()) {
        GlassSubCard {
            Text(
                text = "SAFETY & COMPATIBILITY LOG",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            fw.verificationSteps.forEach { step ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = if (step.contains("Warning") || step.contains("offline")) {
                            Icons.Rounded.WarningAmber
                        } else {
                            Icons.Rounded.CheckCircle
                        },
                        contentDescription = null,
                        tint = if (step.contains("Warning") || step.contains("offline")) StatusAmber else StatusEmerald,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(16.dp),
                    )
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }

    GlassSubCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.VerifiedUser,
                contentDescription = null,
                tint = StatusEmerald,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "Official Firmware Policy",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = "CK strictly verifies hardware compatibility and battery safety (>= 50%) and never flashes unofficial, unsigned, or incompatible firmware images.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Button(
        onClick = onCheckOfficialFirmware,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("firmware_verify_button"),
    ) {
        Icon(
            imageVector = Icons.Rounded.SystemUpdate,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Verify Official Firmware Channel")
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedButton(
            onClick = { onOpenOfficialSupport(uiState.connectedBrand.officialSupportUrl) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Vendor Support")
        }

        OutlinedButton(
            onClick = onOpenBluetoothSettings,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
        ) {
            Text("System BT OTA")
        }
    }
}

@Composable
private fun FirmwareInfoRow(label: String, value: String) {
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

@Composable
fun CustomizationStudioSheetContent(
    uiState: BudsStudioUiState,
    onSelectTheme: (ThemePreference) -> Unit,
    onSelectPopupStyle: (PopupStyle) -> Unit,
    onSelectAnimationStyle: (AnimationStyle) -> Unit,
    onSelectNotificationStyle: (NotificationStyle) -> Unit,
    onUpdateGlassAlpha: (Float) -> Unit,
    onUpdateGlassBlur: (Float) -> Unit,
    onSelectLayoutDensity: (LayoutDensity) -> Unit,
    onToggleShowEqHome: (Boolean) -> Unit,
    onToggleAutoPopup: (Boolean) -> Unit,
    onToggleBackgroundMonitor: (Boolean) -> Unit,
    onPreviewPopup: (EarbudBrand) -> Unit,
) {
    val cfg = uiState.customization

    GlassSubCard {
        Text(
            text = "MONOCHROME THEME",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemePreference.entries.forEach { pref ->
                val selected = cfg.themePreference == pref
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectTheme(pref) }
                        .testTag("theme_${pref.name.lowercase()}"),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                    ) {
                        Text(
                            text = when (pref) {
                                ThemePreference.AUTO -> "Auto"
                                ThemePreference.BLACK -> "Black"
                                ThemePreference.WHITE -> "White"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    GlassSubCard {
        Text(
            text = "LIQUID GLASS OPTICS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Glass Opacity", style = MaterialTheme.typography.titleSmall)
            Text("${(cfg.glassTransparency * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            value = cfg.glassTransparency,
            onValueChange = onUpdateGlassAlpha,
            valueRange = 0.40f..0.96f,
            modifier = Modifier.testTag("glass_alpha_slider"),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Backdrop Blur (Android 12+)", style = MaterialTheme.typography.titleSmall)
            Text("${cfg.glassBlurDp.roundToInt()} dp", style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            value = cfg.glassBlurDp,
            onValueChange = onUpdateGlassBlur,
            valueRange = 0f..28f,
            modifier = Modifier.testTag("glass_blur_slider"),
        )
    }

    GlassSubCard {
        Text(
            text = "CONNECTION POPUP DESIGN",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PopupStyle.entries.forEach { style ->
                FilterChip(
                    selected = cfg.popupStyle == style,
                    onClick = { onSelectPopupStyle(style) },
                    label = { Text(style.label) },
                )
            }
        }
        OutlinedButton(
            onClick = {
                val brand = if (uiState.connectedBrand != EarbudBrand.GENERIC) uiState.connectedBrand else EarbudBrand.VIVO
                onPreviewPopup(brand)
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("preview_popup_button"),
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Preview Popup Animation")
        }
    }

    GlassSubCard {
        Text(
            text = "ANIMATION STYLE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AnimationStyle.entries.forEach { style ->
                FilterChip(
                    selected = cfg.animationStyle == style,
                    onClick = { onSelectAnimationStyle(style) },
                    label = { Text(style.label) },
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Text(
            text = "NOTIFICATION APPEARANCE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NotificationStyle.entries.forEach { style ->
                FilterChip(
                    selected = cfg.notificationStyle == style,
                    onClick = { onSelectNotificationStyle(style) },
                    label = { Text(style.label) },
                )
            }
        }
    }

    GlassSubCard {
        Text(
            text = "LAYOUT & MONITORING",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LayoutDensity.entries.forEach { density ->
                FilterChip(
                    selected = cfg.layoutDensity == density,
                    onClick = { onSelectLayoutDensity(density) },
                    label = { Text(density.label) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        ToggleRow(
            title = "Automatic connection popup",
            subtitle = "Animate popup whenever earbuds connect or disconnect",
            checked = cfg.autoConnectionPopupEnabled,
            onCheckedChange = onToggleAutoPopup,
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        ToggleRow(
            title = "Show Equalizer card on Device tab",
            subtitle = "Pin live 5-band curve preview on your home dashboard",
            checked = cfg.showEqCardOnHome,
            onCheckedChange = onToggleShowEqHome,
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        ToggleRow(
            title = "Background connection monitor",
            subtitle = if (uiState.hasNotificationPermission) {
                "Battery-optimized event listener for alerts & low battery"
            } else {
                "Requires Notifications & Bluetooth permissions when enabled"
            },
            checked = cfg.backgroundMonitoringEnabled,
            onCheckedChange = onToggleBackgroundMonitor,
        )
    }
}

@Composable
private fun ToggleRow(
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
