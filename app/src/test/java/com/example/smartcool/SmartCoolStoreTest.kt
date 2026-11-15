package com.example.smartcool

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.smartcool.data.SmartCoolPreferencesStore
import com.example.smartcool.model.CustomLiquidThemeConfig
import com.example.smartcool.model.SmartCoolThemePreset
import com.example.smartcool.model.SmartMode
import com.example.smartcool.model.TelemetrySamplePoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SmartCoolStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun allSevenThemes_persistAndRestoreCorrectly() = runTest {
        val file = tempFolder.newFile("themes.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { file }
        )
        val store = SmartCoolPreferencesStore.createForTesting(dataStore)

        assertEquals(SmartCoolThemePreset.IOS_LIQUID_GLASS, store.settingsFlow.first().selectedTheme)

        for (preset in SmartCoolThemePreset.entries) {
            store.selectTheme(preset)
            assertEquals(preset, store.settingsFlow.first().selectedTheme)
        }
    }

    @Test
    fun customThemeAndSmartMode_persistAccurately() = runTest {
        val file = tempFolder.newFile("custom.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { file }
        )
        val store = SmartCoolPreferencesStore.createForTesting(dataStore)

        val custom = CustomLiquidThemeConfig(
            isDarkBase = true,
            accentColorHex = 0xFFEC4899L,
            glassOpacity = 0.88f,
            glowIntensity = 0.72f
        )
        store.updateCustomTheme(custom, activateCustom = true)
        store.selectSmartMode(SmartMode.COOLING)

        val saved = store.settingsFlow.first()
        assertEquals(SmartCoolThemePreset.CUSTOM, saved.selectedTheme)
        assertEquals(true, saved.customTheme.isDarkBase)
        assertEquals(0xFFEC4899L, saved.customTheme.accentColorHex)
        assertEquals(SmartMode.COOLING, saved.smartMode)
    }

    @Test
    fun telemetryHistory_appendsAndClearsRealSamples() = runTest {
        val file = tempFolder.newFile("history.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { file }
        )
        val store = SmartCoolPreferencesStore.createForTesting(dataStore)

        store.appendTelemetrySample(
            TelemetrySamplePoint(
                timestampMs = 1_000L,
                dateKey = "2026-11-15",
                tempCelsius = 33.2f,
                batteryPercent = 82,
                isCharging = false
            )
        )
        store.appendTelemetrySample(
            TelemetrySamplePoint(
                timestampMs = 45_000L,
                dateKey = "2026-11-15",
                tempCelsius = 34.0f,
                batteryPercent = 81,
                isCharging = false
            )
        )

        val history = store.historyFlow.first()
        assertEquals(2, history.size)
        assertEquals(34.0f, history.last().tempCelsius, 0.01f)

        store.clearHistory()
        assertTrue(store.historyFlow.first().isEmpty())
    }
}
