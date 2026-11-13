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
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Security
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AudioRouteInfo
import com.example.model.BatteryTelemetry
import com.example.model.BluetoothConnectionState
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusCoral
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioMintGlow

@Composable
fun StudioTopBar(
    hasAllPermissions: Boolean,
    isBluetoothEnabled: Boolean,
    onOpenPermissions: () -> Unit,
    onOpenBluetoothManager: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.studio_brand),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = if (hasAllPermissions) {
                    StatusEmerald.copy(alpha = 0.14f)
                } else {
                    StatusAmber.copy(alpha = 0.16f)
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .clickable(onClick = onOpenPermissions)
                    .testTag("permission_status_chip"),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = if (hasAllPermissions) Icons.Rounded.Security else Icons.Rounded.WarningAmber,
                        contentDescription = stringResource(R.string.cd_permission_shield),
                        tint = if (hasAllPermissions) StatusEmerald else StatusAmber,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = if (hasAllPermissions) "Permissions OK" else "Permissions",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasAllPermissions) StatusEmerald else StatusAmber,
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.size(48.dp),
            ) {
                IconButton(
                    onClick = onOpenBluetoothManager,
                    modifier = Modifier.testTag("top_bluetooth_button"),
                ) {
                    Icon(
                        imageVector = if (isBluetoothEnabled) Icons.Rounded.Bluetooth else Icons.Rounded.BluetoothDisabled,
                        contentDescription = stringResource(R.string.bluetooth_manager_title),
                        tint = if (isBluetoothEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.size(48.dp),
            ) {
                IconButton(
                    onClick = onOpenAbout,
                    modifier = Modifier.testTag("more_options_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.cd_more_options),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
fun HeroEarbudsSection(
    deviceTitle: String,
    connectionState: BluetoothConnectionState,
    statusBanner: String,
    audioRouteInfo: AudioRouteInfo,
    onPrimaryConnectClick: () -> Unit,
    onOpenBluetoothManager: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero_halo")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "halo_scale",
    )

    val isConnected = connectionState == BluetoothConnectionState.CONNECTED
    val isScanning = connectionState == BluetoothConnectionState.SCANNING ||
        connectionState == BluetoothConnectionState.CONNECTING

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = deviceTitle,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.studio_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text = stringResource(R.string.version_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
            if (audioRouteInfo.isBluetoothAudioActive) {
                Surface(
                    shape = CircleShape,
                    color = StudioMint.copy(alpha = 0.18f),
                ) {
                    Text(
                        text = audioRouteInfo.codecLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = StudioMint,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onOpenBluetoothManager)
                .testTag("hero_earbuds_card"),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val glowColor = when {
                    isConnected -> StatusEmerald.copy(alpha = 0.24f)
                    isScanning -> StudioCobalt.copy(alpha = 0.22f)
                    else -> StudioMintGlow.copy(alpha = 0.35f)
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor, Color.Transparent),
                        center = center,
                        radius = (size.minDimension * 0.48f) * pulseScale,
                    ),
                    radius = (size.minDimension * 0.48f) * pulseScale,
                    center = center,
                )
            }

            Image(
                painter = painterResource(id = R.drawable.img_buds_hero),
                contentDescription = stringResource(R.string.cd_hero_earbuds),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(28.dp)),
            )

            // Subtle connection status pill anchored cleanly at top-left
            val badgeColor by animateColorAsState(
                targetValue = when (connectionState) {
                    BluetoothConnectionState.CONNECTED -> StatusEmerald
                    BluetoothConnectionState.SCANNING,
                    BluetoothConnectionState.CONNECTING -> StudioCobalt
                    BluetoothConnectionState.PERMISSION_REQUIRED,
                    BluetoothConnectionState.BLUETOOTH_OFF -> StatusAmber
                    BluetoothConnectionState.UNAVAILABLE -> StatusCoral
                    BluetoothConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                label = "conn_badge_color",
            )

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp),
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
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = when (connectionState) {
                            BluetoothConnectionState.CONNECTED -> "Connected"
                            BluetoothConnectionState.SCANNING -> "Scanning"
                            BluetoothConnectionState.CONNECTING -> "Connecting"
                            BluetoothConnectionState.BLUETOOTH_OFF -> "Bluetooth Off"
                            BluetoothConnectionState.PERMISSION_REQUIRED -> "Permissions Needed"
                            BluetoothConnectionState.UNAVAILABLE -> "BT Unavailable"
                            BluetoothConnectionState.DISCONNECTED -> "Not Connected"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onPrimaryConnectClick,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                contentColor = if (isConnected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.background,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
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
                    isConnected -> "Manage Bluetooth Audio"
                    connectionState == BluetoothConnectionState.PERMISSION_REQUIRED -> "Grant Bluetooth Permissions"
                    connectionState == BluetoothConnectionState.BLUETOOTH_OFF -> "Turn On Bluetooth"
                    isScanning -> "Scanning Nearby Devices…"
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "BATTERY STATUS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = battery.sourceLabel,
                style = MaterialTheme.typography.labelSmall,
                color = StudioMint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("battery_ui_row"),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BatteryRingCard(
                label = "Left bud",
                level = battery.leftLevel,
                isCharging = battery.isLeftCharging,
                accentColor = StudioMint,
                modifier = Modifier.weight(1f),
            )
            BatteryRingCard(
                label = "Right bud",
                level = battery.rightLevel,
                isCharging = battery.isRightCharging,
                accentColor = StudioCobalt,
                modifier = Modifier.weight(1f),
            )
            BatteryRingCard(
                label = "Case",
                level = battery.caseLevel,
                isCharging = battery.isCaseCharging,
                accentColor = StatusEmerald,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BatteryRingCard(
    label: String,
    level: Int?,
    isCharging: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val progressTarget = ((level ?: 0).coerceIn(0, 100)) / 100f
    val animatedSweep by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
        label = "battery_sweep_$label",
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val ringColor = when {
        level == null -> trackColor
        level <= 15 -> StatusCoral
        level <= 30 -> StatusAmber
        else -> accentColor
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
            shape = RoundedCornerShape(20.dp),
        ),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokePx = 6.dp.toPx()
                    drawArc(
                        color = trackColor,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round),
                    )
                    if (level != null) {
                        drawArc(
                            color = ringColor,
                            startAngle = 135f,
                            sweepAngle = 270f * animatedSweep,
                            useCenter = false,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round),
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = level?.let { "$it%" } ?: "—",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (isCharging) {
                    Icon(
                        imageVector = Icons.Rounded.BatteryChargingFull,
                        contentDescription = "Charging",
                        tint = StatusEmerald,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
