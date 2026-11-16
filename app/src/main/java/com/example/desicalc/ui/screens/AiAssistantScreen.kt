package com.example.desicalc.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.desicalc.data.local.AiChatMessageEntity
import com.example.desicalc.model.AiEngineMode
import com.example.desicalc.model.AiUiState
import com.example.desicalc.model.PendingTransactionAction
import com.example.desicalc.model.TransactionType
import com.example.desicalc.model.formatInr
import com.example.desicalc.ui.components.DesiCard
import com.example.desicalc.ui.strings.DesiStrings
import com.example.desicalc.ui.theme.LocalDesiColors
import com.example.desicalc.ui.theme.MonoFontFamily

@Composable
fun AiAssistantScreen(
    messages: List<AiChatMessageEntity>,
    aiUiState: AiUiState,
    engineMode: AiEngineMode,
    strings: DesiStrings,
    decodePendingAction: (String?) -> PendingTransactionAction?,
    onSelectEngineMode: (AiEngineMode) -> Unit,
    onSendMessage: (String) -> Unit,
    onConfirmPendingTransaction: (Long, PendingTransactionAction) -> Unit,
    onClearChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalDesiColors.current
    var input by remember { mutableStateOf("") }

    val quickPrompts = remember {
        listOf(
            "Rahul ko 500 rupaye udhar diye",
            "Rahul se 200 rupaye jama mile",
            "Aaj ka total hisab batao",
            "Kis customer ka sabse zyada baki hai?",
            "Pichhle mahine ki report dikhao",
            "18% GST on 2500"
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("ai_assistant_screen")
    ) {
        // Header & Engine Mode Selector
        DesiCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.img_ai_khata_orb_1794830690327),
                            contentDescription = "Desi AI Khata Orb",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Desi AI Smart System", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Hindi + English • Offline & Gemini AI Logic",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (messages.isNotEmpty()) {
                        IconButton(onClick = onClearChat) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear AI chat")
                        }
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(AiEngineMode.entries) { mode ->
                        FilterChip(
                            selected = engineMode == mode,
                            onClick = { onSelectEngineMode(mode) },
                            label = { Text(mode.label) }
                        )
                    }
                }
            }
        }

        // Quick Bilingual Example Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickPrompts) { prompt ->
                OutlinedButton(
                    onClick = { onSendMessage(prompt) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(prompt, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Chat Transcript
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    DesiCard {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Namaste! Main aapka Desi AI Khata Assistant hoon.", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Try typing or tapping any Hindi/English command above. Financial transactions ALWAYS ask your confirmation before saving, and duplicate entries are flagged automatically.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                val isUser = msg.role == "USER"
                val pending = if (!isUser) decodePendingAction(msg.pendingActionJson) else null
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isUser) MaterialTheme.colorScheme.primaryContainer else ext.cardElevated,
                        tonalElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (!isUser) {
                                Text(
                                    text = if (msg.engineUsed == "GEMINI_AI_LOGIC") "Gemini AI Logic" else "Offline Smart Engine",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )

                            if (pending != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                PendingTransactionConfirmationCard(
                                    action = pending,
                                    confirmed = msg.actionConfirmed,
                                    confirmLabel = strings.confirmAndSave,
                                    onConfirm = { onConfirmPendingTransaction(msg.id, pending) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                AnimatedVisibility(visible = aiUiState is AiUiState.Thinking) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Processing your request...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (aiUiState is AiUiState.Error) {
                    DesiCard(modifier = Modifier.padding(top = 6.dp)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = aiUiState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            OutlinedButton(onClick = { onSelectEngineMode(AiEngineMode.OFFLINE) }) {
                                Text("Switch to Offline Smart Engine")
                            }
                        }
                    }
                }
            }
        }

        // Input Composer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ext.cardElevated)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text(strings.askAiHint) },
                maxLines = 3,
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_prompt_input")
            )
            Button(
                onClick = {
                    if (input.isNotBlank()) {
                        onSendMessage(input)
                        input = ""
                    }
                },
                modifier = Modifier
                    .size(52.dp)
                    .testTag("ai_send_btn"),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send AI prompt")
            }
        }
    }
}

@Composable
private fun PendingTransactionConfirmationCard(
    action: PendingTransactionAction,
    confirmed: Boolean,
    confirmLabel: String,
    onConfirm: () -> Unit
) {
    val ext = LocalDesiColors.current
    val txType = TransactionType.fromCode(action.transactionType)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Transaction Confirmation Required",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text("Customer: ${action.customerName}${if (action.isNewCustomer) " (New Customer)" else ""}")
            Text("Type: ${txType.labelEn}")
            Text(
                text = "Amount: ${formatInr(action.amount)}",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold),
                color = if (txType.isCreditIncrease) ext.udharRed else ext.jamaGreen
            )
            if (action.isPossibleDuplicate && !action.duplicateReason.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Warning, contentDescription = null, tint = ext.goldAccent, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = action.duplicateReason,
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.goldAccent
                    )
                }
            }
            if (confirmed) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = ext.jamaGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Saved to Khata Ledger", style = MaterialTheme.typography.labelLarge, color = ext.jamaGreen)
                }
            } else {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_confirm_tx_btn")
                ) {
                    Text(confirmLabel)
                }
            }
        }
    }
}
