package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ConnectionPopupEvent
import com.example.model.PopupStyle
import com.example.ui.theme.LocalLiquidGlassStyle
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SilverEdgeRim
import com.example.ui.theme.SilverShadowTint
import com.example.ui.theme.SoftSilverGradientEnd
import com.example.ui.theme.StatusCoral
import com.example.ui.theme.StatusEmerald
import kotlinx.coroutines.delay

@Composable
fun BudsConnectionPopupOverlay(
    event: ConnectionPopupEvent?,
    popupStyle: PopupStyle,
    onDismiss: () -> Unit,
    onOpenBluetoothDevices: () -> Unit,
) {
    BackHandler(enabled = event != null, onBack = onDismiss)

    LaunchedEffect(event?.eventId) {
        if (event != null) {
            delay(5_500L)
            onDismiss()
        }
    }

    val isTopCapsule = popupStyle == PopupStyle.DYNAMIC_CAPSULE

    AnimatedVisibility(
        visible = event != null,
        enter = fadeIn(tween(180)) + slideInVertically(
            initialOffsetY = { if (isTopCapsule) -it else it / 3 },
            animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        ) + scaleIn(initialScale = 0.92f),
        exit = fadeOut(tween(150)) + slideOutVertically(
            targetOffsetY = { if (isTopCapsule) -it else it / 3 },
        ) + scaleOut(targetScale = 0.94f),
    ) {
        if (event == null) return@AnimatedVisibility
        val glassStyle = LocalLiquidGlassStyle.current
        val scrimInteraction = remember { MutableInteractionSource() }
        val cardInteraction = remember { MutableInteractionSource() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .let { m ->
                    if (popupStyle == PopupStyle.LIQUID_SHEET) {
                        m.background(
                            if (glassStyle.isDark) Color.Black.copy(alpha = 0.42f)
                            else Color(0xFFCFD6E3).copy(alpha = 0.38f),
                        ).clickable(interactionSource = scrimInteraction, indication = null, onClick = onDismiss)
                    } else {
                        m
                    }
                }
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = when (popupStyle) {
                PopupStyle.DYNAMIC_CAPSULE -> Alignment.TopCenter
                PopupStyle.LIQUID_SHEET -> Alignment.BottomCenter
                PopupStyle.MINIMAL_CARD -> Alignment.BottomCenter
            },
        ) {
            val shape = when (popupStyle) {
                PopupStyle.DYNAMIC_CAPSULE -> RoundedCornerShape(32.dp)
                PopupStyle.LIQUID_SHEET -> RoundedCornerShape(30.dp)
                PopupStyle.MINIMAL_CARD -> RoundedCornerShape(22.dp)
            }
            val bgBrush = if (glassStyle.isDark) {
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
                    ),
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        PureWhite.copy(alpha = 0.96f),
                        SoftSilverGradientEnd.copy(alpha = 0.88f),
                    ),
                )
            }
            val borderColor = if (glassStyle.isDark) Color.White.copy(alpha = 0.30f) else PureWhite

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .shadow(
                        elevation = 22.dp,
                        shape = shape,
                        ambientColor = SilverShadowTint.copy(alpha = 0.35f),
                        spotColor = SilverShadowTint.copy(alpha = 0.30f),
                    )
                    .clip(shape)
                    .background(bgBrush)
                    .border(1.25.dp, borderColor, shape)
                    .clickable(interactionSource = cardInteraction, indication = null, onClick = {})
                    .testTag("connection_popup_card"),
            ) {
                when (popupStyle) {
                    PopupStyle.LIQUID_SHEET -> LiquidSheetPopupBody(
                        event = event,
                        onDismiss = onDismiss,
                        onOpenBluetoothDevices = onOpenBluetoothDevices,
                    )
                    PopupStyle.DYNAMIC_CAPSULE -> CapsulePopupBody(
                        event = event,
                        onDismiss = onDismiss,
                    )
                    PopupStyle.MINIMAL_CARD -> MinimalPopupBody(
                        event = event,
                        onDismiss = onDismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun LiquidSheetPopupBody(
    event: ConnectionPopupEvent,
    onDismiss: () -> Unit,
    onOpenBluetoothDevices: () -> Unit,
) {
    val glassStyle = LocalLiquidGlassStyle.current
    val heroRes = if (glassStyle.isDark) R.drawable.img_ck_hero else R.drawable.img_ck_white_hero

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BrandLogoBadge(brand = event.brand, size = 34.dp, showLabel = true)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (event.isPreview) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        modifier = Modifier.border(1.dp, SilverEdgeRim, CircleShape),
                    ) {
                        Text(
                            text = "Style Preview",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Dismiss connection popup",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = event.deviceName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = if (event.isConnected) Icons.Rounded.BluetoothConnected else Icons.Rounded.BluetoothDisabled,
                    contentDescription = null,
                    tint = if (event.isConnected) StatusEmerald else StatusCoral,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = if (event.isConnected) "Connected · ${event.transportLabel}" else "Disconnected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Image(
            painter = painterResource(id = heroRes),
            contentDescription = event.deviceName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(155.dp)
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, SilverEdgeRim, RoundedCornerShape(22.dp)),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.BatteryStd,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = event.batteryLevel?.let { "$it% Unified Bluetooth Battery" }
                    ?: "Battery telemetry syncs when reported by earbuds",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Button(
            onClick = {
                onDismiss()
                onOpenBluetoothDevices()
            },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(
                text = if (event.isPreview) "Done" else "Open Bluetooth Devices",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun CapsulePopupBody(
    event: ConnectionPopupEvent,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            BrandLogoBadge(brand = event.brand, size = 38.dp, showLabel = false)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.deviceName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (event.isConnected) {
                        val bat = event.batteryLevel?.let { " · $it%" } ?: ""
                        "Connected${bat} · ${event.transportLabel}"
                    } else {
                        "Earbuds Disconnected"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (event.isConnected) StatusEmerald else StatusCoral,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close popup",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun MinimalPopupBody(
    event: ConnectionPopupEvent,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = if (event.isConnected) Icons.Rounded.BluetoothConnected else Icons.Rounded.BluetoothDisabled,
                contentDescription = null,
                tint = if (event.isConnected) StatusEmerald else StatusCoral,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${event.brand.displayName} · ${event.deviceName}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (event.isConnected) "Connected" else "Disconnected",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Dismiss",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
