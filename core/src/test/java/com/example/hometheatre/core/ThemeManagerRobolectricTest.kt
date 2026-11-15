package com.example.hometheatre.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThemeManagerRobolectricTest {

    @Test
    fun contextConstructor_persistsAllRequiredThemesAcrossInstances() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val themes = listOf(
            "Dark" to ThemePreset.PREMIUM_DARK,
            "Monochrome" to ThemePreset.MONOCHROME,
            "Silver Glass" to ThemePreset.SILVER_GLASS,
            "RGB Neon" to ThemePreset.RGB_NEON
        )

        for ((label, expected) in themes) {
            val writer = ThemeManager(context)
            assertTrue(writer.selectThemeByName(label))

            val reader = ThemeManager(context)
            assertEquals(expected, reader.selectedThemeFlow.first())
        }
    }
}
