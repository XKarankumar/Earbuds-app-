package com.example.smartcool.model

import kotlinx.serialization.Serializable

@Serializable
enum class SmartCoolThemePreset(
    val displayName: String,
    val subtitle: String,
    val isDarkDefault: Boolean
) {
    IOS_LIQUID_GLASS(
        displayName = "iOS Liquid Glass",
        subtitle = "Translucent white, brushed silver & ice-blue glassmorphism",
        isDarkDefault = false
    ),
    MIDNIGHT_AMOLED(
        displayName = "Midnight AMOLED",
        subtitle = "True #000000 OLED black with frosted obsidian glass",
        isDarkDefault = true
    ),
    PEARL_LIGHT(
        displayName = "Pearl Light",
        subtitle = "Warm porcelain pearl white with subtle platinum glass",
        isDarkDefault = false
    ),
    ICE_BLUE(
        displayName = "Ice Blue",
        subtitle = "Glacial frost tint with crisp arctic cyan highlights",
        isDarkDefault = false
    ),
    SILVER_GLASS(
        displayName = "Silver Glass",
        subtitle = "Anodized aluminum silver with luminous frosted layers",
        isDarkDefault = false
    ),
    OCEAN_BLUE(
        displayName = "Ocean Blue",
        subtitle = "Deep sapphire abyssal glass with bioluminescent aqua",
        isDarkDefault = true
    ),
    CUSTOM(
        displayName = "Custom Theme",
        subtitle = "Personalized light/dark base, custom accent & glass opacity",
        isDarkDefault = false
    )
}

@Serializable
data class CustomLiquidThemeConfig(
    val isDarkBase: Boolean = false,
    val accentColorHex: Long = 0xFF0EA5E9,
    val secondaryAccentHex: Long = 0xFF38BDF8,
    val glassOpacity: Float = 0.78f,
    val glowIntensity: Float = 0.65f
)

@Serializable
enum class SmartMode(
    val title: String,
    val shortLabel: String,
    val summary: String
) {
    NORMAL(
        title = "Normal Mode",
        shortLabel = "Normal",
        summary = "Balanced telemetry cadence for everyday battery & thermal tracking."
    ),
    COOLING(
        title = "Cooling Mode",
        shortLabel = "Cooling",
        summary = "High-frequency thermal monitoring, charging heat alerts, and safe heat-reduction guidance."
    ),
    EXTREME_BATTERY_SAVER(
        title = "Extreme Battery Saver",
        shortLabel = "Extreme Saver",
        summary = "Quick access to Android Battery Saver controls, OLED black mode, and power-saving actions."
    )
}

enum class ThermalSeverity(val label: String, val rank: Int) {
    NONE("Nominal", 0),
    LIGHT("Warm (Light)", 1),
    MODERATE("Elevated (Moderate)", 2),
    SEVERE("Hot (Severe Throttling)", 3),
    CRITICAL("Critical Heat", 4),
    EMERGENCY("Emergency", 5),
    SHUTDOWN("Shutdown Threshold", 6),
    UNSUPPORTED("OS API Unavailable", -1)
}

enum class BatteryHealthState(val label: String) {
    GOOD("Good"),
    OVERHEAT("Overheat"),
    DEAD("Depleted / Dead"),
    OVER_VOLTAGE("Over Voltage"),
    COLD("Cold"),
    UNSPECIFIED_FAILURE("Unspecified Failure"),
    UNKNOWN("Unknown")
}

enum class SystemActionType {
    BATTERY_SAVER_SETTINGS,
    BATTERY_USAGE_SETTINGS,
    DISPLAY_SETTINGS,
    USAGE_ACCESS_SETTINGS,
    BATTERY_OPTIMIZATION_SETTINGS,
    WIRELESS_SETTINGS
}

data class CoolingRecommendation(
    val id: String,
    val title: String,
    val description: String,
    val priorityHigh: Boolean = false,
    val actionLabel: String? = null,
    val actionType: SystemActionType? = null
)

