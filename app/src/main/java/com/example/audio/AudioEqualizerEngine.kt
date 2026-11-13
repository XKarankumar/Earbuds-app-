package com.example.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.util.Log
import kotlin.math.roundToInt

class AudioEqualizerEngine {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    data class EngineStatus(
        val attached: Boolean,
        val message: String,
    )

    fun initialize(): EngineStatus {
        release()
        return try {
            val eq = Equalizer(0, 0).apply {
                enabled = true
            }
            equalizer = eq

            val bass = runCatching {
                BassBoost(0, 0).apply {
                    enabled = true
                }
            }.getOrNull()
            bassBoost = bass

            val bandCount = eq.numberOfBands.toInt()
            EngineStatus(
                attached = true,
                message = "Android AudioFX active ($bandCount-band hardware EQ)",
            )
        } catch (t: Throwable) {
            Log.w("AudioEqualizerEngine", "Global AudioEffect unavailable: ${t.message}")
            EngineStatus(
                attached = false,
                message = "Saved locally · Device restricts global session-0 AudioFX",
            )
        }
    }

    fun applyCurve(bandsDb: List<Float>, bassBoostFraction: Float): EngineStatus {
        val eq = equalizer ?: return EngineStatus(
            attached = false,
            message = "Saved in studio profile · AudioFX standby",
        )
        return try {
            val range = eq.bandLevelRange
            val minMb = range[0].toInt()
            val maxMb = range[1].toInt()
            val hwBands = eq.numberOfBands.toInt()

            for (bandIndex in 0 until hwBands) {
                val mappedDb = interpolateBandDb(bandsDb, bandIndex, hwBands)
                val millibels = (mappedDb * 100f).roundToInt().coerceIn(minMb, maxMb).toShort()
                eq.setBandLevel(bandIndex.toShort(), millibels)
            }

            bassBoost?.let { boost ->
                if (boost.strengthSupported) {
                    val strength = (bassBoostFraction.coerceIn(0f, 1f) * 1000f).roundToInt().toShort()
                    boost.setStrength(strength)
                }
            }

            EngineStatus(
                attached = true,
                message = "Applied live to Android audio output",
            )
        } catch (t: Throwable) {
            Log.w("AudioEqualizerEngine", "Failed to apply EQ curve: ${t.message}")
            EngineStatus(
                attached = false,
                message = "Curve saved · System audio mix locked",
            )
        }
    }

    private fun interpolateBandDb(uiBands: List<Float>, hwIndex: Int, hwCount: Int): Float {
        if (uiBands.isEmpty()) return 0f
        if (hwCount <= 1) return uiBands.first()
        val pos = hwIndex.toFloat() / (hwCount - 1).toFloat() * (uiBands.size - 1).toFloat()
        val low = pos.toInt().coerceIn(0, uiBands.lastIndex)
        val high = (low + 1).coerceIn(0, uiBands.lastIndex)
        val frac = pos - low
        return uiBands[low] * (1f - frac) + uiBands[high] * frac
    }

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        equalizer = null
        bassBoost = null
    }
}
