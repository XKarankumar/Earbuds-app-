package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.AnimationPreset
import com.example.model.CurrencyOption
import com.example.model.OceanEmeraldMode
import com.example.ui.components.OceanGlassCard
import com.example.ui.components.OceanPillBadge
import com.example.ui.components.OceanTopBar
import com.example.ui.theme.LocalOceanPalette
import com.example.ui.theme.OceanEmeraldColors
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val palette = LocalOceanPalette.current
    var signatureInput by remember(prefs.businessSignature) { mutableStateOf(prefs.businessSignature) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OceanTopBar(
                    title = "Settings & Theme",
                    subtitle = "Ocean Emerald 3.5 • Saved locally offline",
                    onBack = onBack,
                    backTestTag = "btn_back_settings"
                )
            }

            // Light & Dark Theme Variations
            item {
                OceanGlassCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                    Text(
                        text = "Ocean Emerald 3.5 Variation",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Switch smoothly between Deep Ocean Dark and Glass White Light variations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OceanEmeraldMode.entries.forEach { mode ->
                            val selected = prefs.themeMode == mode
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (selected) OceanEmeraldColors.OceanBlue.copy(alpha = 0.22f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(
                                    width = if (selected) 1.6.dp else 1.dp,
                                    color = if (selected) OceanEmeraldColors.MintGreen else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("theme_mode_${mode.name.lowercase()}")
                                    .clickable { viewModel.setThemeMode(mode) }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Icon(
                                        imageVector = if (mode.isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                                        contentDescription = mode.label,
                                        tint = if (selected) palette.jamaAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = if (mode.isDark) "Dark Ocean" else "Glass Light",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (mode.isDark) "#071B33 Base" else "#F5FFFC Base",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Glass Opacity & Frost (${(prefs.glassIntensity * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = prefs.glassIntensity,
                        onValueChange = viewModel::setGlassIntensity,
                        valueRange = 0.45f..1.0f
                    )
                }
            }

            // Adjustable Animation Settings
            item {
                OceanGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Animation,
                            contentDescription = "Animation settings",
                            tint = palette.jamaAccent
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Animation Settings", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AnimationPreset.entries.forEach { preset ->
                            val selected = prefs.animationPreset == preset
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) OceanEmeraldColors.EmeraldGreen.copy(alpha = 0.18f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) OceanEmeraldColors.MintGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("anim_${preset.name.lowercase()}")
                                    .clickable { viewModel.setAnimationPreset(preset) }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = preset.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (selected) palette.jamaAccent else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = preset.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Key Haptic Feedback", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Tactile pulse on calculator button press",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = prefs.hapticFeedbackEnabled,
                            onCheckedChange = viewModel::setHapticFeedback
                        )
                    }
                }
            }

            // Currency & SMS Signature
            item {
                OceanGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Currency & SMS Signature", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CurrencyOption.entries.forEach { opt ->
                            FilterChip(
                                selected = prefs.currency == opt,
                                onClick = { viewModel.setCurrency(opt) },
                                label = { Text("${opt.symbol} ${opt.code}") }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = signatureInput,
                        onValueChange = { signatureInput = it },
                        label = { Text("Business / Sender Signature for SMS & PDF") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.setBusinessSignature(signatureInput) },
                            colors = ButtonDefaults.buttonColors(containerColor = OceanEmeraldColors.EmeraldGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save Signature")
                        }
                        OutlinedButton(
                            onClick = { viewModel.resetSettings() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reset Defaults")
                        }
                    }
                }
            }

            // Branding & Offline Ownership Card
            item {
                OceanGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = palette.jamaAccent)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
                        }
                        OceanPillBadge(text = "100% Offline", accent = OceanEmeraldColors.MintGreen)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Theme: Ocean Emerald 3.5",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Owner: ${stringResource(R.string.brand_owner)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Package: com.hardikkumar.simplesmscalculator",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.brand_copyright),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.jamaAccent
                    )
                }
            }
        }
    }
}
