package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.CalcRecordCategory
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
fun HistoryScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val history by viewModel.calcHistory.collectAsStateWithLifecycle()
    val palette = LocalOceanPalette.current

    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<CalcRecordCategory?>(null) }

    val filtered = remember(history, query, selectedCategory) {
        history.filter { item ->
            val matchCat = selectedCategory == null || CalcRecordCategory.fromKey(item.category) == selectedCategory
            val matchQuery = query.isBlank() ||
                item.expression.contains(query, ignoreCase = true) ||
                item.result.contains(query, ignoreCase = true)
            matchCat && matchQuery
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OceanTopBar(
                    title = "Calculation History",
                    subtitle = "${history.size} local records",
                    onBack = onBack,
                    backTestTag = "btn_back_history",
                    trailing = {
                        if (history.isNotEmpty()) {
                            TextButton(onClick = { viewModel.clearCalcHistory() }) {
                                Text("Clear All", color = palette.dangerAccent)
                            }
                        }
                    }
                )
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search expression or result...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All") }
                        )
                    }
                    items(CalcRecordCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.label) }
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    OceanGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = palette.jamaAccent,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("No calculations recorded yet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Tap = in the Calculator or Save in Unit/GST tools to store offline history.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val fmt = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                items(filtered, key = { it.id }) { item ->
                    val cat = CalcRecordCategory.fromKey(item.category)
                    OceanGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (cat == CalcRecordCategory.STANDARD) {
                                viewModel.loadHistoryIntoCalculator(item.expression, item.result)
                            } else {
                                viewModel.copyToClipboard(context, "Result", item.result)
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OceanPillBadge(text = cat.label, accent = OceanEmeraldColors.OceanBlue)
                                    Text(
                                        text = fmt.format(Date(item.createdAtMillis)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = item.expression,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "= ${item.result}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = palette.jamaAccent
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = { viewModel.prepareSmsForCalculation(item.expression, item.result) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Default.Sms, contentDescription = stringResource(R.string.cd_send_sms), tint = OceanEmeraldColors.OceanBlue)
                                }
                                IconButton(
                                    onClick = { viewModel.copyToClipboard(context, "Result", item.result) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.cd_copy_result), tint = palette.jamaAccent)
                                }
                                IconButton(
                                    onClick = { viewModel.deleteCalcHistory(item.id) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.cd_delete_item), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
