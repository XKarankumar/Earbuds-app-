package com.example.desicalc.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

class DesiNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_TRANSACTIONS = "desi_transactions_channel"
        const val CHANNEL_REMINDERS = "desi_reminders_channel"
        const val CHANNEL_DAILY_SUMMARY = "desi_daily_summary_channel"
    }

    init {
        createNotificationChannels()
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun createNotificationChannels() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val txChannel = NotificationChannel(
            CHANNEL_TRANSACTIONS,
            "Transaction Updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts for new Udhar and Jama ledger entries"
        }

        val reminderChannel = NotificationChannel(
            CHANNEL_REMINDERS,
            "Payment Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Customer payment reminders and scheduled SMS follow-ups"
        }

        val summaryChannel = NotificationChannel(
            CHANNEL_DAILY_SUMMARY,
            "Daily Hisab Summary",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Daily total Udhar, Jama, and pending balance recap"
        }

        nm.createNotificationChannels(listOf(txChannel, reminderChannel, summaryChannel))
    }

    fun showTransactionNotification(title: String, body: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val openIntent = PendingIntent.getActivity(
            context,
            101,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_TRANSACTIONS)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify((System.currentTimeMillis() % 100_000).toInt(), notification)
        } catch (_: SecurityException) {
        }
    }

    fun showPaymentReminderNotification(
        customerName: String,
        mobile: String,
        smsMessage: String,
        notificationId: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val openAppIntent = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Payment Reminder: $customerName")
            .setContentText(smsMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(smsMessage))
            .setContentIntent(openAppIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (mobile.isNotBlank()) {
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${Uri.encode(mobile)}")
                putExtra("sms_body", smsMessage)
            }
            val smsPending = PendingIntent.getActivity(
                context,
                notificationId + 5000,
                smsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_dialog_email,
                "Send SMS",
                smsPending
            )
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
        }
    }

    fun showDailySummaryNotification(title: String, summaryBody: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val openIntent = PendingIntent.getActivity(
            context,
            9001,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY_SUMMARY)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(summaryBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(summaryBody))
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(9001, notification)
        } catch (_: SecurityException) {
        }
    }
}
