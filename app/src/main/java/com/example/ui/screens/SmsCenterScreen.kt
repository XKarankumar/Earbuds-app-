package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.CalculatorEngine
import com.example.model.CustomerSummary
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

private val smsTemplates = listOf(
    "Payment Reminder",
    "Jama Receipt",
    "Ledger Summary",
    "Calc Quote"
)

@Composable
fun SmsCenterScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val draft by viewModel.smsDraft.collectAsStateWithLifecycle()
    val customers by viewModel.customerSummaries.collectAsStateWithLifecycle()
    val smsLogs by viewModel.smsLogs.collectAsStateWithLifecycle()
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val palette = LocalOceanPalette.current

    var selectedCustomer by remember { mutableStateOf<CustomerSummary?>(null) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .testTag("sms_center_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OceanTopBar(
                    title = "SMS Center",
                    subtitle = "Zero-permission system SMS & WhatsApp handoff",
                    trailing = {
                        OceanPillBadge(
                            text = "${smsLogs.size} Sent",
                            accent = OceanEmeraldColors.MintGreen
                        )
                    }
                )
            }

            // Pick a Customer Shortcut
            if (customers.isNotEmpty()) {
                item {
                    Text(
                        text = "Quick Select Customer",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(customers, key = { it.id }) { c ->
                            val isSelected = selectedCustomer?.id == c.id
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isSelected) OceanEmeraldColors.EmeraldGreen.copy(alpha = 0.25f)
                                else palette.glassCardFill,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) OceanEmeraldColors.MintGreen else palette.glassBorderAccent
                                ),
                                modifier = Modifier.clickable {
                                    selectedCustomer = c
                                    viewModel.prepareSmsForCustomer(c, draft.templateIndex)
                                }
                            ) {
                                Text(
                                    text = "${c.name} (${CalculatorEngine.formatCurrency(kotlin.math.abs(c.netBalance), prefs.currency.symbol)})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // SMS Composer Card
            item {
                OceanGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevated = true
                ) {
                    Text(
                        text = "Smart SMS Templates",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(smsTemplates.size) { idx ->
                            FilterChip(
                                selected = draft.templateIndex == idx,
                                onClick = { viewModel.applySmsTemplate(idx, selectedCustomer) },
                                label = { Text(smsTemplates[idx]) }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = draft.recipientName,
                            onValueChange = { viewModel.updateSmsDraft(recipientName = it) },
                            label = { Text("Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = draft.recipientPhone,
                            onValueChange = { viewModel.updateSmsDraft(recipientPhone = it) },
                            label = { Text("Phone Number") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_sms_phone")
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = draft.messageBody,
                        onValueChange = { viewModel.updateSmsDraft(messageBody = it) },
                        label = { Text("SMS Message Preview") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_sms_body")
                    )

                    val charCount = draft.messageBody.length
                    val segments = if (charCount == 0) 0 else ((charCount - 1) / 160) + 1
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$charCount chars • $segments SMS segment(s)",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = { viewModel.applySmsTemplate(draft.templateIndex, selectedCustomer) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("Generate Template")
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.launchSmsIntent(
                                    context = context,
                                    phone = draft.recipientPhone,
                                    message = draft.messageBody,
                                    recipientName = draft.recipientName,
                                    templateName = smsTemplates.getOrElse(draft.templateIndex) { "Custom" }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OceanEmeraldColors.EmeraldGreen),
                            modifier = Modifier
                                .weight(1.4f)
                                .height(48.dp)
                                .testTag("btn_open_sms_app")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open SMS App", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { viewModel.sharePlainText(context, draft.messageBody, "Share Message") },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Share", maxLines = 1)
                        }
                        IconButton(
                            onClick = { viewModel.copyToClipboard(context, "SMS Message", draft.messageBody) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy message",
                                tint = OceanEmeraldColors.OceanBlue
                            )
                        }
                    }
                }
            }

            // Sent SMS History
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SMS Activity Log",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (smsLogs.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearSmsLogs() }) {
                            Text("Clear All", color = palette.dangerAccent)
                        }
                    }
                }
            }

            if (smsLogs.isEmpty()) {
                item {
                    OceanGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No SMS dispatches logged yet. Compose a reminder or calculation quote above and tap Open SMS App.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val fmt = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                items(smsLogs, key = { it.id }) { log ->
                    OceanGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = log.recipientName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    OceanPillBadge(
                                        text = log.templateName,
                                        accent = OceanEmeraldColors.OceanBlue
                                    )
                                }
                                if (log.recipientPhone.isNotBlank()) {
                                    Text(
                                        text = log.recipientPhone,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = log.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = fmt.format(Date(log.createdAtMillis)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { viewModel.deleteSmsLog(log.id) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete log",
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
