package com.example.hometheatre.dj

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * 16 real-time synthesized DJ performance pads mixed directly onto the master stereo bus.
 */
enum class PerformancePadType(val padNumber: Int, val label: String, val category: String) {
    KICK_808(1, "808 KICK", "Drums"),
    SNARE(2, "SNARE", "Drums"),
    CLAP(3, "CLAP", "Drums"),
    CLOSED_HAT(4, "CH HAT", "Drums"),
    OPEN_HAT(5, "OH HAT", "Drums"),
    TOM_LOW(6, "SUB TOM", "Drums"),
    RIMSHOT(7, "RIM", "Drums"),
    CRASH(8, "CRASH", "Drums"),
    SUB_DROP(9, "SUB DROP", "Bass"),
    ACID_STAB(10, "ACID", "Bass"),
    WOBBLE_BASS(11, "WOBBLE", "Bass"),
    CHORD_STAB(12, "CHORD", "Synth"),
    RISER(13, "RISER", "FX"),
    LASER_ZAP(14, "LASER", "FX"),
    AIRHORN(15, "HORN", "FX"),
    DUB_SIREN(16, "SIREN", "FX")
}

class PerformancePadVoice(
    val type: PerformancePadType,
    private val sampleRateHz: Int = 44100
) {
    private var sampleIdx = 0
    private val maxSamples = when (type) {
        PerformancePadType.SUB_DROP, PerformancePadType.RISER, PerformancePadType.CRASH -> (sampleRateHz * 1.1).toInt()
        PerformancePadType.AIRHORN, PerformancePadType.DUB_SIREN -> (sampleRateHz * 0.75).toInt()
        else -> (sampleRateHz * 0.45).toInt()
    }
    private var phaseA = 0.0
    private var phaseB = 0.0
    private var noiseSeed = (type.padNumber * 7919L) + 42L

    val isFinished: Boolean get() = sampleIdx >= maxSamples

    fun renderNextStereo(out: FloatArray, frameOffset: Int) {
        if (isFinished) return
        val t = sampleIdx.toDouble() / sampleRateHz.toDouble()
        noiseSeed = (noiseSeed * 1103515245L + 12345L) and 0x7FFFFFFFL
        val noise = (noiseSeed / 1073741824.0) - 1.0

        val (l, r) = when (type) {
            PerformancePadType.KICK_808 -> {
                val freq = 40.0 + 115.0 * exp(-t * 28.0)
                phaseA += 2.0 * PI * freq / sampleRateHz
                val s = sin(phaseA) * exp(-t * 5.5) * 0.85
                s to s
            }
            PerformancePadType.SNARE -> {
                phaseA += 2.0 * PI * 195.0 / sampleRateHz
                val s = (sin(phaseA) * 0.4 + noise * 0.6) * exp(-t * 14.0) * 0.75
                s to s
            }
            PerformancePadType.CLAP -> {
                val burst = if (t < 0.03) absBurst(t) else 1.0
                val s = noise * burst * exp(-t * 12.0) * 0.7
                s * 0.95 to s * 1.05
            }
            PerformancePadType.CLOSED_HAT -> {
                val s = noise * exp(-t * 36.0) * 0.55
                s to s * 0.9
            }
            PerformancePadType.OPEN_HAT -> {
                val s = noise * exp(-t * 6.5) * 0.55
                s * 0.9 to s
            }
            PerformancePadType.TOM_LOW -> {
                val freq = 65.0 + 80.0 * exp(-t * 16.0)
                phaseA += 2.0 * PI * freq / sampleRateHz
                val s = sin(phaseA) * exp(-t * 7.5) * 0.75
                s to s
            }
            PerformancePadType.RIMSHOT -> {
                phaseA += 2.0 * PI * 820.0 / sampleRateHz
                val s = (sin(phaseA) + noise * 0.25) * exp(-t * 34.0) * 0.65
                s to s
            }
            PerformancePadType.CRASH -> {
                val s = noise * exp(-t * 2.8) * 0.55
                s * 0.85 to s * 1.05
            }
            PerformancePadType.SUB_DROP -> {
                val freq = 95.0 * exp(-t * 1.5).coerceAtLeast(0.32)
                phaseA += 2.0 * PI * freq / sampleRateHz
                val s = sin(phaseA) * exp(-t * 1.8) * 0.88
                s to s
            }
            PerformancePadType.ACID_STAB -> {
                val freq = 110.0
                phaseA += 2.0 * PI * freq / sampleRateHz
                val saw = ((phaseA % (2.0 * PI)) / PI) - 1.0
                val s = saw * exp(-t * 8.5) * 0.55
                s to s
            }
            PerformancePadType.WOBBLE_BASS -> {
                val lfo = 0.5 + 0.5 * sin(2.0 * PI * 7.5 * t)
                phaseA += 2.0 * PI * 55.0 / sampleRateHz
                val s = (sin(phaseA) + 0.5 * lfo * sin(phaseA * 3.0)) * exp(-t * 3.5) * 0.7
                s to s
            }
            PerformancePadType.CHORD_STAB -> {
                phaseA += 2.0 * PI * 261.63 / sampleRateHz
                phaseB += 2.0 * PI * 329.63 / sampleRateHz
                val env = exp(-t * 7.0) * 0.45
                (sin(phaseA) + sin(phaseB)) * env to (sin(phaseA * 1.5) + sin(phaseB)) * env
            }
            PerformancePadType.RISER -> {
                val freq = 180.0 + t * 900.0
                phaseA += 2.0 * PI * freq / sampleRateHz
                val env = (t / 1.1).coerceIn(0.0, 1.0) * 0.55
                (sin(phaseA) * 0.5 + noise * 0.5) * env to (sin(phaseA) * 0.5 + noise * 0.5) * env
            }
            PerformancePadType.LASER_ZAP -> {
                val freq = 1400.0 * exp(-t * 11.0) + 90.0
                phaseA += 2.0 * PI * freq / sampleRateHz
                val s = sin(phaseA) * exp(-t * 7.0) * 0.6
                s to s
            }
            PerformancePadType.AIRHORN -> {
                phaseA += 2.0 * PI * 466.16 / sampleRateHz
                phaseB += 2.0 * PI * 471.0 / sampleRateHz
                val s = (sin(phaseA) + sin(phaseB) + 0.4 * sin(phaseA * 2.0)) * exp(-t * 1.4) * 0.35
                s to s
            }
            PerformancePadType.DUB_SIREN -> {
                val mod = 620.0 + 240.0 * sin(2.0 * PI * 8.0 * t)
                phaseA += 2.0 * PI * mod / sampleRateHz
                val s = sin(phaseA) * exp(-t * 2.2) * 0.48
                s * 0.8 to s * 1.1
            }
        }

        out[frameOffset * 2] += l.toFloat()
        out[frameOffset * 2 + 1] += r.toFloat()
        sampleIdx++
    }

    private fun absBurst(t: Double): Double {
        val mod = (t * 100.0) % 1.0
        return if (mod < 0.5) 1.0 else 0.3
    }
}
