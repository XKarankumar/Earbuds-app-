package com.example.hometheatre.core

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun defaultTheme_isPremiumDark() = runTest {
        val file = tempFolder.newFile("theme_default.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { file }
        )
        val manager = ThemeManager.createForTesting(dataStore)

        assertEquals(ThemePreset.PREMIUM_DARK, manager.getSelectedTheme())
    }

    @Test
    fun themeSelection_persistsAcrossRecreatedDataStoreInstances() = runTest {
        val file = tempFolder.newFile("theme_restart.preferences_pb")

        val labelsToExpected = listOf(
            "Dark" to ThemePreset.PREMIUM_DARK,
            "Monochrome" to ThemePreset.MONOCHROME,
            "Silver Glass" to ThemePreset.SILVER_GLASS,
            "RGB Neon" to ThemePreset.RGB_NEON
        )

        for ((label, expected) in labelsToExpected) {
            val writerJob = SupervisorJob()
            val writerScope = CoroutineScope(writerJob + UnconfinedTestDispatcher(testScheduler))
            val writerStore = PreferenceDataStoreFactory.create(
                scope = writerScope,
                produceFile = { file }
            )
            val writerManager = ThemeManager.createForTesting(writerStore)
            assertTrue("Expected '$label' to resolve", writerManager.selectThemeByName(label))
            assertEquals(expected, writerManager.getSelectedTheme())
            writerJob.cancelAndJoin()

            // Simulate app restart with a brand new DataStore + ThemeManager pointing to the same file
            val readerJob = SupervisorJob()
            val readerScope = CoroutineScope(readerJob + UnconfinedTestDispatcher(testScheduler))
            val readerStore = PreferenceDataStoreFactory.create(
                scope = readerScope,
                produceFile = { file }
            )
            val readerManager = ThemeManager.createForTesting(readerStore)
            assertEquals(expected, readerManager.selectedThemeFlow.first())
            readerJob.cancelAndJoin()
        }
    }

    @Test
    fun selectThemeByName_rejectsUnknownLabel() = runTest {
        val file = tempFolder.newFile("theme_invalid.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { file }
        )
        val manager = ThemeManager.createForTesting(dataStore)
        assertFalse(manager.selectThemeByName("Unknown Theme"))
    }

    @Test
    fun updateCustomTheme_persistsConfigAndCustomPreset() = runTest {
        val file = tempFolder.newFile("theme_custom.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { file }
        )
        val manager = ThemeManager.createForTesting(dataStore)
        val custom = CustomThemeConfig(
            backgroundHex = 0xFF101820,
            primaryAccentHex = 0xFFFF2A85,
            glassOpacity = 0.85f
        )
        manager.updateCustomTheme(custom)

        val state = manager.themeStateFlow.first()
        assertEquals(ThemePreset.CUSTOM, state.selectedPreset)
        assertEquals(0xFF101820, state.customConfig.backgroundHex)
        assertEquals(0xFFFF2A85, state.customConfig.primaryAccentHex)
        assertEquals(0.85f, state.customConfig.glassOpacity, 0.001f)
    }
}
