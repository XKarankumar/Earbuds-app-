package com.example.hometheatre.audio

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.tanh

/**
 * Soft-knee limiter and anti-clipping ceiling protection at -0.3 dBFS (0.966 linear).
 */
class SoftClipLimiter(
    private val ceilingLinear: Float = 0.966f,
    private val kneeThreshold: Float = 0.78f
) {
    @Volatile
    var lastGainReductionDb: Float = 0f
        private set

    @Volatile
    var protectionEngaged: Boolean = false
        private set

    fun processSample(sample: Float, enabled: Boolean): Float {
        if (!enabled) {
            protectionEngaged = false
            lastGainReductionDb = 0f
            return sample.coerceIn(-1f, 1f)
        }
        val mag = abs(sample)
        if (mag <= kneeThreshold) {
            return sample
        }
        val excess = (mag - kneeThreshold) / (1f - kneeThreshold).coerceAtLeast(0.05f)
        val compressed = kneeThreshold + (ceilingLinear - kneeThreshold) * tanh(excess)
        val sign = if (sample >= 0f) 1f else -1f
        val out = (sign * compressed).coerceIn(-ceilingLinear, ceilingLinear)
        if (mag > 0.0001f) {
            val ratio = (abs(out) / mag).coerceIn(0.01f, 1f)
            val reductionDb = (-20f * (ln(ratio) / ln(10f))).coerceAtLeast(0f)
            if (reductionDb > 0.1f) {
                protectionEngaged = true
                lastGainReductionDb = max(lastGainReductionDb * 0.92f, reductionDb)
            }
        }
        return out
    }

    fun decayTelemetry() {
        lastGainReductionDb *= 0.85f
        if (lastGainReductionDb < 0.08f) {
            lastGainReductionDb = 0f
            protectionEngaged = false
        }
    }
}
