package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.CalculatorEngine
import com.example.model.AppDestination
import com.example.ui.components.OceanGlassCard
import com.example.ui.components.OceanTopBar
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalOceanPalette
import com.example.ui.theme.OceanEmeraldColors
import com.example.ui.viewmodel.MainViewModel

@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    onNavigate: (AppDestination) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val customers by viewModel.customerSummaries.collectAsStateWithLifecycle()
    val calcHistory by viewModel.calcHistory.collectAsStateWithLifecycle()
    val smsLogs by viewModel.smsLogs.collectAsStateWithLifecycle()
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val palette = LocalOceanPalette.current
    val currency = prefs.currency.symbol

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
                    title = "Reports & Analytics",
                    subtitle = "Offline ledger breakdown & PDF export",
                    onBack = onBack,
                    backTestTag = "btn_back_reports"
                )
            }

            // Visual Jama vs Udhaar Chart Card
            item {
                val maxVal = maxOf(metrics.totalJama, metrics.totalUdhaar, 1.0)
                val jamaFraction = (metrics.totalJama / maxVal).toFloat().coerceIn(0.04f, 1f)
                val udhaarFraction = (metrics.totalUdhaar / maxVal).toFloat().coerceIn(0.04f, 1f)

                OceanGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevated = true
                ) {
                    Text(
                        text = "Jama vs Udhaar Overview",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Total Jama: ${CalculatorEngine.formatCurrency(metrics.totalJama, currency)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.jamaAccent
                    )
                    Spacer(Modifier.height(4.dp))
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                    ) {
                        drawRoundRect(
                            color = OceanEmeraldColors.MintGreen.copy(alpha = 0.16f),
                            cornerRadius = CornerRadius(16f, 16f),
                            size = size
                        )
                        drawRoundRect(
                            color = OceanEmeraldColors.EmeraldGreen,
                            cornerRadius = CornerRadius(16f, 16f),
                            size = Size(width = size.width * jamaFraction, height = size.height)
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Total Udhaar: ${CalculatorEngine.formatCurrency(metrics.totalUdhaar, currency)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.udhaarAccent
                    )
                    Spacer(Modifier.height(4.dp))
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                    ) {
                        drawRoundRect(
                            color = OceanEmeraldColors.OceanBlue.copy(alpha = 0.16f),
                            cornerRadius = CornerRadius(16f, 16f),
                            size = size
                        )
                        drawRoundRect(
                            color = OceanEmeraldColors.OceanBlue,
                            cornerRadius = CornerRadius(16f, 16f),
                            size = Size(width = size.width * udhaarFraction, height = size.height)
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatBadge("Customers", "${metrics.customerCount}")
                        StatBadge("Transactions", "${metrics.transactionCount}")
                        StatBadge("Calculations", "${calcHistory.size}")
                        StatBadge("SMS Logs", "${smsLogs.size}")
                    }
                }
            }

            // Export Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.exportAndSharePdfReport(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = OceanEmeraldColors.EmeraldGreen),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_export_pdf")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Export PDF", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            val text = buildString {
                                append("Hisab-Kitab Summary (${prefs.businessSignature})\n")
                                append("Total Jama: ${CalculatorEngine.formatCurrency(metrics.totalJama, currency)}\n")
                                append("Total Udhaar: ${CalculatorEngine.formatCurrency(metrics.totalUdhaar, currency)}\n")
                                append("Net Balance: ${CalculatorEngine.formatCurrency(metrics.netBalance, currency)}\n")
                                append("Customers: ${metrics.customerCount}, Entries: ${metrics.transactionCount}")
                            }
                            viewModel.sharePlainText(context, text, "Share Ledger Report")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share Text", maxLines = 1)
                    }
                }
            }

            item {
                Text(
                    text = "Customer Balance Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (customers.isEmpty()) {
                item {
                    OceanGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Add customers in Hisab-Kitab to see customer-wise balance analytics.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(customers, key = { it.id }) { c ->
                    val isDue = c.netBalance > 0.01
                    val balColor = if (isDue) palette.udhaarAccent else palette.jamaAccent
                    OceanGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(14.dp),
                        onClick = { onNavigate(AppDestination.CustomerProfile(c.id)) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(c.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "Jama ${CalculatorEngine.formatCurrency(c.totalJama, currency)} • Udhaar ${CalculatorEngine.formatCurrency(c.totalUdhaar, currency)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = CalculatorEngine.formatCurrency(kotlin.math.abs(c.netBalance), currency),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = balColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold
            ),
            color = OceanEmeraldColors.MintGreen
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
