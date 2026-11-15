package com.example.smartcool.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.smartcool.model.CustomLiquidThemeConfig
import com.example.smartcool.model.SmartCoolSettings
import com.example.smartcool.model.SmartCoolThemePreset
import com.example.smartcool.model.SmartMode
import com.example.smartcool.model.TelemetrySamplePoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

private val Context.smartCoolDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "smart_cool_battery_saver_prefs"
)

class SmartCoolPreferencesStore private constructor(
    private val dataStore: DataStore<Preferences>
) {
    constructor(context: Context) : this(context.applicationContext.smartCoolDataStore)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val safeFlow: Flow<Preferences> = dataStore.data
        .catch { err ->
            if (err is IOException) emit(emptyPreferences()) else throw err
        }

    val settingsFlow: Flow<SmartCoolSettings> = safeFlow.map { prefs ->
        decodeSettings(prefs[KEY_SETTINGS_JSON])
    }

    val historyFlow: Flow<List<TelemetrySamplePoint>> = safeFlow.map { prefs ->
        decodeHistory(prefs[KEY_HISTORY_JSON])
    }

    suspend fun updateSettings(transform: (SmartCoolSettings) -> SmartCoolSettings) {
        dataStore.edit { prefs ->
            val current = decodeSettings(prefs[KEY_SETTINGS_JSON])
            val updated = transform(current)
            prefs[KEY_SETTINGS_JSON] = json.encodeToString(updated)
        }
    }

    suspend fun selectTheme(preset: SmartCoolThemePreset) {
        updateSettings { it.copy(selectedTheme = preset) }
    }

    suspend fun updateCustomTheme(config: CustomLiquidThemeConfig, activateCustom: Boolean = true) {
        updateSettings { current ->
            current.copy(
                selectedTheme = if (activateCustom) SmartCoolThemePreset.CUSTOM else current.selectedTheme,
                customTheme = config
            )
        }
    }

    suspend fun selectSmartMode(mode: SmartMode) {
        updateSettings { it.copy(smartMode = mode) }
    }

    suspend fun appendTelemetrySample(sample: TelemetrySamplePoint, minSpacingMs: Long = 30_000L) {
        dataStore.edit { prefs ->
            val current = decodeHistory(prefs[KEY_HISTORY_JSON])
            val last = current.lastOrNull()
            val shouldAppend = last == null ||
                (sample.timestampMs - last.timestampMs) >= minSpacingMs ||
                kotlin.math.abs(sample.tempCelsius - last.tempCelsius) >= 0.5f ||
                sample.batteryPercent != last.batteryPercent
            if (shouldAppend) {
                val trimmed = (current + sample).takeLast(MAX_HISTORY_POINTS)
                prefs[KEY_HISTORY_JSON] = json.encodeToString(trimmed)
            }
        }
    }

    suspend fun clearHistory() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_HISTORY_JSON)
        }
    }

    private fun decodeSettings(raw: String?): SmartCoolSettings {
        if (raw.isNullOrBlank()) return SmartCoolSettings()
        return try {
            json.decodeFromString<SmartCoolSettings>(raw)
        } catch (_: Throwable) {
            SmartCoolSettings()
        }
    }

    private fun decodeHistory(raw: String?): List<TelemetrySamplePoint> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<TelemetrySamplePoint>>(raw)
        } catch (_: Throwable) {
            emptyList()
        }
    }

    companion object {
        private const val MAX_HISTORY_POINTS = 288
        private val KEY_SETTINGS_JSON = stringPreferencesKey("smart_cool_settings_json")
        private val KEY_HISTORY_JSON = stringPreferencesKey("smart_cool_history_json")

        fun createForTesting(dataStore: DataStore<Preferences>): SmartCoolPreferencesStore {
            return SmartCoolPreferencesStore(dataStore)
        }
    }
}
