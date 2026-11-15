package com.example.smartcool.ui.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartcool.data.SmartCoolPreferencesStore
import com.example.smartcool.model.BatteryTelemetry
import com.example.smartcool.model.CustomLiquidThemeConfig
import com.example.smartcool.model.DailyBatteryReport
import com.example.smartcool.model.MemoryInfoSnapshot
import com.example.smartcool.model.SmartCoolSettings
import com.example.smartcool.model.SmartCoolThemePreset
import com.example.smartcool.model.SmartMode
import com.example.smartcool.model.TelemetrySamplePoint
import com.example.smartcool.model.ThermalSeverity
import com.example.smartcool.model.ThermalTelemetry
import com.example.smartcool.model.UsageInsightsSnapshot
import com.example.smartcool.monitor.BatteryMonitor
import com.example.smartcool.monitor.PerformanceMonitor
import com.example.smartcool.monitor.ThermalMonitor
import com.example.smartcool.notifications.AlertNotificationManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SmartCoolViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsStore = SmartCoolPreferencesStore(application)
    private val batteryMonitor = BatteryMonitor(application)
    private val thermalMonitor = ThermalMonitor(application)
    private val performanceMonitor = PerformanceMonitor(application)
    private val notificationManager = AlertNotificationManager(application)

    private val sessionStartElapsedMs = SystemClock.elapsedRealtime()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    val settings: StateFlow<SmartCoolSettings> = prefsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, SmartCoolSettings())

    val historySamples: StateFlow<List<TelemetrySamplePoint>> = prefsStore.historyFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val batteryTelemetry: StateFlow<BatteryTelemetry> = batteryMonitor.telemetry

    private val _thermalTelemetry = MutableStateFlow(
        thermalMonitor.evaluate(batteryMonitor.telemetry.value, SmartCoolSettings())
    )
    val thermalTelemetry: StateFlow<ThermalTelemetry> = _thermalTelemetry.asStateFlow()

    private val _memorySnapshot = MutableStateFlow(performanceMonitor.readMemoryAndDeviceInfo())
    val memorySnapshot: StateFlow<MemoryInfoSnapshot> = _memorySnapshot.asStateFlow()

    private val _usageInsights = MutableStateFlow(performanceMonitor.readUsageInsights())
    val usageInsights: StateFlow<UsageInsightsSnapshot> = _usageInsights.asStateFlow()

    private val _sessionMinutes = MutableStateFlow(0)
    val sessionMinutes: StateFlow<Int> = _sessionMinutes.asStateFlow()

    val dailyReport: StateFlow<DailyBatteryReport> = combine(
        historySamples,
        usageInsights,
        sessionMinutes
    ) { samples, usage, mins ->
        val todayKey = dateFormatter.format(Date())
        val todaySamples = samples.filter { it.dateKey == todayKey }
        if (todaySamples.isEmpty()) {
            DailyBatteryReport(
                dateKey = todayKey,
                sampleCount = 0,
                screenTimeTodayMs = usage.totalScreenTimeTodayMs,
                sessionScreenOnMinutes = mins
            )
        } else {
            val temps = todaySamples.map { it.tempCelsius }
            val pcts = todaySamples.map { it.batteryPercent }
            val peakRank = todaySamples.maxOf { it.thermalRank }
            val peakLabel = ThermalSeverity.entries.firstOrNull { it.rank == peakRank }?.label ?: "Nominal"
            DailyBatteryReport(
                dateKey = todayKey,
                sampleCount = todaySamples.size,
                minTempCelsius = temps.minOrNull(),
                maxTempCelsius = temps.maxOrNull(),
                avgTempCelsius = temps.average().toFloat(),
                minBatteryPercent = pcts.minOrNull(),
                maxBatteryPercent = pcts.maxOrNull(),
                chargingSamplesCount = todaySamples.count { it.isCharging },
                peakThermalLabel = peakLabel,
                screenTimeTodayMs = usage.totalScreenTimeTodayMs,
                sessionScreenOnMinutes = mins
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DailyBatteryReport())

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    private var monitoringActive = false

    init {
        startMonitoring()

        viewModelScope.launch {
            while (true) {
                if (monitoringActive) {
                    refreshAllTelemetry(recordSample = true)
                }
                val intervalSec = when (settings.value.smartMode) {
                    SmartMode.COOLING -> 2
                    SmartMode.NORMAL -> settings.value.monitorIntervalSeconds.coerceIn(2, 15)
                    SmartMode.EXTREME_BATTERY_SAVER -> 8
                }
                delay(intervalSec * 1000L)
            }
        }
    }

    fun startMonitoring() {
        if (!monitoringActive) {
            monitoringActive = true
            batteryMonitor.startListening()
            thermalMonitor.startListening()
        }
        refreshAllTelemetry(recordSample = true)
    }

    fun pauseMonitoring() {
        if (monitoringActive) {
            monitoringActive = false
            batteryMonitor.stopListening()
            thermalMonitor.stopListening()
        }
    }

    fun refreshAllTelemetry(recordSample: Boolean = false) {
        val battery = batteryMonitor.refresh()
        val currentSettings = settings.value
        val thermal = thermalMonitor.evaluate(battery, currentSettings)
        _thermalTelemetry.value = thermal
        _memorySnapshot.value = performanceMonitor.readMemoryAndDeviceInfo()
        _usageInsights.value = performanceMonitor.readUsageInsights()
        _sessionMinutes.value = ((SystemClock.elapsedRealtime() - sessionStartElapsedMs) / 60_000L).toInt()

        notificationManager.evaluateAndNotify(battery, thermal, currentSettings)

        if (recordSample && battery.temperatureCelsius > 0f) {
            val now = System.currentTimeMillis()
            val sample = TelemetrySamplePoint(
                timestampMs = now,
                dateKey = dateFormatter.format(Date(now)),
                tempCelsius = battery.temperatureCelsius,
                batteryPercent = battery.levelPercent,
                isCharging = battery.isCharging,
                thermalRank = thermal.thermalSeverity.rank
            )
            viewModelScope.launch {
                prefsStore.appendTelemetrySample(sample)
            }
        }
    }

    fun selectTheme(preset: SmartCoolThemePreset) {
        viewModelScope.launch {
            prefsStore.selectTheme(preset)
            _toastEvents.emit("${preset.displayName} theme applied")
        }
    }

    fun updateCustomTheme(config: CustomLiquidThemeConfig, activateCustom: Boolean = true) {
        viewModelScope.launch {
            prefsStore.updateCustomTheme(config, activateCustom)
            if (activateCustom) {
                _toastEvents.emit("Custom theme saved")
            }
        }
    }

    fun selectSmartMode(mode: SmartMode) {
        viewModelScope.launch {
            prefsStore.selectSmartMode(mode)
            refreshAllTelemetry(recordSample = false)
            _toastEvents.emit("${mode.title} activated")
        }
    }

    fun setUseFahrenheit(useFahrenheit: Boolean) {
        viewModelScope.launch {
            prefsStore.updateSettings { it.copy(useFahrenheit = useFahrenheit) }
        }
    }

    fun setHighTempAlert(enabled: Boolean) {
        viewModelScope.launch {
            prefsStore.updateSettings { it.copy(highTempAlertEnabled = enabled) }
        }
    }

    fun setHighTempThreshold(tempC: Float) {
        viewModelScope.launch {
            prefsStore.updateSettings { it.copy(highTempThresholdC = tempC.coerceIn(36f, 48f)) }
        }
    }

    fun setChargingHeatAlert(enabled: Boolean) {
        viewModelScope.launch {
            prefsStore.updateSettings { it.copy(chargingHeatAlertEnabled = enabled) }
        }
    }

    fun setLowBatteryAlert(enabled: Boolean) {
        viewModelScope.launch {
            prefsStore.updateSettings { it.copy(lowBatteryAlertEnabled = enabled) }
        }
    }

    fun setLowBatteryThreshold(percent: Int) {
        viewModelScope.launch {
            prefsStore.updateSettings { it.copy(lowBatteryThresholdPct = percent.coerceIn(5, 40)) }
        }
    }

    fun setMonitorIntervalSeconds(seconds: Int) {
        viewModelScope.launch {
            prefsStore.updateSettings { it.copy(monitorIntervalSeconds = seconds.coerceIn(2, 15)) }
        }
    }

    fun clearRecordedHistory() {
        viewModelScope.launch {
            prefsStore.clearHistory()
            _toastEvents.emit("Recorded telemetry history cleared")
        }
    }

    fun hasNotificationPermission(): Boolean = notificationManager.hasNotificationPermission()

    fun sendTestNotification() {
        val ok = notificationManager.sendTestAlert(
            useFahrenheit = settings.value.useFahrenheit,
            currentTempC = batteryTelemetry.value.temperatureCelsius
        )
        viewModelScope.launch {
            if (ok) {
                _toastEvents.emit("Test alert notification sent")
            } else {
                _toastEvents.emit("Enable Notifications permission to receive alerts")
            }
        }
    }

    override fun onCleared() {
        pauseMonitoring()
        super.onCleared()
    }
}
