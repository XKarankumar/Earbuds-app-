package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.CalculatorEngine
import com.example.model.AppDestination
import com.example.model.CustomerSummary
import com.example.model.OceanEmeraldMode
import com.example.model.TransactionType
import com.example.ui.components.OceanGlassCard
import com.example.ui.components.OceanPillBadge
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalOceanPalette
import com.example.ui.theme.OceanEmeraldColors
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class DashboardShortcut(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val destination: AppDestination,
    val testTag: String
)

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigate: (AppDestination) -> Unit
) {
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val customers by viewModel.customerSummaries.collectAsStateWithLifecycle()
    val recentTransactions by viewModel.recentTransactions.collectAsStateWithLifecycle()
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val palette = LocalOceanPalette.current

    var showAddCustomerDialog by remember { mutableStateOf(false) }

    val shortcuts = remember {
        listOf(
            DashboardShortcut(
                "Quick Calculator",
                "Ocean & Emerald keypad",
                Icons.Default.Calculate,
                OceanEmeraldColors.OceanBlue,
                AppDestination.Calculator,
                "shortcut_calculator"
            ),
            DashboardShortcut(
                "SMS Shortcut",
                "Templates & reminders",
                Icons.Default.Sms,
                OceanEmeraldColors.EmeraldGreen,
                AppDestination.SmsCenter,
                "shortcut_sms"
            ),
            DashboardShortcut(
                "Unit Converter",
                "Length, weight, area",
                Icons.Default.SwapHoriz,
                OceanEmeraldColors.MintGreen,
                AppDestination.UnitConverter,
                "shortcut_converter"
            ),
            DashboardShortcut(
                "GST & Finance",
                "GST, EMI & Discount",
                Icons.Default.Percent,
                OceanEmeraldColors.OceanBlue,
                AppDestination.GstCalculators,
                "shortcut_gst"
            ),
            DashboardShortcut(
                "Hisab-Kitab",
                "Customer accounts",
                Icons.Default.AccountBalanceWallet,
                OceanEmeraldColors.EmeraldGreen,
                AppDestination.HisabKitab,
                "shortcut_hisab"
            ),
            DashboardShortcut(
                "Reports",
                "Charts & PDF export",
                Icons.Default.Assessment,
                OceanEmeraldColors.MintGreen,
                AppDestination.Reports,
                "shortcut_reports"
            )
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .testTag("dashboard_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Header Banner
            item {
                OceanGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevated = true,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(156.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_ocean_emerald_hero_1794570568765),
                            contentDescription = stringResource(R.string.cd_hero_banner),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            OceanEmeraldColors.DeepOceanBlue.copy(alpha = 0.52f),
                                            OceanEmeraldColors.DeepOceanBlue.copy(alpha = 0.86f)
                                        )
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OceanPillBadge(
                                        text = "OCEAN EMERALD 3.5",
                                        accent = OceanEmeraldColors.MintGreen
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = viewModel::toggleThemeMode,
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("btn_dashboard_theme_toggle")
                                        ) {
                                            Icon(
                                                imageVector = if (prefs.themeMode == OceanEmeraldMode.DARK) {
                                                    Icons.Default.LightMode
                                                } else {
                                                    Icons.Default.DarkMode
                                                },
                                                contentDescription = stringResource(R.string.cd_theme_toggle),
                                                tint = OceanEmeraldColors.GlassWhite
                                            )
                                        }
                                        IconButton(
                                            onClick = { onNavigate(AppDestination.Settings) },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("btn_dashboard_settings")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = "Settings",
                                                tint = OceanEmeraldColors.GlassWhite
                                            )
                                        }
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Simple SMS Calculator",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = OceanEmeraldColors.GlassWhite
                                    )
                                    Text(
                                        text = "Smart Calc • Hisab-Kitab • Offline SMS Ledger • By Hardik Kumar",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OceanEmeraldColors.MintGreen.copy(alpha = 0.92f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Total Jama & Total Udhaar Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OceanGlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_total_jama"),
                        glowBorderColor = OceanEmeraldColors.EmeraldGreen,
                        onClick = { onNavigate(AppDestination.HisabKitab) }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = "Total Jama",
                                tint = palette.jamaAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Total Jama",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = CalculatorEngine.formatCurrency(metrics.totalJama, prefs.currency.symbol),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = palette.jamaAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Received / Credit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OceanGlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_total_udhaar"),
                        glowBorderColor = OceanEmeraldColors.OceanBlue,
                        onClick = { onNavigate(AppDestination.HisabKitab) }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = "Total Udhaar",
                                tint = palette.udhaarAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Total Udhaar",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = CalculatorEngine.formatCurrency(metrics.totalUdhaar, prefs.currency.symbol),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = palette.udhaarAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Given / Debit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Remaining Balance Card
            item {
                val net = metrics.netBalance
                val balanceColor = if (net >= 0) palette.jamaAccent else palette.warningAccent
                OceanGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_remaining_balance"),
                    elevated = true,
                    onClick = { onNavigate(AppDestination.Reports) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Remaining Balance",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = CalculatorEngine.formatCurrency(kotlin.math.abs(net), prefs.currency.symbol),
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontFamily = JetBrainsMonoFamily
                                ),
                                color = balanceColor
                            )
                            Text(
                                text = when {
                                    net > 0.01 -> "Net Receivable across ${metrics.customerCount} customers"
                                    net < -0.01 -> "Net Advance held across ${metrics.customerCount} customers"
                                    else -> "All accounts settled • ${metrics.customerCount} customers"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "View reports",
                            tint = balanceColor
                        )
                    }
                }
            }

            // Customer Shortcuts Strip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Customer Shortcuts",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = { onNavigate(AppDestination.HisabKitab) }) {
                        Text("All (${customers.size})")
                    }
                }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OceanGlassCard(
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            glowBorderColor = OceanEmeraldColors.MintGreen,
                            onClick = { showAddCustomerDialog = true },
                            modifier = Modifier.testTag("btn_dashboard_add_customer")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(OceanEmeraldColors.EmeraldGreen.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = stringResource(R.string.cd_add_customer),
                                        tint = palette.jamaAccent
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "Add Customer",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    items(customers.take(10), key = { it.id }) { customer ->
                        CustomerShortcutPill(
                            customer = customer,
                            currencySymbol = prefs.currency.symbol,
                            onClick = { onNavigate(AppDestination.CustomerProfile(customer.id)) }
                        )
                    }
                }
            }

            // Quick Tools & Shortcuts (2-column grid)
            item {
                Text(
                    text = "Quick Workspace",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    shortcuts.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pair.forEach { item ->
                                OceanGlassCard(
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag(item.testTag),
                                    contentPadding = PaddingValues(14.dp),
                                    onClick = { onNavigate(item.destination) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(item.accent.copy(alpha = 0.18f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.title,
                                            tint = item.accent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recent Transactions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (recentTransactions.isNotEmpty()) {
                        TextButton(onClick = { onNavigate(AppDestination.Reports) }) {
                            Text("View Reports")
                        }
                    }
                }
            }

            if (recentTransactions.isEmpty()) {
                item {
                    OceanGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No transactions yet",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Add a customer in Hisab-Kitab or send a calculation directly from the Calculator to track Jama & Udhaar offline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val dateFmt = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                items(recentTransactions.take(8), key = { it.id }) { tx ->
                    val isJama = tx.type == TransactionType.JAMA
                    val txColor = if (isJama) palette.jamaAccent else palette.udhaarAccent
                    OceanGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(14.dp),
                        onClick = { onNavigate(AppDestination.CustomerProfile(tx.customerId)) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.customerName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = buildString {
                                        append(if (isJama) "Jama Received" else "Udhaar Given")
                                        if (tx.note.isNotBlank()) append(" • ${tx.note}")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = dateFmt.format(Date(tx.createdAtMillis)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                )
                            }
                            Text(
                                text = (if (isJama) "+" else "-") +
                                    CalculatorEngine.formatCurrency(tx.amount, prefs.currency.symbol),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = txColor
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.brand_copyright),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 12.dp)
                )
            }
        }
    }

    if (showAddCustomerDialog) {
        AddCustomerDialog(
            currencySymbol = prefs.currency.symbol,
            onDismiss = { showAddCustomerDialog = false },
            onConfirm = { name, phone, notes, openAmt, openType ->
                viewModel.addCustomer(name, phone, notes, openAmt, openType)
                showAddCustomerDialog = false
            }
        )
    }
}

@Composable
private fun CustomerShortcutPill(
    customer: CustomerSummary,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val palette = LocalOceanPalette.current
    val initials = customer.name
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifBlank { "C" }
    val balColor = if (customer.netBalance > 0.01) palette.udhaarAccent else palette.jamaAccent

    OceanGlassCard(
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(OceanEmeraldColors.OceanBlue, OceanEmeraldColors.EmeraldGreen)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.labelLarge,
                    color = OceanEmeraldColors.GlassWhite,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = CalculatorEngine.formatCurrency(kotlin.math.abs(customer.netBalance), currencySymbol),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = balColor
                )
            }
        }
    }
}
