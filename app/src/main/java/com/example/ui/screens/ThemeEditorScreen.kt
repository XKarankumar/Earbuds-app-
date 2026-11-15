package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.hometheatre.core.CustomThemeConfig
import com.example.hometheatre.core.ThemePreset
import com.example.hometheatre.core.UserAudioPreferences
import com.example.ui.components.GlassConsoleCard
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.viewmodel.TheatreViewModel

@Composable
fun ThemeEditorScreen(
    viewModel: TheatreViewModel,
    prefs: UserAudioPreferences,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val palette = LocalStudioPalette.current
    var draft by remember(prefs.customTheme) { mutableStateOf(prefs.customTheme) }

    val bgOptions = listOf(0xFF07090F, 0xFF000000, 0xFF0D1B2A, 0xFF1A0B2E, 0xFFE2E8F0)
    val accentOptions = listOf(0xFF00E5FF, 0xFF10B981, 0xFFFF2A85, 0xFFF59E0B, 0xFFFFFFFF, 0xFF6366F1)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("theme_editor_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.cd_back),
                        tint = palette.textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "CUSTOM THEME EDITOR",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.textPrimary
                )
            }
        }

        item {
            GlassConsoleCard {
                Text("Preset Console Themes", style = MaterialTheme.typography.titleMedium, color = palette.primaryAccent)
                Spacer(modifier = Modifier.height(10.dp))
                ThemePreset.entries.forEach { preset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectTheme(preset) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(preset.displayName, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                            Text(preset.description, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
                        }
                        if (prefs.themePreset == preset) {
                            Text("SELECTED", style = MaterialTheme.typography.labelMedium, color = palette.primaryAccent)
                        }
                    }
                }
            }
        }

        item {
            GlassConsoleCard(highlighted = true) {
                Text("Custom Studio Palette", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                Spacer(modifier = Modifier.height(10.dp))

                Text("Console Background Tone", style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    bgOptions.forEach { hex ->
                        val isLight = hex == 0xFFE2E8F0
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(
                                    width = if (draft.backgroundHex == hex) 3.dp else 1.dp,
                                    color = if (draft.backgroundHex == hex) palette.primaryAccent else palette.borderStroke,
                                    shape = CircleShape
                                )
                                .clickable {
                                    draft = draft.copy(
                                        backgroundHex = hex,
                                        surfaceHex = if (isLight) 0xFFF8FAFC else 0xFF141926,
                                        textPrimaryHex = if (isLight) 0xFF0F172A else 0xFFF8FAFC
                                    )
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Primary Accent & Meter Color", style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    accentOptions.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(
                                    width = if (draft.primaryAccentHex == hex) 3.dp else 1.dp,
                                    color = if (draft.primaryAccentHex == hex) Color.White else palette.borderStroke,
                                    shape = CircleShape
                                )
                                .clickable { draft = draft.copy(primaryAccentHex = hex) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Secondary Highlight Color", style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    accentOptions.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(
                                    width = if (draft.secondaryAccentHex == hex) 3.dp else 1.dp,
                                    color = if (draft.secondaryAccentHex == hex) Color.White else palette.borderStroke,
                                    shape = CircleShape
                                )
                                .clickable { draft = draft.copy(secondaryAccentHex = hex) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Glass Opacity (${(draft.glassOpacity * 100).toInt()}%)", style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary)
                Slider(
                    value = draft.glassOpacity,
                    onValueChange = { draft = draft.copy(glassOpacity = it) },
                    valueRange = 0.45f..0.95f
                )

                Text("Neon Glow Intensity (${(draft.glowIntensity * 100).toInt()}%)", style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary)
                Slider(
                    value = draft.glowIntensity,
                    onValueChange = { draft = draft.copy(glowIntensity = it) },
                    valueRange = 0.1f..1f
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            draft = CustomThemeConfig()
                            viewModel.updateCustomTheme(draft)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset Default")
                    }
                    Button(
                        onClick = { viewModel.updateCustomTheme(draft) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("apply_custom_theme_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primaryAccent,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Apply Custom Theme")
                    }
                }
            }
        }
    }
}
