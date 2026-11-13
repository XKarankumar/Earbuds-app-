package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AnimationStyle
import com.example.model.BluetoothConnectionState
import com.example.model.EarbudBrand
import com.example.ui.theme.LocalLiquidGlassStyle
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SilverEdgeRim
import com.example.ui.theme.SoftSilverGradientEnd
import com.example.ui.theme.StatusEmerald

@Composable
fun BrandLogoBadge(
    brand: EarbudBrand,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    showLabel: Boolean = true,
) {
    val isDark = LocalLiquidGlassStyle.current.isDark
    val badgeBrush = if (isDark) {
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.06f)))
    } else {
        Brush.linearGradient(listOf(PureWhite, SoftSilverGradientEnd))
    }
    val strokeColor = if (isDark) Color.White.copy(alpha = 0.30f) else SilverEdgeRim
    val ink = MaterialTheme.colorScheme.onSurface
    val badgeShape = if (brand == EarbudBrand.ONEPLUS || brand == EarbudBrand.JBL) {
        RoundedCornerShape(10.dp)
    } else {
        CircleShape
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(badgeShape)
                .background(badgeBrush)
                .border(1.dp, strokeColor, badgeShape),
            contentAlignment = Alignment.Center,
        ) {
            when (brand) {
                EarbudBrand.GENERIC -> Icon(
                    imageVector = Icons.Rounded.Headphones,
                    contentDescription = brand.displayName,
                    tint = ink,
                    modifier = Modifier.size(size * 0.52f),
                )
                EarbudBrand.APPLE -> Icon(
                    imageVector = Icons.Rounded.Bluetooth,
                    contentDescription = brand.displayName,
                    tint = ink,
                    modifier = Modifier.size(size * 0.50f),
                )
                else -> Text(
                    text = brand.shortCode,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = if (brand.shortCode.length >= 4) 8.5.sp else 10.5.sp,
                        letterSpacing = 0.4.sp,
                    ),
                    color = ink,
                    maxLines = 1,
                )
            }
        }

        if (showLabel) {
            Text(
                text = brand.displayName,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = ink,
            )
        }
    }
}

@Composable
fun Buds3DShowcaseStage(
    connectionState: BluetoothConnectionState,
    brand: EarbudBrand,
    modifier: Modifier = Modifier,
) {
    val glassStyle = LocalLiquidGlassStyle.current
    val speedFactor = (1f / glassStyle.animationSpeedMultiplier.coerceIn(0.6f, 1.5f))
    val infiniteTransition = rememberInfiniteTransition(label = "buds_3d_stage")

    val orbitalRotation by infiniteTransition.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (3600 * speedFactor).toInt(), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "orbital_rot_y",
    )

    val floatOffsetY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (2600 * speedFactor).toInt(), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "float_y",
    )

    val radarSweep by infiniteTransition.animateFloat(
        initialValue = 0.28f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (2000 * speedFactor).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "radar_sweep",
    )

    var dragRotX by remember { mutableFloatStateOf(0f) }
    var dragRotY by remember { mutableFloatStateOf(0f) }

    val smoothRotX by animateFloatAsState(
        targetValue = dragRotX,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow),
        label = "drag_x",
    )
    val smoothRotY by animateFloatAsState(
        targetValue = dragRotY,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow),
        label = "drag_y",
    )

    val isConnected = connectionState == BluetoothConnectionState.CONNECTED
    val isScanning = connectionState == BluetoothConnectionState.SCANNING ||
        connectionState == BluetoothConnectionState.CONNECTING

    val autoRotY = if (!glassStyle.enable3dAutoOrbit) {
        0f
    } else {
        when (glassStyle.animationStyle) {
            AnimationStyle.ORBITAL_3D -> orbitalRotation * 1.25f
            AnimationStyle.LIQUID_SPRING -> orbitalRotation * 0.55f
            AnimationStyle.CRISP_MINIMAL -> 0f
        }
    }
    val autoTranslateY = when (glassStyle.animationStyle) {
        AnimationStyle.CRISP_MINIMAL -> 0f
        else -> floatOffsetY
    }

    val heroRes = if (glassStyle.isDark) R.drawable.img_ck_hero else R.drawable.img_ck_white_hero

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        dragRotX = 0f
                        dragRotY = 0f
                    },
                    onDragCancel = {
                        dragRotX = 0f
                        dragRotY = 0f
                    },
                ) { change, dragAmount ->
                    change.consume()
                    dragRotY = (dragRotY + dragAmount.x * 0.16f).coerceIn(-18f, 18f)
                    dragRotX = (dragRotX - dragAmount.y * 0.14f).coerceIn(-12f, 12f)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = heroRes),
            contentDescription = stringResource(R.string.cd_hero_earbuds),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    cameraDistance = 15f * density
                    rotationY = autoRotY + smoothRotY
                    rotationX = smoothRotX
                    translationY = autoTranslateY
                }
                .clip(RoundedCornerShape(glassStyle.cornerRadius)),
        )

        // Subtle luminous silver / emerald sonar rings during scan or active connection
        Canvas(modifier = Modifier.fillMaxSize()) {
            val ringColor = when {
                isConnected -> StatusEmerald
                glassStyle.isDark -> Color.White
                else -> Color(0xFF94A3B8)
            }
            if (isScanning) {
                drawCircle(
                    color = ringColor.copy(alpha = (1f - radarSweep) * 0.42f),
                    radius = size.minDimension * 0.46f * radarSweep,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }

        if (brand != EarbudBrand.GENERIC || isConnected) {
            Surface(
                shape = CircleShape,
                color = if (glassStyle.isDark) {
                    Color.Black.copy(alpha = 0.65f)
                } else {
                    PureWhite.copy(alpha = 0.88f)
                },
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .border(
                        1.dp,
                        if (glassStyle.isDark) Color.White.copy(alpha = 0.25f) else SilverEdgeRim,
                        CircleShape,
                    ),
            ) {
                Text(
                    text = brand.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
    }
}
