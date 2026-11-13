package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.HistoryRecordType
import com.example.model.SearchHistoryRecord
import com.example.ui.components.GlassCard
import com.example.ui.components.RiskBadgePill
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SearchHistoryScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val records by viewModel.searchHistory.collectAsStateWithLifecycle()
    var searchFilter by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<HistoryRecordType?>(null) }

    val filtered = remember(records, searchFilter, selectedType) {
        records.filter { rec ->
            val typeMatch = selectedType == null || rec.recordType == selectedType
            val textMatch = searchFilter.isBlank() ||
                rec.title.contains(searchFilter, ignoreCase = true) ||
                rec.subtitle.contains(searchFilter, ignoreCase = true) ||
                rec.summaryText.contains(searchFilter, ignoreCase = true)
            typeMatch && textMatch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 920.dp)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("btn_back_history")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
            }
            Spacer(Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Encrypted Search History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "AES-256-GCM Local Vault • User-Controlled Deletion & PDF Export",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (filtered.isNotEmpty()) {
                IconButton(
                    onClick = {
                        viewModel.exportAndSharePdf(context, "Search History Security Export", filtered)
                    },
                    modifier = Modifier.testTag("btn_export_all_pdf")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = stringResource(R.string.cd_export_pdf))
                }
                IconButton(
                    onClick = viewModel::clearAllHistory,
                    modifier = Modifier.testTag("btn_clear_all_history")
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear all history")
                }
            }
        }

        OutlinedTextField(
            value = searchFilter,
            onValueChange = { searchFilter = it },
            placeholder = { Text("Search encrypted history...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedType == null,
                    onClick = { selectedType = null },
                    label = { Text("All (${records.size})") }
                )
            }
            items(HistoryRecordType.entries) { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    label = { Text(type.label) }
                )
            }
        }

        if (filtered.isEmpty()) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "No saved search records yet. Your lookups, photo scans, and spam checks are encrypted locally on-device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { it.id }) { record ->
                    HistoryRecordCard(
                        record = record,
                        onExportPdf = {
                            viewModel.exportAndSharePdf(context, "Record: ${record.title}", listOf(record))
                        },
                        onDelete = { viewModel.deleteHistoryRecord(record.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRecordCard(
    record: SearchHistoryRecord,
    onExportPdf: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(record.createdAtMillis) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ENGLISH).format(Date(record.createdAtMillis))
    }
    GlassCard(elevated = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "[${record.recordType.label}] ${record.title}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (record.subtitle.isNotBlank()) {
                    Text(
                        text = record.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            RiskBadgePill(riskLevel = record.riskLevel)
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = record.summaryText,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 4
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "$dateStr • ${record.sourceAttribution}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onExportPdf) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("PDF / Share")
            }
            TextButton(
                onClick = onDelete,
                modifier = Modifier.testTag("btn_delete_${record.id}")
            ) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.cd_delete_record), tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(4.dp))
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
