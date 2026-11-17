package com.example.desicalc.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.desicalc.data.local.TransactionEntity
import com.example.desicalc.data.repository.DesiLedgerRepository
import com.example.desicalc.engine.CalculatorEngine
import com.example.desicalc.model.AppSettingsState
import com.example.desicalc.model.CustomerLedgerSummary
import com.example.desicalc.model.DashboardMetrics
import com.example.desicalc.model.TransactionType
import com.example.desicalc.model.formatInr
import com.example.desicalc.ui.components.CopyrightFooter
import com.example.desicalc.ui.components.CustomerAvatar
import com.example.desicalc.ui.components.DesiCard
import com.example.desicalc.ui.components.MetricStatCard
import com.example.desicalc.ui.strings.DesiStrings
import com.example.desicalc.ui.theme.LocalDesiColors
import com.example.desicalc.ui.theme.MonoFontFamily

@Composable
fun DashboardScreen(
    metrics: DashboardMetrics,
    customers: List<CustomerLedgerSummary>,
    recentTransactions: List<TransactionEntity>,
    settings: AppSettingsState,
    strings: DesiStrings,
    onOpenCalculator: () -> Unit,
    onOpenCustomers: () -> Unit,
    onSelectCustomer: (Long) -> Unit,
    onOpenAiWithPrompt: (String) -> Unit,
    onOpenSmsReports: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalDesiColors.current
    val customerById = remember(customers) { customers.associateBy { it.id } }
    val pendingCustomers = remember(customers) {
        customers.filter { it.netBalance > 0.01 }.sortedByDescending { it.netBalance }.take(5)
    }
    var quickCalcInput by remember { mutableStateOf("") }
    var quickCalcOutput by remember { mutableStateOf("= 0") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Business Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(154.dp)
                    .clip(MaterialTheme.shapes.large)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_desi_hero_banner_1794830689537),
                    contentDescription = "Desi Calculator hero banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xE6081226),
                                    Color(0x990A1A33),
                                    Color(0x4D0A1A33)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = settings.businessName,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Desi Calculator AI PRO V2.0 • INR ₹",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF99F6E4)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = strings.remainingBalance,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFDE68A)
                            )
                            Text(
                                text = formatInr(metrics.netRemainingBalance),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                        }
                        FilledTonalButton(
                            onClick = { onOpenAiWithPrompt("") },
                            modifier = Modifier.testTag("hero_ai_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "Open AI Smart Assistant",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.navAi)
                        }
                    }
                }
            }
        }

        // 2x2 Summary Metrics Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatCard(
                        title = strings.totalCustomers,
                        value = metrics.totalCustomers.toString(),
                        subtitle = "${metrics.pendingCustomerCount} pending",
                        icon = Icons.Rounded.Groups,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenCustomers
                    )
                    MetricStatCard(
                        title = strings.totalUdhar,
                        value = formatInr(metrics.totalUdharDiya),
                        subtitle = if (metrics.totalUdharLiya > 0) "Liya: ${formatInr(metrics.totalUdharLiya)}" else "Udhar Diya",
                        icon = Icons.AutoMirrored.Rounded.CallMade,
                        accentColor = ext.udharRed,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenCustomers
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatCard(
                        title = strings.totalJama,
                        value = formatInr(metrics.totalJamaReceived),
                        subtitle = if (metrics.totalPaymentMade > 0) "Paid: ${formatInr(metrics.totalPaymentMade)}" else "Jama Received",
                        icon = Icons.AutoMirrored.Rounded.CallReceived,
                        accentColor = ext.jamaGreen,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenCustomers
                    )
                    MetricStatCard(
                        title = strings.todaysTransactions,
                        value = "${metrics.todayTransactionCount} tx",
                        subtitle = "U: ${formatInr(metrics.todayUdharTotal)} | J: ${formatInr(metrics.todayJamaTotal)}",
                        icon = Icons.Rounded.AccountBalanceWallet,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenSmsReports
                    )
                }
            }
        }

        // Quick Actions Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenCustomers,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_add_khata_btn")
                ) {
                    Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.navKhata, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = onOpenCalculator,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_open_calc_btn")
                ) {
                    Icon(Icons.Rounded.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.navCalc, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        // Quick Inline Calculator Card
        item {
            DesiCard {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Calculate,
                                contentDescription = strings.quickCalculator,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.quickCalculator,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        FilledTonalButton(onClick = onOpenCalculator) {
                            Text("GST & Pro")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = quickCalcInput,
                            onValueChange = { raw ->
                                quickCalcInput = raw
                                val eval = CalculatorEngine.evaluateExpression(raw)
                                quickCalcOutput = if (raw.isBlank()) "= 0"
                                else eval.fold(
                                    onSuccess = { "= ${CalculatorEngine.formatCleanNumber(it)}" },
                                    onFailure = { "..." }
                                )
                            },
                            placeholder = { Text("e.g. 1250*3 + 18%") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_calc_input")
                        )
                        Text(
                            text = quickCalcOutput,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Pending Payments Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.pendingPayments,
                    style = MaterialTheme.typography.titleMedium
                )
                if (pendingCustomers.isNotEmpty()) {
                    Text(
                        text = "${pendingCustomers.size} top due",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (pendingCustomers.isEmpty()) {
            item {
                DesiCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = ext.jamaGreen
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "No pending receivables. Add a customer in Khata or ask AI to record Udhar/Jama.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(pendingCustomers, key = { "pending_${it.id}" }) { cust ->
                DesiCard(onClick = { onSelectCustomer(cust.id) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CustomerAvatar(name = cust.name, photoPath = cust.photoPath)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cust.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = cust.mobile.ifBlank { cust.customerCode },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formatInr(cust.netBalance),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = ext.udharRed
                            )
                            Text(
                                text = "Lena Hai",
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.udharRed
                            )
                        }
                    }
                }
            }
        }

        // Recent Transactions History
        item {
            Text(
                text = strings.recentHistory,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        val recentSlice = recentTransactions.take(8)
        if (recentSlice.isEmpty()) {
            item {
                DesiCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "No transactions yet. Tap Khata or AI Smart to log your first entry.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(recentSlice, key = { "recent_${it.id}" }) { tx ->
                val custName = customerById[tx.customerId]?.name ?: "Customer #${tx.customerId}"
                val txType = TransactionType.fromCode(tx.type)
                val badgeColor = if (txType == TransactionType.UDHAR_DIYA || txType == TransactionType.UDHAR_LIYA) {
                    ext.udharRed
                } else {
                    ext.jamaGreen
                }
                DesiCard(onClick = { onSelectCustomer(tx.customerId) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(badgeColor.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (txType == TransactionType.UDHAR_DIYA || txType == TransactionType.UDHAR_LIYA) {
                                    Icons.AutoMirrored.Rounded.CallMade
                                } else {
                                    Icons.AutoMirrored.Rounded.CallReceived
                                },
                                contentDescription = txType.labelEn,
                                tint = badgeColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = custName,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${txType.labelEn} • ${DesiLedgerRepository.formatTimestamp(tx.timestamp)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (tx.description.isNotBlank()) {
                                Text(
                                    text = tx.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Text(
                            text = formatInr(tx.amount),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = badgeColor
                        )
                    }
                }
            }
        }

        item { CopyrightFooter() }
    }
}
