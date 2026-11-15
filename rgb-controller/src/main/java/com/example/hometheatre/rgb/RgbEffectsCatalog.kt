package com.example.hometheatre.rgb

import android.graphics.Color
import com.example.hometheatre.core.AudioTelemetrySnapshot
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class RgbCategory(val label: String) {
    MUSIC_REACTIVE("Music Reactive"),
    BASS_BEAT_SYNC("Bass Beat Sync"),
    RAINBOW("Rainbow"),
    BREATHING("Breathing"),
    STROBE("Strobe"),
    CINEMA_AMBILIGHT("Cinema Ambilight"),
    CYBER_NEON("Cyber Neon"),
    NATURE_AURORA("Aurora & Fire"),
    MATRIX_CHASE("Matrix & Chase")
}

data class RgbEffectDefinition(
    val id: Int,
    val name: String,
    val category: RgbCategory,
    val variantIndex: Int
)

/**
 * Provides 108 distinct procedural RGB lighting effects (12 variations across 9 categories)
 * reacting in real time to bass energy, beat detection, treble transients, speed, brightness,
 * and user-chosen custom RGB colors.
 */
object RgbEffectsCatalog {
    private val categoryPrefixes = mapOf(
        RgbCategory.MUSIC_REACTIVE to listOf(
            "Spectrum VU", "Harmonic Pulse", "Tri-Band Flow", "Vocal Shimmer",
            "Treble Sparkle", "RMS Wave", "Stereo Mirror", "Stage Wash",
            "Liquid Equalizer", "Dynamic Ribbon", "Transient Flash", "Studio Meter"
        ),
        RgbCategory.BASS_BEAT_SYNC to listOf(
            "Subwoofer Kick", "808 Shockwave", "LFE Earthquake", "Beat Strobe",
            "Bass Cannon", "Punch Ripple", "Sub Heartbeat", "Club Impact",
            "Center Boom", "Drop Detonator", "Kick Chase", "Deep Thump"
        ),
        RgbCategory.RAINBOW to listOf(
            "Prism Cycle", "Liquid Rainbow", "Dual Helix", "Pastel Drift",
            "Hyper Spectrum", "Chromatic Wave", "Spectral Glaze", "Color Wheel",
            "Iridescent Tide", "Velvet Rainbow", "Neon Spectrum", "Prism Mirror"
        ),
        RgbCategory.BREATHING to listOf(
            "Obsidian Breath", "Zen Pulse", "Dual Breath", "Candle Bloom",
            "Tidal Sigh", "Velvet Fade", "Silk Swell", "Moonlight Halo",
            "Soft Aura", "Deep Calm", "Warm Lounge", "Midnight Mist"
        ),
        RgbCategory.STROBE to listOf(
            "Club Strobe", "Alternating Flash", "Rave Shutter", "Lightning Bolt",
            "Syncopated Flash", "Laser Burst", "Dual Pulse", "Hyper Blink",
            "Camera Flash", "Thunder Crack", "Edge Strobe", "Arena Impact"
        ),
        RgbCategory.CINEMA_AMBILIGHT to listOf(
            "IMAX Bias 6500K", "Theatre Amber", "Sci-Fi Cyan", "Noir Silver",
            "Action Flare", "Horizon Glow", "Dolby Bias Warm", "Projector Halo",
            "Sunset Cinema", "Velvet Curtain", "Screen Mirror Sim", "Midnight Frame"
        ),
        RgbCategory.CYBER_NEON to listOf(
            "Synthwave 1984", "Cyberpunk Cyan", "Laser Grid", "Tokyo Rain",
            "Plasma Arc", "Vaporwave Sunset", "Electric Violet", "Hyperdrive",
            "Neon Blade", "Outrun Horizon", "Ion Thruster", "Matrix Glitch"
        ),
        RgbCategory.NATURE_AURORA to listOf(
            "Borealis Green", "Solar Flare", "Ember Hearth", "Ocean Abyss",
            "Lava Magma", "Forest Canopy", "Arctic Ice", "Campfire Sparks",
            "Northern Sky", "Volcanic Glow", "Coral Reef", "Starlight Drift"
        ),
        RgbCategory.MATRIX_CHASE to listOf(
            "Knight Scanner", "Comet Tail", "Theater Marquee", "Twinkle Stars",
            "Binary Rain", "Meteor Shower", "Ping Pong", "Orbiting Nodes",
            "Laser Scan", "Spark Cascade", "Quantum Hop", "Pixel Waterfall"
        )
    )

    val allEffects: List<RgbEffectDefinition> = buildList {
        var nextId = 1
        for (cat in RgbCategory.entries) {
            val names = categoryPrefixes[cat].orEmpty()
            names.forEachIndexed { idx, name ->
                add(RgbEffectDefinition(id = nextId++, name = name, category = cat, variantIndex = idx))
            }
        }
    }

    fun findById(id: Int): RgbEffectDefinition =
        allEffects.firstOrNull { it.id == id } ?: allEffects.first()

