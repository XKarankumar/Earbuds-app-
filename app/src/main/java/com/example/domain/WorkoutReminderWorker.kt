package com.example.domain

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.R
import com.example.data.local.AppDatabase
import com.example.security.AppPreferencesRepository
import com.example.security.KeystoreCryptoManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

class WorkoutReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefsRepo = AppPreferencesRepository(applicationContext)
        val prefs = prefsRepo.current()
        val db = AppDatabase.getInstance(applicationContext)
        val crypto = KeystoreCryptoManager(applicationContext)

        // 1. Run Missed Gym Detection cleanly if deadline reached
        val smsService = SmsGatewayService(
            context = applicationContext,
            crypto = crypto,
            prefsRepo = prefsRepo,
            memberDao = db.memberDao(),
            attendanceDao = db.attendanceDao(),
            smsLogDao = db.smsLogDao()
        )
        runCatching { smsService.runMissedGymDetection(forceIgnoreDeadline = false) }

        // 2. Post Workout Reminder Notification if enabled & near scheduled hour
        if (prefs.workoutReminderEnabled) {
            val cal = Calendar.getInstance()
            val nowHour = cal.get(Calendar.HOUR_OF_DAY)
            if (nowHour == prefs.workoutReminderHour || nowHour == (prefs.workoutReminderHour + 1) % 24) {
                postReminderNotification(prefs.gymName)
            }
        }
        return Result.success()
    }

    private fun postReminderNotification(gymName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        val channelId = "fitness_hub_reminders"
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Workout & Gym Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily workout reminders and gym schedule alerts"
            }
            nm.createNotificationChannel(channel)
        }
        val notif = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("$gymName — Training Time")
            .setContentText("Your daily workout routine is ready. Stay consistent and log your sets!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(1042, notif)
    }

    companion object {
        private const val WORK_NAME = "fitness_hub_periodic_check"

        fun schedulePeriodicChecks(context: Context) {
            val request = PeriodicWorkRequestBuilder<WorkoutReminderWorker>(1, TimeUnit.HOURS).build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
