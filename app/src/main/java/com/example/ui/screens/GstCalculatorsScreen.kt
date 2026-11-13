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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.CalculatorEngine
import com.example.domain.ConverterAndFinanceEngine
import com.example.model.CalcRecordCategory
import com.example.ui.components.OceanGlassCard
import com.example.ui.components.OceanTopBar
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalOceanPalette
import com.example.ui.theme.OceanEmeraldColors
import com.example.ui.viewmodel.MainViewModel

private enum class FinanceCalcTab(val label: String) {
    GST("GST"),
    EMI("EMI"),
    DISCOUNT("Discount"),
    INTEREST("Interest")
}

@Composable
fun GstCalculatorsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val currency = prefs.currency.symbol
    var activeTab by remember { mutableStateOf(FinanceCalcTab.GST) }

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
                    title = "GST & Finance Calculators",
                    subtitle = "GST, EMI Loan, Discount & Interest tools",
                    onBack = onBack,
                    backTestTag = "btn_back_gst"
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FinanceCalcTab.entries) { tab ->
                        FilterChip(
                            selected = activeTab == tab,
                            onClick = { activeTab = tab },
                            label = { Text(tab.label) },
                            modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }

            item {
                when (activeTab) {
                    FinanceCalcTab.GST -> GstCalculatorPane(viewModel, currency)
                    FinanceCalcTab.EMI -> EmiCalculatorPane(viewModel, currency)
                    FinanceCalcTab.DISCOUNT -> DiscountCalculatorPane(viewModel, currency)
                    FinanceCalcTab.INTEREST -> InterestCalculatorPane(viewModel, currency)
                }
            }
        }
    }
}

