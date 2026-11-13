package com.example.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.security.BiometricAvailability
import com.example.security.BiometricHelper
import com.example.security.LocalAppPreferences

enum class PermissionState(val label: String) {
    GRANTED("Granted"),
    NOT_GRANTED("Not Granted"),
    NOT_REQUIRED("Not Required")
}

enum class ManagedPermissionId {
    SEND_SMS,
    READ_CONTACTS,
    CAMERA,
    POST_NOTIFICATIONS,
    SCOPED_STORAGE,
    BIOMETRIC,
    INTERNET,
    BLUETOOTH
}

data class ManagedPermissionItem(
    val id: ManagedPermissionId,
    val title: String,
    val androidPermission: String?,
    val state: PermissionState,
    val explanation: String,
    val isOptionalToggleable: Boolean,
    val isFeatureEnabled: Boolean,
    val canRequestInApp: Boolean,
    val statusNote: String
)

object PermissionManager {

    fun evaluateAll(context: Context, prefs: LocalAppPreferences): List<ManagedPermissionItem> {
        val hasTelephony = context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
        val hasCamera = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

        val smsGranted = isGranted(context, Manifest.permission.SEND_SMS)
        val smsState = when {
            !prefs.directSmsFeatureEnabled -> PermissionState.NOT_REQUIRED
            !hasTelephony -> PermissionState.NOT_REQUIRED
            smsGranted -> PermissionState.GRANTED
            else -> PermissionState.NOT_GRANTED
        }

        val contactsGranted = isGranted(context, Manifest.permission.READ_CONTACTS)
        val contactsState = when {
            !prefs.contactsFeatureEnabled -> PermissionState.NOT_REQUIRED
            contactsGranted -> PermissionState.GRANTED
            else -> PermissionState.NOT_GRANTED
        }

        val cameraGranted = isGranted(context, Manifest.permission.CAMERA)
        val cameraState = when {
            !prefs.cameraScanFeatureEnabled -> PermissionState.NOT_REQUIRED
            !hasCamera -> PermissionState.NOT_REQUIRED
            cameraGranted -> PermissionState.GRANTED
            else -> PermissionState.NOT_GRANTED
        }

        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isGranted(context, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
        val notifState = when {
            !prefs.remindersFeatureEnabled -> PermissionState.NOT_REQUIRED
            notifGranted -> PermissionState.GRANTED
            else -> PermissionState.NOT_GRANTED
        }

        val bioAvailability = BiometricHelper.checkAvailability(context)
        val bioState = when {
            !prefs.biometricLockEnabled -> PermissionState.NOT_REQUIRED
            bioAvailability == BiometricAvailability.AVAILABLE -> PermissionState.GRANTED
            else -> PermissionState.NOT_GRANTED
        }

        return listOf(
            ManagedPermissionItem(
                id = ManagedPermissionId.SEND_SMS,
                title = "SIM & Native SMS Receipts",
                androidPermission = Manifest.permission.SEND_SMS,
                state = smsState,
                explanation = "Optional: Enables sending receipts directly via your phone's SIM card. When disabled or not granted, the app uses Android's official native SMS composer.",
                isOptionalToggleable = true,
                isFeatureEnabled = prefs.directSmsFeatureEnabled,
                canRequestInApp = hasTelephony && prefs.directSmsFeatureEnabled && !smsGranted,
                statusNote = when {
                    !hasTelephony -> "No cellular SIM hardware detected (native SMS composer ready)"
                    !prefs.directSmsFeatureEnabled -> "Optional direct SIM send disabled — using native SMS app"
                    smsGranted -> "Direct SIM SMS sending enabled"
                    else -> "Tap Grant Permission to send receipts directly via SIM"
                }
            ),
            ManagedPermissionItem(
                id = ManagedPermissionId.READ_CONTACTS,
                title = "Contacts Access",
                androidPermission = Manifest.permission.READ_CONTACTS,
                state = contactsState,
                explanation = "Allows picking customer names and phone numbers from your local address book when adding a Hisab-Kitab customer.",
                isOptionalToggleable = true,
                isFeatureEnabled = prefs.contactsFeatureEnabled,
                canRequestInApp = prefs.contactsFeatureEnabled && !contactsGranted,
                statusNote = when {
                    !prefs.contactsFeatureEnabled -> "Disabled by user — manual customer entry active"
                    contactsGranted -> "Ready to import customer contacts offline"
                    else -> "Requested only when you tap 'Pick from Contacts'"
                }
            ),
            ManagedPermissionItem(
                id = ManagedPermissionId.CAMERA,
                title = "Camera (Bill & QR Scanner)",
                androidPermission = Manifest.permission.CAMERA,
                state = cameraState,
                explanation = "Used strictly on-device inside Tools -> Bill & QR Scanner to scan QR codes and extract bill numbers.",
                isOptionalToggleable = true,
                isFeatureEnabled = prefs.cameraScanFeatureEnabled,
                canRequestInApp = hasCamera && prefs.cameraScanFeatureEnabled && !cameraGranted,
                statusNote = when {
                    !hasCamera -> "No camera hardware available"
                    !prefs.cameraScanFeatureEnabled -> "Disabled by user — Photo Picker still available"
                    cameraGranted -> "On-device QR & Bill OCR scanner ready"
                    else -> "Requested only when opening the camera scanner"
                }
            ),
            ManagedPermissionItem(
                id = ManagedPermissionId.POST_NOTIFICATIONS,
                title = "Payment Reminder Notifications",
                androidPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.POST_NOTIFICATIONS
                } else null,
                state = notifState,
                explanation = "Delivers local offline payment due reminders you schedule for customers.",
                isOptionalToggleable = true,
                isFeatureEnabled = prefs.remindersFeatureEnabled,
                canRequestInApp = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    prefs.remindersFeatureEnabled && !notifGranted,
                statusNote = when {
                    !prefs.remindersFeatureEnabled -> "Reminders paused by user"
                    notifGranted -> "Local reminder notifications active"
                    else -> "Required on Android 13+ to display scheduled alerts"
                }
            ),
            ManagedPermissionItem(
                id = ManagedPermissionId.SCOPED_STORAGE,
                title = "Local Device Storage (SAF & Photo Picker)",
                androidPermission = null,
                state = PermissionState.NOT_REQUIRED,
                explanation = "Uses Android's zero-permission Storage Access Framework (SAF) for local .sscbak backups/restores and PDF/CSV/Excel exports, plus Photo Picker for bill images.",
                isOptionalToggleable = false,
                isFeatureEnabled = true,
                canRequestInApp = false,
                statusNote = "Zero broad storage permissions requested (Google Play scoped storage compliant)"
            ),
            ManagedPermissionItem(
                id = ManagedPermissionId.BIOMETRIC,
                title = "Fingerprint / Face Unlock (Optional with PIN)",
                androidPermission = null,
                state = bioState,
                explanation = "Optional convenience unlock alongside your local 4-digit PIN using Android BiometricPrompt.",
                isOptionalToggleable = true,
                isFeatureEnabled = prefs.biometricLockEnabled,
                canRequestInApp = false,
                statusNote = when {
                    !prefs.biometricLockEnabled -> "Optional lock disabled — configure in Security below"
                    bioAvailability == BiometricAvailability.AVAILABLE -> "Hardware biometric enrolled and active"
                    bioAvailability == BiometricAvailability.NOT_ENROLLED -> "Enroll fingerprint/face in Android System Settings"
                    else -> "No biometric sensor available on this device"
                }
            ),
            ManagedPermissionItem(
                id = ManagedPermissionId.INTERNET,
                title = "Internet / Google Account / Tracking",
                androidPermission = null,
                state = PermissionState.NOT_REQUIRED,
                explanation = "App operates 100% offline using an encrypted local SQLite database. No Google account, no cloud sync, no analytics, and no internet permission are declared.",
                isOptionalToggleable = false,
                isFeatureEnabled = false,
                canRequestInApp = false,
                statusNote = "100% Offline Vault — zero network or tracking permissions declared"
            ),
            ManagedPermissionItem(
                id = ManagedPermissionId.BLUETOOTH,
                title = "Bluetooth Peripherals",
                androidPermission = null,
                state = PermissionState.NOT_REQUIRED,
                explanation = "No external Bluetooth hardware is required. Permission is intentionally omitted.",
                isOptionalToggleable = false,
                isFeatureEnabled = false,
                canRequestInApp = false,
                statusNote = "Not Required — zero unnecessary permissions declared"
            )
        )
    }

    fun isGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun openAppSystemSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
