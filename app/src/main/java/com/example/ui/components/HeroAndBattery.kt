package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AudioRouteInfo
import com.example.model.BatteryTelemetry
import com.example.model.BluetoothConnectionState
import com.example.model.EarbudBrand
import com.example.model.LayoutDensity
import com.example.ui.theme.LocalLiquidGlassStyle
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SilverEdgeRim
import com.example.ui.theme.SilverShadowTint
import com.example.ui.theme.SoftSilverGradientEnd
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusCoral
import com.example.ui.theme.StatusEmerald

@Composable
fun StudioTopBar(
    hasAllPermissions: Boolean,
    isBluetoothEnabled: Boolean,
    onOpenPermissions: () -> Unit,
    onOpenFirmware: () -> Unit,
    onOpenBluetoothDevices: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Luxury CK Logo at the top
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val isDark = LocalLiquidGlassStyle.current.isDark
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shadow(8.dp, CircleShape, ambientColor = SilverShadowTint.copy(alpha = 0.35f))
                    .clip(CircleShape)
                    .background(
                        if (isDark) {
                            Brush.linearGradient(listOf(Color(0xFF282A33), Color(0xFF14151B)))
                        } else {
                            Brush.linearGradient(listOf(PureWhite, SoftSilverGradientEnd))
                        },
                    )
                    .border(
                        1.2.dp,
                        if (isDark) Color.White.copy(alpha = 0.35f) else PureWhite,
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "CK",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.6.sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "LIQUID GLASS AUDIO",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = if (hasAllPermissions) {
                    StatusEmerald.copy(alpha = 0.12f)
                } else {
                    StatusAmber.copy(alpha = 0.14f)
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .clickable(onClick = onOpenPermissions)
                    .testTag("permission_status_chip"),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = if (hasAllPermissions) Icons.Rounded.Security else Icons.Rounded.WarningAmber,
                        contentDescription = stringResource(R.string.cd_permission_shield),
                        tint = if (hasAllPermissions) StatusEmerald else StatusAmber,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = if (hasAllPermissions) "OK" else "Perms",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasAllPermissions) StatusEmerald else StatusAmber,
                    )
                }
            }

            GlassCircleIconButton(
                onClick = onOpenFirmware,
                testTag = "top_firmware_button",
            ) {
                Icon(
                    imageVector = Icons.Rounded.SystemUpdate,
                    contentDescription = stringResource(R.string.cd_firmware_check),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            GlassCircleIconButton(
                onClick = onOpenBluetoothDevices,
                testTag = "top_bluetooth_button",
            ) {
                Icon(
                    imageVector = if (isBluetoothEnabled) Icons.Rounded.Bluetooth else Icons.Rounded.BluetoothDisabled,
                    contentDescription = stringResource(R.string.bluetooth_manager_title),
                    tint = if (isBluetoothEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            GlassCircleIconButton(
                onClick = onOpenAbout,
                testTag = "more_options_button",
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = stringResource(R.string.cd_more_options),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun GlassCircleIconButton(
    onClick: () -> Unit,
    testTag: String,
    content: @Composable () -> Unit,
) {
    val glassStyle = LocalLiquidGlassStyle.current
    Surface(
        shape = CircleShape,
        color = if (glassStyle.isDark) {
            MaterialTheme.colorScheme.surface.copy(alpha = glassStyle.glassAlpha)
        } else {
            PureWhite.copy(alpha = 0.90f)
        },
        shadowElevation = 5.dp,
        modifier = Modifier
            .size(44.dp)
            .border(
                1.dp,
                if (glassStyle.isDark) Color.White.copy(alpha = 0.22f) else SilverEdgeRim,
                CircleShape,
            ),
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.testTag(testTag),
            content = content,
        )
    }
}

@Composable
fun HeroEarbudsSection(
    deviceTitle: String,
    brand: EarbudBrand,
    connectionState: BluetoothConnectionState,
    statusBanner: String,
    audioRouteInfo: AudioRouteInfo,
    layoutDensity: LayoutDensity,
    onPrimaryConnectClick: () -> Unit,
    onOpenBluetoothDevices: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glassStyle = LocalLiquidGlassStyle.current
    val isConnected = connectionState == BluetoothConnectionState.CONNECTED
    val isScanning = connectionState == BluetoothConnectionState.SCANNING ||
        connectionState == BluetoothConnectionState.CONNECTING
    val stageHeight = if (layoutDensity == LayoutDensity.COMPACT_MINIMAL) 210.dp else 276.dp

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BrandLogoBadge(
                brand = brand,
                size = 34.dp,
                showLabel = true,
            )

            val badgeColor by animateColorAsState(
                targetValue = when (connectionState) {
                    BluetoothConnectionState.CONNECTED -> StatusEmerald
                    BluetoothConnectionState.SCANNING,
                    BluetoothConnectionState.CONNECTING -> MaterialTheme.colorScheme.onSurface
                    BluetoothConnectionState.PERMISSION_REQUIRED,
                    BluetoothConnectionState.BLUETOOTH_OFF -> StatusAmber
                    BluetoothConnectionState.UNAVAILABLE -> StatusCoral
                    BluetoothConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                label = "conn_badge_color",
            )

            Surface(
                shape = CircleShape,
                color = if (glassStyle.isDark) {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                } else {
                    PureWhite.copy(alpha = 0.92f)
                },
                shadowElevation = 4.dp,
                modifier = Modifier.border(
                    1.dp,
                    if (isConnected) StatusEmerald.copy(alpha = 0.35f) else SilverEdgeRim,
                    CircleShape,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = when (connectionState) {
                            BluetoothConnectionState.CONNECTED -> Icons.Rounded.BluetoothConnected
                            BluetoothConnectionState.SCANNING,
                            BluetoothConnectionState.CONNECTING -> Icons.Rounded.BluetoothSearching
                            BluetoothConnectionState.BLUETOOTH_OFF -> Icons.Rounded.BluetoothDisabled
                            else -> Icons.Rounded.Bluetooth
                        },
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = when (connectionState) {
                            BluetoothConnectionState.CONNECTED -> "Connected"
                            BluetoothConnectionState.SCANNING -> "Scanning"
                            BluetoothConnectionState.CONNECTING -> "Connecting"
                            BluetoothConnectionState.BLUETOOTH_OFF -> "Bluetooth Off"
                            BluetoothConnectionState.PERMISSION_REQUIRED -> "Permissions Needed"
                            BluetoothConnectionState.UNAVAILABLE -> "BT Unavailable"
                            BluetoothConnectionState.DISCONNECTED -> "Disconnected"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = deviceTitle,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Large centered 3D Earbuds Frosted Glass Stage
        LiquidGlassCard(
            cornerRadius = glassStyle.cornerRadius + 4.dp,
            contentPadding = 6.dp,
            onClick = onOpenBluetoothDevices,
            modifier = Modifier.testTag("hero_earbuds_card"),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(stageHeight),
            ) {
                Buds3DShowcaseStage(
                    connectionState = connectionState,
                    brand = brand,
                    modifier = Modifier.fillMaxSize(),
                )

                if (audioRouteInfo.isBluetoothAudioActive) {
                    Surface(
                        shape = CircleShape,
                        color = if (glassStyle.isDark) Color.Black.copy(alpha = 0.6f) else PureWhite.copy(alpha = 0.90f),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .border(1.dp, SilverEdgeRim, CircleShape),
                    ) {
                        Text(
                            text = "${audioRouteInfo.transportType} · ${audioRouteInfo.codecLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onPrimaryConnectClick,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("connect_earbuds_button"),
        ) {
            Icon(
                imageVector = when {
                    isConnected -> Icons.Rounded.CheckCircle
                    isScanning -> Icons.Rounded.BluetoothSearching
                    else -> Icons.Rounded.Add
                },
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when {
                    isConnected -> "Manage Bluetooth Devices"
                    connectionState == BluetoothConnectionState.PERMISSION_REQUIRED -> "Grant Bluetooth Permissions"
                    connectionState == BluetoothConnectionState.BLUETOOTH_OFF -> "Turn On Bluetooth"
                    isScanning -> "Scanning Nearby Earbuds…"
                    else -> stringResource(R.string.connect_earbuds)
                },
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = statusBanner,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
        )
    }
}

@Composable
fun BatteryUiSection(
    battery: BatteryTelemetry,
    onOpenBatteryDashboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onOpenBatteryDashboard)
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "BATTERY DASHBOARD",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (battery.isLowBattery) {
                    Icon(
                        imageVector = Icons.Rounded.BatteryAlert,
                        contentDescription = "Low battery alert",
                        tint = StatusCoral,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Text(
                    text = "Details ›",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (battery.isLowBattery) StatusCoral else MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("battery_ui_row"),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val leftDisplayLabel = if (battery.isUnifiedSingleReport) "Buds (BT)" else "Left"
            val rightSubLabel = if (battery.isUnifiedSingleReport && battery.rightLevel == null) "OEM BLE" else null
            val caseSubLabel = if (battery.isUnifiedSingleReport && battery.caseLevel == null) "OEM BLE" else null

            BatteryRingCard(
                label = leftDisplayLabel,
                level = battery.leftLevel,
                isCharging = battery.isLeftCharging,
                protocolHint = if (battery.isUnifiedSingleReport) "Unified" else null,
                onClick = onOpenBatteryDashboard,
                modifier = Modifier.weight(1f),
            )
            BatteryRingCard(
                label = "Right",
                level = battery.rightLevel,
                isCharging = battery.isRightCharging,
                protocolHint = rightSubLabel,
                onClick = onOpenBatteryDashboard,
                modifier = Modifier.weight(1f),
            )
            BatteryRingCard(
                label = "Case",
                level = battery.caseLevel,
                isCharging = battery.isCaseCharging,
                protocolHint = caseSubLabel,
                onClick = onOpenBatteryDashboard,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun BatteryRingCard(
    label: String,
    level: Int?,
    isCharging: Boolean,
    protocolHint: String?,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val progressTarget = ((level ?: 0).coerceIn(0, 100)) / 100f
    val animatedSweep by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
        label = "battery_sweep_$label",
    )
    val infiniteTransition = rememberInfiniteTransition(label = "charging_pulse_$label")
    val chargingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "charging_alpha",
    )

    val trackColor = SilverEdgeRim.copy(alpha = 0.70f)
    val ringColor = when {
        level == null -> trackColor
        level <= 20 -> StatusCoral
        level <= 35 -> StatusAmber
        isCharging -> StatusEmerald
        else -> MaterialTheme.colorScheme.primary
    }

    LiquidGlassCard(
        modifier = modifier,
        cornerRadius = 22.dp,
        contentPadding = 12.dp,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier.size(58.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokePx = 5.5.dp.toPx()
                    drawArc(
                        color = trackColor,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round),
                    )
                    if (level != null) {
                        drawArc(
                            color = ringColor.copy(alpha = if (isCharging) chargingAlpha else 1f),
                            startAngle = 135f,
                            sweepAngle = 270f * animatedSweep,
                            useCenter = false,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round),
                        )
                    }
                }
                Text(
                    text = level?.let { "$it%" } ?: "—",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                if (isCharging) {
                    Icon(
                        imageVector = Icons.Rounded.BatteryChargingFull,
                        contentDescription = "Charging",
                        tint = StatusEmerald,
                        modifier = Modifier
                            .size(14.dp)
                            .alpha(chargingAlpha),
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }

            if (protocolHint != null) {
                Text(
                    text = protocolHint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
