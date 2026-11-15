package com.example.smartcool.monitor

import android.content.Context
import android.os.Build
import android.os.PowerManager
import com.example.smartcool.model.BatteryTelemetry
import com.example.smartcool.model.CoolingRecommendation
import com.example.smartcool.model.SmartCoolSettings
import com.example.smartcool.model.SmartMode
import com.example.smartcool.model.SystemActionType
import com.example.smartcool.model.ThermalSeverity
import com.example.smartcool.model.ThermalTelemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThermalMonitor(context: Context) {

    private val appContext = context.applicationContext
    private val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager

    private val _osThermalSeverity = MutableStateFlow(readCurrentOsThermalSeverity())
    val osThermalSeverity: StateFlow<ThermalSeverity> = _osThermalSeverity.asStateFlow()

    private var thermalListener: Any? = null

    fun startListening() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && thermalListener == null) {
            val pm = powerManager ?: return
            try {
                val listener = PowerManager.OnThermalStatusChangedListener { status ->
                    _osThermalSeverity.value = mapPowerManagerStatus(status)
                }
                pm.addThermalStatusListener(appContext.mainExecutor, listener)
                thermalListener = listener
            } catch (_: Throwable) {
                // Some custom/emulator builds may not support listener registration
            }
        }
        _osThermalSeverity.value = readCurrentOsThermalSeverity()
    }

    fun stopListening() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val listener = thermalListener as? PowerManager.OnThermalStatusChangedListener
            if (listener != null) {
                try {
                    powerManager?.removeThermalStatusListener(listener)
                } catch (_: Throwable) {
                    // Ignore safely
                }
            }
            thermalListener = null
        }
    }

    fun evaluate(
        battery: BatteryTelemetry,
        settings: SmartCoolSettings
    ): ThermalTelemetry {
        val osSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        val osSeverity = readCurrentOsThermalSeverity()
        _osThermalSeverity.value = osSeverity

        val headroom = readThermalHeadroom()
        val tempC = battery.temperatureCelsius

        val highTempExceeded = tempC >= settings.highTempThresholdC
        val chargingHeatWarning = battery.isCharging && tempC >= 38.5f

        val statusDesc = if (osSupported) {
            when (osSeverity) {
                ThermalSeverity.NONE -> "Android Thermal API reports normal operating headroom with no active throttling."
                ThermalSeverity.LIGHT -> "Slight thermal rise detected by Android Thermal API; light background pacing may occur."
                ThermalSeverity.MODERATE -> "Moderate thermal load; Android may trim peak display brightness or SoC clocks."
                ThermalSeverity.SEVERE -> "Severe thermal throttling active; pause heavy gaming, 4K capture, or fast charging."
                ThermalSeverity.CRITICAL -> "Critical thermal threshold reached; Android is aggressively throttling hardware."
                ThermalSeverity.EMERGENCY, ThermalSeverity.SHUTDOWN -> "Emergency thermal condition; unplug charger and let the device cool down immediately."
                ThermalSeverity.UNSUPPORTED -> "Android Thermal API did not return a status on this HAL."
            }
        } else {
            "Android Thermal Status API requires Android 10 (API 29)+. Monitoring real BatteryManager thermistor temperature."
        }

        val recommendations = buildSafeRecommendations(
            battery = battery,
            osSeverity = osSeverity,
            settings = settings,
            chargingHeatWarning = chargingHeatWarning,
            highTempExceeded = highTempExceeded
        )

        return ThermalTelemetry(
            batteryTempCelsius = tempC,
            androidThermalSupported = osSupported,
            thermalSeverity = osSeverity,
            thermalStatusDescription = statusDesc,
            thermalHeadroom = headroom,
            headroomSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && headroom != null,
            chargingHeatWarning = chargingHeatWarning,
            highTempExceeded = highTempExceeded,
            recommendations = recommendations,
            timestampMs = System.currentTimeMillis()
        )
    }

    private fun readCurrentOsThermalSeverity(): ThermalSeverity {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return ThermalSeverity.UNSUPPORTED
        }
        val pm = powerManager ?: return ThermalSeverity.UNSUPPORTED
        return try {
            mapPowerManagerStatus(pm.currentThermalStatus)
        } catch (_: Throwable) {
            ThermalSeverity.UNSUPPORTED
        }
    }

    private fun readThermalHeadroom(): Float? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val pm = powerManager ?: return null
        return try {
            val val10s = pm.getThermalHeadroom(10)
            if (val10s.isNaN() || val10s < 0f) null else val10s
        } catch (_: Throwable) {
            null
        }
    }

    private fun mapPowerManagerStatus(status: Int): ThermalSeverity {
        return when (status) {
            PowerManager.THERMAL_STATUS_NONE -> ThermalSeverity.NONE
            PowerManager.THERMAL_STATUS_LIGHT -> ThermalSeverity.LIGHT
            PowerManager.THERMAL_STATUS_MODERATE -> ThermalSeverity.MODERATE
            PowerManager.THERMAL_STATUS_SEVERE -> ThermalSeverity.SEVERE
            PowerManager.THERMAL_STATUS_CRITICAL -> ThermalSeverity.CRITICAL
            PowerManager.THERMAL_STATUS_EMERGENCY -> ThermalSeverity.EMERGENCY
            PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalSeverity.SHUTDOWN
            else -> ThermalSeverity.UNSUPPORTED
        }
    }

    private fun buildSafeRecommendations(
        battery: BatteryTelemetry,
        osSeverity: ThermalSeverity,
        settings: SmartCoolSettings,
        chargingHeatWarning: Boolean,
        highTempExceeded: Boolean
    ): List<CoolingRecommendation> {
        val list = mutableListOf<CoolingRecommendation>()

        if (chargingHeatWarning) {
            list += CoolingRecommendation(
                id = "charging_heat",
                title = "Fast-Charging Thermal Dissipation",
                description = "Your battery is warming up while charging. Remove thick protective cases, place the device on a cool flat surface, and avoid gaming or video calls until charging slows down.",
                priorityHigh = true,
                actionLabel = "Battery Usage",
                actionType = SystemActionType.BATTERY_USAGE_SETTINGS
            )
        }

        if (highTempExceeded || osSeverity.rank >= ThermalSeverity.MODERATE.rank) {
            list += CoolingRecommendation(
                id = "reduce_brightness_refresh",
                title = "Lower Display Brightness & Refresh Rate",
                description = "High-nit OLED panels and 120Hz refresh rates draw significant power and trap heat in the frame. Reducing brightness immediately lowers power dissipation.",
                priorityHigh = true,
                actionLabel = "Display Settings",
                actionType = SystemActionType.DISPLAY_SETTINGS
            )
            list += CoolingRecommendation(
                id = "pause_heavy_radios",
                title = "Check Hotspot, 5G & Background Sync",
                description = "Active mobile hotspots, sustained 5G uploads, or weak cellular signals increase modem heat. Switch to Wi-Fi or turn off unused radios when hot.",
                priorityHigh = false,
                actionLabel = "Network Settings",
                actionType = SystemActionType.WIRELESS_SETTINGS
            )
        }

        if (!battery.isPowerSaveMode && (settings.smartMode != SmartMode.NORMAL || battery.levelPercent <= 30 || highTempExceeded)) {
            list += CoolingRecommendation(
                id = "enable_battery_saver",
                title = "Use Android Native Battery Saver",
                description = "Android Battery Saver limits background wakeups, animations, and peak CPU boosts through OS-level scheduling.",
                priorityHigh = settings.smartMode == SmartMode.EXTREME_BATTERY_SAVER,
                actionLabel = "Battery Saver",
                actionType = SystemActionType.BATTERY_SAVER_SETTINGS
            )
        }

        if (list.isEmpty()) {
            list += CoolingRecommendation(
                id = "nominal_state",
                title = "Thermals Are Balanced",
                description = "Battery temperature and OS thermal status are in a healthy range. Keep charging cables well ventilated and avoid direct sunlight.",
                priorityHigh = false,
                actionLabel = "Display Settings",
                actionType = SystemActionType.DISPLAY_SETTINGS
            )
        }

        return list
    }
}
