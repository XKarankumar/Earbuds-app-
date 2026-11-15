package com.example.hometheatre.audio

import com.example.hometheatre.core.AudioTelemetrySnapshot
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Computes real-time RMS, Peak dBFS, Bass/Mid/Treble energy, onset beat detection,
 * and a 24-bin logarithmic frequency spectrum from live stereo PCM frames.
 */
class RealTimeAudioAnalyzer {
    private var bassLp = 0f
    private var midLp = 0f
    private var prevBassEnergy = 0f
    private var avgBassEnergy = 0.05f
    private var lastBeatMs = 0L

    private val goertzelFreqs = floatArrayOf(
        32f, 48f, 64f, 90f, 125f, 175f, 250f, 350f,
        500f, 700f, 1000f, 1400f, 2000f, 2800f, 3600f, 4800f,
        6000f, 7500f, 9000f, 10500f, 12000f, 13800f, 15500f, 17500f
    )
    private val smoothedBins = FloatArray(24)

    fun analyzeInterleavedStereo(
        interleaved: FloatArray,
        frameCount: Int,
        sampleRateHz: Int,
        limiterGainReductionDb: Float,
        clippingProtected: Boolean,
        isPlaying: Boolean,
        sourceLabel: String
    ): AudioTelemetrySnapshot {
        if (frameCount <= 0 || !isPlaying) {
            for (i in smoothedBins.indices) smoothedBins[i] *= 0.78f
            return AudioTelemetrySnapshot(
                spectrumBins = smoothedBins.toList(),
                sampleRateHz = sampleRateHz,
                isPlaying = isPlaying,
                activeSourceLabel = sourceLabel
            )
        }

        var sumSqL = 0.0
        var sumSqR = 0.0
        var peak = 0f
        var bassSq = 0.0
        var midSq = 0.0
        var trebleSq = 0.0

        val dt = 1f / sampleRateHz.coerceAtLeast(8000)
        val bassAlpha = (2f * PI.toFloat() * 130f * dt) / (1f + 2f * PI.toFloat() * 130f * dt)
        val midAlpha = (2f * PI.toFloat() * 2400f * dt) / (1f + 2f * PI.toFloat() * 2400f * dt)

        for (i in 0 until frameCount) {
            val l = interleaved[i * 2]
            val r = interleaved[i * 2 + 1]
            val mono = 0.5f * (l + r)
            sumSqL += l * l
            sumSqR += r * r
            peak = max(peak, max(abs(l), abs(r)))

            bassLp += bassAlpha * (mono - bassLp)
            midLp += midAlpha * (mono - midLp)
            val midBand = midLp - bassLp
            val trebleBand = mono - midLp

            bassSq += bassLp * bassLp
            midSq += midBand * midBand
            trebleSq += trebleBand * trebleBand
        }

        val rmsL = sqrt(sumSqL / frameCount).toFloat().coerceIn(0f, 1f)
        val rmsR = sqrt(sumSqR / frameCount).toFloat().coerceIn(0f, 1f)
        val bassEnergy = (sqrt(bassSq / frameCount).toFloat() * 2.4f).coerceIn(0f, 1f)
        val midEnergy = (sqrt(midSq / frameCount).toFloat() * 2.8f).coerceIn(0f, 1f)
        val trebleEnergy = (sqrt(trebleSq / frameCount).toFloat() * 3.4f).coerceIn(0f, 1f)

        val nowMs = System.currentTimeMillis()
        avgBassEnergy = avgBassEnergy * 0.92f + bassEnergy * 0.08f
        val beat = bassEnergy > (avgBassEnergy * 1.38f + 0.08f) &&
            bassEnergy > prevBassEnergy &&
            (nowMs - lastBeatMs) > 180L
        if (beat) lastBeatMs = nowMs
        prevBassEnergy = bassEnergy

        // Lightweight Goertzel 24-band spectrum over up to 256 mono samples
        val windowLen = minOf(frameCount, 256)
        for (b in goertzelFreqs.indices) {
            val freq = goertzelFreqs[b]
            val k = (0.5 + (windowLen * freq / sampleRateHz.toDouble())).toInt()
            val omega = (2.0 * PI * k) / windowLen
            val coeff = 2.0 * cos(omega)
            var q0 = 0.0
            var q1 = 0.0
            var q2 = 0.0
            for (n in 0 until windowLen) {
                val mono = 0.5 * (interleaved[n * 2] + interleaved[n * 2 + 1])
                val hann = 0.5 * (1.0 - cos(2.0 * PI * n / (windowLen - 1).coerceAtLeast(1)))
                q0 = coeff * q1 - q2 + mono * hann
                q2 = q1
                q1 = q0
            }
            val magSq = q1 * q1 + q2 * q2 - q1 * q2 * coeff
            val norm = (sqrt(max(0.0, magSq)) / (windowLen * 0.25)).toFloat().coerceIn(0f, 1f)
            smoothedBins[b] = max(norm, smoothedBins[b] * 0.72f)
        }

        val peakDbfs = if (peak > 0.0001f) {
            (20f * (ln(peak) / ln(10f))).coerceIn(-60f, 0f)
        } else {
            -60f
        }

        return AudioTelemetrySnapshot(
            rmsLeft = rmsL,
            rmsRight = rmsR,
            peakDbfs = peakDbfs,
            bassEnergy = bassEnergy,
            midEnergy = midEnergy,
            trebleEnergy = trebleEnergy,
            beatDetected = beat,
            limiterGainReductionDb = limiterGainReductionDb,
            isClippingProtected = clippingProtected,
            spectrumBins = smoothedBins.toList(),
            sampleRateHz = sampleRateHz,
            isPlaying = true,
            activeSourceLabel = sourceLabel
        )
    }
}
