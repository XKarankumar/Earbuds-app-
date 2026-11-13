package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.LedgerRepository
import com.example.model.CalcHistoryEntry
import com.example.model.Customer
import com.example.model.SmsDispatchMethod
import com.example.model.TransactionType
import com.example.sms.SmsReceiptHelper
import com.example.ui.components.BillScannerSheet
import com.example.ui.components.GlassCard
import com.example.ui.components.SpringCalculatorKey
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.UiState

@Composable
fun CalculatorScreen(
    viewModel: MainViewModel,
    onNavigateToLedger: () -> Unit
) {
    val calcState by viewModel.calcState.collectAsStateWithLifecycle()
    val history by viewModel.calcHistory.collectAsStateWithLifecycle()
    val customersState by viewModel.customersState.collectAsStateWithLifecycle()
    val prefs by viewModel.localPrefs.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()

    var showHistorySheet by remember { mutableStateOf(false) }
    var showLogToKhataSheet by remember { mutableStateOf(false) }
    var showScannerSheet by remember { mutableStateOf(false) }
    var showGstQuickMenu by remember { mutableStateOf(false) }

    val customers = (customersState as? UiState.Success)?.data.orEmpty()
    val businessName = profile?.businessName ?: "Hardik Kumar Enterprises"

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        val isWideTablet = maxWidth >= 700.dp

        if (isWideTablet) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CalculatorDisplayCard(
                        expression = calcState.expression,
                        previewResult = calcState.previewResult,
                        memoryValue = calcState.memoryValue,
                        hasError = calcState.hasError
                    )
                    QuickCalculatorActionStrip(
                        onLogToKhata = { showLogToKhataSheet = true },
                        onScanBill = { showScannerSheet = true },
                        onHistory = { showHistorySheet = true },
                        showGstMenu = showGstQuickMenu,
                        onToggleGstMenu = { showGstQuickMenu = it },
                        onApplyGst = { rate ->
                            val currentVal = calcState.previewResult.toDoubleOrNull() ?: 0.0
                            val withGst = currentVal * (1.0 + rate / 100.0)
                            viewModel.loadValueIntoCalculator(
                                com.example.model.CalculatorEvaluator.formatNumber(withGst)
                            )
                            viewModel.emitMessage("Added ${rate.toInt()}% GST")
                        }
                    )
                    CalculatorKeypad(
                        modifier = Modifier.weight(1f),
                        onKey = viewModel::onCalcKey
                    )
                }

                GlassCard(
                    modifier = Modifier
                        .weight(0.8f)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = "Calculation Tape",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    CalculationTapeList(
                        history = history,
                        onSelectEntry = { viewModel.loadValueIntoCalculator(it.result) },
                        onDeleteEntry = viewModel::deleteCalcHistoryEntry
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CalculatorDisplayCard(
                    expression = calcState.expression,
                    previewResult = calcState.previewResult,
                    memoryValue = calcState.memoryValue,
                    hasError = calcState.hasError
                )
                QuickCalculatorActionStrip(
                    onLogToKhata = { showLogToKhataSheet = true },
                    onScanBill = { showScannerSheet = true },
                    onHistory = { showHistorySheet = true },
                    showGstMenu = showGstQuickMenu,
                    onToggleGstMenu = { showGstQuickMenu = it },
                    onApplyGst = { rate ->
                        val currentVal = calcState.previewResult.toDoubleOrNull() ?: 0.0
                        val withGst = currentVal * (1.0 + rate / 100.0)
                        viewModel.loadValueIntoCalculator(
                            com.example.model.CalculatorEvaluator.formatNumber(withGst)
                        )
                        viewModel.emitMessage("Added ${rate.toInt()}% GST")
                    }
                )
                CalculatorKeypad(
                    modifier = Modifier.weight(1f),
                    onKey = viewModel::onCalcKey
                )
            }
        }
    }

    if (showHistorySheet) {
        @OptIn(ExperimentalMaterial3Api::class)
        ModalBottomSheet(onDismissRequest = { showHistorySheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
            ) {
                Text("Calculation Tape", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                CalculationTapeList(
                    history = history,
                    onSelectEntry = {
                        viewModel.loadValueIntoCalculator(it.result)
                        showHistorySheet = false
                    },
                    onDeleteEntry = viewModel::deleteCalcHistoryEntry
                )
            }
        }
    }

    if (showScannerSheet) {
        BillScannerSheet(
            cameraFeatureEnabled = prefs.cameraScanFeatureEnabled,
            onDismiss = { showScannerSheet = false },
            onResultExtracted = { info ->
                info.extractedAmount?.let { amt ->
                    viewModel.loadValueIntoCalculator(amt)
                    viewModel.emitMessage("Loaded ₹$amt from scanner")
                }
            }
        )
    }

    if (showLogToKhataSheet) {
        QuickLogToKhataSheet(
            initialAmount = calcState.previewResult.toDoubleOrNull()?.takeIf { it > 0 }?.toString() ?: "",
            customers = customers,
            businessName = businessName,
            preferDirectSms = prefs.directSmsFeatureEnabled,
            onDismiss = { showLogToKhataSheet = false },
            onCreateCustomerFirst = {
                showLogToKhataSheet = false
                onNavigateToLedger()
            },
            viewModel = viewModel
        )
    }
}

