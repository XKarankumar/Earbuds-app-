package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.CalculatorEngine
import com.example.model.TransactionType
import com.example.ui.components.OceanGlassCard
import com.example.ui.components.OceanPillBadge
import com.example.ui.components.OceanTopBar
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalOceanPalette
import com.example.ui.theme.OceanEmeraldColors
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CustomerProfileScreen(
    customerId: Long,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val customers by viewModel.customerSummaries.collectAsStateWithLifecycle()
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val transactions by viewModel.observeCustomerTransactions(customerId)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val palette = LocalOceanPalette.current

    val customer = customers.firstOrNull { it.id == customerId }
    var activeEntryType by remember { mutableStateOf<TransactionType?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

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
                    title = customer?.name ?: "Customer Profile",
                    subtitle = customer?.phone?.ifBlank { "No phone number" } ?: "Profile",
                    onBack = onBack,
                    backTestTag = "btn_back_customer_profile",
                    trailing = {
                        if (customer != null) {
                            IconButton(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete customer",
                                    tint = palette.dangerAccent
                                )
                            }
                        }
                    }
                )
            }

            if (customer == null) {
                item {
                    OceanGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Customer not found or removed.", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onBack) { Text("Back") }
                    }
                }
            } else {
                item {
                    val isDue = customer.netBalance > 0.01
                    val balColor = if (isDue) palette.udhaarAccent else palette.jamaAccent
                    OceanGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevated = true
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isDue) "Net Udhaar Due" else "Net Balance / Advance",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CalculatorEngine.formatCurrency(
                                        kotlin.math.abs(customer.netBalance),
                                        prefs.currency.symbol
                                    ),
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontFamily = JetBrainsMonoFamily
                                    ),
                                    color = balColor
                                )
                            }
                            OceanPillBadge(
                                text = "${customer.transactionCount} tx",
                                accent = OceanEmeraldColors.MintGreen
                            )
                        }

                        if (customer.notes.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = customer.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.dialPhone(context, customer.phone) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = stringResource(R.string.cd_call_customer), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Call", maxLines = 1)
                            }
                            Button(
                                onClick = { viewModel.prepareSmsForCustomer(customer) },
                                colors = ButtonDefaults.buttonColors(containerColor = OceanEmeraldColors.OceanBlue),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_profile_sms")
                            ) {
                                Icon(Icons.Default.Sms, contentDescription = stringResource(R.string.cd_send_sms), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("SMS", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { viewModel.exportAndSharePdfReport(context) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = stringResource(R.string.cd_export_pdf), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("PDF", maxLines = 1)
                            }
                        }
                    }
                }

                // Jama & Udhaar Breakdown + Action Buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { activeEntryType = TransactionType.JAMA },
                            colors = ButtonDefaults.buttonColors(containerColor = OceanEmeraldColors.EmeraldGreen),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("btn_add_jama")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Jama (+)")
                        }
                        Button(
                            onClick = { activeEntryType = TransactionType.UDHAAR },
                            colors = ButtonDefaults.buttonColors(containerColor = OceanEmeraldColors.OceanBlue),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("btn_add_udhaar")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Udhaar (-)")
                        }
                    }
                }

                item {
                    Text(
                        text = "Transaction History",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (transactions.isEmpty()) {
                    item {
                        OceanGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "No transactions recorded for ${customer.name} yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    val fmt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                    items(transactions, key = { it.id }) { tx ->
                        val isJama = TransactionType.fromKey(tx.type) == TransactionType.JAMA
                        val txColor = if (isJama) palette.jamaAccent else palette.udhaarAccent
                        OceanGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isJama) "Jama Received" else "Udhaar Given",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = txColor
                                    )
                                    if (tx.note.isNotBlank()) {
                                        Text(
                                            text = tx.note,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (tx.expression.isNotBlank()) {
                                        Text(
                                            text = "Calc: ${tx.expression}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = fmt.format(Date(tx.createdAtMillis)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = (if (isJama) "+" else "-") +
                                            CalculatorEngine.formatCurrency(tx.amount, prefs.currency.symbol),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = txColor
                                    )
                                    IconButton(
                                        onClick = { viewModel.deleteLedgerTransaction(tx.id) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.cd_delete_item),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
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

    activeEntryType?.let { entryType ->
        var amountText by remember { mutableStateOf("") }
        var noteText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { activeEntryType = null },
            title = { Text("Add ${entryType.label}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount (${prefs.currency.symbol}) *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_tx_amount")
                    )
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Item / Note (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (entryType == TransactionType.JAMA) {
                            OceanEmeraldColors.EmeraldGreen
                        } else {
                            OceanEmeraldColors.OceanBlue
                        }
                    ),
                    modifier = Modifier.testTag("btn_confirm_tx"),
                    onClick = {
                        viewModel.addLedgerTransaction(
                            customerId = customerId,
                            type = entryType,
                            amount = amountText.toDoubleOrNull() ?: 0.0,
                            note = noteText
                        )
                        activeEntryType = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeEntryType = null }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteConfirm && customer != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove ${customer.name}?") },
            text = { Text("This deletes the customer and all their local ledger entries.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = palette.dangerAccent),
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteCustomer(customer.id)
                        onBack()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