    fun renderFrame(
        effectId: Int,
        ledCount: Int,
        timeMs: Long,
        brightness: Float,
        speed: Float,
        customColorArgb: Int,
        telemetry: AudioTelemetrySnapshot
    ): IntArray {
        val effect = findById(effectId)
        val out = IntArray(ledCount.coerceAtLeast(1))
        val safeBright = brightness.coerceIn(0.05f, 1f)
        val t = (timeMs / 1000.0) * (0.25 + speed.coerceIn(0.05f, 1f) * 2.75)
        val v = effect.variantIndex
        val customHue = argbToHue(customColorArgb)

        val bass = telemetry.bassEnergy.coerceIn(0f, 1f)
        val mid = telemetry.midEnergy.coerceIn(0f, 1f)
        val treble = telemetry.trebleEnergy.coerceIn(0f, 1f)
        val rms = ((telemetry.rmsLeft + telemetry.rmsRight) * 0.5f).coerceIn(0f, 1f)

        for (i in 0 until ledCount) {
            val pos = i.toFloat() / max(1, ledCount - 1)
            val centerDist = abs(pos - 0.5f) * 2f

            val color = when (effect.category) {
                RgbCategory.MUSIC_REACTIVE -> {
                    val level = (rms * 1.4f + bass * 0.5f).coerceIn(0.08f, 1f)
                    val active = if (v % 2 == 0) (centerDist <= level) else (pos <= level)
                    val hue = (customHue + pos * 90f + v * 15f + treble * 60f) % 360f
                    val intensity = if (active) safeBright * (0.4f + 0.6f * level) else safeBright * 0.08f
                    hsv(hue, 0.9f, intensity)
                }
                RgbCategory.BASS_BEAT_SYNC -> {
                    val pulse = if (telemetry.beatDetected) 1f else (0.12f + bass * 0.88f)
                    val wave = max(0f, 1f - abs(centerDist - (t % 1.0).toFloat() * 1.1f) * 3f)
                    val valScale = if (v % 3 == 0) pulse else max(pulse * 0.7f, wave * bass)
                    val hue = (customHue + v * 24f + if (telemetry.beatDetected) 45f else 0f) % 360f
                    hsv(hue, 0.95f, safeBright * valScale.coerceIn(0.06f, 1f))
                }
                RgbCategory.RAINBOW -> {
                    val freq = 1f + (v % 4) * 0.5f
                    val hue = ((pos * 360f * freq) + (t * 95.0) + v * 18.0).toFloat() % 360f
                    val sat = if (v % 3 == 1) 0.65f else 1f
                    hsv(if (hue < 0) hue + 360f else hue, sat, safeBright)
                }
                RgbCategory.BREATHING -> {
                    val phase = (sin(t * 1.6 + pos * (v % 4) * 0.5) * 0.5 + 0.5).toFloat()
                    val hue = (customHue + v * 28f) % 360f
                    hsv(hue, 0.85f, safeBright * (0.1f + 0.9f * phase))
                }
                RgbCategory.STROBE -> {
                    val rate = 6.0 + (v % 6) * 2.5
                    val gate = sin(t * rate * PI) > (0.25 - bass * 0.35)
                    val alt = (v % 2 == 1) && ((i + (t * 8).toInt()) % 2 == 0)
                    val on = gate && !alt
                    val hue = (customHue + v * 30f) % 360f
                    val sat = if (v % 3 == 0) 0.1f else 0.95f
                    hsv(hue, sat, if (on) safeBright else 0.02f)
                }
                RgbCategory.CINEMA_AMBILIGHT -> {
                    val baseHue = when (v % 4) {
                        0 -> 38f
                        1 -> 195f
                        2 -> 265f
                        else -> customHue
                    }
                    val shimmer = (0.75f + 0.25f * sin(t * 0.9 + pos * 3.0).toFloat()) *
                        (0.7f + 0.3f * rms)
                    val sat = if (v == 0) 0.12f else 0.72f
                    hsv(baseHue, sat, safeBright * shimmer)
                }
                RgbCategory.CYBER_NEON -> {
                    val wave = (sin(pos * 6.0 - t * 3.2 + v) * 0.5 + 0.5).toFloat()
                    val hue = 185f + wave * 125f
                    hsv(hue % 360f, 0.96f, safeBright * (0.45f + 0.55f * wave + mid * 0.2f).coerceIn(0f, 1f))
                }
                RgbCategory.NATURE_AURORA -> {
                    val isFire = v % 3 == 1
                    val wave = (sin(pos * 5.0 + t * 2.1 + v) * cos(pos * 3.0 - t * 1.4) * 0.5 + 0.5).toFloat()
                    val hue = if (isFire) 8f + wave * 36f else 135f + wave * 95f
                    hsv(hue, 0.92f, safeBright * (0.3f + 0.7f * wave))
                }
                RgbCategory.MATRIX_CHASE -> {
                    val head = ((t * (1.2 + (v % 3) * 0.4)) % 1.0).toFloat()
                    val dist = abs(pos - head)
                    val glow = max(0f, 1f - dist * (5f + (v % 4)))
                    val hue = (customHue + v * 25f + pos * 40f) % 360f
                    hsv(hue, 0.95f, safeBright * max(0.06f, glow))
                }
            }
            out[i] = color
        }
        return out
    }

    private fun argbToHue(argb: Int): Float {
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        val maxC = max(r, max(g, b))
        val minC = min(r, min(g, b))
        val delta = maxC - minC
        if (delta < 0.0001f) return 195f
        val raw = when (maxC) {
            r -> ((g - b) / delta) % 6f
            g -> (b - r) / delta + 2f
            else -> (r - g) / delta + 4f
        } * 60f
        return if (raw < 0f) raw + 360f else raw
    }

    private fun hsv(hue: Float, sat: Float, value: Float): Int {
        val h = ((hue % 360f) + 360f) % 360f
        val s = sat.coerceIn(0f, 1f)
        val v = value.coerceIn(0f, 1f)
        val c = v * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = v - c
        val (rf, gf, bf) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        val r = ((rf + m) * 255f).toInt().coerceIn(0, 255)
        val g = ((gf + m) * 255f).toInt().coerceIn(0, 255)
        val b = ((bf + m) * 255f).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}
