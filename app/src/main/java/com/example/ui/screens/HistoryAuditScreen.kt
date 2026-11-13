package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.LedgerRepository
import com.example.export.ReportExporter
import com.example.model.AuditLog
import com.example.model.AuditAction
import com.example.model.LedgerTransaction
import com.example.model.TransactionType
import com.example.ui.components.GlassCard
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryAuditScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val transactionsState by viewModel.transactionsState.collectAsStateWithLifecycle()
    val auditLogsState by viewModel.auditLogsState.collectAsStateWithLifecycle()
    val customersState by viewModel.customersState.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Transactions, 1 = Audit Logs

    val customers = (customersState as? UiState.Success)?.data.orEmpty()
    val transactions = (transactionsState as? UiState.Success)?.data.orEmpty()
    val auditLogs = (auditLogsState as? UiState.Success)?.data.orEmpty()
    val businessName = profile?.businessName ?: "Hardik Kumar Enterprises"
    val ownerName = profile?.displayName ?: "Hardik Kumar"

    // SAF CreateDocument launchers (zero storage permission required!)
    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                ReportExporter.writePdfReport(out, businessName, ownerName, customers, transactions)
            }
            viewModel.logAudit(AuditAction.EXPORT_PDF, "full_ledger", "Saved PDF ledger report")
            viewModel.emitMessage("PDF report saved")
        }
    }

    val saveCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                ReportExporter.writeCsvReport(out, transactions)
            }
            viewModel.logAudit(AuditAction.EXPORT_CSV, "full_ledger", "Saved CSV ledger report")
            viewModel.emitMessage("CSV report saved")
        }
    }

    val saveExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                ReportExporter.writeExcelXlsxReport(out, businessName, customers, transactions)
            }
            viewModel.logAudit(AuditAction.EXPORT_XLSX, "full_ledger", "Saved Excel (.xlsx) ledger report")
            viewModel.emitMessage("Excel (.xlsx) report saved")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Permanent History & Audit", style = MaterialTheme.typography.headlineSmall)

        GlassCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
            Text("Export Full Ledger Statement", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Uses Android Storage Access Framework — zero broad storage permission needed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { savePdfLauncher.launch("Hisab_Kitab_Statement.pdf") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_pdf_btn"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("PDF")
                }
                OutlinedButton(
                    onClick = { saveCsvLauncher.launch("Hisab_Kitab_Transactions.csv") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_csv_btn"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("CSV")
                }
                OutlinedButton(
                    onClick = { saveExcelLauncher.launch("Hisab_Kitab_Workbook.xlsx") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_excel_btn"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Excel")
                }
                IconButton(
                    onClick = {
                        val file = ReportExporter.createCacheExportFile(context, "Hisab_Kitab_Workbook.xlsx")
                        file.outputStream().use { out ->
                            ReportExporter.writeExcelXlsxReport(out, businessName, customers, transactions)
                        }
                        ReportExporter.shareFile(
                            context,
                            file,
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            "Share Excel Workbook"
                        )
                        viewModel.logAudit(AuditAction.EXPORT_XLSX, "share", "Shared Excel (.xlsx) workbook")
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share Excel workbook")
                }
            }
        }

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
                icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp)) }
            ) {
                Text("Transactions (${transactions.size})")
            }
            SegmentedButton(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
                icon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp)) }
            ) {
                Text("Audit Logs (${auditLogs.size})")
            }
        }

        if (selectedTab == 0) {
            TransactionsFeed(transactionsState = transactionsState)
        } else {
            AuditLogsFeed(auditLogsState = auditLogsState)
        }
    }
}

@Composable
private fun TransactionsFeed(transactionsState: UiState<List<LedgerTransaction>>) {
    val glass = LocalGlassPalette.current
    when (transactionsState) {
        is UiState.Loading -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        }
        is UiState.Error -> {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(transactionsState.message, color = MaterialTheme.colorScheme.error)
            }
        }
        is UiState.Success -> {
            val list = transactionsState.data
            if (list.isEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No permanent transactions recorded yet. All Udhaar and Jama entries are immutable once logged.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(list, key = { it.id }) { tx ->
                        val isUdhaar = tx.type == TransactionType.UDHAAR.name
                        val color = if (isUdhaar) glass.udhaarAccent else glass.jamaAccent
                        val dateStr = if (tx.createdAtEpochMs > 0L) {
                            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                                .format(Date(tx.createdAtEpochMs))
                        } else "Saved"

                        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tx.customerName, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "${tx.type} • $dateStr",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (tx.note.isNotBlank() || tx.billReference.isNotBlank()) {
                                        Text(
                                            text = listOf(tx.billReference, tx.note).filter { it.isNotBlank() }.joinToString(" • "),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                                Text(
                                    text = (if (isUdhaar) "+ " else "- ") +
                                        LedgerRepository.formatCurrencyPaise(tx.amountPaise),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = JetBrainsMonoFamily,
                                    color = color
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditLogsFeed(auditLogsState: UiState<List<AuditLog>>) {
    when (auditLogsState) {
        is UiState.Loading -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        }
        is UiState.Error -> {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(auditLogsState.message, color = MaterialTheme.colorScheme.error)
            }
        }
        is UiState.Success -> {
            val list = auditLogsState.data
            if (list.isEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Immutable audit trail is ready. Customer additions, ledger transactions, SMS dispatches, exports, and security changes appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(list, key = { it.id }) { log ->
                        val dateStr = if (log.createdAtEpochMs > 0L) {
                            SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
                                .format(Date(log.createdAtEpochMs))
                        } else "Recorded"

                        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = log.action,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(log.summary, style = MaterialTheme.typography.titleSmall)
                            if (log.details.isNotBlank()) {
                                Text(
                                    text = log.details,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
