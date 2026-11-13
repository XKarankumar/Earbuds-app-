package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.LedgerRepository
import com.example.export.ReportExporter
import com.example.model.AuditAction
import com.example.model.Customer
import com.example.model.LedgerTransaction
import com.example.model.SmsDispatchMethod
import com.example.model.TransactionType
import com.example.notifications.ReminderScheduler
import com.example.permissions.PermissionManager
import com.example.sms.SmsReceiptHelper
import com.example.ui.components.ContactsPickerSheet
import com.example.ui.components.GlassCard
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class CustomerFilter(val label: String) {
    ALL("All"),
    DUE("Udhaar Due"),
    SETTLED("Settled / Jama")
}

@Composable
fun LedgerScreen(viewModel: MainViewModel) {
    val customersState by viewModel.customersState.collectAsStateWithLifecycle()
    val transactionsState by viewModel.transactionsState.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedCustomerId.collectAsStateWithLifecycle()
    val prefs by viewModel.localPrefs.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(CustomerFilter.ALL) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var activeTxTypeDialog by remember { mutableStateOf<TransactionType?>(null) }

    val customers = (customersState as? UiState.Success)?.data.orEmpty()
    val allTransactions = (transactionsState as? UiState.Success)?.data.orEmpty()
    val selectedCustomer = customers.firstOrNull { it.id == selectedId }
    val businessName = profile?.businessName ?: "Hardik Kumar Enterprises"
    val ownerName = profile?.displayName ?: "Hardik Kumar"

    val filteredCustomers = remember(customers, searchQuery, filter) {
        customers.filter { cust ->
            val matchesSearch = searchQuery.isBlank() ||
                cust.name.contains(searchQuery, ignoreCase = true) ||
                cust.phone.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (filter) {
                CustomerFilter.ALL -> true
                CustomerFilter.DUE -> cust.balancePaise > 0L
                CustomerFilter.SETTLED -> cust.balancePaise <= 0L
            }
            matchesSearch && matchesFilter
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTabletSplit = maxWidth >= 720.dp

        if (isTabletSplit) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.95f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hisab-Kitab Khata", style = MaterialTheme.typography.headlineSmall)
                        Button(
                            onClick = { showAddCustomerDialog = true },
                            modifier = Modifier.testTag("add_customer_btn")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Add Customer")
                        }
                    }

                    LedgerSummaryBanner(customers = customers)

                    LedgerSearchAndFilters(
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        filter = filter,
                        onFilterChange = { filter = it }
                    )

                    CustomerListContent(
                        state = customersState,
                        customers = filteredCustomers,
                        selectedId = selectedCustomer?.id,
                        onSelectCustomer = { viewModel.selectCustomer(it.id) }
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1.25f)
                        .fillMaxHeight()
                ) {
                    if (selectedCustomer != null) {
                        CustomerDetailPane(
                            customer = selectedCustomer,
                            transactions = allTransactions.filter { it.customerId == selectedCustomer.id },
                            businessName = businessName,
                            ownerName = ownerName,
                            preferDirectSms = prefs.directSmsFeatureEnabled,
                            showBackButton = false,
                            onBack = { viewModel.selectCustomer(null) },
                            onAddTx = { activeTxTypeDialog = it },
                            onAuditLog = { action, summary ->
                                viewModel.logAudit(action, selectedCustomer.id, summary)
                            },
                            onMessage = viewModel::emitMessage
                        )
                    } else {
                        GlassCard(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(28.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Select a Customer", style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Choose any customer from the left to inspect Udhaar/Jama balance, send SMS receipts, or export statements.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        } else {
            if (selectedCustomer != null) {
                BackHandler { viewModel.selectCustomer(null) }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    CustomerDetailPane(
                        customer = selectedCustomer,
                        transactions = allTransactions.filter { it.customerId == selectedCustomer.id },
                        businessName = businessName,
                        ownerName = ownerName,
                        preferDirectSms = prefs.directSmsFeatureEnabled,
                        showBackButton = true,
                        onBack = { viewModel.selectCustomer(null) },
                        onAddTx = { activeTxTypeDialog = it },
                        onAuditLog = { action, summary ->
                            viewModel.logAudit(action, selectedCustomer.id, summary)
                        },
                        onMessage = viewModel::emitMessage
                    )
                }
            } else {
                Scaffold(
                    containerColor = Color.Transparent,
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { showAddCustomerDialog = true },
                            modifier = Modifier.testTag("add_customer_fab"),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Customer")
                        }
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Hisab-Kitab Khata", style = MaterialTheme.typography.headlineSmall)
                        LedgerSummaryBanner(customers = customers)
                        LedgerSearchAndFilters(
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            filter = filter,
                            onFilterChange = { filter = it }
                        )
                        CustomerListContent(
                            state = customersState,
                            customers = filteredCustomers,
                            selectedId = null,
                            onSelectCustomer = { viewModel.selectCustomer(it.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddCustomerDialog) {
        AddCustomerModal(
            contactsEnabled = prefs.contactsFeatureEnabled,
            onDismiss = { showAddCustomerDialog = false },
            onSave = { name, phone, notes ->
                viewModel.addCustomer(name, phone, notes)
                showAddCustomerDialog = false
            }
        )
    }

    val txTarget = selectedCustomer
    val txType = activeTxTypeDialog
    if (txTarget != null && txType != null) {
        RecordTransactionModal(
            customer = txTarget,
            type = txType,
            businessName = businessName,
            preferDirectSms = prefs.directSmsFeatureEnabled,
            onDismiss = { activeTxTypeDialog = null },
            onConfirm = { amountRupees, note, billRef, smsSent, smsMethod ->
                viewModel.addTransaction(
                    customer = txTarget,
                    type = txType,
                    amountRupees = amountRupees,
                    note = note,
                    billRef = billRef,
                    smsSent = smsSent,
                    smsMethod = smsMethod
                )
                activeTxTypeDialog = null
            }
        )
    }
}

@Composable
private fun LedgerSummaryBanner(customers: List<Customer>) {
    val glass = LocalGlassPalette.current
    val totalUdhaar = customers.sumOf { it.totalUdhaarPaise }
    val totalJama = customers.sumOf { it.totalJamaPaise }
    val netDue = totalUdhaar - totalJama

    GlassCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("YOU WILL GET (UDHAAR)", style = MaterialTheme.typography.labelSmall, color = glass.udhaarAccent)
                Text(
                    text = LedgerRepository.formatCurrencyPaise(totalUdhaar),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = JetBrainsMonoFamily,
                    color = glass.udhaarAccent
                )
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("RECEIVED (JAMA)", style = MaterialTheme.typography.labelSmall, color = glass.jamaAccent)
                Text(
                    text = LedgerRepository.formatCurrencyPaise(totalJama),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = JetBrainsMonoFamily,
                    color = glass.jamaAccent
                )
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("NET BALANCE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = LedgerRepository.formatCurrencyPaise(netDue),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = JetBrainsMonoFamily
                )
            }
        }
    }
}

@Composable
private fun LedgerSearchAndFilters(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    filter: CustomerFilter,
    onFilterChange: (CustomerFilter) -> Unit
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Search customers by name or phone") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CustomerFilter.entries.forEach { f ->
            FilterChip(
                selected = filter == f,
                onClick = { onFilterChange(f) },
                label = { Text(f.label) }
            )
        }
    }
}

@Composable
private fun CustomerListContent(
    state: UiState<List<Customer>>,
    customers: List<Customer>,
    selectedId: String?,
    onSelectCustomer: (Customer) -> Unit
) {
    val glass = LocalGlassPalette.current
    when (state) {
        is UiState.Loading -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(8.dp))
                Text("Syncing Hisab-Kitab...", style = MaterialTheme.typography.bodyMedium)
            }
        }
        is UiState.Error -> {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(state.message, color = MaterialTheme.colorScheme.error)
            }
        }
        is UiState.Success -> {
            if (customers.isEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("No customers yet", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Tap + to create a customer manually or import directly from your Contacts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 84.dp)
                ) {
                    items(customers, key = { it.id }) { cust ->
                        val isSelected = cust.id == selectedId
                        val balColor = when {
                            cust.balancePaise > 0L -> glass.udhaarAccent
                            cust.balancePaise < 0L -> glass.jamaAccent
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("customer_card_${cust.id}"),
                            elevated = isSelected,
                            onClick = { onSelectCustomer(cust) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(cust.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = cust.phone.ifBlank { "No phone saved" },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = LedgerRepository.formatCurrencyPaise(kotlin.math.abs(cust.balancePaise)),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontFamily = JetBrainsMonoFamily,
                                        color = balColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = when {
                                            cust.balancePaise > 0L -> "Udhaar Due"
                                            cust.balancePaise < 0L -> "Advance Jama"
                                            else -> "Settled"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = balColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerDetailPane(
    customer: Customer,
    transactions: List<LedgerTransaction>,
    businessName: String,
    ownerName: String,
    preferDirectSms: Boolean,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onAddTx: (TransactionType) -> Unit,
    onAuditLog: (AuditAction, String) -> Unit,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val glass = LocalGlassPalette.current
    var showReminderDialog by remember { mutableStateOf(false) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showReminderDialog = true
        } else {
            onMessage("Notification permission denied. Enable it in Settings -> Permission Manager.")
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBackButton) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to customers")
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(customer.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = customer.phone.ifBlank { "No phone number" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = {
                    val file = ReportExporter.createCacheExportFile(
                        context,
                        "statement_${customer.name.replace(" ", "_")}.pdf"
                    )
                    file.outputStream().use { out ->
                        ReportExporter.writePdfReport(
                            outputStream = out,
                            businessName = businessName,
                            ownerName = ownerName,
                            customers = listOf(customer),
                            transactions = transactions,
                            titleOverride = "${customer.name} — Ledger Statement"
                        )
                    }
                    ReportExporter.shareFile(context, file, "application/pdf", "Share PDF Statement")
                    onAuditLog(AuditAction.EXPORT_PDF, "Exported PDF statement for ${customer.name}")
                }
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF Statement")
            }
            IconButton(
                onClick = {
                    val file = ReportExporter.createCacheExportFile(
                        context,
                        "statement_${customer.name.replace(" ", "_")}.xlsx"
                    )
                    file.outputStream().use { out ->
                        ReportExporter.writeExcelXlsxReport(
                            outputStream = out,
                            businessName = businessName,
                            customers = listOf(customer),
                            transactions = transactions
                        )
                    }
                    ReportExporter.shareFile(
                        context,
                        file,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "Share Excel Statement"
                    )
                    onAuditLog(AuditAction.EXPORT_XLSX, "Exported Excel statement for ${customer.name}")
                }
            ) {
                Icon(Icons.Default.TableChart, contentDescription = "Export Excel Statement")
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("NET CUSTOMER BALANCE", style = MaterialTheme.typography.labelSmall)
                    val balColor = when {
                        customer.balancePaise > 0L -> glass.udhaarAccent
                        customer.balancePaise < 0L -> glass.jamaAccent
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Text(
                        text = LedgerRepository.formatCurrencyPaise(kotlin.math.abs(customer.balancePaise)),
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = JetBrainsMonoFamily,
                        color = balColor
                    )
                    Text(
                        text = when {
                            customer.balancePaise > 0L -> "Customer owes you (Udhaar)"
                            customer.balancePaise < 0L -> "Advance paid by customer (Jama)"
                            else -> "Account is settled"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Udhaar: ${LedgerRepository.formatCurrencyPaise(customer.totalUdhaarPaise)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = glass.udhaarAccent
                    )
                    Text(
                        "Jama: ${LedgerRepository.formatCurrencyPaise(customer.totalJamaPaise)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = glass.jamaAccent
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onAddTx(TransactionType.UDHAAR) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("add_udhaar_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = glass.udhaarAccent,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Give Udhaar")
            }
            Button(
                onClick = { onAddTx(TransactionType.JAMA) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("add_jama_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = glass.jamaAccent,
                    contentColor = Color.Black
                )
            ) {
                Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Got Jama")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val body = SmsReceiptHelper.formatCustomerReminderBody(businessName, customer)
                    val res = SmsReceiptHelper.sendSms(
                        context = context,
                        phone = customer.phone,
                        body = body,
                        preferDirectSms = preferDirectSms
                    )
                    onMessage(res.message)
                    if (res.dispatched) {
                        onAuditLog(AuditAction.SMS_RECEIPT, "Sent balance SMS to ${customer.name}")
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("SMS Reminder")
            }

            OutlinedButton(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !PermissionManager.isGranted(context, Manifest.permission.POST_NOTIFICATIONS)
                    ) {
                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        showReminderDialog = true
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("Alert Me")
            }
        }

        Text("Permanent Transaction Timeline", style = MaterialTheme.typography.titleMedium)

        if (transactions.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No transactions yet for ${customer.name}. Permanent ledger entries appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(transactions, key = { it.id }) { tx ->
                    val isUdhaar = tx.type == TransactionType.UDHAAR.name
                    val accent = if (isUdhaar) glass.udhaarAccent else glass.jamaAccent
                    val dateStr = if (tx.createdAtEpochMs > 0L) {
                        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                            .format(Date(tx.createdAtEpochMs))
                    } else "Saved"

                    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isUdhaar) "Udhaar Given" else "Jama Received",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = accent
                                )
                                if (tx.note.isNotBlank() || tx.billReference.isNotBlank()) {
                                    Text(
                                        text = listOf(tx.billReference, tx.note).filter { it.isNotBlank() }.joinToString(" • "),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Text(
                                    text = "$dateStr • SMS: ${tx.smsMethod}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = (if (isUdhaar) "+ " else "- ") +
                                    LedgerRepository.formatCurrencyPaise(tx.amountPaise),
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = JetBrainsMonoFamily,
                                color = accent
                            )
                        }
                    }
                }
            }
        }
    }

    if (showReminderDialog) {
        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            title = { Text("Schedule Payment Alert") },
            text = { Text("Choose when to receive a local reminder notification for ${customer.name}.") },
            confirmButton = {
                Button(onClick = {
                    val amt = LedgerRepository.formatCurrencyPaise(kotlin.math.abs(customer.balancePaise))
                    val scheduled = ReminderScheduler.scheduleReminder(
                        context = context,
                        customerName = customer.name,
                        amountText = "Balance: $amt",
                        note = "Follow up for Hisab-Kitab payment",
                        delayMinutes = 0L
                    )
                    onMessage(if (scheduled) "Reminder alert sent!" else "Enable notifications in Settings")
                    showReminderDialog = false
                }) {
                    Text("Notify Now")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    val amt = LedgerRepository.formatCurrencyPaise(kotlin.math.abs(customer.balancePaise))
                    val scheduled = ReminderScheduler.scheduleReminder(
                        context = context,
                        customerName = customer.name,
                        amountText = "Balance: $amt",
                        note = "Scheduled follow-up",
                        delayMinutes = 60L
                    )
                    onMessage(if (scheduled) "Reminder scheduled in 1 hour" else "Enable notifications in Settings")
                    showReminderDialog = false
                }) {
                    Text("In 1 Hour")
                }
            }
        )
    }
}

@Composable
private fun AddCustomerModal(
    contactsEnabled: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showContactsSheet by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Hisab-Kitab Customer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (contactsEnabled) {
                    OutlinedButton(
                        onClick = { showContactsSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pick_from_contacts_btn")
                    ) {
                        Icon(Icons.Default.Contacts, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Pick from Device Contacts")
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile Number (for SMS)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Address (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSave(name, phone, notes) },
                enabled = name.isNotBlank()
            ) {
                Text("Save Customer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showContactsSheet) {
        ContactsPickerSheet(
            onDismiss = { showContactsSheet = false },
            onSelectContact = { contact ->
                name = contact.name
                phone = contact.phone
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordTransactionModal(
    customer: Customer,
    type: TransactionType,
    businessName: String,
    preferDirectSms: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String, Boolean, SmsDispatchMethod) -> Unit
) {
    val context = LocalContext.current
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var billRef by remember { mutableStateOf("") }
    var sendSms by remember { mutableStateOf(customer.phone.isNotBlank()) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "${type.label} — ${customer.name}",
                style = MaterialTheme.typography.titleLarge
            )
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount (₹) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            OutlinedTextField(
                value = billRef,
                onValueChange = { billRef = it },
                label = { Text("Bill / Invoice Ref (Optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Description / Items (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Send SMS Receipt", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = if (customer.phone.isBlank()) "Add customer phone to enable SMS"
                        else if (preferDirectSms) "Direct SMS (if granted) or SMS Composer"
                        else "Official Android SMS Composer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = sendSms && customer.phone.isNotBlank(),
                    enabled = customer.phone.isNotBlank(),
                    onCheckedChange = { sendSms = it }
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val rupees = amountText.toDoubleOrNull() ?: 0.0
                        if (rupees > 0.0) {
                            val paise = kotlin.math.round(rupees * 100.0).toLong()
                            val nextBalance = if (type == TransactionType.UDHAAR) {
                                customer.balancePaise + paise
                            } else {
                                customer.balancePaise - paise
                            }
                            var sent = false
                            var method = SmsDispatchMethod.NONE
                            if (sendSms && customer.phone.isNotBlank()) {
                                val body = SmsReceiptHelper.formatReceiptBody(
                                    businessName = businessName,
                                    customerName = customer.name,
                                    type = type,
                                    amountPaise = paise,
                                    newBalancePaise = nextBalance,
                                    note = note,
                                    billRef = billRef
                                )
                                val smsRes = SmsReceiptHelper.sendSms(
                                    context = context,
                                    phone = customer.phone,
                                    body = body,
                                    preferDirectSms = preferDirectSms
                                )
                                sent = smsRes.dispatched
                                method = smsRes.method
                            }
                            onConfirm(rupees, note, billRef, sent, method)
                        }
                    },
                    enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Record Entry")
                }
            }
        }
    }
}
