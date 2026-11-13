package com.example.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.model.PermissionItemState

object PermissionHelper {

    fun requiredBluetoothPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
            else -> arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        }
    }

    fun requiredNotificationPermission(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyArray()
        }
    }

    fun hasConnectPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH,
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasScanPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            val fine = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
            fine || coarse
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasAllBluetoothPermissions(context: Context): Boolean {
        return hasConnectPermission(context) && hasScanPermission(context)
    }

    fun buildChecklist(context: Context): List<PermissionItemState> {
        val items = mutableListOf<PermissionItemState>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            items += PermissionItemState(
                permission = Manifest.permission.BLUETOOTH_CONNECT,
                title = "Bluetooth Connect",
                description = "Reads paired earbuds, active audio route, brand info, and GATT battery/firmware.",
                whyNeeded = "Required on Android 12+ (including Vivo OriginOS / FuntouchOS) to read connected earbuds without crashing.",
                isGranted = hasConnectPermission(context),
                isRuntimeRequestable = true,
            )
            items += PermissionItemState(
                permission = Manifest.permission.BLUETOOTH_SCAN,
                title = "Nearby Devices · Scan",
                description = "Discovers nearby Bluetooth & BLE earbuds with neverForLocation privacy flag.",
                whyNeeded = "Used only when you tap Scan Nearby so CK can list unpaired earbuds without accessing GPS.",
                isGranted = hasScanPermission(context),
                isRuntimeRequestable = true,
            )
        } else {
            val locationPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Manifest.permission.ACCESS_FINE_LOCATION
            } else {
                Manifest.permission.ACCESS_COARSE_LOCATION
            }
            items += PermissionItemState(
                permission = Manifest.permission.BLUETOOTH,
                title = "Bluetooth Access",
                description = "Install-time access for paired audio devices on Android 11 and older.",
                whyNeeded = "Allows CK to detect paired A2DP/HFP earbuds.",
                isGranted = hasConnectPermission(context),
                isRuntimeRequestable = false,
            )
            items += PermissionItemState(
                permission = locationPerm,
                title = "Nearby Scan Discovery",
                description = "Required by Android 11 and below when scanning BLE beacons.",
                whyNeeded = "Android 11 requires location permission for BLE scans; CK never stores coordinates.",
                isGranted = hasScanPermission(context),
                isRuntimeRequestable = true,
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            items += PermissionItemState(
                permission = Manifest.permission.POST_NOTIFICATIONS,
                title = "Notifications & Background Monitor",
                description = "Displays low-battery alerts, connection status, and background monitor notification.",
                whyNeeded = "Requested only when you enable background monitoring or connection notifications on Android 13+.",
                isGranted = hasNotificationPermission(context),
                isRuntimeRequestable = true,
            )
        }

        items += PermissionItemState(
            permission = Manifest.permission.MODIFY_AUDIO_SETTINGS,
            title = "AudioFX & Equalizer",
            description = "Connects 5-band Equalizer and Bass Boost to Android's active audio session.",
            whyNeeded = "Install-time permission so studio EQ curves work over Bluetooth A2DP/LE Audio.",
            isGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
            ) == PackageManager.PERMISSION_GRANTED,
            isRuntimeRequestable = false,
        )

        return items
    }

    fun createEnableBluetoothIntent(): Intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

    fun openAppSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openBluetoothSettings(context: Context) {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openUrl(context: Context, url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
