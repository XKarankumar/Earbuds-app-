package com.example.hometheatre.dj

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DjEngineUnitTest {
    @Test
    fun builtInTracks_generateRealNonSilentAudioAndValidWaveforms() {
        val tracks = PcmTrackDecoder.buildBuiltInTracks()
        assertEquals(4, tracks.size)
        tracks.forEach { track ->
            assertTrue(track.totalFrames > 44100)
            assertEquals(96, track.waveformPeaks.size)
            val maxPeak = track.waveformPeaks.maxOrNull() ?: 0f
            assertTrue("Expected audible waveform peaks", maxPeak > 0.2f)
        }
    }

    @Test
    fun bpmEstimator_detectsReasonableTempoOnBuiltInTrack() {
        val track = PcmTrackDecoder.buildBuiltInTracks().first()
        val estimated = PcmTrackDecoder.estimateBpm(track.stereoInterleaved, track.sampleRateHz)
        assertTrue("Expected BPM in 75..165, got $estimated", estimated in 75f..165f)
    }

    @Test
    fun performancePadVoice_rendersAudioAndCompletes() {
        val voice = PerformancePadVoice(PerformancePadType.KICK_808, 44100)
        val out = FloatArray(512)
        for (i in 0 until 256) {
            voice.renderNextStereo(out, i)
        }
        assertTrue(out.any { kotlin.math.abs(it) > 0.05f })
    }
}
