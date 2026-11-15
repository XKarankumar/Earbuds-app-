package com.example.hometheatre.core

import kotlinx.serialization.Serializable

@Serializable
enum class ThemePreset(
    val displayName: String,
    val description: String
) {
    PREMIUM_DARK("Premium Dark", "Deep obsidian studio console with electric cyan meters"),
    MONOCHROME("Monochrome B&W", "High-contrast pure black & crisp white reference console"),
    SILVER_GLASS("Silver Glass", "Brushed anodized aluminum with frosted glass surfaces"),
    RGB_NEON("RGB Neon", "Kinetic magenta-cyan prism glow synced to studio ambiance"),
    CUSTOM("Custom Studio", "User-calibrated palette from the Custom Theme Editor")
}

@Serializable
data class CustomThemeConfig(
    val backgroundHex: Long = 0xFF0A0D14,
    val surfaceHex: Long = 0xFF141926,
    val primaryAccentHex: Long = 0xFF00E5FF,
    val secondaryAccentHex: Long = 0xFF8B5CF6,
    val textPrimaryHex: Long = 0xFFF8FAFC,
    val glassOpacity: Float = 0.72f,
    val glowIntensity: Float = 0.85f
)

@Serializable
data class UserAudioPreferences(
    val themePreset: ThemePreset = ThemePreset.PREMIUM_DARK,
    val customTheme: CustomThemeConfig = CustomThemeConfig(),
    val selectedEqPresetId: String = "cinema_reference",
    val customBandGainsDb: List<Float> = List(10) { 0f },
    val bassBoostLevel: Float = 0.25f,
    val deepBassEnabled: Boolean = false,
    val cinemaBassEnabled: Boolean = true,
    val subwooferGainDb: Float = 2.0f,
    val midGainDb: Float = 0f,
    val trebleGainDb: Float = 1.0f,
    val loudnessGainDb: Float = 0f,
    val limiterEnabled: Boolean = true,
    val safeVolumeWarningEnabled: Boolean = true,
    val spatialMode: SpatialMode = SpatialMode.CINEMA,
    val virtualSurroundEnabled: Boolean = false,
    val headTrackingEnabled: Boolean = false,
    val rgbEffectId: Int = 1,
    val rgbBrightness: Float = 0.8f,
    val rgbSpeed: Float = 0.6f,
    val rgbCustomColorHex: Long = 0xFF00E5FF,
    val wledHostIp: String = "",
    val esp32HostIp: String = "",
    val savedEqPresets: List<EqPreset> = emptyList(),
    val savedRgbPresets: List<SavedRgbPreset> = emptyList()
)

@Serializable
data class SavedRgbPreset(
    val id: String,
    val name: String,
    val effectId: Int,
    val brightness: Float,
    val speed: Float,
    val customColorHex: Long
)
