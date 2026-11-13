package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.CalculatorEngine
import com.example.model.AppDestination
import com.example.model.CustomerSummary
import com.example.model.TransactionType
import com.example.ui.components.OceanGlassCard
import com.example.ui.components.OceanPillBadge
import com.example.ui.components.OceanTopBar
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalOceanPalette
import com.example.ui.theme.OceanEmeraldColors
import com.example.ui.viewmodel.MainViewModel

private enum class LedgerFilter(val label: String) {
    ALL("All"),
    DUE("Udhaar Due"),
    SETTLED("Settled / Jama")
}

@Composable
fun HisabKitabScreen(
    viewModel: MainViewModel,
    onNavigate: (AppDestination) -> Unit
) {
    val customers by viewModel.customerSummaries.collectAsStateWithLifecycle()
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val palette = LocalOceanPalette.current

    var searchQuery by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(LedgerFilter.ALL) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredCustomers = remember(customers, searchQuery, filter) {
        customers.filter { c ->
            val matchesQuery = searchQuery.isBlank() ||
                c.name.contains(searchQuery, ignoreCase = true) ||
                c.phone.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (filter) {
                LedgerFilter.ALL -> true
                LedgerFilter.DUE -> c.netBalance > 0.01
                LedgerFilter.SETTLED -> c.netBalance <= 0.01
            }
            matchesQuery && matchesFilter
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .testTag("hisab_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OceanTopBar(
                    title = "Hisab-Kitab",
                    subtitle = "${metrics.customerCount} customers • 100% Offline Room Ledger",
                    trailing = {
                        OceanPillBadge(
                            text = "Net ${CalculatorEngine.formatCurrency(kotlin.math.abs(metrics.netBalance), prefs.currency.symbol)}",
                            accent = if (metrics.netBalance >= 0) palette.udhaarAccent else palette.jamaAccent
                        )
                    }
                )
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search customer name or phone...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_customer_search")
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LedgerFilter.entries.forEach { option ->
                        FilterChip(
                            selected = filter == option,
                            onClick = { filter = option },
                            label = { Text(option.label) }
                        )
                    }
                }
            }

            if (filteredCustomers.isEmpty()) {
                item {
                    OceanGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevated = true
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = palette.jamaAccent,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = if (customers.isEmpty()) "Start Your Hisab-Kitab" else "No Matching Customers",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (customers.isEmpty()) {
                                "Tap + Add Customer to track Jama (received), Udhaar (given), and send instant SMS reminders."
                            } else {
                                "Try clearing your search or switching the filter chip."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (customers.isEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = OceanEmeraldColors.EmeraldGreen),
                                modifier = Modifier.testTag("btn_empty_add_customer")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Add First Customer")
                            }
                        }
                    }
                }
            } else {
                items(filteredCustomers, key = { it.id }) { customer ->
                    CustomerLedgerCard(
                        customer = customer,
                        currencySymbol = prefs.currency.symbol,
                        onOpenProfile = { onNavigate(AppDestination.CustomerProfile(customer.id)) },
                        onSendSms = { viewModel.prepareSmsForCustomer(customer) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = OceanEmeraldColors.EmeraldGreen,
            contentColor = OceanEmeraldColors.GlassWhite,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp)
                .testTag("fab_add_customer")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.cd_add_customer)
            )
        }
    }

    if (showAddDialog) {
        AddCustomerDialog(
            currencySymbol = prefs.currency.symbol,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, phone, notes, openAmt, openType ->
                viewModel.addCustomer(name, phone, notes, openAmt, openType)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun CustomerLedgerCard(
    customer: CustomerSummary,
    currencySymbol: String,
    onOpenProfile: () -> Unit,
    onSendSms: () -> Unit
) {
    val palette = LocalOceanPalette.current
    val isDue = customer.netBalance > 0.01
    val isAdvance = customer.netBalance < -0.01
    val balColor = when {
        isDue -> palette.udhaarAccent
        isAdvance -> palette.jamaAccent
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val initials = customer.name
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifBlank { "C" }

    OceanGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_card_${customer.id}"),
        onClick = onOpenProfile
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
                        style = MaterialTheme.typography.titleSmall,
                        color = OceanEmeraldColors.GlassWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = customer.phone.ifBlank { "${customer.transactionCount} entries" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CalculatorEngine.formatCurrency(kotlin.math.abs(customer.netBalance), currencySymbol),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = balColor
                    )
                    Text(
                        text = when {
                            isDue -> "Udhaar Due"
                            isAdvance -> "Advance Jama"
                            else -> "Settled"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = balColor
                    )
                }
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = onSendSms,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Sms,
                        contentDescription = stringResource(R.string.cd_send_sms),
                        tint = palette.jamaAccent
                    )
                }
            }
        }
    }
}

@Composable
fun AddCustomerDialog(
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Double, TransactionType) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var openingAmountText by remember { mutableStateOf("") }
    var openingType by remember { mutableStateOf(TransactionType.UDHAAR) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Customer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_customer_name")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (for SMS)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_customer_phone")
                )
                OutlinedTextField(
                    value = openingAmountText,
                    onValueChange = { openingAmountText = it },
                    label = { Text("Opening Amount ($currencySymbol, optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                if ((openingAmountText.toDoubleOrNull() ?: 0.0) > 0.0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TransactionType.entries.forEach { t ->
                            val selected = openingType == t
                            OutlinedButton(
                                onClick = { openingType = t },
                                border = BorderStroke(
                                    1.2.dp,
                                    if (selected) OceanEmeraldColors.EmeraldGreen else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (t == TransactionType.JAMA) "+ Jama" else "- Udhaar", maxLines = 1)
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Shop Ref") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = OceanEmeraldColors.EmeraldGreen),
                modifier = Modifier.testTag("btn_confirm_add_customer"),
                onClick = {
                    onConfirm(
                        name,
                        phone,
                        notes,
                        openingAmountText.toDoubleOrNull() ?: 0.0,
                        openingType
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
