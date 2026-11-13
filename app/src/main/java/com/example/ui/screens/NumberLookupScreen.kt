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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ai.PhoneValidatorEngine
import com.example.model.AsyncUiState
import com.example.model.NumberLookupReport
import com.example.ui.components.AiRadarLoadingCard
import com.example.ui.components.GlassCard
import com.example.ui.components.RiskBadgePill
import com.example.ui.components.RiskMeterBar
import com.example.ui.components.SourceAttributionBanner
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NumberLookupScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val query by viewModel.lookupQuery.collectAsStateWithLifecycle()
    val selectedRegion by viewModel.selectedRegion.collectAsStateWithLifecycle()
    val businessHint by viewModel.businessHint.collectAsStateWithLifecycle()
    val isBusinessMode by viewModel.isBusinessLookupMode.collectAsStateWithLifecycle()
    val lookupState by viewModel.lookupState.collectAsStateWithLifecycle()
    val glass = LocalGlassPalette.current

    val liveValidation = remember(query, selectedRegion) {
        if (query.isBlank()) null else PhoneValidatorEngine.validate(query, selectedRegion)
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
                modifier = Modifier.testTag("btn_back_lookup")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back)
                )
            }
            Spacer(Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isBusinessMode) "Public Business Lookup" else "AI Phone Number Lookup",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ITU-T E.164 Validation + Privacy-Safe AI Risk Engine",
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
            // Mode Switcher & Input Card
            item {
                GlassCard(elevated = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isBusinessMode,
                            onClick = { viewModel.setBusinessMode(false) },
                            label = { Text("Phone Lookup") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = isBusinessMode,
                            onClick = { viewModel.setBusinessMode(true) },
                            label = { Text("Business Verify") },
                            leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Default Region / Calling Code",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PhoneValidatorEngine.popularCountries) { option ->
                            FilterChip(
                                selected = selectedRegion == option.regionCode,
                                onClick = { viewModel.updateSelectedRegion(option.regionCode) },
                                label = { Text("${option.regionCode} (${option.callingCode})") }
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = query,
                        onValueChange = viewModel::updateLookupQuery,
                        label = { Text("Enter international or local phone number") },
                        placeholder = { Text("e.g. +91 98110 12345 or 1930") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = { viewModel.lookupPhoneNumber() }
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_phone_number")
                    )

                    if (isBusinessMode) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = businessHint,
                            onValueChange = viewModel::updateBusinessHint,
                            label = { Text("Optional claimed business name") },
                            placeholder = { Text("e.g. SBI Customer Care, Courier Desk") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_business_hint")
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssistChip(
                            onClick = { viewModel.lookupPhoneNumber("1930", isBusinessMode) },
                            label = { Text("Try 1930 (Cyber Helpline)") }
                        )
                        AssistChip(
                            onClick = { viewModel.lookupPhoneNumber("112", isBusinessMode) },
                            label = { Text("Try 112 (Emergency)") }
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.lookupPhoneNumber() },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_run_lookup")
                    ) {
                        Text(
                            text = if (isBusinessMode) "Verify Public Business" else "Analyze Number with AI",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Live On-Device Format & Country Detection Preview
            if (liveValidation != null) {
                item {
                    GlassCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (liveValidation.isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (liveValidation.isValid) glass.safeAccent else glass.warningAccent
                            )
                            Text(
                                text = if (liveValidation.isValid) {
                                    "Valid ${liveValidation.countryName} (${liveValidation.countryCallingCode}) • ${liveValidation.numberType}"
                                } else {
                                    "Format Check: ${liveValidation.numberType} (${liveValidation.countryName})"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "E.164: ${liveValidation.e164Format}   •   National: ${liveValidation.nationalFormat}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Async AI Result State
            when (val state = lookupState) {
                is AsyncUiState.Idle -> {
                    item {
                        GlassCard {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Enter any phone number to validate country, calling code, ITU number type, public caller/business status, and fraud risk. Private personal info is never fabricated.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                is AsyncUiState.Loading -> {
                    item {
                        AiRadarLoadingCard(message = state.stepMessage)
                    }
                }

                is AsyncUiState.Error -> {
                    item {
                        GlassCard {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                is AsyncUiState.Success -> {
                    item {
                        NumberLookupResultCard(
                            report = state.data,
                            onExportPdf = { viewModel.exportCurrentLookupPdf(context, state.data) },
                            onExplainWithAi = {
                                viewModel.openAssistantWithContext(
                                    prompt = "Explain this phone number verification result in detail (both English and Hindi) and tell me if it is safe.",
                                    contextText = "Number: ${state.data.validation.internationalFormat}, Entity: ${state.data.publicCallerOrBusinessName}, Risk: ${state.data.riskLevel} (${state.data.spamRiskScore}/100), Carrier Note: ${state.data.carrierNetworkNote}"
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberLookupResultCard(
    report: NumberLookupReport,
    onExportPdf: () -> Unit,
    onExplainWithAi: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassCard(elevated = true) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.validation.internationalFormat,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${report.validation.countryName} (${report.validation.countryCallingCode}) • ${report.validation.numberType}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                RiskBadgePill(riskLevel = report.riskLevel, score = report.spamRiskScore)
            }

            Spacer(Modifier.height(14.dp))
            RiskMeterBar(score = report.spamRiskScore, riskLevel = report.riskLevel)
            Spacer(Modifier.height(14.dp))

            Text(
                text = "Public Caller / Business ID",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = report.publicCallerOrBusinessName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Category: ${report.entityCategory}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Network & Carrier Notice",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = report.carrierNetworkNote,
                style = MaterialTheme.typography.bodyMedium
            )

            if (report.summaryEn.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "AI Assessment (English)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(text = report.summaryEn, style = MaterialTheme.typography.bodyMedium)
            }

            if (report.summaryHi.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "AI सारांश (हिन्दी)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(text = report.summaryHi, style = MaterialTheme.typography.bodyMedium)
            }

            if (report.fraudIndicators.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Risk & Verification Observations",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                report.fraudIndicators.forEach { item ->
                    Text("• $item", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (report.safetyRecommendations.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Safety Recommendations",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                report.safetyRecommendations.forEach { rec ->
                    Text("✓ $rec", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onExportPdf,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("PDF Report")
                }
                Button(
                    onClick = onExplainWithAi,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.SmartToy, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Ask AI")
                }
            }
        }

        SourceAttributionBanner(
            confidenceLevel = report.confidenceLevel,
            sourceAttribution = report.sourceAttribution,
            freshness = report.dataFreshness
        )
    }
}
