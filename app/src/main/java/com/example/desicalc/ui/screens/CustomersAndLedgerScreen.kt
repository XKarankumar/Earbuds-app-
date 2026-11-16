package com.example.desicalc.ui.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.desicalc.data.local.TransactionEntity
import com.example.desicalc.data.repository.DesiLedgerRepository
import com.example.desicalc.model.AppLanguage
import com.example.desicalc.model.CustomerFilter
import com.example.desicalc.model.CustomerLedgerSummary
import com.example.desicalc.model.SmsTemplateType
import com.example.desicalc.model.TransactionType
import com.example.desicalc.model.formatInr
import com.example.desicalc.ui.components.CustomerAvatar
import com.example.desicalc.ui.components.DesiCard
import com.example.desicalc.ui.strings.DesiStrings
import com.example.desicalc.ui.theme.LocalDesiColors
import com.example.desicalc.ui.theme.MonoFontFamily
import kotlin.math.abs

@Composable
fun CustomersAndLedgerScreen(
    customers: List<CustomerLedgerSummary>,
    selectedCustomer: CustomerLedgerSummary?,
    selectedCustomerTransactions: List<TransactionEntity>,
    searchQuery: String,
    activeFilter: CustomerFilter,
    language: AppLanguage,
    strings: DesiStrings,
    hasSmsPermission: Boolean,
    buildSmsPreview: (CustomerLedgerSummary, SmsTemplateType, Double) -> String,
    onSearchChange: (String) -> Unit,
    onFilterChange: (CustomerFilter) -> Unit,
    onSelectCustomer: (Long?) -> Unit,
    onAddCustomer: (String, String, String, String, Uri?, Boolean, Boolean, Int) -> Unit,
    onUpdateCustomer: (CustomerLedgerSummary, String, String, String, String, Uri?, Boolean, Boolean, Int) -> Unit,
    onDeleteCustomer: (Long, Boolean) -> Unit,
    onAddTransaction: (Long, TransactionType, Double, String, Boolean) -> Unit,
    onUpdateTransaction: (TransactionEntity, TransactionType, Double, String) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onOpenManualSms: (CustomerLedgerSummary, String, SmsTemplateType, String) -> Unit,
    onSendDirectSms: (CustomerLedgerSummary, String, SmsTemplateType, String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomerDialog by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<CustomerLedgerSummary?>(null) }

    if (selectedCustomer != null) {
        BackHandler { onSelectCustomer(null) }
        CustomerLedgerDetailView(
            customer = selectedCustomer,
            transactions = selectedCustomerTransactions,
            strings = strings,
            hasSmsPermission = hasSmsPermission,
            buildSmsPreview = buildSmsPreview,
            onBack = { onSelectCustomer(null) },
            onEditCustomer = {
                editingCustomer = selectedCustomer
                showCustomerDialog = true
            },
            onDeleteCustomer = { deleteTx -> onDeleteCustomer(selectedCustomer.id, deleteTx) },
            onAddTransaction = { type, amount, desc, partial ->
                onAddTransaction(selectedCustomer.id, type, amount, desc, partial)
            },
            onUpdateTransaction = onUpdateTransaction,
            onDeleteTransaction = onDeleteTransaction,
            onOpenManualSms = onOpenManualSms,
            onSendDirectSms = onSendDirectSms,
            modifier = modifier
        )
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        editingCustomer = null
                        showCustomerDialog = true
                    },
                    modifier = Modifier.testTag("fab_add_customer")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = strings.addCustomer)
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("customers_list_screen"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        placeholder = { Text(strings.searchCustomers) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_search_input")
                    )
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(CustomerFilter.entries) { filter ->
                            FilterChip(
                                selected = activeFilter == filter,
                                onClick = { onFilterChange(filter) },
                                label = {
                                    Text(if (language == AppLanguage.HINDI) filter.labelHi else filter.labelEn)
                                }
                            )
                        }
                    }
                }
                if (customers.isEmpty()) {
                    item {
                        DesiCard {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("No customers found", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Tap '+' to add your first customer or import legacy LocalStorage data from Profile -> Backup & Migration.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(customers, key = { it.id }) { cust ->
                        CustomerRowCard(
                            customer = cust,
                            onClick = { onSelectCustomer(cust.id) }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }

    if (showCustomerDialog) {
        CustomerFormDialog(
            existing = editingCustomer,
            onDismiss = { showCustomerDialog = false },
            onSave = { name, mobile, address, notes, photoUri, autoSms, consent, intervalDays ->
                val target = editingCustomer
                if (target == null) {
                    onAddCustomer(name, mobile, address, notes, photoUri, autoSms, consent, intervalDays)
                } else {
                    onUpdateCustomer(target, name, mobile, address, notes, photoUri, autoSms, consent, intervalDays)
                }
                showCustomerDialog = false
            }
        )
    }
}

@Composable
private fun CustomerRowCard(
    customer: CustomerLedgerSummary,
    onClick: () -> Unit
) {
    val ext = LocalDesiColors.current
    val balanceColor = when {
        customer.netBalance > 0.01 -> ext.udharRed
        customer.netBalance < -0.01 -> ext.jamaGreen
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusLabel = when {
        customer.netBalance > 0.01 -> "Lena Hai"
        customer.netBalance < -0.01 -> "Dena Hai"
        else -> "Settled"
    }

    DesiCard(
        onClick = onClick,
        modifier = Modifier.testTag("customer_card_${customer.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CustomerAvatar(name = customer.name, photoPath = customer.photoPath)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${customer.customerCode}${if (customer.mobile.isNotBlank()) " • ${customer.mobile}" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${customer.transactionCount} entries",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatInr(abs(customer.netBalance)),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = balanceColor
                )
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = balanceColor
                )
            }
        }
    }
}

@Composable
private fun CustomerLedgerDetailView(
    customer: CustomerLedgerSummary,
    transactions: List<TransactionEntity>,
    strings: DesiStrings,
    hasSmsPermission: Boolean,
    buildSmsPreview: (CustomerLedgerSummary, SmsTemplateType, Double) -> String,
    onBack: () -> Unit,
    onEditCustomer: () -> Unit,
    onDeleteCustomer: (Boolean) -> Unit,
    onAddTransaction: (TransactionType, Double, String, Boolean) -> Unit,
    onUpdateTransaction: (TransactionEntity, TransactionType, Double, String) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onOpenManualSms: (CustomerLedgerSummary, String, SmsTemplateType, String) -> Unit,
    onSendDirectSms: (CustomerLedgerSummary, String, SmsTemplateType, String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalDesiColors.current
    var activeTxTypeDialog by remember { mutableStateOf<TransactionType?>(null) }
    var editingTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showSmsDialog by remember { mutableStateOf(false) }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("customer_ledger_detail"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("ledger_back_btn")) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to customers")
                }
                CustomerAvatar(name = customer.name, photoPath = customer.photoPath, size = 48.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(customer.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "${customer.customerCode} • ${customer.mobile.ifBlank { "No phone" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEditCustomer) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit customer")
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete customer", tint = ext.udharRed)
                }
            }
        }

        // Balance Summary Card (strictly separated receivable & payable totals)
        item {
            DesiCard {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val netColor = when {
                        customer.netBalance > 0.01 -> ext.udharRed
                        customer.netBalance < -0.01 -> ext.jamaGreen
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Text(
                        text = when {
                            customer.netBalance > 0.01 -> "Net Receivable (Lena Hai): ${formatInr(customer.netBalance)}"
                            customer.netBalance < -0.01 -> "Net Payable (Dena Hai): ${formatInr(abs(customer.netBalance))}"
                            else -> "Account Settled (₹0)"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold),
                        color = netColor
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Udhar Diya: ${formatInr(customer.udharDiyaTotal)}", style = MaterialTheme.typography.bodyMedium)
                        Text("Jama Received: ${formatInr(customer.jamaReceivedTotal)}", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (customer.udharLiyaTotal > 0 || customer.paymentMadeTotal > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Udhar Liya: ${formatInr(customer.udharLiyaTotal)}", style = MaterialTheme.typography.bodyMedium)
                            Text("Payment Made: ${formatInr(customer.paymentMadeTotal)}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (customer.address.isNotBlank() || customer.notes.isNotBlank()) {
                        Text(
                            text = listOf(customer.address, customer.notes).filter { it.isNotBlank() }.joinToString(" • "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 4 Clear Ledger Entry Actions + SMS Reminder
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { activeTxTypeDialog = TransactionType.UDHAR_DIYA },
                        colors = ButtonDefaults.buttonColors(containerColor = ext.udharRed, contentColor = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_udhar_diya")
                    ) {
                        Text(strings.udharDiya, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Button(
                        onClick = { activeTxTypeDialog = TransactionType.JAMA_RECEIVED },
                        colors = ButtonDefaults.buttonColors(containerColor = ext.jamaGreen, contentColor = Color.Black),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_jama_received")
                    ) {
                        Text(strings.jamaReceived, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { activeTxTypeDialog = TransactionType.UDHAR_LIYA },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(strings.udharLiya, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = { activeTxTypeDialog = TransactionType.PAYMENT_MADE },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(strings.paymentMade, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                OutlinedButton(
                    onClick = { showSmsDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_customer_sms_reminder")
                ) {
                    Icon(Icons.Rounded.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.sendReminder)
                }
            }
        }

        item {
            Text("Ledger Entries (${transactions.size})", style = MaterialTheme.typography.titleMedium)
        }

        if (transactions.isEmpty()) {
            item {
                DesiCard {
                    Text(
                        "No entries yet for ${customer.name}. Use Udhar Diya / Jama Received buttons above.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(transactions, key = { it.id }) { tx ->
                val type = TransactionType.fromCode(tx.type)
                val color = if (type.isCreditIncrease) ext.udharRed else ext.jamaGreen
                DesiCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${type.labelEn}${if (tx.isPartialPayment) " (Partial)" else ""}",
                                style = MaterialTheme.typography.titleMedium,
                                color = color
                            )
                            Text(
                                text = DesiLedgerRepository.formatTimestamp(tx.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (tx.description.isNotBlank()) {
                                Text(tx.description, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Text(
                            text = formatInr(tx.amount),
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold),
                            color = color
                        )
                        IconButton(onClick = { editingTx = tx }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit transaction")
                        }
                        IconButton(onClick = { onDeleteTransaction(tx.id) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete transaction", tint = ext.udharRed)
                        }
                    }
                }
            }
        }
    }

    activeTxTypeDialog?.let { txType ->
        TransactionEntryDialog(
            initialType = txType,
            initialAmount = "",
            initialDescription = "",
            isEdit = false,
            onDismiss = { activeTxTypeDialog = null },
            onConfirm = { type, amount, desc, partial ->
                onAddTransaction(type, amount, desc, partial)
                activeTxTypeDialog = null
            }
        )
    }

    editingTx?.let { tx ->
        TransactionEntryDialog(
            initialType = TransactionType.fromCode(tx.type),
            initialAmount = tx.amount.toString(),
            initialDescription = tx.description,
            isEdit = true,
            onDismiss = { editingTx = null },
            onConfirm = { type, amount, desc, _ ->
                onUpdateTransaction(tx, type, amount, desc)
                editingTx = null
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Customer?") },
            text = {
                Text("Deleting ${customer.name} will also delete ${customer.transactionCount} ledger entries. Consider exporting a JSON backup first.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCustomer(true)
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ext.udharRed)
                ) { Text("Delete Permanently") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showSmsDialog) {
        var selectedTemplate by remember { mutableStateOf(SmsTemplateType.REMINDER) }
        var messageText by remember {
            mutableStateOf(buildSmsPreview(customer, SmsTemplateType.REMINDER, abs(customer.netBalance)))
        }
        var consentChecked by remember { mutableStateOf(customer.smsConsentGranted) }

        AlertDialog(
            onDismissRequest = { showSmsDialog = false },
            title = { Text("SMS to ${customer.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(
                            listOf(
                                SmsTemplateType.REMINDER,
                                SmsTemplateType.UDHAR_CONFIRM,
                                SmsTemplateType.JAMA_CONFIRM,
                                SmsTemplateType.BALANCE_SUMMARY
                            )
                        ) { tpl ->
                            FilterChip(
                                selected = selectedTemplate == tpl,
                                onClick = {
                                    selectedTemplate = tpl
                                    messageText = buildSmsPreview(customer, tpl, abs(customer.netBalance))
                                },
                                label = { Text(tpl.titleEn) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        label = { Text("SMS Message") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = consentChecked, onCheckedChange = { consentChecked = it })
                        Text(
                            "I authorize sending this SMS directly",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            onOpenManualSms(customer, customer.mobile, selectedTemplate, messageText)
                            showSmsDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Open SMS Composer (Safe)") }
                    if (consentChecked) {
                        OutlinedButton(
                            onClick = {
                                if (hasSmsPermission) {
                                    onSendDirectSms(customer, customer.mobile, selectedTemplate, messageText, true)
                                    showSmsDialog = false
                                } else {
                                    smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (hasSmsPermission) "Send Direct SMS" else "Grant SEND_SMS Permission")
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showSmsDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun CustomerFormDialog(
    existing: CustomerLedgerSummary?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Uri?, Boolean, Boolean, Int) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var mobile by remember { mutableStateOf(existing?.mobile ?: "") }
    var address by remember { mutableStateOf(existing?.address ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var pickedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var autoSmsEnabled by remember { mutableStateOf(existing?.autoSmsEnabled ?: false) }
    var smsConsent by remember { mutableStateOf(existing?.smsConsentGranted ?: false) }
    var reminderDays by remember { mutableStateOf((existing?.reminderIntervalDays ?: 7).toString()) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> pickedPhotoUri = uri }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add New Customer" else "Edit Customer") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Customer Name *") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_customer_name")
                    )
                }
                item {
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        label = { Text("Mobile Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_customer_mobile")
                    )
                }
                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address / Shop") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (pickedPhotoUri != null) "Photo Selected ✓" else "Pick Profile Photo (Photo Picker)")
                    }
                }
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Scheduled SMS Reminders", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = autoSmsEnabled, onCheckedChange = { autoSmsEnabled = it })
                    }
                }
                if (autoSmsEnabled) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = smsConsent, onCheckedChange = { smsConsent = it })
                            Text("Customer & owner consent granted for auto-reminders", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = reminderDays,
                            onValueChange = { reminderDays = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Reminder Interval (Days)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            name,
                            mobile,
                            address,
                            notes,
                            pickedPhotoUri,
                            autoSmsEnabled,
                            smsConsent,
                            reminderDays.toIntOrNull()?.coerceAtLeast(1) ?: 7
                        )
                    }
                },
                modifier = Modifier.testTag("dialog_save_customer_btn")
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun TransactionEntryDialog(
    initialType: TransactionType,
    initialAmount: String,
    initialDescription: String,
    isEdit: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (TransactionType, Double, String, Boolean) -> Unit
) {
    var selectedType by remember { mutableStateOf(initialType) }
    var amountStr by remember { mutableStateOf(initialAmount) }
    var desc by remember { mutableStateOf(initialDescription) }
    var isPartial by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit Transaction" else selectedType.labelEn) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(TransactionType.entries) { t ->
                        FilterChip(
                            selected = selectedType == t,
                            onClick = { selectedType = t },
                            label = { Text(t.labelEn) }
                        )
                    }
                }
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_amount_input")
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description / Item Note") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_desc_input")
                )
                if (!isEdit && (selectedType == TransactionType.JAMA_RECEIVED || selectedType == TransactionType.PAYMENT_MADE)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isPartial, onCheckedChange = { isPartial = it })
                        Text("Mark as Partial Payment", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0) {
                        onConfirm(selectedType, amt, desc, isPartial)
                    }
                },
                modifier = Modifier.testTag("tx_confirm_save_btn")
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
