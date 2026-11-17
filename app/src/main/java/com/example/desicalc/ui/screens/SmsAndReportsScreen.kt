package com.example.desicalc.ui.screens

import android.Manifest
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.automirrored.rounded.ScheduleSend
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.desicalc.data.local.SmsLogEntity
import com.example.desicalc.data.repository.DesiLedgerRepository
import com.example.desicalc.model.AppLanguage
import com.example.desicalc.model.AppSettingsState
import com.example.desicalc.model.CustomerLedgerSummary
import com.example.desicalc.model.PeriodReport
import com.example.desicalc.model.ReportPeriod
import com.example.desicalc.model.SmsTemplateType
import com.example.desicalc.model.formatInr
import com.example.desicalc.ui.components.DesiCard
import com.example.desicalc.ui.components.UdharJamaBarChart
import com.example.desicalc.ui.theme.LocalDesiColors
import com.example.desicalc.ui.theme.MonoFontFamily
import kotlin.math.abs

@Composable
fun SmsAndReportsScreen(
    customers: List<CustomerLedgerSummary>,
    smsLogs: List<SmsLogEntity>,
    report: PeriodReport,
    selectedPeriod: ReportPeriod,
    settings: AppSettingsState,
    hasSmsPermission: Boolean,
    buildSmsPreview: (CustomerLedgerSummary, SmsTemplateType, Double) -> String,
    onSelectPeriod: (ReportPeriod) -> Unit,
    onOpenManualSms: (CustomerLedgerSummary?, String, SmsTemplateType, String) -> Unit,
    onSendDirectSms: (CustomerLedgerSummary?, String, SmsTemplateType, String, Boolean) -> Unit,
    onSaveTemplates: (String, String, String, String) -> Unit,
    onTriggerScheduledReminders: () -> Unit,
    onExportCsv: () -> Unit,
    onExportJsonBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalDesiColors.current
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Reports, 1 = SMS Hub

    val smsPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("sms_reports_screen")
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            SegmentedButton(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                shape = SegmentedButtonDefaults.itemShape(0, 2)
            ) { Text("Reports & Export") }
            SegmentedButton(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                shape = SegmentedButtonDefaults.itemShape(1, 2)
            ) { Text("SMS & Templates") }
        }

        if (activeTab == 0) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ReportPeriod.entries) { p ->
                            FilterChip(
                                selected = selectedPeriod == p,
                                onClick = { onSelectPeriod(p) },
                                label = {
                                    Text(if (settings.language == AppLanguage.HINDI) p.labelHi else p.labelEn)
                                }
                            )
                        }
                    }
                }

                item {
                    DesiCard {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(report.periodTitle, style = MaterialTheme.typography.titleMedium)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Transactions: ${report.transactionCount}")
                                Text(
                                    "Net Flow: ${formatInr(report.netFlow)}",
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = if (report.netFlow >= 0) ext.jamaGreen else ext.udharRed
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Udhar Diya: ${formatInr(report.udharDiyaTotal)}", color = ext.udharRed, fontFamily = MonoFontFamily)
                                Text("Jama Recv: ${formatInr(report.jamaReceivedTotal)}", color = ext.jamaGreen, fontFamily = MonoFontFamily)
                            }
                            if (report.udharLiyaTotal > 0 || report.paymentMadeTotal > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Udhar Liya: ${formatInr(report.udharLiyaTotal)}", fontFamily = MonoFontFamily)
                                    Text("Paid Made: ${formatInr(report.paymentMadeTotal)}", fontFamily = MonoFontFamily)
                                }
                            }
                            UdharJamaBarChart(buckets = report.dailyBuckets)
                        }
                    }
                }

                item {
                    DesiCard {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Export & Backup", style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(onClick = onExportCsv, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Rounded.Download, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("CSV Export")
                                }
                                OutlinedButton(onClick = onExportJsonBackup, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Rounded.Download, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("JSON Backup")
                                }
                            }
                        }
                    }
                }
            }
        } else {
            var selectedCustomer by remember(customers) { mutableStateOf(customers.firstOrNull()) }
            var selectedTpl by remember { mutableStateOf(SmsTemplateType.REMINDER) }
            var customBody by remember(selectedCustomer, selectedTpl, settings) {
                mutableStateOf(
                    selectedCustomer?.let {
                        buildSmsPreview(it, selectedTpl, abs(it.netBalance))
                    } ?: settings.reminderTemplate
                )
            }
            var userConsent by remember { mutableStateOf(false) }

            var tplReminder by remember(settings.reminderTemplate) { mutableStateOf(settings.reminderTemplate) }
            var tplUdhar by remember(settings.udharTemplate) { mutableStateOf(settings.udharTemplate) }
            var tplJama by remember(settings.jamaTemplate) { mutableStateOf(settings.jamaTemplate) }
            var tplSummary by remember(settings.summaryTemplate) { mutableStateOf(settings.summaryTemplate) }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    DesiCard {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Send Customer SMS", style = MaterialTheme.typography.titleMedium)
                            if (customers.isEmpty()) {
                                Text("Add a customer with a mobile number in Khata to send SMS reminders.")
                            } else {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(customers.take(20)) { c ->
                                        FilterChip(
                                            selected = selectedCustomer?.id == c.id,
                                            onClick = { selectedCustomer = c },
                                            label = { Text(c.name) }
                                        )
                                    }
                                }
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
                                            selected = selectedTpl == tpl,
                                            onClick = { selectedTpl = tpl },
                                            label = { Text(tpl.titleEn) }
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = customBody,
                                    onValueChange = { customBody = it },
                                    label = { Text("Message Preview") },
                                    minLines = 3,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = userConsent, onCheckedChange = { userConsent = it })
                                    Text("I consent to direct SMS dispatch (never sent silently)")
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = {
                                            val target = selectedCustomer
                                            onOpenManualSms(
                                                target,
                                                target?.mobile.orEmpty(),
                                                selectedTpl,
                                                customBody
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.Sms, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("SMS Composer")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            if (!hasSmsPermission) {
                                                smsPermLauncher.launch(Manifest.permission.SEND_SMS)
                                            } else {
                                                val target = selectedCustomer
                                                onSendDirectSms(
                                                    target,
                                                    target?.mobile.orEmpty(),
                                                    selectedTpl,
                                                    customBody,
                                                    userConsent
                                                )
                                            }
                                        },
                                        enabled = userConsent,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(if (hasSmsPermission) "Direct Send" else "Allow SMS")
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick = onTriggerScheduledReminders,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ScheduleSend, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Run Scheduled Reminder Check Now")
                            }
                        }
                    }
                }

                item {
                    DesiCard {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Custom SMS Templates", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Placeholders: {name}, {business}, {amount}, {balance}, {date}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(value = tplReminder, onValueChange = { tplReminder = it }, label = { Text("Reminder Template") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = tplUdhar, onValueChange = { tplUdhar = it }, label = { Text("Udhar Template") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = tplJama, onValueChange = { tplJama = it }, label = { Text("Jama Template") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = tplSummary, onValueChange = { tplSummary = it }, label = { Text("Summary Template") }, modifier = Modifier.fillMaxWidth())
                            Button(
                                onClick = { onSaveTemplates(tplReminder, tplUdhar, tplJama, tplSummary) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Save SMS Templates") }
                        }
                    }
                }

                item {
                    Text("SMS Sending & Delivery History (${smsLogs.size})", style = MaterialTheme.typography.titleMedium)
                }

                if (smsLogs.isEmpty()) {
                    item {
                        DesiCard {
                            Text(
                                "No SMS sent or composed yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(smsLogs.take(30), key = { it.id }) { log ->
                        DesiCard {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${log.customerName} (${log.mobile})", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = log.status,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = when (log.status) {
                                            "DELIVERED", "SENT", "COMPOSER_OPENED" -> ext.jamaGreen
                                            "FAILED" -> ext.udharRed
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                    )
                                }
                                Text(log.messageBody, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "${log.templateType} • ${DesiLedgerRepository.formatTimestamp(log.timestamp)}${if (!log.errorDetail.isNullOrBlank()) " • ${log.errorDetail}" else ""}",
                                    style = MaterialTheme.typography.labelSmall,
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
