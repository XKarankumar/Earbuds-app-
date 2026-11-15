package com.example.hometheatre.audio

import com.example.hometheatre.core.DefaultEqCatalog
import com.example.hometheatre.core.SpatialMode
import com.example.hometheatre.core.UserAudioPreferences
import kotlin.math.pow

/**
 * Full custom DSP audio pipeline:
 * - 10-band Peaking Equalizer (31Hz..16kHz)
 * - Bass Booster + Subwoofer LFE Low-Shelf + Deep Bass / Cinema Bass contour
 * - Dedicated Mid & Treble shelves
 * - Virtual Surround Haas/crossfeed widening when SpatialMode uses software virtualization
 * - Software Loudness gain with mandatory SoftClipLimiter protection
 */
class TheatreDspChain {
    private val eqFilters = Array(10) { StereoBiquadFilter() }
    private val subBassShelf = StereoBiquadFilter()
    private val deepBassFilter = StereoBiquadFilter()
    private val cinemaBassFilter = StereoBiquadFilter()
    private val midShelf = StereoBiquadFilter()
    private val trebleShelf = StereoBiquadFilter()

    val limiter = SoftClipLimiter()

    @Volatile private var linearMakeupGain: Float = 1f
    @Volatile private var limiterEnabled: Boolean = true
    @Volatile private var virtualSurroundEnabled: Boolean = false
    @Volatile private var sampleRateHz: Double = 44100.0

    // Haas delay buffer (~11ms at 48kHz)
    private val haasBuffer = FloatArray(1024)
    private var haasWriteIdx = 0

    fun configure(prefs: UserAudioPreferences, sampleRate: Int = 44100) {
        sampleRateHz = sampleRate.coerceAtLeast(8000).toDouble()
        val bands = prefs.customBandGainsDb
        for (i in 0 until 10) {
            val gainDb = bands.getOrElse(i) { 0f }.coerceIn(-12f, 12f).toDouble()
            eqFilters[i].configurePeaking(
                freqHz = DefaultEqCatalog.bandFrequenciesHz[i].toDouble(),
                q = 1.4,
                gainDb = gainDb,
                sampleRate = sampleRateHz
            )
        }

        val subDb = (prefs.bassBoostLevel * 6f + prefs.subwooferGainDb).coerceIn(-6f, 12f).toDouble()
        subBassShelf.configureLowShelf(85.0, subDb, sampleRateHz)

        deepBassFilter.configurePeaking(
            freqHz = 40.0,
            q = 0.9,
            gainDb = if (prefs.deepBassEnabled) 4.5 else 0.0,
            sampleRate = sampleRateHz
        )

        cinemaBassFilter.configurePeaking(
            freqHz = 58.0,
            q = 1.1,
            gainDb = if (prefs.cinemaBassEnabled) 3.5 else 0.0,
            sampleRate = sampleRateHz
        )

        midShelf.configurePeaking(
            freqHz = 1100.0,
            q = 0.7,
            gainDb = prefs.midGainDb.coerceIn(-12f, 12f).toDouble(),
            sampleRate = sampleRateHz
        )

        trebleShelf.configureHighShelf(
            freqHz = 7500.0,
            gainDb = prefs.trebleGainDb.coerceIn(-12f, 12f).toDouble(),
            sampleRate = sampleRateHz
        )

        val loudnessDb = prefs.loudnessGainDb.coerceIn(0f, 12f)
        linearMakeupGain = 10.0.pow(loudnessDb / 20.0).toFloat()
        limiterEnabled = prefs.limiterEnabled
        virtualSurroundEnabled = prefs.virtualSurroundEnabled ||
            prefs.spatialMode == SpatialMode.VIRTUAL_SURROUND ||
            prefs.spatialMode == SpatialMode.CINEMA
    }

    fun processStereoInPlace(interleaved: FloatArray, frameCount: Int) {
        val haasDelaySamples = ((sampleRateHz * 0.009).toInt()).coerceIn(64, haasBuffer.size - 1)
        for (i in 0 until frameCount) {
            var l = interleaved[i * 2]
            var r = interleaved[i * 2 + 1]

            // 10-band EQ
            for (band in 0 until 10) {
                l = eqFilters[band].processLeft(l)
                r = eqFilters[band].processRight(r)
            }

            // Bass, Subwoofer, Deep Bass, Cinema Bass, Mid, Treble
            l = subBassShelf.processLeft(l)
            r = subBassShelf.processRight(r)
            l = deepBassFilter.processLeft(l)
            r = deepBassFilter.processRight(r)
            l = cinemaBassFilter.processLeft(l)
            r = cinemaBassFilter.processRight(r)
            l = midShelf.processLeft(l)
            r = midShelf.processRight(r)
            l = trebleShelf.processLeft(l)
            r = trebleShelf.processRight(r)

            // Virtual Surround Mid/Side + Haas ambiance
            if (virtualSurroundEnabled) {
                val mid = 0.5f * (l + r)
                val side = 0.5f * (l - r)
                val readIdx = (haasWriteIdx - haasDelaySamples + haasBuffer.size) % haasBuffer.size
                val delayedSide = haasBuffer[readIdx]
                haasBuffer[haasWriteIdx] = side
                haasWriteIdx = (haasWriteIdx + 1) % haasBuffer.size

                val wideSide = side * 1.28f + delayedSide * 0.18f
                l = mid + wideSide
                r = mid - wideSide
            }

            // Software loudness enhancement + soft clipping limiter
            l = limiter.processSample(l * linearMakeupGain, limiterEnabled)
            r = limiter.processSample(r * linearMakeupGain, limiterEnabled)

            interleaved[i * 2] = l
            interleaved[i * 2 + 1] = r
        }
        limiter.decayTelemetry()
    }
}
