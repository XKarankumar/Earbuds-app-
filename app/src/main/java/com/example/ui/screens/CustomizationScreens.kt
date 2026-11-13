package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.example.model.BudsStudioUiState
import com.example.model.EarbudBrand
import com.example.model.LayoutDensity
import com.example.model.NotificationStyle
import com.example.model.PopupStyle
import com.example.model.ThemePreference
import com.example.ui.BudsStudioViewModel
import com.example.ui.components.Buds3DShowcaseStage
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.WhiteGlassScreenHeader
import com.example.ui.sheets.DeviceManagementSheetContent
import com.example.ui.sheets.EarbudControlsSettingsSheetContent
import com.example.ui.sheets.EqualizerSettingsSheetContent
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun AnimationSettingsScreen(
    uiState: BudsStudioUiState,
    viewModel: BudsStudioViewModel,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
) {
    val cfg = uiState.customization

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        WhiteGlassScreenHeader(
            title = "Animation Settings",
            subtitle = "3D earbuds physics & popup transitions",
            onBack = onBack,
        )

        // Live 3D Showcase Preview
        LiquidGlassCard(contentPadding = 8.dp) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp),
            ) {
                Buds3DShowcaseStage(
                    connectionState = uiState.connectionState,
                    brand = uiState.connectedBrand,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        LiquidGlassCard {
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
                        onClick = { viewModel.setAnimationStyle(style) },
                        label = { Text(style.label) },
                        modifier = Modifier.testTag("anim_style_${style.name.lowercase()}"),
                    )
                }
            }
            Text(
                text = cfg.animationStyle.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("3D Earbuds Auto-Orbit", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Gentle 3D parallax motion on center showcase",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = cfg.enable3dAutoOrbit,
                    onCheckedChange = viewModel::setEnable3dAutoOrbit,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Motion Speed", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = String.format(Locale.US, "%.1fx", cfg.animationSpeedMultiplier),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Slider(
                value = cfg.animationSpeedMultiplier,
                onValueChange = viewModel::setAnimationSpeed,
                valueRange = 0.6f..1.5f,
            )
        }

        LiquidGlassCard {
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
                        onClick = { viewModel.setPopupStyle(style) },
                        label = { Text(style.label) },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Auto Connection Popup", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Trigger frosted popup when earbuds connect",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = cfg.autoConnectionPopupEnabled,
                    onCheckedChange = viewModel::setAutoConnectionPopupEnabled,
                )
            }

            Button(
                onClick = {
                    val brand = if (uiState.connectedBrand != EarbudBrand.GENERIC) {
                        uiState.connectedBrand
                    } else {
                        EarbudBrand.VIVO
                    }
                    viewModel.previewConnectionPopup(brand)
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("preview_popup_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Preview Connection Popup")
            }
        }
    }
}

@Composable
fun GlassCustomizationScreen(
    uiState: BudsStudioUiState,
    viewModel: BudsStudioViewModel,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenAnimationSettings: () -> Unit,
    onToggleBackgroundMonitor: (Boolean) -> Unit,
) {
    val cfg = uiState.customization

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        WhiteGlassScreenHeader(
            title = "Glass Customization",
            subtitle = "Blur, frosted opacity, silver sheen & layout",
            onBack = onBack,
        )

        LiquidGlassCard {
            Text(
                text = "LIQUID GLASS THEME",
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
                            .clickable { viewModel.setThemePreference(pref) }
                            .testTag("theme_${pref.name.lowercase()}"),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                        ) {
                            Text(
                                text = when (pref) {
                                    ThemePreference.WHITE -> "White"
                                    ThemePreference.AUTO -> "Auto"
                                    ThemePreference.BLACK -> "Black"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Text(
                text = cfg.themePreference.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LiquidGlassCard {
            Text(
                text = "FROSTED OPTICS & REFLECTIONS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Glass Frosted Opacity", style = MaterialTheme.typography.titleSmall)
                Text("${(cfg.glassTransparency * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
            }
            Slider(
                value = cfg.glassTransparency,
                onValueChange = viewModel::setGlassTransparency,
                valueRange = 0.45f..0.96f,
                modifier = Modifier.testTag("glass_alpha_slider"),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Backdrop Glass Blur", style = MaterialTheme.typography.titleSmall)
                Text("${cfg.glassBlurDp.roundToInt()} dp", style = MaterialTheme.typography.labelMedium)
            }
            Slider(
                value = cfg.glassBlurDp,
                onValueChange = viewModel::setGlassBlurDp,
                valueRange = 0f..28f,
                modifier = Modifier.testTag("glass_blur_slider"),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Silver Reflection Sheen", style = MaterialTheme.typography.titleSmall)
                Text("${(cfg.silverReflectionIntensity * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
            }
            Slider(
                value = cfg.silverReflectionIntensity,
                onValueChange = viewModel::setSilverReflection,
                valueRange = 0.15f..1.0f,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Card Corner Radius", style = MaterialTheme.typography.titleSmall)
                Text("${cfg.cardCornerRadiusDp.roundToInt()} dp", style = MaterialTheme.typography.labelMedium)
            }
            Slider(
                value = cfg.cardCornerRadiusDp,
                onValueChange = viewModel::setCardCornerRadius,
                valueRange = 14f..34f,
            )
        }

        LiquidGlassCard {
            Text(
                text = "LAYOUT & NOTIFICATION APPEARANCE",
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
                        onClick = { viewModel.setLayoutDensity(density) },
                        label = { Text(density.label) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NotificationStyle.entries.forEach { style ->
                    FilterChip(
                        selected = cfg.notificationStyle == style,
                        onClick = { viewModel.setNotificationStyle(style) },
                        label = { Text(style.label) },
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Pin Equalizer on Home", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Show live 5-band curve card on the Home screen",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = cfg.showEqCardOnHome,
                    onCheckedChange = viewModel::setShowEqCardOnHome,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Background Connection Monitor", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Keep event listener active for alerts",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = cfg.backgroundMonitoringEnabled,
                    onCheckedChange = onToggleBackgroundMonitor,
                )
            }
        }

        OutlinedButton(
            onClick = onOpenAnimationSettings,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text("Open Animation & Popup Settings")
        }
    }
}

@Composable
fun EqualizerScreen(
    uiState: BudsStudioUiState,
    viewModel: BudsStudioViewModel,
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
            title = "Equalizer Studio",
            subtitle = "5-band AudioFX DSP & Bass Boost",
            onBack = onBack,
        )
        EqualizerSettingsSheetContent(
            uiState = uiState,
            onSelectPreset = viewModel::selectEqPreset,
            onUpdateBand = viewModel::updateEqBand,
            onUpdateBassBoost = viewModel::updateBassBoost,
        )
    }
}

@Composable
fun ControlsAndDeviceScreen(
    uiState: BudsStudioUiState,
    viewModel: BudsStudioViewModel,
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
            title = "Controls & Protocol",
            subtitle = "Touch gesture map & brand details",
            onBack = onBack,
        )
        EarbudControlsSettingsSheetContent(
            uiState = uiState,
            onSetDoubleTapLeft = viewModel::setDoubleTapLeft,
            onSetDoubleTapRight = viewModel::setDoubleTapRight,
            onSetTripleTap = viewModel::setTripleTap,
            onSetPressAndHold = viewModel::setPressAndHold,
        )
        DeviceManagementSheetContent(
            uiState = uiState,
            onToggleDualConnection = viewModel::setDualConnection,
            onToggleInEarDetection = viewModel::setInEarDetection,
            onToggleLowLatency = viewModel::setLowLatencyMode,
        )
    }
}
