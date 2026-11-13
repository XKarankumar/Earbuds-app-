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

    fun requiredRuntimePermissions(): Array<String> {
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

    fun hasAllBluetoothPermissions(context: Context): Boolean {
        return hasConnectPermission(context) && hasScanPermission(context)
    }

    fun buildChecklist(context: Context): List<PermissionItemState> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                PermissionItemState(
                    permission = Manifest.permission.BLUETOOTH_CONNECT,
                    title = "Nearby Devices · Connect",
                    description = "Reads paired earbuds, audio routing, and GATT battery level.",
                    isGranted = hasConnectPermission(context),
                    isRuntimeRequestable = true,
                ),
                PermissionItemState(
                    permission = Manifest.permission.BLUETOOTH_SCAN,
                    title = "Nearby Devices · Scan (No Location)",
                    description = "Discovers nearby Bluetooth earbuds without requesting GPS location.",
                    isGranted = hasScanPermission(context),
                    isRuntimeRequestable = true,
                ),
                PermissionItemState(
                    permission = Manifest.permission.MODIFY_AUDIO_SETTINGS,
                    title = "Audio & Equalizer Control",
                    description = "Connects studio EQ curves and Bass Boost to active playback.",
                    isGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.MODIFY_AUDIO_SETTINGS,
                    ) == PackageManager.PERMISSION_GRANTED,
                    isRuntimeRequestable = false,
                ),
            )
        } else {
            val locationPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Manifest.permission.ACCESS_FINE_LOCATION
            } else {
                Manifest.permission.ACCESS_COARSE_LOCATION
            }
            listOf(
                PermissionItemState(
                    permission = Manifest.permission.BLUETOOTH,
                    title = "Legacy Bluetooth Access",
                    description = "Install-time access for paired audio devices on Android 11 and below.",
                    isGranted = hasConnectPermission(context),
                    isRuntimeRequestable = false,
                ),
                PermissionItemState(
                    permission = locationPerm,
                    title = "Bluetooth Scan Discovery",
                    description = "Required by Android 11 and below to scan nearby BLE earbuds.",
                    isGranted = hasScanPermission(context),
                    isRuntimeRequestable = true,
                ),
                PermissionItemState(
                    permission = Manifest.permission.MODIFY_AUDIO_SETTINGS,
                    title = "Audio & Equalizer Control",
                    description = "Connects studio EQ curves and Bass Boost to active playback.",
                    isGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.MODIFY_AUDIO_SETTINGS,
                    ) == PackageManager.PERMISSION_GRANTED,
                    isRuntimeRequestable = false,
                ),
            )
        }
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
}
