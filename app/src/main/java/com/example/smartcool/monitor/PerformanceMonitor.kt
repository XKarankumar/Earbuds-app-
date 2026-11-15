package com.example.smartcool.monitor

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import com.example.smartcool.model.AppUsageSummary
import com.example.smartcool.model.MemoryInfoSnapshot
import com.example.smartcool.model.UsageInsightsSnapshot
import java.util.Calendar

class PerformanceMonitor(private val context: Context) {

    private val appContext = context.applicationContext
    private val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    private val usageStatsManager = appContext.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    private val appOpsManager = appContext.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
    private val packageManager = appContext.packageManager

    fun readMemoryAndDeviceInfo(): MemoryInfoSnapshot {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)

        val totalMb = (memInfo.totalMem / (1024L * 1024L)).coerceAtLeast(1L)
        val availMb = (memInfo.availMem / (1024L * 1024L)).coerceAtLeast(0L)
        val usedMb = (totalMb - availMb).coerceAtLeast(0L)
        val usedPercent = ((usedMb.toFloat() / totalMb.toFloat()) * 100f).coerceIn(0f, 100f)
        val thresholdMb = (memInfo.threshold / (1024L * 1024L)).coerceAtLeast(0L)

        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        val isVivo = Build.MANUFACTURER.equals("vivo", ignoreCase = true)
        val profileNote = if (isVivo) {
            "Vivo hardware detected ($model) — calibrated for OriginOS / FuntouchOS & Android thermal HAL."
        } else {
            "Target profile: Vivo X300 & Android 8.0+ • Running live on $manufacturer $model."
        }

        return MemoryInfoSnapshot(
            availableMb = availMb,
            totalMb = totalMb,
            usedMb = usedMb,
            usedPercent = usedPercent,
            thresholdMb = thresholdMb,
            isLowMemory = memInfo.lowMemory,
            cpuCoreCount = Runtime.getRuntime().availableProcessors().coerceAtLeast(1),
            deviceManufacturer = manufacturer,
            deviceModel = model,
            androidVersionLabel = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            hardwareBoard = "${Build.HARDWARE} • ${Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64"}",
            deviceProfileNote = profileNote
        )
    }

    fun hasUsageStatsPermission(): Boolean {
        val ops = appOpsManager ?: return false
        return try {
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ops.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    appContext.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                ops.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    appContext.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Throwable) {
            false
        }
    }

    fun readUsageInsights(): UsageInsightsSnapshot {
        val granted = hasUsageStatsPermission()
        if (!granted) {
            return UsageInsightsSnapshot(
                hasUsagePermission = false,
                totalScreenTimeTodayMs = 0L,
                topApps = emptyList(),
                backgroundInsightSummary = "Usage Access permission is not granted yet. Enable it to view real per-app foreground screen time and background service insights."
            )
        }

        val usm = usageStatsManager ?: return UsageInsightsSnapshot(
            hasUsagePermission = true,
            backgroundInsightSummary = "UsageStatsManager service is unavailable on this device."
        )

        val ownBucketLabel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                when (usm.appStandbyBucket) {
                    UsageStatsManager.STANDBY_BUCKET_ACTIVE -> "Active Bucket"
                    UsageStatsManager.STANDBY_BUCKET_WORKING_SET -> "Working Set"
                    UsageStatsManager.STANDBY_BUCKET_FREQUENT -> "Frequent"
                    UsageStatsManager.STANDBY_BUCKET_RARE -> "Rare"
                    UsageStatsManager.STANDBY_BUCKET_RESTRICTED -> "Restricted"
                    else -> "Standard"
                }
            } catch (_: Throwable) {
                "Standard"
            }
        } else {
            "API 28+ Standby Buckets"
        }

        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis

        val statsMap = try {
            usm.queryAndAggregateUsageStats(startOfDay, now)
        } catch (_: Throwable) {
            emptyMap()
        }

        val summaries = statsMap.values
            .filter { it.totalTimeInForeground > 0L }
            .sortedByDescending { it.totalTimeInForeground }
            .take(15)
            .map { stat ->
                val pkg = stat.packageName
                val appLabel = resolveAppLabel(pkg)
                val fgServiceMs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    stat.totalTimeForegroundServiceUsed
                } else {
                    0L
                }
                val bucketNote = when {
                    pkg == appContext.packageName -> ownBucketLabel
                    fgServiceMs > 60_000L -> "Bg Svc Active"
                    else -> "Foreground"
                }
                AppUsageSummary(
                    packageName = pkg,
                    appName = appLabel,
                    foregroundTimeMs = stat.totalTimeInForeground,
                    lastTimeUsedMs = stat.lastTimeUsed,
                    standbyBucketLabel = bucketNote
                )
            }

        val totalTodayMs = summaries.sumOf { it.foregroundTimeMs }
        val bgServiceApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            statsMap.values.count { it.totalTimeForegroundServiceUsed > 60_000L }
        } else {
            0
        }

        val insightSummary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "$bgServiceApps app(s) logged >1m foreground-service background activity today. Self bucket: $ownBucketLabel."
        } else {
            "Showing daily foreground usage sorted by active screen time (Android 10+ adds foreground-service telemetry)."
        }

        return UsageInsightsSnapshot(
            hasUsagePermission = true,
            totalScreenTimeTodayMs = totalTodayMs,
            topApps = summaries,
            backgroundInsightSummary = insightSummary
        )
    }

    private fun resolveAppLabel(packageName: String): String {
        return try {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, 0)
            }
            packageManager.getApplicationLabel(info).toString()
        } catch (_: Throwable) {
            packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
    }
}
