package com.example.desicalc.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.desicalc.data.local.CalculationHistoryEntity
import com.example.desicalc.engine.CalculatorEngine
import com.example.desicalc.engine.UnitCategory
import com.example.desicalc.model.AppLanguage
import com.example.desicalc.model.CalculatorMode
import com.example.desicalc.model.formatInr
import com.example.desicalc.ui.components.DesiCard
import com.example.desicalc.ui.theme.LocalDesiColors
import com.example.desicalc.ui.theme.MonoFontFamily
import java.util.Locale

@Composable
fun CalculatorScreen(
    activeMode: CalculatorMode,
    expression: String,
    result: String,
    error: String?,
    history: List<CalculationHistoryEntity>,
    language: AppLanguage,
    onSelectMode: (CalculatorMode) -> Unit,
    onAppendToken: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onEvaluate: () -> Unit,
    onSaveSpecialized: (String, String, String, String) -> Unit,
    onReuseHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    onAskAiCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var nlInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("calculator_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Mode Selector Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CalculatorMode.entries) { mode ->
                    FilterChip(
                        selected = activeMode == mode,
                        onClick = { onSelectMode(mode) },
                        label = {
                            Text(if (language == AppLanguage.HINDI) mode.labelHi else mode.labelEn)
                        },
                        modifier = Modifier.testTag("calc_mode_${mode.name}")
                    )
                }
            }
        }

        // Bilingual NL Quick Command Bar
        item {
            DesiCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = nlInput,
                        onValueChange = { nlInput = it },
                        placeholder = { Text("Hindi/English math or ledger command...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (nlInput.isNotBlank()) {
                                onAskAiCommand(nlInput)
                                nlInput = ""
                            }
                        },
                        modifier = Modifier.testTag("calc_nl_ask_btn")
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = "Ask AI", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Active Calculator Body
        item {
            when (activeMode) {
                CalculatorMode.BASIC -> BasicCalculatorPad(
                    expression = expression,
                    result = result,
                    error = error,
                    onAppendToken = onAppendToken,
                    onBackspace = onBackspace,
                    onClear = onClear,
                    onEvaluate = onEvaluate
                )
                CalculatorMode.GST -> GstCalculatorCard(onSave = onSaveSpecialized)
                CalculatorMode.DISCOUNT -> DiscountCalculatorCard(onSave = onSaveSpecialized)
                CalculatorMode.PERCENTAGE -> PercentageCalculatorCard(onSave = onSaveSpecialized)
                CalculatorMode.PROFIT_LOSS -> ProfitLossCalculatorCard(onSave = onSaveSpecialized)
                CalculatorMode.UNIT -> UnitConverterCard(onSave = onSaveSpecialized)
            }
        }

        // Calculation History
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Calculation History (${history.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                if (history.isNotEmpty()) {
                    TextButton(onClick = onClearHistory) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear calculation history")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear")
                    }
                }
            }
        }

        if (history.isEmpty()) {
            item {
                DesiCard {
                    Text(
                        text = "Your calculation history (Basic, GST, Discount, Profit/Loss, Units) will appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(history.take(25), key = { it.id }) { item ->
                DesiCard(onClick = {
                    if (item.category == "BASIC") onReuseHistory(item.expression)
                }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${item.category} • ${item.expression}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (item.details.isNotBlank()) {
                                Text(
                                    text = item.details,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = item.result,
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
    }
}

@Composable
private fun BasicCalculatorPad(
    expression: String,
    result: String,
    error: String?,
    onAppendToken: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onEvaluate: () -> Unit
) {
    DesiCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = expression.ifEmpty { "0" },
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = error ?: result,
                style = MaterialTheme.typography.displaySmall.copy(fontFamily = MonoFontFamily),
                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calc_result_text"),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(14.dp))

            val rows = listOf(
                listOf("AC", "(", ")", "÷"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "-"),
                listOf("1", "2", "3", "+"),
                listOf("%", "0", ".", "=")
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onBackspace,
                        modifier = Modifier.testTag("calc_backspace")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = "Backspace")
                    }
                }
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { key ->
                            val isEquals = key == "="
                            val isClear = key == "AC"
                            val isOperator = key in listOf("÷", "×", "-", "+", "%", "(", ")")
                            CalcKeyButton(
                                label = key,
                                containerColor = when {
                                    isEquals -> MaterialTheme.colorScheme.primary
                                    isClear -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                                    isOperator -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentColor = when {
                                    isEquals -> MaterialTheme.colorScheme.onPrimary
                                    isClear -> MaterialTheme.colorScheme.error
                                    isOperator -> MaterialTheme.colorScheme.onSecondaryContainer
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                onClick = {
                                    when (key) {
                                        "AC" -> onClear()
                                        "=" -> onEvaluate()
                                        else -> onAppendToken(key)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .testTag("calc_key_$key")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalcKeyButton(
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "keyPress"
    )
    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier.scale(scale)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun GstCalculatorCard(onSave: (String, String, String, String) -> Unit) {
    var amountStr by remember { mutableStateOf("1000") }
    var rateStr by remember { mutableStateOf("18") }
    var isInclusive by remember { mutableStateOf(false) }

    val amount = amountStr.toDoubleOrNull() ?: 0.0
    val rate = rateStr.toDoubleOrNull() ?: 0.0
    val gst = remember(amount, rate, isInclusive) {
        CalculatorEngine.calculateGst(amount.coerceAtLeast(0.0), rate.coerceAtLeast(0.0), isInclusive)
    }

    DesiCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("GST Calculator (CGST + SGST)", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = { Text("Amount (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("3", "5", "12", "18", "28")) { slab ->
                    FilterChip(
                        selected = rateStr == slab,
                        onClick = { rateStr = slab },
                        label = { Text("$slab%") }
                    )
                }
            }
            OutlinedTextField(
                value = rateStr,
                onValueChange = { rateStr = it },
                label = { Text("Custom GST Rate (%)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isInclusive) "GST Included in Amount" else "Add GST Extra (Exclusive)")
                Switch(checked = isInclusive, onCheckedChange = { isInclusive = it })
            }
            Text("Base Amount: ${formatInr(gst.baseAmount)}", fontFamily = MonoFontFamily)
            Text("CGST (${rate / 2}%): ${formatInr(gst.cgstAmount)} | SGST (${rate / 2}%): ${formatInr(gst.sgstAmount)}", fontFamily = MonoFontFamily)
            Text(
                "Total Payable: ${formatInr(gst.totalAmount)}",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Button(
                onClick = {
                    onSave(
                        "GST",
                        "${formatInr(amount)} @ $rate% (${if (isInclusive) "Incl" else "Excl"})",
                        formatInr(gst.totalAmount),
                        "Base ${formatInr(gst.baseAmount)}, GST ${formatInr(gst.gstAmount)}"
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save GST Calculation")
            }
        }
    }
}

@Composable
private fun DiscountCalculatorCard(onSave: (String, String, String, String) -> Unit) {
    var mrpStr by remember { mutableStateOf("1500") }
    var d1Str by remember { mutableStateOf("20") }
    var d2Str by remember { mutableStateOf("0") }

    val res = remember(mrpStr, d1Str, d2Str) {
        CalculatorEngine.calculateDiscount(
            mrp = (mrpStr.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
            discountPercent = d1Str.toDoubleOrNull() ?: 0.0,
            extraDiscountPercent = d2Str.toDoubleOrNull() ?: 0.0
        )
    }

    DesiCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Discount & Savings Calculator", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = mrpStr,
                onValueChange = { mrpStr = it },
                label = { Text("MRP / Original Price (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = d1Str,
                    onValueChange = { d1Str = it },
                    label = { Text("Discount %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = d2Str,
                    onValueChange = { d2Str = it },
                    label = { Text("Extra %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Text("You Save: ${formatInr(res.totalDiscountAmount)} (${String.format(Locale.ROOT, "%.1f", res.effectiveDiscountPercent)}%)", fontFamily = MonoFontFamily)
            Text(
                "Final Price: ${formatInr(res.finalPrice)}",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Button(
                onClick = {
                    onSave(
                        "DISCOUNT",
                        "${formatInr(res.originalPrice)} - $d1Str% - $d2Str%",
                        formatInr(res.finalPrice),
                        "Saved ${formatInr(res.totalDiscountAmount)}"
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Discount")
            }
        }
    }
}

@Composable
private fun PercentageCalculatorCard(onSave: (String, String, String, String) -> Unit) {
    var pctStr by remember { mutableStateOf("15") }
    var totalStr by remember { mutableStateOf("2000") }
    val pct = pctStr.toDoubleOrNull() ?: 0.0
    val total = totalStr.toDoubleOrNull() ?: 0.0
    val partValue = CalculatorEngine.calculatePercentageOf(pct, total)

    DesiCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Percentage Calculator", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pctStr,
                    onValueChange = { pctStr = it },
                    label = { Text("Percent (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = totalStr,
                    onValueChange = { totalStr = it },
                    label = { Text("Of Value") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                "$pctStr% of $totalStr = ${CalculatorEngine.formatCleanNumber(partValue)}",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Button(
                onClick = {
                    onSave("PERCENTAGE", "$pctStr% of $totalStr", CalculatorEngine.formatCleanNumber(partValue), "")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Percentage")
            }
        }
    }
}

@Composable
private fun ProfitLossCalculatorCard(onSave: (String, String, String, String) -> Unit) {
    val ext = LocalDesiColors.current
    var cpStr by remember { mutableStateOf("450") }
    var spStr by remember { mutableStateOf("550") }
    var qtyStr by remember { mutableStateOf("10") }

    val res = remember(cpStr, spStr, qtyStr) {
        CalculatorEngine.calculateProfitLoss(
            costPricePerUnit = (cpStr.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
            sellingPricePerUnit = (spStr.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
            quantity = (qtyStr.toDoubleOrNull() ?: 1.0).coerceAtLeast(1.0)
        )
    }

    DesiCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Profit & Loss Calculator", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = cpStr,
                    onValueChange = { cpStr = it },
                    label = { Text("Cost Price (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = spStr,
                    onValueChange = { spStr = it },
                    label = { Text("Sell Price (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = qtyStr,
                onValueChange = { qtyStr = it },
                label = { Text("Quantity") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "${if (res.isProfit) "Net Profit" else "Net Loss"}: ${formatInr(kotlin.math.abs(res.profitOrLoss))} (${String.format(Locale.ROOT, "%.1f", kotlin.math.abs(res.profitPercentOnCost))}%)",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold),
                color = if (res.isProfit) ext.jamaGreen else ext.udharRed
            )
            Text("Margin on Sale: ${String.format(Locale.ROOT, "%.1f", res.marginPercentOnSell)}%", fontFamily = MonoFontFamily)
            Button(
                onClick = {
                    onSave(
                        "PROFIT_LOSS",
                        "CP ₹$cpStr -> SP ₹$spStr (x$qtyStr)",
                        formatInr(res.profitOrLoss),
                        if (res.isProfit) "Profit ${String.format(Locale.ROOT, "%.1f", res.profitPercentOnCost)}%" else "Loss"
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Profit/Loss")
            }
        }
    }
}

@Composable
private fun UnitConverterCard(onSave: (String, String, String, String) -> Unit) {
    var selectedCat by remember { mutableStateOf(UnitCategory.WEIGHT) }
    val catUnits = remember(selectedCat) { CalculatorEngine.allUnits.filter { it.category == selectedCat } }
    var fromUnit by remember(selectedCat) { mutableStateOf(catUnits.first()) }
    var toUnit by remember(selectedCat) { mutableStateOf(catUnits.getOrElse(1) { catUnits.first() }) }
    var valueStr by remember { mutableStateOf("1") }
    var showFromMenu by remember { mutableStateOf(false) }
    var showToMenu by remember { mutableStateOf(false) }

    val inputVal = valueStr.toDoubleOrNull() ?: 0.0
    val converted = CalculatorEngine.convertUnit(inputVal, fromUnit, toUnit)

    DesiCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Indian & Standard Unit Converter", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(UnitCategory.entries) { cat ->
                    FilterChip(
                        selected = selectedCat == cat,
                        onClick = { selectedCat = cat },
                        label = { Text(cat.title) }
                    )
                }
            }
            OutlinedTextField(
                value = valueStr,
                onValueChange = { valueStr = it },
                label = { Text("Enter Value") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedButton(onClick = { showFromMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(fromUnit.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    DropdownMenu(expanded = showFromMenu, onDismissRequest = { showFromMenu = false }) {
                        catUnits.forEach { u ->
                            DropdownMenuItem(text = { Text(u.label) }, onClick = {
                                fromUnit = u
                                showFromMenu = false
                            })
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedButton(onClick = { showToMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(toUnit.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    DropdownMenu(expanded = showToMenu, onDismissRequest = { showToMenu = false }) {
                        catUnits.forEach { u ->
                            DropdownMenuItem(text = { Text(u.label) }, onClick = {
                                toUnit = u
                                showToMenu = false
                            })
                        }
                    }
                }
            }
            Text(
                text = "${CalculatorEngine.formatCleanNumber(converted)} ${toUnit.id}",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Button(
                onClick = {
                    onSave(
                        "UNIT",
                        "$valueStr ${fromUnit.id} -> ${toUnit.id}",
                        "${CalculatorEngine.formatCleanNumber(converted)} ${toUnit.id}",
                        selectedCat.title
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Conversion")
            }
        }
    }
}
