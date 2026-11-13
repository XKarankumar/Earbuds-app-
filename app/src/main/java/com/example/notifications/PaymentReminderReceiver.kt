package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.permissions.PermissionManager

class PaymentReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val customerName = intent.getStringExtra(EXTRA_CUSTOMER_NAME) ?: "Customer"
        val amountText = intent.getStringExtra(EXTRA_AMOUNT_TEXT) ?: "Payment due"
        val note = intent.getStringExtra(EXTRA_NOTE).orEmpty()
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, (System.currentTimeMillis() % 100000).toInt())

        ReminderScheduler.showReminderNotification(
            context = context,
            notificationId = notifId,
            title = "Payment Reminder: $customerName",
            body = if (note.isNotBlank()) "$amountText • $note" else amountText
        )
    }

    companion object {
        const val EXTRA_CUSTOMER_NAME = "extra_customer_name"
        const val EXTRA_AMOUNT_TEXT = "extra_amount_text"
        const val EXTRA_NOTE = "extra_note"
        const val EXTRA_NOTIF_ID = "extra_notif_id"
    }
}

object ReminderScheduler {

    private const val CHANNEL_ID = "payment_reminders_channel"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Payment Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Scheduled payment alerts and Hisab-Kitab follow-up reminders"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    fun canPostNotifications(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionManager.isGranted(context, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
    }

    fun scheduleReminder(
        context: Context,
        customerName: String,
        amountText: String,
        note: String,
        delayMinutes: Long
    ): Boolean {
        if (!canPostNotifications(context)) return false
        ensureChannel(context)

        if (delayMinutes <= 0L) {
            showReminderNotification(
                context = context,
                notificationId = (System.currentTimeMillis() % 100000).toInt(),
                title = "Payment Reminder: $customerName",
                body = if (note.isNotBlank()) "$amountText • $note" else amountText
            )
            return true
        }

        val triggerAtMs = System.currentTimeMillis() + delayMinutes * 60_000L
        val notifId = (System.currentTimeMillis() % 100000).toInt()
        val intent = Intent(context, PaymentReminderReceiver::class.java).apply {
            putExtra(PaymentReminderReceiver.EXTRA_CUSTOMER_NAME, customerName)
            putExtra(PaymentReminderReceiver.EXTRA_AMOUNT_TEXT, amountText)
            putExtra(PaymentReminderReceiver.EXTRA_NOTE, note)
            putExtra(PaymentReminderReceiver.EXTRA_NOTIF_ID, notifId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        alarmManager?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
        return true
    }

    fun showReminderNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String
    ) {
        if (!canPostNotifications(context)) return
        ensureChannel(context)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission revoked mid-flight
        }
    }
}
