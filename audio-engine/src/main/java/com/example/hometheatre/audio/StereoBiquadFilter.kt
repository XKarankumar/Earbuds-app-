package com.example.hometheatre.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Stereo 2nd-order RBJ biquad filter for zero-allocation real-time DSP.
 */
class StereoBiquadFilter {
    @Volatile private var b0 = 1.0
    @Volatile private var b1 = 0.0
    @Volatile private var b2 = 0.0
    @Volatile private var a1 = 0.0
    @Volatile private var a2 = 0.0

    private var x1L = 0.0
    private var x2L = 0.0
    private var y1L = 0.0
    private var y2L = 0.0

    private var x1R = 0.0
    private var x2R = 0.0
    private var y1R = 0.0
    private var y2R = 0.0

    fun resetState() {
        x1L = 0.0; x2L = 0.0; y1L = 0.0; y2L = 0.0
        x1R = 0.0; x2R = 0.0; y1R = 0.0; y2R = 0.0
    }

    fun configurePeaking(freqHz: Double, q: Double, gainDb: Double, sampleRate: Double) {
        if (kotlin.math.abs(gainDb) < 0.05) {
            setBypass()
            return
        }
        val safeFreq = freqHz.coerceIn(20.0, sampleRate * 0.45)
        val a = 10.0.pow(gainDb / 40.0)
        val omega = 2.0 * PI * safeFreq / sampleRate
        val sn = sin(omega)
        val cs = cos(omega)
        val alpha = sn / (2.0 * q.coerceAtLeast(0.25))

        val normB0 = 1.0 + alpha * a
        val normB1 = -2.0 * cs
        val normB2 = 1.0 - alpha * a
        val normA0 = 1.0 + alpha / a
        val normA1 = -2.0 * cs
        val normA2 = 1.0 - alpha / a

        b0 = normB0 / normA0
        b1 = normB1 / normA0
        b2 = normB2 / normA0
        a1 = normA1 / normA0
        a2 = normA2 / normA0
    }

    fun configureLowShelf(freqHz: Double, gainDb: Double, sampleRate: Double, slope: Double = 0.85) {
        if (kotlin.math.abs(gainDb) < 0.05) {
            setBypass()
            return
        }
        val safeFreq = freqHz.coerceIn(20.0, sampleRate * 0.45)
        val a = 10.0.pow(gainDb / 40.0)
        val omega = 2.0 * PI * safeFreq / sampleRate
        val sn = sin(omega)
        val cs = cos(omega)
        val alpha = sn / 2.0 * sqrt((a + 1.0 / a) * (1.0 / slope - 1.0) + 2.0)
        val beta = 2.0 * sqrt(a) * alpha

        val normB0 = a * ((a + 1.0) - (a - 1.0) * cs + beta)
        val normB1 = 2.0 * a * ((a - 1.0) - (a + 1.0) * cs)
        val normB2 = a * ((a + 1.0) - (a - 1.0) * cs - beta)
        val normA0 = (a + 1.0) + (a - 1.0) * cs + beta
        val normA1 = -2.0 * ((a - 1.0) + (a + 1.0) * cs)
        val normA2 = (a + 1.0) + (a - 1.0) * cs - beta

        b0 = normB0 / normA0
        b1 = normB1 / normA0
        b2 = normB2 / normA0
        a1 = normA1 / normA0
        a2 = normA2 / normA0
    }

    fun configureHighShelf(freqHz: Double, gainDb: Double, sampleRate: Double, slope: Double = 0.85) {
        if (kotlin.math.abs(gainDb) < 0.05) {
            setBypass()
            return
        }
        val safeFreq = freqHz.coerceIn(20.0, sampleRate * 0.45)
        val a = 10.0.pow(gainDb / 40.0)
        val omega = 2.0 * PI * safeFreq / sampleRate
        val sn = sin(omega)
        val cs = cos(omega)
        val alpha = sn / 2.0 * sqrt((a + 1.0 / a) * (1.0 / slope - 1.0) + 2.0)
        val beta = 2.0 * sqrt(a) * alpha

        val normB0 = a * ((a + 1.0) + (a - 1.0) * cs + beta)
        val normB1 = -2.0 * a * ((a - 1.0) + (a + 1.0) * cs)
        val normB2 = a * ((a + 1.0) + (a - 1.0) * cs - beta)
        val normA0 = (a + 1.0) - (a - 1.0) * cs + beta
        val normA1 = 2.0 * ((a - 1.0) - (a + 1.0) * cs)
        val normA2 = (a + 1.0) - (a - 1.0) * cs - beta

        b0 = normB0 / normA0
        b1 = normB1 / normA0
        b2 = normB2 / normA0
        a1 = normA1 / normA0
        a2 = normA2 / normA0
    }

    fun configureLowPass(freqHz: Double, q: Double, sampleRate: Double) {
        val safeFreq = freqHz.coerceIn(40.0, sampleRate * 0.45)
        val omega = 2.0 * PI * safeFreq / sampleRate
        val sn = sin(omega)
        val cs = cos(omega)
        val alpha = sn / (2.0 * q.coerceAtLeast(0.5))

        val normB0 = (1.0 - cs) / 2.0
        val normB1 = 1.0 - cs
        val normB2 = (1.0 - cs) / 2.0
        val normA0 = 1.0 + alpha
        val normA1 = -2.0 * cs
        val normA2 = 1.0 - alpha

        b0 = normB0 / normA0
        b1 = normB1 / normA0
        b2 = normB2 / normA0
        a1 = normA1 / normA0
        a2 = normA2 / normA0
    }

    fun configureHighPass(freqHz: Double, q: Double, sampleRate: Double) {
        val safeFreq = freqHz.coerceIn(25.0, sampleRate * 0.45)
        val omega = 2.0 * PI * safeFreq / sampleRate
        val sn = sin(omega)
        val cs = cos(omega)
        val alpha = sn / (2.0 * q.coerceAtLeast(0.5))

        val normB0 = (1.0 + cs) / 2.0
        val normB1 = -(1.0 + cs)
        val normB2 = (1.0 + cs) / 2.0
        val normA0 = 1.0 + alpha
        val normA1 = -2.0 * cs
        val normA2 = 1.0 - alpha

        b0 = normB0 / normA0
        b1 = normB1 / normA0
        b2 = normB2 / normA0
        a1 = normA1 / normA0
        a2 = normA2 / normA0
    }

    fun setBypass() {
        b0 = 1.0; b1 = 0.0; b2 = 0.0; a1 = 0.0; a2 = 0.0
    }

    fun processLeft(input: Float): Float {
        val x = input.toDouble()
        val y = b0 * x + b1 * x1L + b2 * x2L - a1 * y1L - a2 * y2L
        x2L = x1L
        x1L = x
        y2L = y1L
        y1L = y
        return y.toFloat()
    }

    fun processRight(input: Float): Float {
        val x = input.toDouble()
        val y = b0 * x + b1 * x1R + b2 * x2R - a1 * y1R - a2 * y2R
        x2R = x1R
        x1R = x
        y2R = y1R
        y1R = y
        return y.toFloat()
    }
}
