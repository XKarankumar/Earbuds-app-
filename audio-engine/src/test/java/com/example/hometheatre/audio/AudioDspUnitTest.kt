package com.example.hometheatre.audio

import com.example.hometheatre.core.UserAudioPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class AudioDspUnitTest {
    @Test
    fun softClipLimiter_preventsHardClippingAboveCeiling() {
        val limiter = SoftClipLimiter(ceilingLinear = 0.966f, kneeThreshold = 0.78f)
        val out = limiter.processSample(2.8f, enabled = true)
        assertTrue("Expected output <= ceilingLinear, got $out", out <= 0.966f)
        assertTrue("Expected clipping protection flag true", limiter.protectionEngaged)
        assertTrue("Expected positive gain reduction", limiter.lastGainReductionDb > 0f)
    }

    @Test
    fun theatreDspChain_processesStereoBuffersSafelyWithBoost() {
        val chain = TheatreDspChain()
        val prefs = UserAudioPreferences(
            bassBoostLevel = 1.0f,
            subwooferGainDb = 12f,
            deepBassEnabled = true,
            cinemaBassEnabled = true,
            loudnessGainDb = 12f,
            limiterEnabled = true
        )
        chain.configure(prefs, 44100)
        val stereo = FloatArray(512) { idx -> if (idx % 2 == 0) 0.5f else -0.5f }
        chain.processStereoInPlace(stereo, 256)
        for (sample in stereo) {
            assertTrue("Expected bounded output, got $sample", abs(sample) <= 0.966f)
        }
        assertEquals(10, prefs.customBandGainsDb.size)
    }
}