@Composable
private fun CalculatorDisplayCard(
    expression: String,
    previewResult: String,
    memoryValue: Double,
    hasError: Boolean
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevated = true,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (memoryValue != 0.0) "MEM: ${com.example.model.CalculatorEvaluator.formatNumber(memoryValue)}" else "PRO CALCULATOR",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "₹ $previewResult",
                style = MaterialTheme.typography.labelMedium,
                color = if (hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = expression,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("calc_expression_text"),
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun QuickCalculatorActionStrip(
    onLogToKhata: () -> Unit,
    onScanBill: () -> Unit,
    onHistory: () -> Unit,
    showGstMenu: Boolean,
    onToggleGstMenu: (Boolean) -> Unit,
    onApplyGst: (Double) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AssistChip(
            onClick = onLogToKhata,
            modifier = Modifier.testTag("calc_log_khata_chip"),
            label = { Text("Log Udhaar / Jama + SMS") },
            leadingIcon = {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        )
        BoxWithConstraints {
            AssistChip(
                onClick = { onToggleGstMenu(true) },
                label = { Text("+GST") },
                leadingIcon = {
                    Icon(Icons.Default.Percent, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            )
            DropdownMenu(
                expanded = showGstMenu,
                onDismissRequest = { onToggleGstMenu(false) }
            ) {
                listOf(5.0, 12.0, 18.0, 28.0).forEach { rate ->
                    DropdownMenuItem(
                        text = { Text("Add ${rate.toInt()}% GST") },
                        onClick = {
                            onToggleGstMenu(false)
                            onApplyGst(rate)
                        }
                    )
                }
            }
        }
        AssistChip(
            onClick = onScanBill,
            label = { Text("Scan Bill / QR") },
            leadingIcon = {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        )
        AssistChip(
            onClick = onHistory,
            label = { Text("Tape") },
            leadingIcon = {
                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        )
    }
}

@Composable
private fun CalculatorKeypad(
    modifier: Modifier = Modifier,
    onKey: (String) -> Unit
) {
    val rows = listOf(
        listOf("MC", "MR", "M+", "M-"),
        listOf("AC", "(", ")", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "−"),
        listOf("1", "2", "3", "+"),
        listOf("±", "0", ".", "="),
        listOf("√", "x²", "%", "DEL")
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    val isPrimary = key == "="
                    val isOperator = key in setOf("÷", "×", "−", "+", "%")
                    val isUtility = key in setOf("AC", "DEL", "MC", "MR", "M+", "M-", "(", ")", "√", "x²", "±")

                    val bg = when {
                        isPrimary -> MaterialTheme.colorScheme.primary
                        isOperator -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f)
                        isUtility -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                    }
                    val fg = when {
                        isPrimary -> MaterialTheme.colorScheme.onPrimary
                        isOperator -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    SpringCalculatorKey(
                        label = key,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("calc_key_$key"),
                        containerColor = bg,
                        contentColor = fg,
                        onClick = { onKey(key) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CalculationTapeList(
    history: List<CalcHistoryEntry>,
    onSelectEntry: (CalcHistoryEntry) -> Unit,
    onDeleteEntry: (String) -> Unit
) {
    if (history.isEmpty()) {
        Text(
            text = "Evaluate expressions with '=' to build your synced calculation tape.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp)
        )
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(history, key = { it.id }) { entry ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onSelectEntry(entry) },
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.expression,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "= ${entry.result}",
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = JetBrainsMonoFamily,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { onDeleteEntry(entry.id) }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete history entry"
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickLogToKhataSheet(
    initialAmount: String,
    customers: List<Customer>,
    businessName: String,
    preferDirectSms: Boolean,
    onDismiss: () -> Unit,
    onCreateCustomerFirst: () -> Unit,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var selectedCustomer by remember(customers) { mutableStateOf(customers.firstOrNull()) }
    var txType by remember { mutableStateOf(TransactionType.UDHAAR) }
    var amountText by remember { mutableStateOf(initialAmount) }
    var note by remember { mutableStateOf("") }
    var billRef by remember { mutableStateOf("") }
    var sendSms by remember { mutableStateOf(true) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Log Calculator Amount to Hisab-Kitab", style = MaterialTheme.typography.titleLarge)

            if (customers.isEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Add a customer in Hisab-Kitab first to log Udhaar or Jama entries.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = onCreateCustomerFirst, modifier = Modifier.fillMaxWidth()) {
                        Text("Go to Hisab-Kitab")
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    customers.forEach { customer ->
                        FilterChip(
                            selected = selectedCustomer?.id == customer.id,
                            onClick = { selectedCustomer = customer },
                            label = { Text(customer.name) }
                        )
                    }
                }

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    TransactionType.entries.forEachIndexed { idx, type ->
                        SegmentedButton(
                            selected = txType == type,
                            onClick = { txType = type },
                            shape = SegmentedButtonDefaults.itemShape(idx, TransactionType.entries.size)
                        ) {
                            Text(type.label)
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = billRef,
                    onValueChange = { billRef = it },
                    label = { Text("Bill / Invoice Reference (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Item / Payment Note (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Sms, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Send SMS Payment Receipt", style = MaterialTheme.typography.titleSmall)
                    }
                    Switch(checked = sendSms, onCheckedChange = { sendSms = it })
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val cust = selectedCustomer ?: return@Button
                            val rupees = amountText.toDoubleOrNull() ?: 0.0
                            if (rupees <= 0.0) {
                                viewModel.emitMessage("Enter a valid positive amount")
                                return@Button
                            }
                            val paise = kotlin.math.round(rupees * 100.0).toLong()
                            val newBal = if (txType == TransactionType.UDHAAR) {
                                cust.balancePaise + paise
                            } else {
                                cust.balancePaise - paise
                            }
                            var smsMethod = SmsDispatchMethod.NONE
                            var dispatched = false
                            if (sendSms && cust.phone.isNotBlank()) {
                                val body = SmsReceiptHelper.formatReceiptBody(
                                    businessName = businessName,
                                    customerName = cust.name,
                                    type = txType,
                                    amountPaise = paise,
                                    newBalancePaise = newBal,
                                    note = note,
                                    billRef = billRef
                                )
                                val smsRes = SmsReceiptHelper.sendSms(
                                    context = context,
                                    phone = cust.phone,
                                    body = body,
                                    preferDirectSms = preferDirectSms
                                )
                                dispatched = smsRes.dispatched
                                smsMethod = smsRes.method
                            }
                            viewModel.addTransaction(
                                customer = cust,
                                type = txType,
                                amountRupees = rupees,
                                note = note,
                                billRef = billRef,
                                smsSent = dispatched,
                                smsMethod = smsMethod
                            )
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Entry")
                    }
                }
            }
        }
    }
}
