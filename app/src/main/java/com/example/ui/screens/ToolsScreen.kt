package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.CalculatorEvaluator
import com.example.model.FinancialCalculators
import com.example.model.UnitCategory
import com.example.model.UnitConverterEngine
import com.example.ui.components.BillScannerSheet
import com.example.ui.components.GlassCard
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.viewmodel.MainViewModel

private enum class ToolsSection(val label: String) {
    CONVERTER("Unit Converter"),
    GST("GST"),
    INTEREST("Interest"),
    DISCOUNT("Discount"),
    EMI("EMI")
}

@Composable
fun ToolsScreen(
    viewModel: MainViewModel,
    onSendToCalculator: () -> Unit
) {
    val prefs by viewModel.localPrefs.collectAsStateWithLifecycle()
    var activeSection by remember { mutableStateOf(ToolsSection.CONVERTER) }
    var showScanner by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Converters & Calculators", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "KL, Litre, KG, GM, GST, Interest, Discount, EMI & QR Scan",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = { showScanner = true },
                modifier = Modifier.testTag("tools_open_scanner_btn")
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Scan")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToolsSection.entries.forEach { section ->
                FilterChip(
                    selected = activeSection == section,
                    onClick = { activeSection = section },
                    label = { Text(section.label) },
                    modifier = Modifier.testTag("tools_tab_${section.name}")
                )
            }
        }

        when (activeSection) {
            ToolsSection.CONVERTER -> UnitConverterCard(
                onSendResult = { valStr ->
                    viewModel.loadValueIntoCalculator(valStr)
                    onSendToCalculator()
                }
            )
            ToolsSection.GST -> GstCalculatorCard(
                onSendResult = { valStr ->
                    viewModel.loadValueIntoCalculator(valStr)
                    onSendToCalculator()
                }
            )
            ToolsSection.INTEREST -> InterestCalculatorCard()
            ToolsSection.DISCOUNT -> DiscountCalculatorCard()
            ToolsSection.EMI -> EmiCalculatorCard()
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showScanner) {
        BillScannerSheet(
            cameraFeatureEnabled = prefs.cameraScanFeatureEnabled,
            onDismiss = { showScanner = false },
            onResultExtracted = { info ->
                info.extractedAmount?.let { amt ->
                    viewModel.loadValueIntoCalculator(amt)
                    viewModel.emitMessage("Loaded ₹$amt into Calculator")
                    onSendToCalculator()
                }
            }
        )
    }
}