data class BatteryTelemetry(
    val levelPercent: Int = 0,
    val statusLabel: String = "Reading...",
    val isCharging: Boolean = false,
    val isFull: Boolean = false,
    val plugTypeLabel: String = "Unplugged",
    val temperatureCelsius: Float = 0f,
    val voltageMv: Int = 0,
    val currentMicroAmps: Int? = null,
    val chargeCounterMicroAh: Int? = null,
    val cycleCount: Int? = null,
    val healthState: BatteryHealthState = BatteryHealthState.UNKNOWN,
    val technology: String = "Li-ion",
    val isPresent: Boolean = true,
    val isPowerSaveMode: Boolean = false,
    val timestampMs: Long = 0L
) {
    val voltageVolts: Float get() = voltageMv / 1000f
    val currentMilliamps: Int? get() = currentMicroAmps?.let { it / 1000 }
    val chargeCounterMah: Int? get() = chargeCounterMicroAh?.let { it / 1000 }
}

data class ThermalTelemetry(
    val batteryTempCelsius: Float = 0f,
    val androidThermalSupported: Boolean = false,
    val thermalSeverity: ThermalSeverity = ThermalSeverity.UNSUPPORTED,
    val thermalStatusDescription: String = "",
    val thermalHeadroom: Float? = null,
    val headroomSupported: Boolean = false,
    val chargingHeatWarning: Boolean = false,
    val highTempExceeded: Boolean = false,
    val recommendations: List<CoolingRecommendation> = emptyList(),
    val timestampMs: Long = 0L
)

data class MemoryInfoSnapshot(
    val availableMb: Long = 0L,
    val totalMb: Long = 0L,
    val usedMb: Long = 0L,
    val usedPercent: Float = 0f,
    val thresholdMb: Long = 0L,
    val isLowMemory: Boolean = false,
    val cpuCoreCount: Int = 1,
    val deviceManufacturer: String = "",
    val deviceModel: String = "",
    val androidVersionLabel: String = "",
    val hardwareBoard: String = "",
    val deviceProfileNote: String = ""
)

data class AppUsageSummary(
    val packageName: String,
    val appName: String,
    val foregroundTimeMs: Long,
    val lastTimeUsedMs: Long,
    val standbyBucketLabel: String
)

data class UsageInsightsSnapshot(
    val hasUsagePermission: Boolean = false,
    val totalScreenTimeTodayMs: Long = 0L,
    val topApps: List<AppUsageSummary> = emptyList(),
    val backgroundInsightSummary: String = ""
)

@Serializable
data class TelemetrySamplePoint(
    val timestampMs: Long,
    val dateKey: String,
    val tempCelsius: Float,
    val batteryPercent: Int,
    val isCharging: Boolean,
    val thermalRank: Int = 0
)

data class DailyBatteryReport(
    val dateKey: String = "",
    val sampleCount: Int = 0,
    val minTempCelsius: Float? = null,
    val maxTempCelsius: Float? = null,
    val avgTempCelsius: Float? = null,
    val minBatteryPercent: Int? = null,
    val maxBatteryPercent: Int? = null,
    val chargingSamplesCount: Int = 0,
    val peakThermalLabel: String = "Nominal",
    val screenTimeTodayMs: Long = 0L,
    val sessionScreenOnMinutes: Int = 0
)

@Serializable
data class SmartCoolSettings(
    val selectedTheme: SmartCoolThemePreset = SmartCoolThemePreset.IOS_LIQUID_GLASS,
    val customTheme: CustomLiquidThemeConfig = CustomLiquidThemeConfig(),
    val smartMode: SmartMode = SmartMode.NORMAL,
    val useFahrenheit: Boolean = false,
    val highTempAlertEnabled: Boolean = true,
    val highTempThresholdC: Float = 41.0f,
    val chargingHeatAlertEnabled: Boolean = true,
    val lowBatteryAlertEnabled: Boolean = true,
    val lowBatteryThresholdPct: Int = 20,
    val monitorIntervalSeconds: Int = 3
)

fun formatTemperature(tempCelsius: Float, useFahrenheit: Boolean): String {
    return if (useFahrenheit) {
        val f = tempCelsius * 9f / 5f + 32f
        "%.1f°F".format(f)
    } else {
        "%.1f°C".format(tempCelsius)
    }
}

fun formatDurationShort(millis: Long): String {
    if (millis <= 0L) return "0m"
    val totalMinutes = millis / 60_000L
    val hours = totalMinutes / 60L
    val mins = totalMinutes % 60L
    return when {
        hours > 0L -> "${hours}h ${mins}m"
        mins > 0L -> "${mins}m"
        else -> "<1m"
    }
}
