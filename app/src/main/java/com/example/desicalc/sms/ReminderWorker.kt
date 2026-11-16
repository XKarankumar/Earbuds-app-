package com.example.desicalc.sms

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.desicalc.data.local.DesiCalculatorDatabase
import com.example.desicalc.data.preferences.DesiPreferencesStore
import com.example.desicalc.data.repository.DesiLedgerRepository
import com.example.desicalc.model.ReportPeriod
import com.example.desicalc.model.SmsTemplateType
import com.example.desicalc.model.formatInr
import com.example.desicalc.notifications.DesiNotificationManager
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val db = DesiCalculatorDatabase.getInstance(applicationContext)
        val repo = DesiLedgerRepository(db)
        val prefs = DesiPreferencesStore(applicationContext)
        val settings = prefs.settingsFlow.first()
        val notifier = DesiNotificationManager(applicationContext)
        val smsManager = SmsReminderManager(applicationContext, repo)

        val summaries = repo.getAllSummariesOnce()
        val now = System.currentTimeMillis()

        // 1. Check due customer reminders respecting explicit customer-wise consent & background rules
        val dueCustomers = summaries.filter { c ->
            c.netBalance > 0.01 &&
                c.mobile.isNotBlank() &&
                c.autoSmsEnabled &&
                c.smsConsentGranted &&
                c.reminderIntervalDays > 0 &&
                (c.lastReminderSentAt == null ||
                    now - c.lastReminderSentAt >= c.reminderIntervalDays * 24L * 60L * 60L * 1000L)
        }

        dueCustomers.forEachIndexed { idx, c ->
            val msg = smsManager.renderTemplate(
                template = settings.reminderTemplate,
                customerName = c.name,
                businessName = settings.businessName,
                amount = c.netBalance,
                netBalance = c.netBalance
            )

            // Only attempt direct background SMS if SEND_SMS is explicitly granted & telephony exists
            if (smsManager.hasSendSmsPermission() && smsManager.hasTelephonyHardware()) {
                val res = smsManager.sendDirectSmsWithConsent(
                    customerId = c.id,
                    customerName = c.name,
                    mobile = c.mobile,
                    templateType = SmsTemplateType.REMINDER,
                    messageBody = msg,
                    userConsented = c.smsConsentGranted
                )
                if (res.isFailure && settings.notificationsEnabled && settings.notifyPaymentReminder) {
                    notifier.showPaymentReminderNotification(c.name, c.mobile, msg, 4000 + idx)
                }
            } else if (settings.notificationsEnabled && settings.notifyPaymentReminder) {
                // Respect Android background/permission restrictions: surface actionable reminder notification
                notifier.showPaymentReminderNotification(c.name, c.mobile, msg, 4000 + idx)
            }
        }

        // 2. Daily summary notification if enabled
        if (settings.notificationsEnabled && settings.notifyDailySummary && settings.scheduledDailySummaryEnabled) {
            val allTx = repo.getAllTransactionsOnce()
            val today = repo.buildPeriodReport(allTx, ReportPeriod.DAILY)
            val totalPending = summaries.sumOf { maxOf(0.0, it.netBalance) }
            notifier.showDailySummaryNotification(
                title = "${settings.businessName}: Daily Hisab",
                summaryBody = "Today: ${today.transactionCount} entries | Udhar ${formatInr(today.udharDiyaTotal)} | Jama ${formatInr(today.jamaReceivedTotal)} | Total Receivable ${formatInr(totalPending)}"
            )
        }

        return Result.success()
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "desi_calc_scheduled_reminders"

        fun schedulePeriodicReminders(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(
                12, TimeUnit.HOURS
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun runImmediateReminderCheck(context: Context) {
            val oneTime = OneTimeWorkRequestBuilder<ReminderWorker>().build()
            WorkManager.getInstance(context).enqueue(oneTime)
        }

        fun cancelPeriodicReminders(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
        }
    }
}
