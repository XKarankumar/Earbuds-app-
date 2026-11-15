package com.example.hometheatre.rgb

import android.graphics.Color
import com.example.hometheatre.core.AudioTelemetrySnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RgbCatalogUnitTest {
    @Test
    fun catalog_containsMoreThan100DistinctEffects() {
        assertTrue("Expected 100+ effects, got ${RgbEffectsCatalog.allEffects.size}", RgbEffectsCatalog.allEffects.size >= 100)
        val distinctIds = RgbEffectsCatalog.allEffects.map { it.id }.toSet()
        assertEquals(RgbEffectsCatalog.allEffects.size, distinctIds.size)
    }

    @Test
    fun renderFrame_producesRequestedLedCount() {
        val frame = RgbEffectsCatalog.renderFrame(
            effectId = 13,
            ledCount = 24,
            timeMs = 1500L,
            brightness = 0.8f,
            speed = 0.6f,
            customColorArgb = Color.CYAN,
            telemetry = AudioTelemetrySnapshot(bassEnergy = 0.9f, beatDetected = true)
        )
        assertEquals(24, frame.size)
    }
}