@Composable
private fun UnitConverterCard(onSendResult: (String) -> Unit) {
    var category by remember { mutableStateOf(UnitCategory.VOLUME) }
    val units = UnitConverterEngine.unitsByCategory[category].orEmpty()
    var fromId by remember(category) { mutableStateOf(units.firstOrNull()?.id ?: "kl") }
    var toId by remember(category) { mutableStateOf(units.getOrNull(1)?.id ?: "l") }
    var inputText by remember { mutableStateOf("1") }

    val inputNum = inputText.toDoubleOrNull() ?: 0.0
    val converted = UnitConverterEngine.convert(inputNum, category, fromId, toId)
    val formatted = CalculatorEvaluator.formatNumber(converted)
    val toUnit = units.firstOrNull { it.id == toId }

    GlassCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
        Text("All Major Unit Conversions", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            UnitCategory.entries.forEach { cat ->
                FilterChip(
                    selected = category == cat,
                    onClick = { category = cat },
                    label = { Text(cat.title) }
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Input Value") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )
        Spacer(Modifier.height(10.dp))

        Text("From Unit", style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            units.forEach { unit ->
                FilterChip(
                    selected = fromId == unit.id,
                    onClick = { fromId = unit.id },
                    label = { Text("${unit.label} (${unit.symbol})") }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = {
                val tmp = fromId
                fromId = toId
                toId = tmp
            }) {
                Icon(Icons.Default.SwapHoriz, contentDescription = "Swap units")
            }
        }

        Text("To Unit", style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            units.forEach { unit ->
                FilterChip(
                    selected = toId == unit.id,
                    onClick = { toId = unit.id },
                    label = { Text("${unit.label} (${unit.symbol})") }
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("CONVERTED RESULT", style = MaterialTheme.typography.labelSmall)
            Text(
                text = "$formatted ${toUnit?.symbol.orEmpty()}",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = JetBrainsMonoFamily,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onSendResult(formatted) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Use Result in Calculator")
            }
        }
    }
}

@Composable
private fun GstCalculatorCard(onSendResult: (String) -> Unit) {
    var amountText by remember { mutableStateOf("1000") }
    var rate by remember { mutableDoubleStateOf(18.0) }
    var inclusive by remember { mutableStateOf(false) }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val res = FinancialCalculators.calculateGst(amount, rate, inclusive)

    GlassCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
        Text("GST Calculator", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !inclusive,
                onClick = { inclusive = false },
                shape = SegmentedButtonDefaults.itemShape(0, 2)
            ) { Text("Exclusive (+GST)") }
            SegmentedButton(
                selected = inclusive,
                onClick = { inclusive = true },
                shape = SegmentedButtonDefaults.itemShape(1, 2)
            ) { Text("Inclusive (Extract)") }
        }
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            label = { Text("Base / Gross Amount (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(3.0, 5.0, 12.0, 18.0, 28.0).forEach { slab ->
                FilterChip(
                    selected = rate == slab,
                    onClick = { rate = slab },
                    label = { Text("${slab.toInt()}%") }
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        MetricLine("Net Base Price", "₹ ${CalculatorEvaluator.formatNumber(res.netAmount)}")
        MetricLine("CGST (${CalculatorEvaluator.formatNumber(rate / 2)}%)", "₹ ${CalculatorEvaluator.formatNumber(res.cgstAmount)}")
        MetricLine("SGST (${CalculatorEvaluator.formatNumber(rate / 2)}%)", "₹ ${CalculatorEvaluator.formatNumber(res.sgstAmount)}")
        MetricLine("Total GST", "₹ ${CalculatorEvaluator.formatNumber(res.gstAmount)}")
        MetricLine("Final Total", "₹ ${CalculatorEvaluator.formatNumber(res.totalAmount)}", highlight = true)
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = { onSendResult(CalculatorEvaluator.formatNumber(res.totalAmount)) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Send Total to Calculator")
        }
    }
}

@Composable
private fun InterestCalculatorCard() {
    var principalText by remember { mutableStateOf("50000") }
    var rateText by remember { mutableStateOf("12") }
    var yearsText by remember { mutableStateOf("2") }
    var compound by remember { mutableStateOf(false) }

    val res = FinancialCalculators.calculateInterest(
        principal = principalText.toDoubleOrNull() ?: 0.0,
        annualRatePercent = rateText.toDoubleOrNull() ?: 0.0,
        years = yearsText.toDoubleOrNull() ?: 0.0,
        compound = compound
    )

    GlassCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
        Text("Simple & Compound Interest", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !compound,
                onClick = { compound = false },
                shape = SegmentedButtonDefaults.itemShape(0, 2)
            ) { Text("Simple") }
            SegmentedButton(
                selected = compound,
                onClick = { compound = true },
                shape = SegmentedButtonDefaults.itemShape(1, 2)
            ) { Text("Compound (Yearly)") }
        }
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = principalText,
            onValueChange = { principalText = it },
            label = { Text("Principal (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = rateText,
                onValueChange = { rateText = it },
                label = { Text("Rate % p.a.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = yearsText,
                onValueChange = { yearsText = it },
                label = { Text("Years") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Spacer(Modifier.height(12.dp))
        MetricLine("Principal", "₹ ${CalculatorEvaluator.formatNumber(res.principal)}")
        MetricLine("Interest Earned", "₹ ${CalculatorEvaluator.formatNumber(res.interest)}")
        MetricLine("Maturity Total", "₹ ${CalculatorEvaluator.formatNumber(res.totalAmount)}", highlight = true)
    }
}

@Composable
private fun DiscountCalculatorCard() {
    var priceText by remember { mutableStateOf("2500") }
    var discountPct by remember { mutableStateOf("15") }
    var extraTaxPct by remember { mutableStateOf("0") }

    val res = FinancialCalculators.calculateDiscount(
        originalPrice = priceText.toDoubleOrNull() ?: 0.0,
        discountPercent = discountPct.toDoubleOrNull() ?: 0.0,
        extraTaxPercent = extraTaxPct.toDoubleOrNull() ?: 0.0
    )

    GlassCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
        Text("Discount Calculator", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = priceText,
            onValueChange = { priceText = it },
            label = { Text("Original MRP (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = discountPct,
                onValueChange = { discountPct = it },
                label = { Text("Discount %") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = extraTaxPct,
                onValueChange = { extraTaxPct = it },
                label = { Text("Extra Tax %") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Spacer(Modifier.height(12.dp))
        MetricLine("You Save", "₹ ${CalculatorEvaluator.formatNumber(res.discountAmount)}")
        MetricLine("Extra Tax", "₹ ${CalculatorEvaluator.formatNumber(res.taxAmount)}")
        MetricLine("Final Payable", "₹ ${CalculatorEvaluator.formatNumber(res.finalPrice)}", highlight = true)
    }
}

@Composable
private fun EmiCalculatorCard() {
    var principalText by remember { mutableStateOf("200000") }
    var ratePercent by remember { mutableDoubleStateOf(10.5) }
    var tenureMonths by remember { mutableIntStateOf(24) }

    val res = FinancialCalculators.calculateEmi(
        principal = principalText.toDoubleOrNull() ?: 0.0,
        annualRatePercent = ratePercent,
        months = tenureMonths
    )

    GlassCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
        Text("Loan EMI Calculator", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = principalText,
            onValueChange = { principalText = it },
            label = { Text("Loan Principal (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(10.dp))
        Text("Interest Rate: ${String.format(java.util.Locale.US, "%.1f", ratePercent)}% p.a.", style = MaterialTheme.typography.titleSmall)
        Slider(
            value = ratePercent.toFloat(),
            onValueChange = { ratePercent = it.toDouble() },
            valueRange = 1f..30f
        )
        Text("Tenure: $tenureMonths Months", style = MaterialTheme.typography.titleSmall)
        Slider(
            value = tenureMonths.toFloat(),
            onValueChange = { tenureMonths = it.toInt().coerceAtLeast(1) },
            valueRange = 3f..120f
        )
        Spacer(Modifier.height(8.dp))
        MetricLine("Monthly EMI", "₹ ${CalculatorEvaluator.formatNumber(res.monthlyEmi)}", highlight = true)
        MetricLine("Total Interest", "₹ ${CalculatorEvaluator.formatNumber(res.totalInterest)}")
        MetricLine("Total Payment", "₹ ${CalculatorEvaluator.formatNumber(res.totalPayable)}")
        Spacer(Modifier.height(8.dp))
        Text("Principal vs Interest Ratio", style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { res.principalPercent },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MetricLine(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = if (highlight) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
