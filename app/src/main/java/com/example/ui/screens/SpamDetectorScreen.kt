package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.AsyncUiState
import com.example.ui.components.AiRadarLoadingCard
import com.example.ui.components.GlassCard
import com.example.ui.components.RiskBadgePill
import com.example.ui.components.RiskMeterBar
import com.example.ui.components.SourceAttributionBanner
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpamDetectorScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val spamInput by viewModel.spamInput.collectAsStateWithLifecycle()
    val spamState by viewModel.spamState.collectAsStateWithLifecycle()

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
                modifier = Modifier.testTag("btn_back_spam")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
            }
            Spacer(Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI Spam & Fraud Detector",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Analyze suspicious SMS, WhatsApp messages, links & caller claims",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                GlassCard(elevated = true) {
                    OutlinedTextField(
                        value = spamInput,
                        onValueChange = viewModel::updateSpamInput,
                        label = { Text("Paste suspicious SMS, message, or link") },
                        placeholder = { Text("e.g. 'Dear customer your KYC is pending, click link within 2 hours or account blocked'") },
                        minLines = 4,
                        maxLines = 7,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_spam_text")
                    )

                    Spacer(Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                viewModel.updateSpamInput(
                                    "URGENT: Your bank KYC has expired today. Click http://kyc-verify-now.example to avoid account freeze."
                                )
                            },
                            label = { Text("Test KYC Phishing") }
                        )
                        AssistChip(
                            onClick = {
                                viewModel.updateSpamInput(
                                    "TRAI Notice: Your mobile number will be disconnected in 2 hours due to illegal activity. Press 9 to speak to officer."
                                )
                            },
                            label = { Text("Test Digital Arrest") }
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.analyzeSpamMessage() },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_analyze_spam")
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Detect Fraud Risk with AI", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            when (val state = spamState) {
                is AsyncUiState.Idle -> {
                    item {
                        GlassCard {
                            Text(
                                text = "Paste any unknown message or caller script. The AI engine highlights red flags, explains the scam tactic in English & Hindi, and recommends official reporting steps (1930 / Chakshu).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is AsyncUiState.Loading -> {
                    item { AiRadarLoadingCard(message = state.stepMessage) }
                }

                is AsyncUiState.Error -> {
                    item {
                        GlassCard {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                                Text(state.message, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                is AsyncUiState.Success -> {
                    val report = state.data
                    item {
                        GlassCard(elevated = true) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = report.scamCategory,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                RiskBadgePill(riskLevel = report.riskLevel, score = report.spamRiskScore)
                            }

                            Spacer(Modifier.height(12.dp))
                            RiskMeterBar(score = report.spamRiskScore, riskLevel = report.riskLevel)
                            Spacer(Modifier.height(12.dp))

                            if (report.redFlags.isNotEmpty()) {
                                Text(
                                    text = "Detected Red Flags",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                report.redFlags.forEach { flag ->
                                    Text("• $flag", style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(Modifier.height(10.dp))
                            }

                            Text(
                                text = "Explanation (English)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(report.explanationEn, style = MaterialTheme.typography.bodyMedium)

                            if (report.explanationHi.isNotBlank()) {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = "स्पष्टीकरण (हिन्दी)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(report.explanationHi, style = MaterialTheme.typography.bodyMedium)
                            }

                            if (report.recommendedActions.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "Recommended Defensive Actions",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                report.recommendedActions.forEach { action ->
                                    Text("✓ $action", style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    viewModel.openAssistantWithContext(
                                        prompt = "Generate a concise security report for this suspicious message in English and Hindi.",
                                        contextText = "Category: ${report.scamCategory}, Risk: ${report.riskLevel} (${report.spamRiskScore}/100)\nMessage: ${report.inputPreview}"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.SmartToy, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Generate Report in AI Assistant")
                            }
                        }
                    }

                    item {
                        SourceAttributionBanner(
                            confidenceLevel = report.confidenceLevel,
                            sourceAttribution = report.sourceAttribution
                        )
                    }
                }
            }
        }
    }
}
