package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SwapVert
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.R
import com.example.domain.CalculatorEngine
import com.example.domain.ConverterAndFinanceEngine
import com.example.domain.UnitCategory
import com.example.domain.UnitSpec
import com.example.model.CalcRecordCategory
import com.example.ui.components.OceanGlassCard
import com.example.ui.components.OceanTopBar
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalOceanPalette
import com.example.ui.theme.OceanEmeraldColors
import com.example.ui.viewmodel.MainViewModel

@Composable
fun UnitConverterScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val palette = LocalOceanPalette.current

    var category by remember { mutableStateOf(UnitCategory.LENGTH) }
    val units = remember(category) { ConverterAndFinanceEngine.unitCatalog[category].orEmpty() }
    var fromIndex by remember(category) { mutableIntStateOf(2.coerceAtMost((units.size - 1).coerceAtLeast(0))) }
    var toIndex by remember(category) { mutableIntStateOf(3.coerceAtMost((units.size - 1).coerceAtLeast(0))) }
    var inputText by remember { mutableStateOf("1") }

    val fromUnit = units.getOrElse(fromIndex) { units.first() }
    val toUnit = units.getOrElse(toIndex) { units.last() }
    val numericInput = inputText.toDoubleOrNull() ?: 0.0
    val convertedValue = ConverterAndFinanceEngine.convert(category, numericInput, fromUnit, toUnit)
    val convertedFormatted = CalculatorEngine.formatNumber(convertedValue)
    val summaryLine = "$inputText ${fromUnit.symbol} = $convertedFormatted ${toUnit.symbol}"

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
                    title = "Unit Converter",
                    subtitle = "8 categories • Instant offline conversion",
                    onBack = onBack,
                    backTestTag = "btn_back_converter"
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(UnitCategory.entries) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.label) }
                        )
                    }
                }
            }

            item {
                OceanGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevated = true
                ) {
                    Text(
                        text = "From",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily),
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("input_converter_value")
                        )
                        UnitDropdownPicker(
                            selected = fromUnit,
                            options = units,
                            onSelect = { fromIndex = units.indexOf(it) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = {
                                val tmp = fromIndex
                                fromIndex = toIndex
                                toIndex = tmp
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("btn_swap_units")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = stringResource(R.string.cd_swap_units),
                                tint = palette.jamaAccent,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Text(
                        text = "To (${toUnit.label})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = OceanEmeraldColors.EmeraldGreen.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, OceanEmeraldColors.MintGreen.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text(
                                text = convertedFormatted,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = palette.jamaAccent,
                                modifier = Modifier
                                    .padding(horizontal = 14.dp, vertical = 14.dp)
                                    .testTag("text_converter_result")
                            )
                        }
                        UnitDropdownPicker(
                            selected = toUnit,
                            options = units,
                            onSelect = { toIndex = units.indexOf(it) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.copyToClipboard(context, "Conversion", summaryLine) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Copy", maxLines = 1)
                        }
                        Button(
                            onClick = { viewModel.prepareSmsForCalculation("${category.label}: $inputText ${fromUnit.symbol}", "$convertedFormatted ${toUnit.symbol}") },
                            colors = ButtonDefaults.buttonColors(containerColor = OceanEmeraldColors.OceanBlue),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("SMS", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.saveCalculationRecord(
                                    CalcRecordCategory.UNIT,
                                    "$inputText ${fromUnit.symbol} → ${toUnit.symbol}",
                                    "$convertedFormatted ${toUnit.symbol}"
                                )
                                viewModel.emitToast("Saved to history")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Save", maxLines = 1)
                        }
                    }
                }
            }

            // All Unit Equivalents Table
            item {
                Text(
                    text = "Quick Reference for $inputText ${fromUnit.symbol}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(units, key = { it.id }) { target ->
                val v = ConverterAndFinanceEngine.convert(category, numericInput, fromUnit, target)
                OceanGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(target.label, style = MaterialTheme.typography.titleSmall)
                            Text(target.symbol, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = "${CalculatorEngine.formatNumber(v)} ${target.symbol}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = palette.udhaarAccent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UnitDropdownPicker(
    selected: UnitSpec,
    options: List<UnitSpec>,
    onSelect: (UnitSpec) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(
                    text = selected.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = selected.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { item ->
                DropdownMenuItem(
                    text = { Text("${item.label} (${item.symbol})") },
                    onClick = {
                        onSelect(item)
                        expanded = false
                    }
                )
            }
        }
    }
}