@Composable
private fun GstCalculatorPane(viewModel: MainViewModel, currency: String) {
    val context = LocalContext.current
    val palette = LocalOceanPalette.current
    var amountText by remember { mutableStateOf("1000") }
    var gstRate by remember { mutableDoubleStateOf(18.0) }
    var isInclusive by remember { mutableStateOf(false) }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val res = ConverterAndFinanceEngine.calculateGst(amount, gstRate, isInclusive)
    val summary = "GST ${gstRate.toInt()}% (${if (isInclusive) "Incl" else "Excl"}): Base ${CalculatorEngine.formatCurrency(res.baseAmount, currency)}, GST ${CalculatorEngine.formatCurrency(res.totalGst, currency)}, Total ${CalculatorEngine.formatCurrency(res.totalAmount, currency)}"

    OceanGlassCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            label = { Text("Amount ($currency)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_gst_amount")
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(false to "Exclusive (+GST)", true to "Inclusive (Incl)").forEach { (incl, label) ->
                val selected = isInclusive == incl
                OutlinedButton(
                    onClick = { isInclusive = incl },
                    border = BorderStroke(
                        1.2.dp,
                        if (selected) OceanEmeraldColors.EmeraldGreen else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(label, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("GST Slab", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf(3.0, 5.0, 12.0, 18.0, 28.0)) { slab ->
                FilterChip(
                    selected = gstRate == slab,
                    onClick = { gstRate = slab },
                    label = { Text("${slab.toInt()}%") }
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        ResultRow("Base Price", CalculatorEngine.formatCurrency(res.baseAmount, currency), palette.udhaarAccent)
        ResultRow("CGST (${gstRate / 2}%)", CalculatorEngine.formatCurrency(res.cgst, currency), MaterialTheme.colorScheme.onSurface)
        ResultRow("SGST (${gstRate / 2}%)", CalculatorEngine.formatCurrency(res.sgst, currency), MaterialTheme.colorScheme.onSurface)
        ResultRow("Total GST", CalculatorEngine.formatCurrency(res.totalGst, currency), palette.warningAccent)
        Spacer(Modifier.height(6.dp))
        ResultRow("Final Total", CalculatorEngine.formatCurrency(res.totalAmount, currency), palette.jamaAccent, bold = true)

        Spacer(Modifier.height(12.dp))
        FinanceActionRow(
            onCopy = { viewModel.copyToClipboard(context, "GST Summary", summary) },
            onSms = { viewModel.prepareSmsForCalculation("GST ${gstRate.toInt()}% on $currency$amountText", CalculatorEngine.formatCurrency(res.totalAmount, currency)) },
            onSave = {
                viewModel.saveCalculationRecord(
                    CalcRecordCategory.GST,
                    "$currency$amountText + ${gstRate.toInt()}% GST",
                    CalculatorEngine.formatCurrency(res.totalAmount, currency)
                )
                viewModel.emitToast("GST calculation saved")
            }
        )
    }
}

@Composable
private fun EmiCalculatorPane(viewModel: MainViewModel, currency: String) {
    val context = LocalContext.current
    val palette = LocalOceanPalette.current
    var principalText by remember { mutableStateOf("100000") }
    var rateText by remember { mutableStateOf("10.5") }
    var monthsText by remember { mutableStateOf("12") }

    val principal = principalText.toDoubleOrNull() ?: 0.0
    val rate = rateText.toDoubleOrNull() ?: 0.0
    val months = (monthsText.toIntOrNull() ?: 1).coerceAtLeast(1)
    val res = ConverterAndFinanceEngine.calculateEmi(principal, rate, months)
    val principalRatio = if (res.totalPayable > 0) (principal / res.totalPayable).toFloat().coerceIn(0f, 1f) else 1f

    OceanGlassCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
        OutlinedTextField(
            value = principalText,
            onValueChange = { principalText = it },
            label = { Text("Loan Principal ($currency)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = rateText,
                onValueChange = { rateText = it },
                label = { Text("Interest % / yr") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = monthsText,
                onValueChange = { monthsText = it },
                label = { Text("Months") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(14.dp))
        ResultRow("Monthly EMI", CalculatorEngine.formatCurrency(res.monthlyEmi, currency), palette.jamaAccent, bold = true)
        ResultRow("Total Interest", CalculatorEngine.formatCurrency(res.totalInterest, currency), palette.warningAccent)
        ResultRow("Total Payable", CalculatorEngine.formatCurrency(res.totalPayable, currency), palette.udhaarAccent)
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { principalRatio },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50)),
            color = OceanEmeraldColors.OceanBlue,
            trackColor = OceanEmeraldColors.EmeraldGreen.copy(alpha = 0.45f)
        )
        Spacer(Modifier.height(12.dp))
        FinanceActionRow(
            onCopy = { viewModel.copyToClipboard(context, "EMI", "Monthly EMI: ${CalculatorEngine.formatCurrency(res.monthlyEmi, currency)} for $months mo") },
            onSms = { viewModel.prepareSmsForCalculation("Loan $currency$principalText @ $rateText% ($months mo)", "EMI ${CalculatorEngine.formatCurrency(res.monthlyEmi, currency)}") },
            onSave = {
                viewModel.saveCalculationRecord(
                    CalcRecordCategory.EMI,
                    "$currency$principalText @ $rateText% / $months mo",
                    CalculatorEngine.formatCurrency(res.monthlyEmi, currency)
                )
                viewModel.emitToast("EMI saved to history")
            }
        )
    }
}

@Composable
private fun DiscountCalculatorPane(viewModel: MainViewModel, currency: String) {
    val context = LocalContext.current
    val palette = LocalOceanPalette.current
    var priceText by remember { mutableStateOf("2500") }
    var discountText by remember { mutableStateOf("15") }
    var taxText by remember { mutableStateOf("0") }

    val res = ConverterAndFinanceEngine.calculateDiscount(
        priceText.toDoubleOrNull() ?: 0.0,
        discountText.toDoubleOrNull() ?: 0.0,
        taxText.toDoubleOrNull() ?: 0.0
    )

    OceanGlassCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
        OutlinedTextField(
            value = priceText,
            onValueChange = { priceText = it },
            label = { Text("Original Price ($currency)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = discountText,
                onValueChange = { discountText = it },
                label = { Text("Discount %") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = taxText,
                onValueChange = { taxText = it },
                label = { Text("Extra Tax %") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(14.dp))
        ResultRow("You Save", CalculatorEngine.formatCurrency(res.savings, currency), palette.warningAccent)
        ResultRow("After Discount", CalculatorEngine.formatCurrency(res.priceAfterDiscount, currency), palette.udhaarAccent)
        ResultRow("Final Price", CalculatorEngine.formatCurrency(res.finalPrice, currency), palette.jamaAccent, bold = true)
        Spacer(Modifier.height(12.dp))
        FinanceActionRow(
            onCopy = { viewModel.copyToClipboard(context, "Discount", "Final Price: ${CalculatorEngine.formatCurrency(res.finalPrice, currency)} (Saved ${CalculatorEngine.formatCurrency(res.savings, currency)})") },
            onSms = { viewModel.prepareSmsForCalculation("$currency$priceText - $discountText% off", CalculatorEngine.formatCurrency(res.finalPrice, currency)) },
            onSave = {
                viewModel.saveCalculationRecord(
                    CalcRecordCategory.DISCOUNT,
                    "$currency$priceText - $discountText%",
                    CalculatorEngine.formatCurrency(res.finalPrice, currency)
                )
                viewModel.emitToast("Discount saved")
            }
        )
    }
}

@Composable
private fun InterestCalculatorPane(viewModel: MainViewModel, currency: String) {
    val context = LocalContext.current
    val palette = LocalOceanPalette.current
    var principalText by remember { mutableStateOf("50000") }
    var rateText by remember { mutableStateOf("8") }
    var yearsText by remember { mutableStateOf("2") }
    var isCompound by remember { mutableStateOf(false) }

    val res = ConverterAndFinanceEngine.calculateInterest(
        principalText.toDoubleOrNull() ?: 0.0,
        rateText.toDoubleOrNull() ?: 0.0,
        yearsText.toDoubleOrNull() ?: 0.0,
        isCompound
    )

    OceanGlassCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
        OutlinedTextField(
            value = principalText,
            onValueChange = { principalText = it },
            label = { Text("Principal ($currency)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = rateText,
                onValueChange = { rateText = it },
                label = { Text("Rate % / yr") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = yearsText,
                onValueChange = { yearsText = it },
                label = { Text("Years") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(false to "Simple Interest", true to "Compound").forEach { (comp, label) ->
                OutlinedButton(
                    onClick = { isCompound = comp },
                    border = BorderStroke(
                        1.2.dp,
                        if (isCompound == comp) OceanEmeraldColors.EmeraldGreen else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(label, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        ResultRow("Interest Earned", CalculatorEngine.formatCurrency(res.interestEarned, currency), palette.udhaarAccent)
        ResultRow("Maturity Value", CalculatorEngine.formatCurrency(res.maturityAmount, currency), palette.jamaAccent, bold = true)
        Spacer(Modifier.height(12.dp))
        FinanceActionRow(
            onCopy = { viewModel.copyToClipboard(context, "Interest", "Maturity: ${CalculatorEngine.formatCurrency(res.maturityAmount, currency)}") },
            onSms = { viewModel.prepareSmsForCalculation("$currency$principalText @ $rateText% (${yearsText}y)", CalculatorEngine.formatCurrency(res.maturityAmount, currency)) },
            onSave = {
                viewModel.saveCalculationRecord(
                    CalcRecordCategory.INTEREST,
                    "$currency$principalText @ $rateText% × ${yearsText}y",
                    CalculatorEngine.formatCurrency(res.maturityAmount, currency)
                )
                viewModel.emitToast("Interest saved")
            }
        )
    }
}

@Composable
private fun ResultRow(label: String, value: String, color: androidx.compose.ui.graphics.Color, bold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = (if (bold) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium)
                .copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

@Composable
private fun FinanceActionRow(
    onCopy: () -> Unit,
    onSms: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(onClick = onCopy, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Copy", maxLines = 1)
        }
        Button(
            onClick = onSms,
            colors = ButtonDefaults.buttonColors(containerColor = OceanEmeraldColors.OceanBlue),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("SMS", maxLines = 1)
        }
        OutlinedButton(onClick = onSave, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Save", maxLines = 1)
        }
    }
}
