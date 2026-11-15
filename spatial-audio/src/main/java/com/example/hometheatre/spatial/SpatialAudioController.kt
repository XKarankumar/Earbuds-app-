package com.example.hometheatre.spatial

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.Spatializer
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.hometheatre.core.SpatialCapabilityStatus
import com.example.hometheatre.core.SpatialMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Integrates Android's platform Spatializer API (API 32+) and Head Tracking (API 33+)
 * while providing a transparent Virtual Surround DSP fallback for API 26-31 or devices
 * without OEM Spatializer support.
 */
class SpatialAudioController(context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _status = MutableStateFlow(readCurrentStatus(SpatialMode.CINEMA))
    val status: StateFlow<SpatialCapabilityStatus> = _status.asStateFlow()

    private var activeMode: SpatialMode = SpatialMode.CINEMA
    private var stateListenerRegistered = false

    @RequiresApi(Build.VERSION_CODES.S_V2)
    private var spatializerListener: Spatializer.OnSpatializerStateChangedListener? = null

    fun startMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2 && !stateListenerRegistered) {
            registerSpatializerListenerApi32()
        }
        refreshStatus()
    }

    fun stopMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2 && stateListenerRegistered) {
            unregisterSpatializerListenerApi32()
        }
    }

    fun setSpatialMode(mode: SpatialMode) {
        activeMode = mode
        refreshStatus()
    }

    fun refreshStatus() {
        _status.value = readCurrentStatus(activeMode)
    }

    /**
     * Checks whether the current audio route can spatialize a given AudioAttributes + AudioFormat.
     */
    fun canSpatializeMultichannel(usage: Int = AudioAttributes.USAGE_MEDIA): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S_V2) return false
        return try {
            val spatializer = audioManager.spatializer
            val attrs = AudioAttributes.Builder()
                .setUsage(usage)
                .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                .build()
            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_5POINT1)
                .build()
            spatializer.isAvailable && spatializer.isEnabled && spatializer.canBeSpatialized(attrs, format)
        } catch (_: Throwable) {
            false
        }
    }

    fun recommendedAudioAttributes(mode: SpatialMode): AudioAttributes {
        val usage = when (mode) {
            SpatialMode.GAMING -> AudioAttributes.USAGE_GAME
            else -> AudioAttributes.USAGE_MEDIA
        }
        val contentType = when (mode) {
            SpatialMode.CINEMA -> AudioAttributes.CONTENT_TYPE_MOVIE
            SpatialMode.MUSIC, SpatialMode.VIRTUAL_SURROUND -> AudioAttributes.CONTENT_TYPE_MUSIC
            SpatialMode.GAMING -> AudioAttributes.CONTENT_TYPE_SONIFICATION
            SpatialMode.OFF -> AudioAttributes.CONTENT_TYPE_MUSIC
        }
        val builder = AudioAttributes.Builder()
            .setUsage(usage)
            .setContentType(contentType)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setAllowedCapturePolicy(AudioAttributes.ALLOW_CAPTURE_BY_ALL)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2 && mode == SpatialMode.OFF) {
            builder.setIsContentSpatialized(false)
        }
        return builder.build()
    }

    private fun readCurrentStatus(mode: SpatialMode): SpatialCapabilityStatus {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S_V2) {
            val fallbackActive = mode != SpatialMode.OFF
            return SpatialCapabilityStatus(
                apiSupported = false,
                isAvailable = false,
                isEnabled = false,
                immersiveLevelLabel = "Unavailable (Requires Android 12L+)",
                headTrackerAvailable = false,
                headTrackerEnabled = false,
                fallbackVirtualizerActive = fallbackActive,
                diagnosticNote = if (fallbackActive) {
                    "Android ${Build.VERSION.RELEASE} lacks system Spatializer API. Using custom Virtual Surround DSP."
                } else {
                    "System Spatializer API unavailable on Android ${Build.VERSION.RELEASE}; Stereo bypass active."
                }
            )
        }

        return readApi32Status(mode)
    }

    @RequiresApi(Build.VERSION_CODES.S_V2)
    private fun readApi32Status(mode: SpatialMode): SpatialCapabilityStatus {
        val spatializer = try {
            audioManager.spatializer
        } catch (_: Throwable) {
            null
        }
        if (spatializer == null) {
            return SpatialCapabilityStatus(
                apiSupported = true,
                isAvailable = false,
                isEnabled = false,
                immersiveLevelLabel = "No Spatializer Service",
                headTrackerAvailable = false,
                headTrackerEnabled = false,
                fallbackVirtualizerActive = mode != SpatialMode.OFF,
                diagnosticNote = "Device OEM did not expose a system Spatializer effect; DSP virtualizer available."
            )
        }

        val levelLabel = when (spatializer.immersiveAudioLevel) {
            Spatializer.SPATIALIZER_IMMERSIVE_LEVEL_MULTICHANNEL -> "Multichannel Spatializer"
            Spatializer.SPATIALIZER_IMMERSIVE_LEVEL_OTHER -> "Bed + Object / OEM Spatializer"
            else -> "None (OEM Spatializer Inactive)"
        }
        val available = spatializer.isAvailable
        val enabled = spatializer.isEnabled
        val headAvailable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                spatializer.isHeadTrackerAvailable
            } catch (_: Throwable) {
                false
            }
        } else {
            false
        }

        val useFallback = mode == SpatialMode.VIRTUAL_SURROUND || (!available && mode != SpatialMode.OFF)
        val note = when {
            available && enabled -> "System Spatializer active ($levelLabel). Respecting Android audio routing."
            available && !enabled -> "Spatializer hardware supported; enable Spatial Audio in Android System Settings."
            useFallback -> "System Spatializer unavailable for current route; Software Virtual Surround DSP active."
            else -> "Spatializer unavailable on current output route."
        }

        return SpatialCapabilityStatus(
            apiSupported = true,
            isAvailable = available,
            isEnabled = enabled,
            immersiveLevelLabel = levelLabel,
            headTrackerAvailable = headAvailable,
            headTrackerEnabled = headAvailable && enabled,
            fallbackVirtualizerActive = useFallback,
            diagnosticNote = note
        )
    }

    @RequiresApi(Build.VERSION_CODES.S_V2)
    private fun registerSpatializerListenerApi32() {
        try {
            val listener = object : Spatializer.OnSpatializerStateChangedListener {
                override fun onSpatializerEnabledChanged(spat: Spatializer, enabled: Boolean) {
                    refreshStatus()
                }

                override fun onSpatializerAvailableChanged(spat: Spatializer, available: Boolean) {
                    refreshStatus()
                }
            }
            audioManager.spatializer.addOnSpatializerStateChangedListener({ it.run() }, listener)
            spatializerListener = listener
            stateListenerRegistered = true
        } catch (_: Throwable) {
            stateListenerRegistered = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.S_V2)
    private fun unregisterSpatializerListenerApi32() {
        try {
            spatializerListener?.let {
                audioManager.spatializer.removeOnSpatializerStateChangedListener(it)
            }
        } catch (_: Throwable) {
            // Safe ignore during teardown
        } finally {
            spatializerListener = null
            stateListenerRegistered = false
        }
    }
}
