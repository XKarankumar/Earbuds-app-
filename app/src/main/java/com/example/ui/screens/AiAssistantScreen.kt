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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.model.AppLanguage
import com.example.model.ChatMessage
import com.example.model.HistoryRecordType
import com.example.model.SearchHistoryRecord
import com.example.ui.components.GlassCard
import com.example.ui.viewmodel.MainViewModel

@Composable
fun AiAssistantScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isStreaming by viewModel.isChatStreaming.collectAsStateWithLifecycle()
    val prefs by viewModel.localPrefs.collectAsStateWithLifecycle()
    val attachedContext by viewModel.attachedContextSummary.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 920.dp)
            .padding(horizontal = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("btn_back_assistant")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
            }
            Spacer(Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Karan AI Assistant",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Hindi & English Security Copilot • Streaming AI",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = {
                    val lastAi = messages.lastOrNull { !it.isUser }
                    if (lastAi != null) {
                        val rec = SearchHistoryRecord(
                            id = "ai_report",
                            recordType = HistoryRecordType.SPAM,
                            title = "AI Security Assistant Report",
                            subtitle = "Language: ${prefs.language.label}",
                            riskLevel = "Info",
                            riskScore = 0,
                            summaryText = lastAi.text,
                            sourceAttribution = lastAi.sourceTag ?: "Firebase AI Logic",
                            createdAtMillis = lastAi.timestampMillis
                        )
                        viewModel.exportAndSharePdf(context, "AI Assistant Security Report", listOf(rec))
                    }
                }
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = stringResource(R.string.cd_export_pdf))
            }
        }

        // Language chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            items(AppLanguage.entries) { lang ->
                FilterChip(
                    selected = prefs.language == lang,
                    onClick = { viewModel.setLanguage(lang) },
                    label = { Text(lang.label) }
                )
            }
        }

        // Quick Bilingual Prompts
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            item {
                AssistChip(
                    onClick = {
                        viewModel.sendChatMessage("Explain my latest search result clearly and tell me any safety risks.")
                    },
                    label = { Text("Explain Last Result") }
                )
            }
            item {
                AssistChip(
                    onClick = {
                        viewModel.sendChatMessage("फ़र्ज़ी KYC या डिजिटल अरेस्ट कॉल की पहचान कैसे करें? हिन्दी में बताएं।")
                    },
                    label = { Text("हिन्दी: स्कैम पहचान") }
                )
            }
            item {
                AssistChip(
                    onClick = {
                        viewModel.sendChatMessage("Generate a structured Security Report with confidence level and reporting steps (1930 / Chakshu).")
                    },
                    label = { Text("Generate Security Report") }
                )
            }
        }

        if (!attachedContext.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    text = "Context Attached: ${attachedContext!!.lineSequence().firstOrNull().orEmpty()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Chat Feed
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubble(msg = msg)
            }
        }

        // Input Composer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Ask in English or हिन्दी...") },
                maxLines = 3,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("input_chat_message")
            )
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (isStreaming) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary
            ) {
                IconButton(
                    enabled = !isStreaming && inputText.isNotBlank(),
                    onClick = {
                        val toSend = inputText
                        inputText = ""
                        viewModel.sendChatMessage(toSend)
                    },
                    modifier = Modifier.testTag("btn_send_chat")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send message",
                        tint = if (isStreaming) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(msg: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        GlassCard(
            elevated = !msg.isUser,
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(14.dp),
            modifier = Modifier.widthIn(max = 560.dp)
        ) {
            Text(
                text = if (msg.isStreaming && msg.text.isBlank()) "Thinking..." else msg.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!msg.isUser && (!msg.confidenceTag.isNullOrBlank() || !msg.sourceTag.isNullOrBlank())) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = listOfNotNull(msg.confidenceTag, msg.sourceTag).joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
