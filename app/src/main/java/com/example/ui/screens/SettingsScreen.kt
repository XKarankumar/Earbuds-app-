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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ThemeMode
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPillButton
import com.example.ui.components.GlassTopHeader
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val palette = LocalGlassPalette.current
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()

    var gymName by remember(prefs.gymName) { mutableStateOf(prefs.gymName) }
    var ownerName by remember(prefs.ownerSignature) { mutableStateOf(prefs.ownerSignature) }
    var deadlineHour by remember(prefs.attendanceDeadlineHour) { mutableStateOf(prefs.attendanceDeadlineHour.toString()) }
    var deadlineMin by remember(prefs.attendanceDeadlineMinute) { mutableStateOf(prefs.attendanceDeadlineMinute.toString()) }
    var smsEnabled by remember(prefs.smsGatewayEnabled) { mutableStateOf(prefs.smsGatewayEnabled) }
    var smsUrl by remember(prefs.smsGatewayUrl) { mutableStateOf(prefs.smsGatewayUrl) }
    var smsApiKey by remember { mutableStateOf("") }
    var smsSenderId by remember(prefs.smsSenderId) { mutableStateOf(prefs.smsSenderId) }
    var tokenBackendUrl by remember(prefs.tokenBackendUrl) { mutableStateOf(prefs.tokenBackendUrl) }
    var leaseHours by remember(prefs.offlineLeaseHours) { mutableStateOf(prefs.offlineLeaseHours.toString()) }

    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showWipeConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().widthIn(max = 680.dp)) {
        GlassTopHeader(
            title = "Settings & Security",
            subtitle = "Theme, HTTPS Gateways, Privacy & Data Deletion",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GlassCard(elevated = true) {
                    Text("Appearance & Glass Theme", style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = prefs.themeMode == ThemeMode.AMOLED_DARK,
                            onClick = { viewModel.setThemeMode(ThemeMode.AMOLED_DARK) },
                            leadingIcon = { Icon(Icons.Filled.DarkMode, contentDescription = null) },
                            label = { Text("AMOLED Black") },
                            modifier = Modifier.testTag("theme_amoled_chip")
                        )
                        FilterChip(
                            selected = prefs.themeMode == ThemeMode.LIGHT_GLASS,
                            onClick = { viewModel.setThemeMode(ThemeMode.LIGHT_GLASS) },
                            leadingIcon = { Icon(Icons.Filled.LightMode, contentDescription = null) },
                            label = { Text("Light Glass") },
                            modifier = Modifier.testTag("theme_light_chip")
                        )
                    }
                }
            }

            item {
                GlassCard(elevated = true) {
                    Text("Gym & Missed Attendance Deadline", style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = gymName, onValueChange = { gymName = it }, label = { Text("Gym Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text("Owner / Receipt Signature") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = deadlineHour,
                            onValueChange = { deadlineHour = it },
                            label = { Text("Deadline Hour (0–23)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = deadlineMin,
                            onValueChange = { deadlineMin = it },
                            label = { Text("Minute (0–59)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }

            item {
                GlassCard(elevated = true, accentBorder = palette.accentCyan) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Authorized HTTPS SMS Gateway", style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
                        Switch(checked = smsEnabled, onCheckedChange = { smsEnabled = it })
                    }
                    Text(
                        "HTTPS-only endpoint. API Key is encrypted with AndroidKeyStore AES-256-GCM.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = smsUrl,
                        onValueChange = { smsUrl = it },
                        label = { Text("SMS Gateway HTTPS URL") },
                        placeholder = { Text("https://api.yourgateway.com/v1/sms") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = smsApiKey,
                        onValueChange = { smsApiKey = it },
                        label = { Text("Gateway API Key / Bearer Token (leave blank to keep existing)") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = smsSenderId,
                        onValueChange = { smsSenderId = it },
                        label = { Text("Sender ID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            item {
                GlassCard(elevated = true) {
                    Text("Token Backend & Offline Lease", style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tokenBackendUrl,
                        onValueChange = { tokenBackendUrl = it },
                        label = { Text("Token Verification HTTPS URL (optional)") },
                        placeholder = { Text("https://auth.yourdomain.com/verify") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = leaseHours,
                        onValueChange = { leaseHours = it },
                        label = { Text("Offline Cache Lease TTL (1–72 hours)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    GlassPillButton(
                        text = "Save All Settings",
                        icon = Icons.Filled.Save,
                        onClick = {
                            viewModel.saveGatewayAndGymSettings(
                                gymName = gymName,
                                owner = ownerName,
                                deadlineHour = deadlineHour.toIntOrNull() ?: 21,
                                deadlineMinute = deadlineMin.toIntOrNull() ?: 0,
                                smsEnabled = smsEnabled,
                                smsUrl = smsUrl,
                                smsApiKeyPlain = smsApiKey,
                                smsSenderId = smsSenderId,
                                tokenBackendUrl = tokenBackendUrl,
                                leaseHours = leaseHours.toIntOrNull() ?: 24
                            )
                            smsApiKey = ""
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_settings_btn")
                    )
                }
            }

            item {
                GlassCard(accentBorder = palette.accentDanger) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.PrivacyTip, contentDescription = null, tint = palette.accentCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Privacy Policy & Data Deletion", style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassPillButton(
                            text = "Privacy Policy",
                            filled = false,
                            onClick = { showPrivacyDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        GlassPillButton(
                            text = "Delete All Data",
                            icon = Icons.Filled.DeleteForever,
                            accent = palette.accentDanger,
                            onClick = { showWipeConfirm = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                GlassCard {
                    Text("FITNESS HUB • Version 1.0", style = MaterialTheme.typography.titleSmall, color = palette.primaryText)
                    Text("Developer: Hardik Kumar", style = MaterialTheme.typography.bodySmall, color = palette.accentCyan)
                    Text("Native Android • Kotlin • Jetpack Compose • Room • AndroidKeyStore AES-256-GCM", style = MaterialTheme.typography.labelSmall, color = palette.secondaryText)
                }
            }
        }
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Fitness Hub Privacy Policy") },
            text = {
                Text(
                    "1. All sensitive member phone numbers, emergency contacts, SMS gateway API keys, and offline token leases are encrypted on-device using AndroidKeyStore AES-256-GCM.\n\n" +
                        "2. Zero tracking, zero spyware, and zero cleartext HTTP traffic.\n\n" +
                        "3. Camera permission is requested strictly for QR Check-In scanning, and notifications strictly for user-enabled workout reminders.\n\n" +
                        "4. You may permanently erase all local personal data anytime via 'Delete All Data'."
                )
            },
            confirmButton = { TextButton(onClick = { showPrivacyDialog = false }) { Text("Close") } }
        )
    }

    if (showWipeConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm = false },
            title = { Text("Erase All Local Data?") },
            text = { Text("This permanently deletes all members, attendance records, payments, workouts, diet logs, and access tokens on this device.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.wipeAllPersonalData()
                    showWipeConfirm = false
                }) { Text("Erase Everything", color = palette.accentDanger) }
            },
            dismissButton = { TextButton(onClick = { showWipeConfirm = false }) { Text("Cancel") } }
        )
    }
}
