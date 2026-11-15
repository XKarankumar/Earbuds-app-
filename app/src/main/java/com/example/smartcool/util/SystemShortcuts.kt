package com.example.smartcool.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.example.smartcool.model.SystemActionType

object SystemShortcuts {

    fun executeAction(context: Context, action: SystemActionType): Boolean {
        return when (action) {
            SystemActionType.BATTERY_SAVER_SETTINGS -> openBatterySaverSettings(context)
            SystemActionType.BATTERY_USAGE_SETTINGS -> openBatteryUsageSettings(context)
            SystemActionType.DISPLAY_SETTINGS -> openDisplaySettings(context)
            SystemActionType.USAGE_ACCESS_SETTINGS -> openUsageAccessSettings(context)
            SystemActionType.BATTERY_OPTIMIZATION_SETTINGS -> openBatteryOptimizationSettings(context)
            SystemActionType.WIRELESS_SETTINGS -> openWirelessSettings(context)
        }
    }

    fun openBatterySaverSettings(context: Context): Boolean {
        return launchIntent(context, Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)) ||
            openBatteryUsageSettings(context)
    }

    fun openBatteryUsageSettings(context: Context): Boolean {
        return launchIntent(context, Intent(Intent.ACTION_POWER_USAGE_SUMMARY)) ||
            launchIntent(context, Intent(Settings.ACTION_SETTINGS))
    }

    fun openDisplaySettings(context: Context): Boolean {
        return launchIntent(context, Intent(Settings.ACTION_DISPLAY_SETTINGS)) ||
            launchIntent(context, Intent(Settings.ACTION_SETTINGS))
    }

    fun openUsageAccessSettings(context: Context): Boolean {
        return launchIntent(context, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) ||
            launchIntent(context, Intent(Settings.ACTION_SETTINGS))
    }

    fun openBatteryOptimizationSettings(context: Context): Boolean {
        return launchIntent(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) ||
            openBatterySaverSettings(context)
    }

    fun openWirelessSettings(context: Context): Boolean {
        return launchIntent(context, Intent(Settings.ACTION_WIRELESS_SETTINGS)) ||
            launchIntent(context, Intent(Settings.ACTION_SETTINGS))
    }

    fun openAppDetailsSettings(context: Context, packageName: String): Boolean {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        return launchIntent(context, intent)
    }

    private fun launchIntent(context: Context, intent: Intent): Boolean {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (_: Throwable) {
            false
        }
    }
}
