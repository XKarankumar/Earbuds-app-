package com.example.desicalc.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.desicalc.model.AppLanguage
import com.example.desicalc.model.AppSettingsState
import com.example.desicalc.model.AppThemePreset
import com.example.desicalc.model.CustomBackgroundStyle
import com.example.desicalc.ui.components.CopyrightFooter
import com.example.desicalc.ui.components.DesiCard
import java.io.File

@Composable
fun ProfileAndSettingsScreen(
    settings: AppSettingsState,
    hasNotificationPermission: Boolean,
    lastBackupMessage: String?,
    onSelectTheme: (AppThemePreset) -> Unit,
    onSelectCustomAccent: (Long) -> Unit,
    onSelectCustomBg: (CustomBackgroundStyle) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onUpdateProfile: (String, String) -> Unit,
    onUpdateProfilePhoto: (Uri) -> Unit,
    onUpdateNotifications: (Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onExportJsonBackup: () -> Unit,
    onExportCsv: () -> Unit,
    onImportJsonFile: () -> Unit,
    onImportRawJsonText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var businessName by remember(settings.businessName) { mutableStateOf(settings.businessName) }
    var ownerMobile by remember(settings.ownerMobile) { mutableStateOf(settings.ownerMobile) }
    var showPasteMigrationDialog by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(onUpdateProfilePhoto) }

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val accentPalette = remember {
        listOf(
            0xFFF59E0B, // Saffron Gold
            0xFF14B8A6, // Emerald Teal
            0xFF38BDF8, // Sky Blue
            0xFF10B981, // Jade Green
            0xFFA855F7, // Royal Purple
            0xFFF43F5E  // Crimson Rose
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("profile_settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Business & Developer Profile Card
        item {
            DesiCard {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val photoFile = settings.profilePhotoPath?.let(::File)
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .clickable {
                                    photoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                        ) {
                            if (photoFile != null && photoFile.exists()) {
                                AsyncImage(
                                    model = photoFile,
                                    contentDescription = "Business Profile Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.img_developer_avatar_1794830691573),
                                    contentDescription = "Developer Hardik Kumar Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(settings.businessName, style = MaterialTheme.typography.titleLarge)
                            Text("Currency: INR ₹ • Version 2.0", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Developer: Hardik Kumar",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change Profile Image")
                    }

                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text("Business / Shop Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ownerMobile,
                        onValueChange = { ownerMobile = it },
                        label = { Text("Owner Mobile Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { onUpdateProfile(businessName, ownerMobile) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save Business Profile") }
                }
            }
        }

        // Language Selection
        item {
            DesiCard {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("App Language / भाषा", style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AppLanguage.entries) { lang ->
                            FilterChip(
                                selected = settings.language == lang,
                                onClick = { onSelectLanguage(lang) },
                                label = { Text(lang.displayName) },
                                modifier = Modifier.testTag("lang_chip_${lang.code}")
                            )
                        }
                    }
                }
            }
        }

        // Themes & Custom Studio
        item {
            DesiCard {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Themes & Custom Appearance", style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AppThemePreset.entries) { preset ->
                            FilterChip(
                                selected = settings.themePreset == preset,
                                onClick = { onSelectTheme(preset) },
                                label = { Text(preset.displayName) },
                                modifier = Modifier.testTag("theme_chip_${preset.id}")
                            )
                        }
                    }

                    Text("Custom Accent Colour", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        accentPalette.forEach { argb ->
                            val selected = settings.themePreset == AppThemePreset.CUSTOM && settings.customAccentColor == argb
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(argb))
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    )
                                    .clickable { onSelectCustomAccent(argb) }
                            )
                        }
                    }

                    Text("Custom Background Canvas", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(CustomBackgroundStyle.entries) { bg ->
                            FilterChip(
                                selected = settings.themePreset == AppThemePreset.CUSTOM && settings.customBackgroundStyle == bg,
                                onClick = { onSelectCustomBg(bg) },
                                label = { Text(bg.displayName) }
                            )
                        }
                    }
                }
            }
        }

        // Notification Settings
        item {
            DesiCard {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Notifications", style = MaterialTheme.typography.titleMedium)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                            OutlinedButton(onClick = {
                                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }) { Text("Allow Permission") }
                        }
                    }
                    SettingToggleRow("Enable All Notifications", settings.notificationsEnabled) {
                        onUpdateNotifications(
                            it,
                            settings.notifyOnTransaction,
                            settings.notifyPaymentReminder,
                            settings.notifyDailySummary,
                            settings.scheduledDailySummaryEnabled
                        )
                    }
                    SettingToggleRow("New Transaction Alerts", settings.notifyOnTransaction) {
                        onUpdateNotifications(
                            settings.notificationsEnabled,
                            it,
                            settings.notifyPaymentReminder,
                            settings.notifyDailySummary,
                            settings.scheduledDailySummaryEnabled
                        )
                    }
                    SettingToggleRow("Payment & Scheduled Reminders", settings.notifyPaymentReminder) {
                        onUpdateNotifications(
                            settings.notificationsEnabled,
                            settings.notifyOnTransaction,
                            it,
                            settings.notifyDailySummary,
                            settings.scheduledDailySummaryEnabled
                        )
                    }
                    SettingToggleRow("Scheduled Daily Summary (WorkManager)", settings.scheduledDailySummaryEnabled) {
                        onUpdateNotifications(
                            settings.notificationsEnabled,
                            settings.notifyOnTransaction,
                            settings.notifyPaymentReminder,
                            settings.notifyDailySummary,
                            it
                        )
                    }
                }
            }
        }

        // Backup, Restore & Legacy LocalStorage Migration
        item {
            DesiCard {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Backup, Restore & HTML Data Migration", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Non-destructive upgrade: importing JSON backups or legacy custom_hisab_calculator.html LocalStorage exports safely merges records without deleting or overwriting existing data.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!lastBackupMessage.isNullOrBlank()) {
                        Text(
                            text = lastBackupMessage,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = onExportJsonBackup, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Backup, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("JSON Backup")
                        }
                        OutlinedButton(onClick = onExportCsv, modifier = Modifier.weight(1f)) {
                            Text("CSV Export")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = onImportJsonFile, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Restore, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import JSON File")
                        }
                        OutlinedButton(
                            onClick = { showPasteMigrationDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Paste Legacy JSON")
                        }
                    }
                }
            }
        }

        // About & Copyright
        item {
            DesiCard {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("About Desi Calculator", style = MaterialTheme.typography.titleMedium)
                    Text("Desi Calculator AI PRO V2.0", fontWeight = FontWeight.Bold)
                    Text("Developed by Hardik Kumar")
                    Text("© 2026 Hardik Kumar. All Rights Reserved.")
                    Text(
                        "Built with Kotlin, Jetpack Compose Material 3, Room Database, DataStore, WorkManager, Android Notifications, SmsManager/Composer, and Firebase AI Logic.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { CopyrightFooter() }
    }

    if (showPasteMigrationDialog) {
        var jsonInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasteMigrationDialog = false },
            title = { Text("Migrate Legacy HTML Data") },
            text = {
                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = { jsonInput = it },
                    label = { Text("Paste LocalStorage / JSON backup") },
                    minLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (jsonInput.isNotBlank()) {
                        onImportRawJsonText(jsonInput)
                        showPasteMigrationDialog = false
                    }
                }) { Text("Migrate Safely") }
            },
            dismissButton = {
                TextButton(onClick = { showPasteMigrationDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
