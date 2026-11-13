package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.CalculatorEngine
import com.example.model.AnimationPreset
import com.example.model.AppDestination
import com.example.model.CustomerSummary
import com.example.model.TransactionType
import com.example.ui.components.CalculatorKeyStyle
import com.example.ui.components.OceanCalculatorKey
import com.example.ui.components.OceanGlassCard
import com.example.ui.components.OceanPillBadge
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalAnimationPreset
import com.example.ui.theme.LocalOceanPalette
import com.example.ui.theme.OceanEmeraldColors
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: MainViewModel,
    onNavigate: (AppDestination) -> Unit
) {
    val context = LocalContext.current
    val calcState by viewModel.calcState.collectAsStateWithLifecycle()
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val history by viewModel.calcHistory.collectAsStateWithLifecycle()
    val customers by viewModel.customerSummaries.collectAsStateWithLifecycle()
    val palette = LocalOceanPalette.current
    val animPreset = LocalAnimationPreset.current

    var showHistorySheet by remember { mutableStateOf(false) }
    var showSaveToLedgerDialog by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        val isCompactHeight = maxHeight < 640.dp

        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 8.dp else 12.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Simple SMS Calculator",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Ocean Emerald 3.5",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.jamaAccent
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (calcState.memoryValue != 0.0) {
                        OceanPillBadge(
                            text = "M: ${CalculatorEngine.formatNumber(calcState.memoryValue)}",
                            accent = OceanEmeraldColors.OceanBlue
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = palette.glassCardFill,
                        border = BorderStroke(1.dp, palette.glassBorderAccent),
                        modifier = Modifier
                            .testTag("btn_calc_history_sheet")
                            .clickable { showHistorySheet = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Calculation history",
                                tint = palette.jamaAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "History (${history.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Large Calculation Display Card
            OceanGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calc_display_card"),
                elevated = true,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = if (isCompactHeight) 12.dp else 16.dp)
            ) {
                // Expression line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = calcState.expression.ifBlank { "Tap keys to calculate" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 18.sp
                        ),
                        color = if (calcState.expression.isBlank()) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        textAlign = TextAlign.End,
                        modifier = Modifier.testTag("calc_expression_text")
                    )
                }

                Spacer(Modifier.height(6.dp))

                // Animated Result Display
                val displayedValue = calcState.errorText
                    ?: calcState.preview
                    ?: calcState.result

                AnimatedContent(
                    targetState = displayedValue,
                    transitionSpec = {
                        if (animPreset == AnimationPreset.REDUCED_MOTION) {
                            fadeIn() togetherWith fadeOut()
                        } else {
                            (fadeIn() + scaleIn(initialScale = 0.94f)) togetherWith fadeOut()
                        }
                    },
                    label = "calc_result_anim",
                    modifier = Modifier.fillMaxWidth()
                ) { target ->
                    Text(
                        text = if (calcState.preview != null && !calcState.hasEvaluated) "= $target" else target,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = when {
                                target.length > 14 -> 26.sp
                                target.length > 10 -> 34.sp
                                else -> 42.sp
                            },
                            fontWeight = FontWeight.Bold
                        ),
                        color = when {
                            calcState.errorText != null -> palette.dangerAccent
                            calcState.hasEvaluated -> palette.jamaAccent
                            calcState.preview != null -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calc_result_text")
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Copy, Share, SMS, Save-to-Ledger Quick Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val activeCopyText = if (calcState.expression.isNotBlank()) {
                        "${calcState.expression} = ${calcState.preview ?: calcState.result}"
                    } else {
                        calcState.result
                    }

                    CalcDisplayActionChip(
                        icon = Icons.Default.ContentCopy,
                        label = "Copy",
                        accent = OceanEmeraldColors.OceanBlue,
                        testTag = "btn_calc_copy",
                        onClick = { viewModel.copyToClipboard(context, "Calculation", activeCopyText) }
                    )
                    CalcDisplayActionChip(
                        icon = Icons.Default.Share,
                        label = "Share",
                        accent = OceanEmeraldColors.OceanBlue,
                        testTag = "btn_calc_share",
                        onClick = { viewModel.sharePlainText(context, activeCopyText) }
                    )
                    CalcDisplayActionChip(
                        icon = Icons.Default.Sms,
                        label = "Send SMS",
                        accent = OceanEmeraldColors.EmeraldGreen,
                        testTag = "btn_calc_sms",
                        onClick = {
                            viewModel.prepareSmsForCalculation(
                                expression = calcState.expression,
                                result = calcState.preview ?: calcState.result
                            )
                        }
                    )
                    CalcDisplayActionChip(
                        icon = Icons.Default.AccountBalanceWallet,
                        label = "To Ledger",
                        accent = palette.jamaAccent,
                        testTag = "btn_calc_to_ledger",
                        onClick = { showSaveToLedgerDialog = true }
                    )
                }
            }

            // Memory row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("MC", "MR", "M+", "M-").forEach { memKey ->
                    OceanCalculatorKey(
                        label = memKey,
                        style = CalculatorKeyStyle.UTILITY,
                        hapticEnabled = prefs.hapticFeedbackEnabled,
                        fontSize = 13.sp,
                        isCircular = false,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("key_$memKey"),
                        onClick = { viewModel.onCalcKey(memKey) }
                    )
                }
            }

            // Keypad Grid (5 rows x 4 columns)
            val keyRows = listOf(
                listOf(
                    "AC" to CalculatorKeyStyle.UTILITY,
                    "( )" to CalculatorKeyStyle.UTILITY,
                    "%" to CalculatorKeyStyle.OPERATOR,
                    "÷" to CalculatorKeyStyle.OPERATOR
                ),
                listOf(
                    "7" to CalculatorKeyStyle.NUMBER,
                    "8" to CalculatorKeyStyle.NUMBER,
                    "9" to CalculatorKeyStyle.NUMBER,
                    "×" to CalculatorKeyStyle.OPERATOR
                ),
                listOf(
                    "4" to CalculatorKeyStyle.NUMBER,
                    "5" to CalculatorKeyStyle.NUMBER,
                    "6" to CalculatorKeyStyle.NUMBER,
                    "−" to CalculatorKeyStyle.OPERATOR
                ),
                listOf(
                    "1" to CalculatorKeyStyle.NUMBER,
                    "2" to CalculatorKeyStyle.NUMBER,
                    "3" to CalculatorKeyStyle.NUMBER,
                    "+" to CalculatorKeyStyle.OPERATOR
                ),
                listOf(
                    "0" to CalculatorKeyStyle.NUMBER,
                    "." to CalculatorKeyStyle.NUMBER,
                    "⌫" to CalculatorKeyStyle.UTILITY,
                    "=" to CalculatorKeyStyle.EQUALS
                )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 7.dp else 10.dp)
            ) {
                keyRows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { (label, style) ->
                            val tagSafe = when (label) {
                                "+" -> "plus"
                                "−" -> "minus"
                                "×" -> "mul"
                                "÷" -> "div"
                                "=" -> "equals"
                                "." -> "dot"
                                "%" -> "pct"
                                "⌫" -> "bksp"
                                "( )" -> "parens"
                                else -> label
                            }
                            OceanCalculatorKey(
                                label = label,
                                style = style,
                                hapticEnabled = prefs.hapticFeedbackEnabled,
                                isCircular = false,
                                fontSize = if (label == "AC" || label == "( )") 18.sp else 23.sp,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .testTag("key_$tagSafe"),
                                onClick = { viewModel.onCalcKey(label) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Calculation History", style = MaterialTheme.typography.headlineSmall)
                    Row {
                        TextButton(onClick = {
                            showHistorySheet = false
                            onNavigate(AppDestination.History)
                        }) {
                            Text("Full View")
                        }
                        if (history.isNotEmpty()) {
                            TextButton(onClick = { viewModel.clearCalcHistory() }) {
                                Text("Clear", color = palette.dangerAccent)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (history.isEmpty()) {
                    Text(
                        text = "Your calculations will appear here automatically. Tap any record to reload it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(history, key = { it.id }) { item ->
                            OceanGlassCard(
                                contentPadding = PaddingValues(12.dp),
                                onClick = {
                                    showHistorySheet = false
                                    viewModel.loadHistoryIntoCalculator(item.expression, item.result)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.expression,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "= ${item.result}",
                                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily),
                                            color = palette.jamaAccent
                                        )
                                    }
                                    IconButton(onClick = {
                                        viewModel.copyToClipboard(context, "Result", item.result)
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = stringResource(R.string.cd_copy_result),
                                            tint = OceanEmeraldColors.OceanBlue
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showSaveToLedgerDialog) {
        val rawAmount = (calcState.preview ?: calcState.result).toDoubleOrNull() ?: 0.0
        SaveCalcToLedgerDialog(
            amount = rawAmount,
            expression = calcState.expression,
            currencySymbol = prefs.currency.symbol,
            customers = customers,
            onDismiss = { showSaveToLedgerDialog = false },
            onSave = { customerId, type, note ->
                viewModel.addLedgerTransaction(
                    customerId = customerId,
                    type = type,
                    amount = rawAmount,
                    note = note.ifBlank { "From Calculator" },
                    expression = calcState.expression
                )
                showSaveToLedgerDialog = false
            },
            onOpenHisabKitab = {
                showSaveToLedgerDialog = false
                onNavigate(AppDestination.HisabKitab)
            }
        )
    }
}

@Composable
private fun CalcDisplayActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = accent.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = Modifier
            .testTag(testTag)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SaveCalcToLedgerDialog(
    amount: Double,
    expression: String,
    currencySymbol: String,
    customers: List<CustomerSummary>,
    onDismiss: () -> Unit,
    onSave: (Long, TransactionType, String) -> Unit,
    onOpenHisabKitab: () -> Unit
) {
    var selectedCustomerId by remember { mutableStateOf(customers.firstOrNull()?.id) }
    var selectedType by remember { mutableStateOf(TransactionType.UDHAAR) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save to Hisab-Kitab") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Amount: ${CalculatorEngine.formatCurrency(amount, currencySymbol)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = OceanEmeraldColors.EmeraldGreen
                )
                if (expression.isNotBlank()) {
                    Text(
                        text = "Expression: $expression",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (customers.isEmpty()) {
                    Text(
                        text = "Add a customer in Hisab-Kitab first to link this calculation.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TransactionType.entries.forEach { t ->
                            val selected = selectedType == t
                            OutlinedButton(
                                onClick = { selectedType = t },
                                border = BorderStroke(
                                    1.2.dp,
                                    if (selected) OceanEmeraldColors.EmeraldGreen else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (t == TransactionType.JAMA) "+ Jama" else "- Udhaar",
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    Text("Select Customer:", style = MaterialTheme.typography.labelLarge)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        customers.take(5).forEach { c ->
                            val isSelected = selectedCustomerId == c.id
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) OceanEmeraldColors.OceanBlue.copy(alpha = 0.22f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) OceanEmeraldColors.OceanBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCustomerId = c.id }
                            ) {
                                Text(
                                    text = c.name,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (customers.isEmpty()) {
                Button(onClick = onOpenHisabKitab) {
                    Text("Open Hisab-Kitab")
                }
            } else {
                Button(
                    enabled = selectedCustomerId != null && amount > 0.0,
                    colors = ButtonDefaults.buttonColors(containerColor = OceanEmeraldColors.EmeraldGreen),
                    onClick = {
                        selectedCustomerId?.let { onSave(it, selectedType, "") }
                    }
                ) {
                    Text("Save Entry")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
