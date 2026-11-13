package com.example.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
    CAMERA,
    PHOTO_PICKER,
    INTERNET_HTTPS,
    BIOMETRIC,
    CONTACTS_PRIVACY,
    STORAGE_PRIVACY
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
        val hasCamera = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        val cameraGranted = isGranted(context, Manifest.permission.CAMERA)
        val cameraState = when {
            !prefs.cameraScanFeatureEnabled -> PermissionState.NOT_REQUIRED
            !hasCamera -> PermissionState.NOT_REQUIRED
            cameraGranted -> PermissionState.GRANTED
            else -> PermissionState.NOT_GRANTED
        }

        val internetGranted = isGranted(context, Manifest.permission.INTERNET)
        val bioAvailability = BiometricHelper.checkAvailability(context)
        val bioState = when {
            !prefs.biometricLockEnabled -> PermissionState.NOT_REQUIRED
            bioAvailability == BiometricAvailability.AVAILABLE -> PermissionState.GRANTED
            else -> PermissionState.NOT_GRANTED
        }

        return listOf(
            ManagedPermissionItem(
                id = ManagedPermissionId.CAMERA,
                title = "Camera (AI Photo Scanner)",
                androidPermission = Manifest.permission.CAMERA,
                state = cameraState,
                explanation = "Requested only when you tap 'Capture Photo' in AI Photo Scanner. Photos are never uploaded automatically.",
                isOptionalToggleable = true,
                isFeatureEnabled = prefs.cameraScanFeatureEnabled,
                canRequestInApp = hasCamera && prefs.cameraScanFeatureEnabled && !cameraGranted,
                statusNote = when {
                    !hasCamera -> "No camera hardware detected"
                    !prefs.cameraScanFeatureEnabled -> "Disabled by user — Photo Picker remains available"
                    cameraGranted -> "Camera capture ready on explicit user tap"
                    else -> "Tap Grant Permission or enable when capturing a photo"
                }
            ),
            ManagedPermissionItem(
                id = ManagedPermissionId.PHOTO_PICKER,
                title = "Gallery Access (Zero-Permission Photo Picker)",
                androidPermission = null,
                state = PermissionState.NOT_REQUIRED,
                explanation = "Uses Android's official Photo Picker so you choose only the exact image you want to scan without broad storage access.",
                isOptionalToggleable = false,
                isFeatureEnabled = true,
                canRequestInApp = false,
                statusNote = "Zero broad media permissions requested (Play Policy compliant)"
            ),
            ManagedPermissionItem(
                id = ManagedPermissionId.INTERNET_HTTPS,
                title = "Encrypted HTTPS AI Communication",
                androidPermission = Manifest.permission.INTERNET,
                state = if (internetGranted) PermissionState.GRANTED else PermissionState.NOT_GRANTED,
                explanation = "Used strictly for user-initiated HTTPS Firebase AI Logic + App Check verification requests. Zero background tracking.",
                isOptionalToggleable = false,
                isFeatureEnabled = true,
                canRequestInApp = false,
                statusNote = "App Check attested HTTPS transport active"
            ),
            ManagedPermissionItem(
                id = ManagedPermissionId.BIOMETRIC,
                title = "Biometric Vault Lock (Optional)",
                androidPermission = null,
                state = bioState,
                explanation = "Optional fingerprint/face lock on top of Guest Mode to protect your encrypted local search history.",
                isOptionalToggleable = true,
                isFeatureEnabled = prefs.biometricLockEnabled,
                canRequestInApp = false,
                statusNote = when {
                    !prefs.biometricLockEnabled -> "Guest Mode open — enable PIN + Biometric below to lock vault"
                    bioAvailability == BiometricAvailability.AVAILABLE -> "Biometric sensor active"
                    bioAvailability == BiometricAvailability.NOT_ENROLLED -> "Enroll fingerprint/face in Android Settings"
                    else -> "No biometric hardware available"
                }
            ),
            ManagedPermissionItem(
                id = ManagedPermissionId.CONTACTS_PRIVACY,
                title = "Contacts & SMS Access",
                androidPermission = null,
                state = PermissionState.NOT_REQUIRED,
                explanation = "Intentionally NOT requested. Karan Number Finder Pro AI never reads, uploads, or harvests your contacts or SMS inbox.",
                isOptionalToggleable = false,
                isFeatureEnabled = false,
                canRequestInApp = false,
                statusNote = "Zero Contacts / Zero SMS permissions declared"
            ),
            ManagedPermissionItem(
                id = ManagedPermissionId.STORAGE_PRIVACY,
                title = "Broad External Storage",
                androidPermission = null,
                state = PermissionState.NOT_REQUIRED,
                explanation = "PDF reports are generated in private app cache and shared only when you tap Share via Android FileProvider.",
                isOptionalToggleable = false,
                isFeatureEnabled = false,
                canRequestInApp = false,
                statusNote = "Scoped FileProvider sharing only — zero storage permissions"
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
