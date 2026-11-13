package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.export.ReportExporter
import com.example.model.AppThemeId
import com.example.permissions.ManagedPermissionId
import com.example.permissions.ManagedPermissionItem
import com.example.permissions.PermissionManager
import com.example.permissions.PermissionState
import com.example.security.BiometricAvailability
import com.example.security.BiometricHelper
import com.example.ui.components.GlassCard
import com.example.ui.components.PermissionStatusPill
import com.example.ui.theme.AppThemes
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.UiState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class BackupPassphraseAction {
    EXPORT_SAF,
    CREATE_SNAPSHOT
}

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val prefs by viewModel.localPrefs.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val customersState by viewModel.customersState.collectAsStateWithLifecycle()
    val transactionsState by viewModel.transactionsState.collectAsStateWithLifecycle()
    val localSnapshots by viewModel.localSnapshots.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshLocalSnapshots(context)
    }

    var permissionRefreshTick by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionRefreshTick++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissions = remember(prefs, permissionRefreshTick) {
        PermissionManager.evaluateAll(context, prefs)
    }

    val runtimePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionRefreshTick++
        viewModel.emitMessage(
            if (granted) "Permission granted!" else "Permission not granted. You can enable it in System Settings."
        )
    }

    var businessNameInput by remember(profile?.businessName) {
        mutableStateOf(profile?.businessName ?: "Hardik Kumar Enterprises")
    }
    var businessPhoneInput by remember(profile?.phone) {
        mutableStateOf(profile?.phone ?: "")
    }

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var passphraseDialogAction by remember { mutableStateOf<BackupPassphraseAction?>(null) }
    var pendingRestoreEncryptedText by remember { mutableStateOf<String?>(null) }

    val customers = (customersState as? UiState.Success)?.data.orEmpty()
    val transactions = (transactionsState as? UiState.Success)?.data.orEmpty()

    var pendingExportPassphrase by remember { mutableStateOf("") }
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null && pendingExportPassphrase.isNotEmpty()) {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                ReportExporter.exportEncryptedBackup(out, pendingExportPassphrase, customers, transactions)
            }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            viewModel.markExternalBackupSaved("SimpleSmsCalc_$stamp.sscbak")
            pendingExportPassphrase = ""
        }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (!content.isNullOrBlank()) {
                pendingRestoreEncryptedText = content
            } else {
                viewModel.emitMessage("Selected backup file is empty")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Settings & Permission Manager", style = MaterialTheme.typography.headlineSmall)

        // 1. Permission Manager Dashboard
        PermissionManagerDashboard(
            permissions = permissions,
            onToggleFeature = { id, enabled ->
                when (id) {
                    ManagedPermissionId.SEND_SMS -> viewModel.setDirectSmsEnabled(enabled)
                    ManagedPermissionId.READ_CONTACTS -> viewModel.setContactsEnabled(enabled)
                    ManagedPermissionId.CAMERA -> viewModel.setCameraScanEnabled(enabled)
                    ManagedPermissionId.POST_NOTIFICATIONS -> viewModel.setRemindersEnabled(enabled)
                    ManagedPermissionId.BIOMETRIC -> viewModel.setBiometricEnabled(enabled)
                    else -> Unit
                }
            },
            onRequestPermission = { permString ->
                runtimePermissionLauncher.launch(permString)
            },
            onOpenSystemSettings = { PermissionManager.openAppSystemSettings(context) }
        )

        // 2. Five Premium Themes + Glassmorphism Intensity
        GlassCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("5 Premium Themes & Glassmorphism", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppThemeId.entries.forEach { theme ->
                    val selected = prefs.themeId == theme
                    val palette = AppThemes.glassPaletteFor(theme)
                    Column(
                        modifier = Modifier
                            .width(142.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { viewModel.setTheme(theme) }
                            .padding(12.dp)
                            .testTag("theme_option_${theme.name}")
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            palette.swatchColors.forEach { c ->
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(theme.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = theme.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "Glass Opacity: ${(prefs.glassIntensity * 100).toInt()}%",
                style = MaterialTheme.typography.titleSmall
            )
            Slider(
                value = prefs.glassIntensity,
                onValueChange = viewModel::setGlassIntensity,
                valueRange = 0.35f..1.0f
            )
        }

        // 3. Optional App PIN Lock (+ Optional Biometric)
        GlassCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Optional App PIN Security", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Optional 4-Digit Vault PIN", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = if (prefs.pinLockEnabled) "Enabled (PBKDF2 + Android Keystore AES-256 protected)"
                        else "Off by default — enable anytime to lock app with a 4-digit PIN",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = prefs.pinLockEnabled,
                    onCheckedChange = { wantEnable ->
                        if (wantEnable) showPinSetupDialog = true
                        else viewModel.disablePinLock()
                    }
                )
            }

            if (prefs.pinLockEnabled) {
                Spacer(Modifier.height(8.dp))
                val bioAvail = BiometricHelper.checkAvailability(context)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Fingerprint / Face Unlock", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = when (bioAvail) {
                                    BiometricAvailability.AVAILABLE -> "Optional BiometricPrompt ready"
                                    BiometricAvailability.NOT_ENROLLED -> "Enroll biometrics in Android Settings"
                                    BiometricAvailability.UNAVAILABLE -> "Not available on this device"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = prefs.biometricLockEnabled,
                        enabled = bioAvail == BiometricAvailability.AVAILABLE,
                        onCheckedChange = viewModel::setBiometricEnabled
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = viewModel::lockAppNow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Lock App Now")
                }
            }
        }

        // 4. Local Device Storage Backup & Restore (100% Offline, AES-256-GCM)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Local Backup & Restore", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(6.dp))

            val lastBackupText = profile?.lastBackupEpochMs?.takeIf { it > 0L }?.let { ms ->
                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(ms))
            } ?: "No local backup exported yet"
            Text(
                text = "Encrypted Local Database (AES-256-GCM) • Last Backup: $lastBackupText",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))

            Button(
                onClick = { passphraseDialogAction = BackupPassphraseAction.CREATE_SNAPSHOT },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save Encrypted Snapshot on Device")
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { passphraseDialogAction = BackupPassphraseAction.EXPORT_SAF },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Export to Folder")
                }
                OutlinedButton(
                    onClick = { openBackupLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Restore File")
                }
            }

            if (localSnapshots.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Saved Local Vault Snapshots", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    localSnapshots.forEach { snap ->
                        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                            .format(Date(snap.modifiedEpochMs))
                        val kb = (snap.sizeBytes / 1024.0).coerceAtLeast(0.1)
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(snap.fileName, style = MaterialTheme.typography.labelLarge)
                                    Text(
                                        text = "$dateStr • ${String.format(Locale.US, "%.1f KB", kb)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row {
                                    IconButton(
                                        onClick = {
                                            val file = File(snap.filePath)
                                            if (file.exists()) {
                                                pendingRestoreEncryptedText = file.readText()
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Restore, contentDescription = "Restore snapshot")
                                    }
                                    IconButton(
                                        onClick = {
                                            val file = File(snap.filePath)
                                            if (file.exists()) {
                                                ReportExporter.shareFile(
                                                    context = context,
                                                    file = file,
                                                    mimeType = "application/octet-stream",
                                                    chooserTitle = "Save or Share Encrypted Backup"
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Share snapshot")
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteLocalSnapshot(context, snap.filePath) }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete snapshot",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Offline Business Profile (No Google Account Needed)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Business & Owner Profile", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "100% Offline • Encrypted Local Database • No Google Account or Tracking",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = businessNameInput,
                onValueChange = { businessNameInput = it },
                label = { Text("Business / Shop Name (on SMS & PDF)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = businessPhoneInput,
                onValueChange = { businessPhoneInput = it },
                label = { Text("Business Phone") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { viewModel.updateBusinessProfile(businessNameInput, businessPhoneInput) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Business Details Offline")
            }
        }

        // Branding & Copyright Footer
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Owner: Hardik Kumar • 100% Offline Encrypted Vault",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.brand_copyright),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showPinSetupDialog) {
        var newPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPinSetupDialog = false },
            title = { Text("Set Optional 4-Digit PIN") },
            text = {
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) newPin = it },
                    label = { Text("Enter 4 digits") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.enablePinLock(newPin)
                        showPinSetupDialog = false
                    },
                    enabled = newPin.length == 4
                ) {
                    Text("Enable PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinSetupDialog = false }) { Text("Cancel") }
            }
        )
    }

    passphraseDialogAction?.let { action ->
        var pass by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { passphraseDialogAction = null },
            title = {
                Text(
                    if (action == BackupPassphraseAction.CREATE_SNAPSHOT) "Save Local Encrypted Snapshot"
                    else "Export Encrypted Backup (.sscbak)"
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter a passphrase to encrypt your offline backup using AES-256-GCM.")
                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = { Text("Backup Passphrase (min 4 chars)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        passphraseDialogAction = null
                        if (action == BackupPassphraseAction.CREATE_SNAPSHOT) {
                            viewModel.createLocalVaultSnapshot(context, pass, customers, transactions)
                        } else {
                            pendingExportPassphrase = pass
                            val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                            createBackupLauncher.launch("SimpleSmsCalc_$stamp.sscbak")
                        }
                    },
                    enabled = pass.length >= 4
                ) {
                    Text(if (action == BackupPassphraseAction.CREATE_SNAPSHOT) "Save Snapshot" else "Choose Folder")
                }
            },
            dismissButton = {
                TextButton(onClick = { passphraseDialogAction = null }) { Text("Cancel") }
            }
        )
    }

    pendingRestoreEncryptedText?.let { encText ->
        var pass by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { pendingRestoreEncryptedText = null },
            title = { Text("Decrypt & Restore Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter the passphrase used when this .sscbak file was created.")
                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = { Text("Backup Passphrase") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = ReportExporter.parseEncryptedBackup(encText, pass)
                        parsed.onSuccess { payload ->
                            viewModel.restoreBackupData(payload.customers, payload.transactions)
                            pendingRestoreEncryptedText = null
                        }.onFailure { err ->
                            viewModel.emitMessage(err.localizedMessage ?: "Failed to decrypt backup")
                        }
                    },
                    enabled = pass.isNotBlank()
                ) {
                    Text("Restore Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreEncryptedText = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PermissionManagerDashboard(
    permissions: List<ManagedPermissionItem>,
    onToggleFeature: (ManagedPermissionId, Boolean) -> Unit,
    onRequestPermission: (String) -> Unit,
    onOpenSystemSettings: () -> Unit
) {
    val glass = LocalGlassPalette.current
    val grantedCount = permissions.count { it.state == PermissionState.GRANTED }
    val notGrantedCount = permissions.count { it.state == PermissionState.NOT_GRANTED }
    val notReqCount = permissions.count { it.state == PermissionState.NOT_REQUIRED }

    GlassCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Permission Manager", style = MaterialTheme.typography.titleLarge)
            }
            OutlinedButton(onClick = onOpenSystemSettings) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("OS Settings")
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Permissions are requested only when you use their feature. Disable optional features anytime.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PermissionCountTile("Granted", grantedCount, glass.jamaAccent, Modifier.weight(1f))
            PermissionCountTile("Not Granted", notGrantedCount, glass.warningAccent, Modifier.weight(1f))
            PermissionCountTile("Not Required", notReqCount, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            permissions.forEach { item ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("perm_card_${item.id.name}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            PermissionStatusPill(state = item.state)
                        }
                        if (item.isOptionalToggleable) {
                            Switch(
                                checked = item.isFeatureEnabled,
                                onCheckedChange = { enabled -> onToggleFeature(item.id, enabled) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = item.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.statusNote,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (item.canRequestInApp && item.androidPermission != null) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onRequestPermission(item.androidPermission) },
                                modifier = Modifier.testTag("grant_perm_${item.id.name}")
                            ) {
                                Text("Grant Permission")
                            }
                            OutlinedButton(onClick = onOpenSystemSettings) {
                                Text("App Settings")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionCountTile(
    label: String,
    count: Int,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = JetBrainsMonoFamily,
            color = accent
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
