package com.example.hometheatre.core

import kotlinx.serialization.Serializable

@Serializable
enum class SpatialMode(val label: String, val subtitle: String) {
    OFF("Stereo Bypass", "Pure unprocessed 2.0 channel routing"),
    CINEMA("Cinema 3D", "Wide room reflections and center anchor clarity"),
    MUSIC("Concert Stage", "Natural acoustic width with cohesive imaging"),
    GAMING("Tactical Gaming", "Pinpoint directional cues and tight transient edge"),
    VIRTUAL_SURROUND("Virtual Surround DSP", "Software Haas/crossfeed virtualization fallback")
}

@Serializable
data class EqBand(
    val index: Int,
    val centerFreqHz: Int,
    val label: String,
    val gainDb: Float
)

@Serializable
data class EqPreset(
    val id: String,
    val name: String,
    val category: String,
    val bandsDb: List<Float>,
    val isBuiltIn: Boolean = false
)

object DefaultEqCatalog {
    val bandFrequenciesHz = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
    val bandLabels = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

    val builtInPresets = listOf(
        EqPreset(
            id = "flat_reference",
            name = "Reference Flat",
            category = "Studio",
            bandsDb = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
            isBuiltIn = true
        ),
        EqPreset(
            id = "cinema_reference",
            name = "Cinema IMAX",
            category = "Theatre",
            bandsDb = listOf(4.5f, 4.0f, 2.5f, 0.5f, -0.5f, 1.0f, 2.0f, 2.5f, 2.0f, 1.5f),
            isBuiltIn = true
        ),
        EqPreset(
            id = "deep_sub_club",
            name = "Subwoofer Club",
            category = "Bass",
            bandsDb = listOf(6.0f, 5.5f, 3.5f, 0.5f, -1.0f, 0f, 1.0f, 2.0f, 2.5f, 2.0f),
            isBuiltIn = true
        ),
        EqPreset(
            id = "vocal_clarity",
            name = "Dialogue & Vocal",
            category = "Theatre",
            bandsDb = listOf(-2.0f, -1.5f, -0.5f, 1.5f, 3.0f, 4.0f, 3.5f, 2.0f, 1.0f, 0.5f),
            isBuiltIn = true
        ),
        EqPreset(
            id = "night_theatre",
            name = "Late Night",
            category = "Theatre",
            bandsDb = listOf(-3.0f, -1.5f, 0.5f, 1.5f, 2.0f, 2.5f, 2.0f, 1.0f, 0f, -1.0f),
            isBuiltIn = true
        ),
        EqPreset(
            id = "audiophile_bright",
            name = "Planar Air",
            category = "Hi-Fi",
            bandsDb = listOf(1.5f, 1.0f, 0.5f, 0f, 0f, 0.5f, 1.5f, 3.0f, 4.0f, 4.5f),
            isBuiltIn = true
        )
    )
}

data class AudioTelemetrySnapshot(
    val rmsLeft: Float = 0f,
    val rmsRight: Float = 0f,
    val peakDbfs: Float = -60f,
    val bassEnergy: Float = 0f,
    val midEnergy: Float = 0f,
    val trebleEnergy: Float = 0f,
    val beatDetected: Boolean = false,
    val limiterGainReductionDb: Float = 0f,
    val isClippingProtected: Boolean = false,
    val spectrumBins: List<Float> = List(24) { 0f },
    val sampleRateHz: Int = 44100,
    val isPlaying: Boolean = false,
    val activeSourceLabel: String = "Idle"
)
