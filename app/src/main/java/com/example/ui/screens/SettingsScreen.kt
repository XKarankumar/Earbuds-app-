package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.AppLanguage
import com.example.model.AppThemeId
import com.example.ui.components.GlassCard
import com.example.ui.theme.AppThemes
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val prefs by viewModel.localPrefs.collectAsStateWithLifecycle()
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showApiDocsDialog by remember { mutableStateOf(false) }

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
                modifier = Modifier.testTag("btn_back_settings")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
            }
            Spacer(Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Settings & Documentation",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Dark 3D Glass • White Glass • Privacy Policy • API Docs",
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
            // Theme Selector
            item {
                GlassCard(elevated = true) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Glass Interface Theme",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppThemeId.entries.forEach { themeId ->
                            val selected = prefs.themeId == themeId
                            val palette = AppThemes.glassPaletteFor(themeId)
                            GlassCard(
                                onClick = { viewModel.setTheme(themeId) },
                                contentPadding = PaddingValues(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("theme_${themeId.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            palette.swatchColors.forEach { color ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(color)
                                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = themeId.displayName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = themeId.subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected theme",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Glass Translucency Intensity (${(prefs.glassIntensity * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Slider(
                        value = prefs.glassIntensity,
                        onValueChange = viewModel::setGlassIntensity,
                        valueRange = 0.35f..1.0f
                    )
                }
            }

            // Language Preference
            item {
                GlassCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "AI Assistant Language",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AppLanguage.entries) { lang ->
                            FilterChip(
                                selected = prefs.language == lang,
                                onClick = { viewModel.setLanguage(lang) },
                                label = { Text(lang.label) }
                            )
                        }
                    }
                }
            }

            // Policy & API Documentation
            item {
                GlassCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Policy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Legal, Privacy & API Setup Docs",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showPrivacyDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Policy, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Privacy Policy")
                        }
                        OutlinedButton(
                            onClick = { showApiDocsDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Api, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("API Docs")
                        }
                    }
                }
            }

            // Developer Branding
            item {
                GlassCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text(
                                text = "Karan Number Finder Pro AI",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Version 1.0 • Developer: Hardik Kumar",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.brand_copyright),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy (DPDP Act 2023 & Play Compliant)") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = """
                            Developer: Hardik Kumar
                            App: Karan Number Finder Pro AI (v1.0)

                            1. Zero Mandatory Login: Guest Mode works immediately with no Google account required.
                            2. No Private Data Exposure: The app never reveals or fabricates private home addresses, Aadhaar details, bank details, private photographs, or live GPS locations.
                            3. Zero Background Harvesting: Contacts and SMS inbox permissions are never declared or accessed. Photos are scanned only on explicit user action via Android Photo Picker or permission-gated Camera.
                            4. Local Encryption: Saved search history is encrypted locally using Android Keystore AES-256-GCM inside Room DB and can be deleted individually or cleared at any time.
                            5. HTTPS & App Check: AI requests travel over encrypted HTTPS protected by Firebase App Check attestation.
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text("Close") }
            }
        )
    }

    if (showApiDocsDialog) {
        AlertDialog(
            onDismissRequest = { showApiDocsDialog = false },
            title = { Text("API & Service Requirements") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = """
                            Active Real Integrations:
                            • Google libphonenumber (Offline ITU-T E.164 validation, country/region, calling code & number type).
                            • Google ML Kit Text Recognition (On-device OCR text & phone extraction).
                            • Firebase AI Logic + App Check (Gemini 3.8 Flash HTTPS inference for fraud analysis, multimodal photo description, and Hindi/English AI Assistant).
                            • Room + Android Keystore AES-256-GCM (Encrypted local search history).

                            Features Requiring Paid / External Telecom APIs:
                            • Real-time HLR/MNP subscriber carrier status and licensed commercial caller-ID directories require an authorized paid API subscription (e.g., Twilio Lookup v2, Numverify, or Truecaller Enterprise) hosted behind your backend HTTPS proxy.
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showApiDocsDialog = false }) { Text("Close") }
            }
        )
    }
}
