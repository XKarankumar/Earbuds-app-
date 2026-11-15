package com.example.smartcool.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.smartcool.model.BatteryTelemetry
import com.example.smartcool.model.SmartCoolSettings
import com.example.smartcool.model.ThermalTelemetry
import com.example.smartcool.model.formatTemperature

class AlertNotificationManager(private val context: Context) {

    private var lastHighTempAlertMs: Long = 0L
    private var lastChargingHeatAlertMs: Long = 0L
    private var lastLowBatteryAlertMs: Long = 0L

    init {
        ensureChannels()
    }

    fun ensureChannels() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val thermalChannel = NotificationChannel(
            CHANNEL_THERMAL,
            "Thermal & Cooling Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when battery temperature rises above your threshold or heats up while charging."
        }
        val batteryChannel = NotificationChannel(
            CHANNEL_BATTERY,
            "Battery Level Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications when battery drops below your configured low-battery level."
        }
        manager.createNotificationChannel(thermalChannel)
        manager.createNotificationChannel(batteryChannel)
    }

    fun hasNotificationPermission(): Boolean {
        val permGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return permGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun evaluateAndNotify(
        battery: BatteryTelemetry,
        thermal: ThermalTelemetry,
        settings: SmartCoolSettings,
        nowMs: Long = System.currentTimeMillis()
    ) {
        if (!hasNotificationPermission()) return

        if (settings.highTempAlertEnabled && thermal.highTempExceeded) {
            if (nowMs - lastHighTempAlertMs >= THERMAL_COOLDOWN_MS) {
                lastHighTempAlertMs = nowMs
                val tempText = formatTemperature(battery.temperatureCelsius, settings.useFahrenheit)
                postNotification(
                    id = NOTIF_ID_HIGH_TEMP,
                    channelId = CHANNEL_THERMAL,
                    title = "High Battery Temperature ($tempText)",
                    body = "Device temperature is above ${formatTemperature(settings.highTempThresholdC, settings.useFahrenheit)}. Consider reducing screen brightness or pausing heavy tasks."
                )
            }
        }

        if (settings.chargingHeatAlertEnabled && thermal.chargingHeatWarning) {
            if (nowMs - lastChargingHeatAlertMs >= THERMAL_COOLDOWN_MS) {
                lastChargingHeatAlertMs = nowMs
                val tempText = formatTemperature(battery.temperatureCelsius, settings.useFahrenheit)
                postNotification(
                    id = NOTIF_ID_CHARGING_HEAT,
                    channelId = CHANNEL_THERMAL,
                    title = "Charging Heat Advisory ($tempText)",
                    body = "Fast charging is generating extra warmth. Removing thick cases or pausing heavy use helps protect battery longevity."
                )
            }
        }

        if (settings.lowBatteryAlertEnabled && !battery.isCharging && battery.levelPercent in 1..settings.lowBatteryThresholdPct) {
            if (nowMs - lastLowBatteryAlertMs >= BATTERY_COOLDOWN_MS) {
                lastLowBatteryAlertMs = nowMs
                postNotification(
                    id = NOTIF_ID_LOW_BATTERY,
                    channelId = CHANNEL_BATTERY,
                    title = "Low Battery (${battery.levelPercent}%)",
                    body = "Battery is at ${battery.levelPercent}%. Enable Android Battery Saver or Extreme Saver mode to extend runtime."
                )
            }
        }
    }

    fun sendTestAlert(useFahrenheit: Boolean, currentTempC: Float): Boolean {
        if (!hasNotificationPermission()) return false
        ensureChannels()
        val tempLabel = if (currentTempC > 0f) formatTemperature(currentTempC, useFahrenheit) else "Normal"
        postNotification(
            id = NOTIF_ID_TEST,
            channelId = CHANNEL_THERMAL,
            title = "Smart Cool Pro Alerts Active",
            body = "Notification pipeline verified. Live battery sensor reads $tempLabel."
        )
        return true
    }

    private fun postNotification(
        id: Int,
        channelId: String,
        title: String,
        body: String
    ) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Ignored gracefully if permission was revoked concurrently
        }
    }

    companion object {
        private const val CHANNEL_THERMAL = "smart_cool_thermal_alerts"
        private const val CHANNEL_BATTERY = "smart_cool_battery_alerts"
        private const val NOTIF_ID_HIGH_TEMP = 1001
        private const val NOTIF_ID_CHARGING_HEAT = 1002
        private const val NOTIF_ID_LOW_BATTERY = 1003
        private const val NOTIF_ID_TEST = 1004
        private const val THERMAL_COOLDOWN_MS = 10 * 60_000L
        private const val BATTERY_COOLDOWN_MS = 15 * 60_000L
    }
}
