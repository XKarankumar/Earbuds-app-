package com.example.smartcool.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.example.smartcool.model.BatteryHealthState
import com.example.smartcool.model.BatteryTelemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BatteryMonitor(private val context: Context) {

    private val appContext = context.applicationContext
    private val batteryManager = appContext.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    private val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager

    private val _telemetry = MutableStateFlow(readCurrentBattery())
    val telemetry: StateFlow<BatteryTelemetry> = _telemetry.asStateFlow()

    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent != null) {
                _telemetry.value = parseBatteryIntent(intent)
            } else {
                refresh()
            }
        }
    }

    fun startListening() {
        if (registered) {
            refresh()
            return
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                appContext.registerReceiver(receiver, filter)
            }
            registered = true
        } catch (_: Throwable) {
            registered = false
        }
        refresh()
    }

    fun stopListening() {
        if (!registered) return
        try {
            appContext.unregisterReceiver(receiver)
        } catch (_: Throwable) {
            // Safe ignore if already unregistered
        } finally {
            registered = false
        }
    }

    fun refresh(): BatteryTelemetry {
        val current = readCurrentBattery()
        _telemetry.value = current
        return current
    }

    private fun readCurrentBattery(): BatteryTelemetry {
        val stickyIntent = try {
            appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } catch (_: Throwable) {
            null
        }
        return if (stickyIntent != null) {
            parseBatteryIntent(stickyIntent)
        } else {
            val capacity = safeIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            BatteryTelemetry(
                levelPercent = capacity ?: 0,
                statusLabel = "Sensor unavailable",
                isPowerSaveMode = safePowerSaveMode(),
                timestampMs = System.currentTimeMillis()
            )
        }
    }

    private fun parseBatteryIntent(intent: Intent): BatteryTelemetry {
        val rawLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val rawScale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val computedLevel = if (rawLevel >= 0) {
            ((rawLevel * 100f) / rawScale).toInt().coerceIn(0, 100)
        } else {
            safeIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.coerceIn(0, 100) ?: 0
        }

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val isFull = status == BatteryManager.BATTERY_STATUS_FULL

        val statusLabel = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Fully Charged"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            else -> "Unknown"
        }

        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val plugTypeLabel = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Fast / Wall"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Pad"
            BatteryManager.BATTERY_PLUGGED_DOCK -> "Dock"
            else -> "Unplugged"
        }

        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val tempC = if (tempTenths != Int.MIN_VALUE && tempTenths > -500 && tempTenths < 1500) {
            tempTenths / 10f
        } else {
            0f
        }

        val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0).coerceAtLeast(0)

        val healthInt = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
        val healthState = when (healthInt) {
            BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealthState.GOOD
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealthState.OVERHEAT
            BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealthState.DEAD
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealthState.OVER_VOLTAGE
            BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealthState.COLD
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> BatteryHealthState.UNSPECIFIED_FAILURE
            else -> BatteryHealthState.UNKNOWN
        }

        val technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
            ?.takeIf { it.isNotBlank() } ?: "Li-ion"
        val isPresent = intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true)

        val cycleCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val rawCycle = intent.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1)
            if (rawCycle >= 0) rawCycle else null
        } else {
            null
        }

        val currentNow = safeIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val chargeCounter = safeIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            ?.takeIf { it > 0 }

        return BatteryTelemetry(
            levelPercent = computedLevel,
            statusLabel = statusLabel,
            isCharging = isCharging,
            isFull = isFull,
            plugTypeLabel = plugTypeLabel,
            temperatureCelsius = tempC,
            voltageMv = voltageMv,
            currentMicroAmps = currentNow,
            chargeCounterMicroAh = chargeCounter,
            cycleCount = cycleCount,
            healthState = healthState,
            technology = technology,
            isPresent = isPresent,
            isPowerSaveMode = safePowerSaveMode(),
            timestampMs = System.currentTimeMillis()
        )
    }

    private fun safeIntProperty(propertyId: Int): Int? {
        val bm = batteryManager ?: return null
        return try {
            val value = bm.getIntProperty(propertyId)
            if (value == Int.MIN_VALUE) null else value
        } catch (_: Throwable) {
            null
        }
    }

    private fun safePowerSaveMode(): Boolean {
        return try {
            powerManager?.isPowerSaveMode == true
        } catch (_: Throwable) {
            false
        }
    }
}
