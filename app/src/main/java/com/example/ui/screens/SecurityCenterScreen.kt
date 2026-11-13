package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.permissions.ManagedPermissionId
import com.example.permissions.PermissionManager
import com.example.security.BiometricAvailability
import com.example.security.BiometricHelper
import com.example.ui.components.GlassCard
import com.example.ui.components.PermissionStatusPill
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SecurityCenterScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val prefs by viewModel.localPrefs.collectAsStateWithLifecycle()
    val rateLimit by viewModel.rateLimitSnapshot.collectAsStateWithLifecycle()
    val history by viewModel.searchHistory.collectAsStateWithLifecycle()
    val glass = LocalGlassPalette.current

    var refreshTick by remember { mutableIntStateOf(0) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.refreshRateLimiter()
    }

    val permissions = remember(prefs, refreshTick) {
        PermissionManager.evaluateAll(context, prefs)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            refreshTick++
            viewModel.emitToast(if (granted) "Permission granted." else "Permission remains disabled.")
        }
    )

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
                modifier = Modifier.testTag("btn_back_security")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
            }
            Spacer(Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Security Center & Privacy",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "App Check HTTPS • AES-256 Vault • Rate Limiter • Permissions",
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
            // Privacy Dashboard
            item {
                GlassCard(elevated = true) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.EnhancedEncryption, contentDescription = null, tint = glass.safeAccent)
                        Text(
                            text = "Privacy Dashboard",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("• Transport: HTTPS + Firebase App Check Attestation", style = MaterialTheme.typography.bodySmall)
                    Text("• Local Storage: Android Keystore AES-256-GCM (${history.size} records)", style = MaterialTheme.typography.bodySmall)
                    Text("• Account Policy: Guest Mode active — zero mandatory login", style = MaterialTheme.typography.bodySmall)
                    Text("• Data Isolation: Zero auto-upload of contacts or photos", style = MaterialTheme.typography.bodySmall)

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Save Local Search History", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Encrypt searches locally in Room DB",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = prefs.saveSearchHistory,
                            onCheckedChange = viewModel::setSaveSearchHistory
                        )
                    }
                }
            }

            // Rate Limiting & Abuse Prevention
            item {
                GlassCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Rate Limiting & Abuse Shield",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    val ratio = (rateLimit.requestsInLastMinute.toFloat() / rateLimit.maxPerMinute.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${rateLimit.requestsInLastMinute} / ${rateLimit.maxPerMinute} AI requests used this minute • Session total: ${rateLimit.totalRequestsThisSession}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Optional PIN & Biometric Vault Lock
            item {
                GlassCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Optional Local Vault Lock",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (prefs.pinLockEnabled) "4-digit PIN vault lock is active." else "Guest Mode is open without PIN lock.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { showPinDialog = true },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(if (prefs.pinLockEnabled) "Change PIN" else "Set 4-Digit PIN")
                        }
                        if (prefs.pinLockEnabled) {
                            OutlinedButton(
                                onClick = viewModel::disablePinCode,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Disable Lock")
                            }
                        }
                    }

                    if (prefs.pinLockEnabled) {
                        Spacer(Modifier.height(10.dp))
                        val bioAvailable = BiometricHelper.checkAvailability(context) == BiometricAvailability.AVAILABLE
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (bioAvailable) "Biometric Fingerprint / Face" else "Biometric unavailable on device",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(
                                checked = prefs.biometricLockEnabled,
                                enabled = bioAvailable,
                                onCheckedChange = viewModel::setBiometricEnabled
                            )
                        }
                    }
                }
            }

            // Permission Manager
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Permission Manager",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            items(permissions, key = { it.id.name }) { item ->
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        PermissionStatusPill(state = item.state)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(item.explanation, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.statusNote,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (item.id == ManagedPermissionId.CAMERA) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Allow Camera Capture", style = MaterialTheme.typography.bodySmall)
                            Switch(
                                checked = prefs.cameraScanFeatureEnabled,
                                onCheckedChange = {
                                    viewModel.setCameraScanEnabled(it)
                                    refreshTick++
                                }
                            )
                        }
                    }

                    if (item.canRequestInApp && item.androidPermission != null) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { permissionLauncher.launch(item.androidPermission) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Grant Permission")
                            }
                            OutlinedButton(
                                onClick = { PermissionManager.openAppSystemSettings(context) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("System Settings")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Set 4-Digit Vault PIN") },
            text = {
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) pinInput = it },
                    label = { Text("4-digit PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
            },
            confirmButton = {
                Button(
                    enabled = pinInput.length == 4,
                    onClick = {
                        viewModel.setPinCode(pinInput)
                        pinInput = ""
                        showPinDialog = false
                    }
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pinInput = ""
                    showPinDialog = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
