package com.example.hometheatre.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import com.example.hometheatre.core.UserAudioPreferences

/**
 * Safely binds Android platform audio effects (Equalizer, BassBoost, LoudnessEnhancer,
 * and API 28+ DynamicsProcessing) to an active audio session ID, with clean fallback
 * on Android 8.0 (API 26-27) or devices that restrict AudioEffect creation.
 */
class HardwareEffectsBridge {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var dynamicsProcessing: Any? = null
    private var attachedSessionId: Int = 0

    @Volatile
    var hardwareEffectsBound: Boolean = false
        private set

    fun attachToSession(sessionId: Int, prefs: UserAudioPreferences) {
        if (sessionId <= 0) return
        if (attachedSessionId == sessionId && hardwareEffectsBound) {
            applyPreferences(prefs)
            return
        }
        release()
        attachedSessionId = sessionId

        var anyBound = false
        try {
            equalizer = Equalizer(0, sessionId).apply { enabled = true }
            anyBound = true
        } catch (_: Throwable) {
            equalizer = null
        }

        try {
            bassBoost = BassBoost(0, sessionId).apply { enabled = true }
            anyBound = true
        } catch (_: Throwable) {
            bassBoost = null
        }

        try {
            loudnessEnhancer = LoudnessEnhancer(sessionId).apply { enabled = prefs.loudnessGainDb > 0.1f }
            anyBound = true
        } catch (_: Throwable) {
            loudnessEnhancer = null
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val config = DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    2,
                    true,
                    10,
                    true,
                    4,
                    true,
                    4,
                    true
                ).build()
                dynamicsProcessing = DynamicsProcessing(0, sessionId, config).apply {
                    enabled = true
                }
                anyBound = true
            } catch (_: Throwable) {
                dynamicsProcessing = null
            }
        }

        hardwareEffectsBound = anyBound
        applyPreferences(prefs)
    }

    fun applyPreferences(prefs: UserAudioPreferences) {
        try {
            bassBoost?.let { bb ->
                if (bb.strengthSupported) {
                    val strength = (prefs.bassBoostLevel * 1000f).toInt().coerceIn(0, 1000)
                    bb.setStrength(strength.toShort())
                }
            }
        } catch (_: Throwable) {
        }

        try {
            loudnessEnhancer?.let { le ->
                val targetGainMb = (prefs.loudnessGainDb * 100f).toInt().coerceIn(0, 1200)
                le.enabled = targetGainMb > 0
                if (targetGainMb > 0) {
                    le.setTargetGain(targetGainMb)
                }
            }
        } catch (_: Throwable) {
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            (dynamicsProcessing as? DynamicsProcessing)?.let { dp ->
                try {
                    val limiter = dp.getLimiterByChannelIndex(0)
                    limiter.isEnabled = prefs.limiterEnabled
                    dp.setLimiterAllChannelsTo(limiter)
                } catch (_: Throwable) {
                }
            }
        }
    }

    fun release() {
        try { equalizer?.release() } catch (_: Throwable) {}
        try { bassBoost?.release() } catch (_: Throwable) {}
        try { loudnessEnhancer?.release() } catch (_: Throwable) {}
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try { (dynamicsProcessing as? DynamicsProcessing)?.release() } catch (_: Throwable) {}
        }
        equalizer = null
        bassBoost = null
        loudnessEnhancer = null
        dynamicsProcessing = null
        attachedSessionId = 0
        hardwareEffectsBound = false
    }
}
