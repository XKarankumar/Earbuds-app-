package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.bluetooth.PermissionHelper
import com.example.model.EarbudBrand
import com.example.model.NotificationStyle

object NotificationHelper {

    const val MONITOR_CHANNEL_ID = "ck_monitor_channel"
    const val ALERTS_CHANNEL_ID = "ck_alerts_channel"
    const val MONITOR_NOTIFICATION_ID = 4001
    private const val CONNECTION_ALERT_ID = 4002
    private const val LOW_BATTERY_ALERT_ID = 4003

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return

        val monitorChannel = NotificationChannel(
            MONITOR_CHANNEL_ID,
            "Background Connection Monitor",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Persistent status while CK monitors Bluetooth earbuds in the background."
            setShowBadge(false)
        }

        val alertsChannel = NotificationChannel(
            ALERTS_CHANNEL_ID,
            "Earbuds Alerts & Battery",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Connection popups, low battery warnings, and firmware verification alerts."
        }

        nm.createNotificationChannel(monitorChannel)
        nm.createNotificationChannel(alertsChannel)
    }

    fun buildMonitorNotification(
        context: Context,
        deviceName: String?,
        brand: EarbudBrand,
        batteryLevel: Int?,
        style: NotificationStyle,
    ): Notification {
        ensureChannels(context)
        val pendingIntent = mainActivityPendingIntent(context)

        val title = when {
            deviceName != null && style == NotificationStyle.DETAILED_TELEMETRY ->
                "${brand.displayName} · $deviceName"
            deviceName != null -> deviceName
            else -> "CK Earbuds Monitor Active"
        }

        val text = when {
            deviceName == null -> "Listening for Bluetooth earbuds connections (event-driven)"
            style == NotificationStyle.QUIET_STATUS -> "Connected"
            style == NotificationStyle.COMPACT_MONOCHROME ->
                batteryLevel?.let { "Connected · Battery $it%" } ?: "Connected"
            else -> {
                val bat = batteryLevel?.let { "Battery $it% · " } ?: ""
                "${bat}Monitoring A2DP/HFP audio & firmware telemetry"
            }
        }

        return NotificationCompat.Builder(context, MONITOR_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(style == NotificationStyle.QUIET_STATUS)
            .setContentIntent(pendingIntent)
            .build()
    }

    fun postConnectionAlert(
        context: Context,
        deviceName: String,
        brand: EarbudBrand,
        connected: Boolean,
        batteryLevel: Int?,
        style: NotificationStyle,
    ) {
        if (style == NotificationStyle.QUIET_STATUS) return
        if (!PermissionHelper.hasNotificationPermission(context)) return

        ensureChannels(context)
        val nm = context.getSystemService(NotificationManager::class.java) ?: return

        val title = if (connected) {
            "${brand.displayName} Connected"
        } else {
            "${brand.displayName} Disconnected"
        }
        val body = when {
            !connected -> "$deviceName disconnected from Bluetooth audio"
            style == NotificationStyle.COMPACT_MONOCHROME -> deviceName
            batteryLevel != null -> "$deviceName · Reported battery $batteryLevel%"
            else -> "$deviceName · Active Bluetooth audio link"
        }

        val notification = NotificationCompat.Builder(context, ALERTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(mainActivityPendingIntent(context))
            .build()

        nm.notify(CONNECTION_ALERT_ID, notification)
    }

    fun postLowBatteryAlert(
        context: Context,
        deviceName: String,
        batteryLevel: Int,
        style: NotificationStyle,
    ) {
        if (style == NotificationStyle.QUIET_STATUS) return
        if (!PermissionHelper.hasNotificationPermission(context)) return

        ensureChannels(context)
        val nm = context.getSystemService(NotificationManager::class.java) ?: return

        val notification = NotificationCompat.Builder(context, ALERTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Low Earbuds Battery · $batteryLevel%")
            .setContentText("$deviceName needs charging soon.")
            .setAutoCancel(true)
            .setContentIntent(mainActivityPendingIntent(context))
            .build()

        nm.notify(LOW_BATTERY_ALERT_ID, notification)
    }

    private fun mainActivityPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
