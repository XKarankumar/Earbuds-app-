package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.AppDestination
import com.example.ui.components.OceanGlassCard
import com.example.ui.components.OceanPillBadge
import com.example.ui.components.OceanTopBar
import com.example.ui.theme.LocalOceanPalette
import com.example.ui.theme.OceanEmeraldColors
import com.example.ui.viewmodel.MainViewModel

private data class ToolEntry(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val destination: AppDestination,
    val testTag: String
)

@Composable
fun ToolsHubScreen(
    viewModel: MainViewModel,
    onNavigate: (AppDestination) -> Unit
) {
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val history by viewModel.calcHistory.collectAsStateWithLifecycle()
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val palette = LocalOceanPalette.current

    val tools = listOf(
        ToolEntry(
            "Unit Converter",
            "Length, weight, area, volume, temp, data & speed",
            Icons.Default.SwapHoriz,
            OceanEmeraldColors.MintGreen,
            AppDestination.UnitConverter,
            "tool_unit_converter"
        ),
        ToolEntry(
            "GST & Finance Calculators",
            "GST (Inclusive/Exclusive), EMI, Discount & Interest",
            Icons.Default.Percent,
            OceanEmeraldColors.OceanBlue,
            AppDestination.GstCalculators,
            "tool_gst_calculators"
        ),
        ToolEntry(
            "Calculation History",
            "${history.size} saved offline records • Tap to reload",
            Icons.Default.History,
            OceanEmeraldColors.EmeraldGreen,
            AppDestination.History,
            "tool_history"
        ),
        ToolEntry(
            "Reports & PDF Export",
            "${metrics.transactionCount} ledger entries • Visual charts",
            Icons.Default.Assessment,
            OceanEmeraldColors.OceanBlue,
            AppDestination.Reports,
            "tool_reports"
        ),
        ToolEntry(
            "Theme & App Settings",
            "${prefs.themeMode.label} • ${prefs.animationPreset.label}",
            Icons.Default.Settings,
            OceanEmeraldColors.MintGreen,
            AppDestination.Settings,
            "tool_settings"
        )
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .testTag("tools_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OceanTopBar(
                    title = "Tools & Suites",
                    subtitle = "All utilities styled with Ocean Emerald 3.5"
                )
            }

            // Active Ocean Emerald 3.5 Palette Strip
            item {
                OceanGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevated = true,
                    onClick = { onNavigate(AppDestination.Settings) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Ocean Emerald 3.5 theme",
                                tint = palette.jamaAccent
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Ocean Emerald 3.5", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${prefs.themeMode.label} • ${prefs.animationPreset.label}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OceanPillBadge(text = "Customize", accent = OceanEmeraldColors.MintGreen)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(
                            OceanEmeraldColors.DeepOceanBlue,
                            OceanEmeraldColors.OceanBlue,
                            OceanEmeraldColors.EmeraldGreen,
                            OceanEmeraldColors.MintGreen,
                            OceanEmeraldColors.GlassWhite
                        ).forEach { swatch ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(swatch)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            )
                        }
                    }
                }
            }

            items(tools.size) { index ->
                val item = tools[index]
                OceanGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(item.testTag),
                    onClick = { onNavigate(item.destination) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(item.accent.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = item.accent
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = item.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Open ${item.title}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Simple SMS Calculator • ${stringResource(R.string.brand_copyright)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
                )
            }
        }
    }
}
