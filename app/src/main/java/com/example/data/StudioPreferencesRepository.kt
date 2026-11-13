package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AncStrength
import com.example.model.AnimationStyle
import com.example.model.EarbudControlsConfig
import com.example.model.EqPreset
import com.example.model.GestureAction
import com.example.model.LayoutDensity
import com.example.model.NoiseControlMode
import com.example.model.NotificationStyle
import com.example.model.PopupStyle
import com.example.model.ThemePreference
import com.example.model.UiCustomizationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.studioDataStore by preferencesDataStore(name = "ck_buds_studio_prefs")

data class PersistedStudioPrefs(
    val noiseControlMode: NoiseControlMode = NoiseControlMode.ANC,
    val ancStrength: AncStrength = AncStrength.HIGH,
    val personalisedAnc: Boolean = true,
    val windNoiseReduction: Boolean = false,
    val transparencyVoiceFocus: Boolean = true,
    val dualConnection: Boolean = false,
    val inEarDetection: Boolean = true,
    val lowLatencyMode: Boolean = false,
    val eqPreset: EqPreset = EqPreset.BALANCED,
    val eqBandsDb: List<Float> = EqPreset.BALANCED.bandsDb,
    val bassBoostStrength: Float = 0.25f,
    val controls: EarbudControlsConfig = EarbudControlsConfig(),
    val customization: UiCustomizationConfig = UiCustomizationConfig(),
    val lowBatteryThreshold: Int = 20,
    val lastDeviceAddress: String? = null,
    val lastDeviceName: String? = null,
    val rememberedDeviceAddresses: Set<String> = emptySet(),
)

class StudioPreferencesRepository(private val context: Context) {

    private object Keys {
        val NOISE_MODE = stringPreferencesKey("noise_mode")
        val ANC_STRENGTH = stringPreferencesKey("anc_strength")
        val PERSONALISED_ANC = booleanPreferencesKey("personalised_anc")
        val WIND_REDUCTION = booleanPreferencesKey("wind_reduction")
        val TRANSPARENCY_VOICE = booleanPreferencesKey("transparency_voice")
        val DUAL_CONNECTION = booleanPreferencesKey("dual_connection")
        val IN_EAR_DETECTION = booleanPreferencesKey("in_ear_detection")
        val LOW_LATENCY = booleanPreferencesKey("low_latency")
        val EQ_PRESET = stringPreferencesKey("eq_preset")
        val EQ_BANDS = stringPreferencesKey("eq_bands_csv")
        val BASS_BOOST = floatPreferencesKey("bass_boost")
        val TAP_LEFT = stringPreferencesKey("tap_left")
        val TAP_RIGHT = stringPreferencesKey("tap_right")
        val TRIPLE_TAP = stringPreferencesKey("triple_tap")
        val PRESS_HOLD = stringPreferencesKey("press_hold")
        val LAST_DEVICE_ADDR = stringPreferencesKey("last_device_addr")
        val LAST_DEVICE_NAME = stringPreferencesKey("last_device_name")
        val REMEMBERED_ADDRESSES = stringSetPreferencesKey("remembered_device_addresses")
        val LOW_BATTERY_THRESHOLD = intPreferencesKey("low_battery_threshold")

        // White Liquid Glass & Animation keys
        val THEME_PREF = stringPreferencesKey("theme_pref_v2")
        val POPUP_STYLE = stringPreferencesKey("popup_style")
        val ANIMATION_STYLE = stringPreferencesKey("animation_style")
        val NOTIFICATION_STYLE = stringPreferencesKey("notification_style")
        val GLASS_ALPHA = floatPreferencesKey("glass_alpha")
        val GLASS_BLUR_DP = floatPreferencesKey("glass_blur_dp")
        val SILVER_REFLECTION = floatPreferencesKey("silver_reflection")
        val CARD_CORNER_RADIUS = floatPreferencesKey("card_corner_radius")
        val ANIM_SPEED = floatPreferencesKey("anim_speed")
        val ENABLE_3D_ORBIT = booleanPreferencesKey("enable_3d_orbit")
        val LAYOUT_DENSITY = stringPreferencesKey("layout_density")
        val SHOW_EQ_HOME = booleanPreferencesKey("show_eq_home")
        val AUTO_POPUP_ENABLED = booleanPreferencesKey("auto_popup_enabled")
        val BG_MONITOR_ENABLED = booleanPreferencesKey("bg_monitor_enabled")
    }

    val prefsFlow: Flow<PersistedStudioPrefs> = context.studioDataStore.data.map { prefs ->
        val preset = prefs[Keys.EQ_PRESET]
            ?.let { runCatching { EqPreset.valueOf(it) }.getOrNull() }
            ?: EqPreset.BALANCED

        val parsedBands = prefs[Keys.EQ_BANDS]
            ?.split(",")
            ?.mapNotNull { it.toFloatOrNull() }
            ?.takeIf { it.size == 5 }
            ?: preset.bandsDb

        PersistedStudioPrefs(
            noiseControlMode = prefs[Keys.NOISE_MODE]
                ?.let { runCatching { NoiseControlMode.valueOf(it) }.getOrNull() }
                ?: NoiseControlMode.ANC,
            ancStrength = prefs[Keys.ANC_STRENGTH]
                ?.let { runCatching { AncStrength.valueOf(it) }.getOrNull() }
                ?: AncStrength.HIGH,
            personalisedAnc = prefs[Keys.PERSONALISED_ANC] ?: true,
            windNoiseReduction = prefs[Keys.WIND_REDUCTION] ?: false,
            transparencyVoiceFocus = prefs[Keys.TRANSPARENCY_VOICE] ?: true,
            dualConnection = prefs[Keys.DUAL_CONNECTION] ?: false,
            inEarDetection = prefs[Keys.IN_EAR_DETECTION] ?: true,
            lowLatencyMode = prefs[Keys.LOW_LATENCY] ?: false,
            eqPreset = preset,
            eqBandsDb = parsedBands,
            bassBoostStrength = prefs[Keys.BASS_BOOST] ?: 0.25f,
            controls = EarbudControlsConfig(
                doubleTapLeft = prefs[Keys.TAP_LEFT]
                    ?.let { runCatching { GestureAction.valueOf(it) }.getOrNull() }
                    ?: GestureAction.NEXT_TRACK,
                doubleTapRight = prefs[Keys.TAP_RIGHT]
                    ?.let { runCatching { GestureAction.valueOf(it) }.getOrNull() }
                    ?: GestureAction.PLAY_PAUSE,
                tripleTap = prefs[Keys.TRIPLE_TAP]
                    ?.let { runCatching { GestureAction.valueOf(it) }.getOrNull() }
                    ?: GestureAction.PREVIOUS_TRACK,
                pressAndHold = prefs[Keys.PRESS_HOLD]
                    ?.let { runCatching { GestureAction.valueOf(it) }.getOrNull() }
                    ?: GestureAction.SWITCH_NOISE_CONTROL,
            ),
            customization = UiCustomizationConfig(
                themePreference = prefs[Keys.THEME_PREF]
                    ?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() }
                    ?: ThemePreference.WHITE,
                popupStyle = prefs[Keys.POPUP_STYLE]
                    ?.let { runCatching { PopupStyle.valueOf(it) }.getOrNull() }
                    ?: PopupStyle.LIQUID_SHEET,
                animationStyle = prefs[Keys.ANIMATION_STYLE]
                    ?.let { runCatching { AnimationStyle.valueOf(it) }.getOrNull() }
                    ?: AnimationStyle.LIQUID_SPRING,
                notificationStyle = prefs[Keys.NOTIFICATION_STYLE]
                    ?.let { runCatching { NotificationStyle.valueOf(it) }.getOrNull() }
                    ?: NotificationStyle.DETAILED_TELEMETRY,
                glassTransparency = (prefs[Keys.GLASS_ALPHA] ?: 0.80f).coerceIn(0.45f, 0.96f),
                glassBlurDp = (prefs[Keys.GLASS_BLUR_DP] ?: 20f).coerceIn(0f, 28f),
                silverReflectionIntensity = (prefs[Keys.SILVER_REFLECTION] ?: 0.72f).coerceIn(0.15f, 1.0f),
                cardCornerRadiusDp = (prefs[Keys.CARD_CORNER_RADIUS] ?: 26f).coerceIn(14f, 34f),
                animationSpeedMultiplier = (prefs[Keys.ANIM_SPEED] ?: 1.0f).coerceIn(0.6f, 1.5f),
                enable3dAutoOrbit = prefs[Keys.ENABLE_3D_ORBIT] ?: true,
                layoutDensity = prefs[Keys.LAYOUT_DENSITY]
                    ?.let { runCatching { LayoutDensity.valueOf(it) }.getOrNull() }
                    ?: LayoutDensity.EXPANDED_STUDIO,
                showEqCardOnHome = prefs[Keys.SHOW_EQ_HOME] ?: true,
                autoConnectionPopupEnabled = prefs[Keys.AUTO_POPUP_ENABLED] ?: true,
                backgroundMonitoringEnabled = prefs[Keys.BG_MONITOR_ENABLED] ?: false,
            ),
            lowBatteryThreshold = (prefs[Keys.LOW_BATTERY_THRESHOLD] ?: 20).coerceIn(10, 35),
            lastDeviceAddress = prefs[Keys.LAST_DEVICE_ADDR],
            lastDeviceName = prefs[Keys.LAST_DEVICE_NAME],
            rememberedDeviceAddresses = prefs[Keys.REMEMBERED_ADDRESSES] ?: emptySet(),
        )
    }

    suspend fun setNoiseMode(mode: NoiseControlMode) {
        context.studioDataStore.edit { it[Keys.NOISE_MODE] = mode.name }
    }

    suspend fun setAncStrength(strength: AncStrength) {
        context.studioDataStore.edit { it[Keys.ANC_STRENGTH] = strength.name }
    }

    suspend fun setPersonalisedAnc(enabled: Boolean) {
        context.studioDataStore.edit { it[Keys.PERSONALISED_ANC] = enabled }
    }

    suspend fun setWindReduction(enabled: Boolean) {
        context.studioDataStore.edit { it[Keys.WIND_REDUCTION] = enabled }
    }

    suspend fun setTransparencyVoiceFocus(enabled: Boolean) {
        context.studioDataStore.edit { it[Keys.TRANSPARENCY_VOICE] = enabled }
    }

    suspend fun setDualConnection(enabled: Boolean) {
        context.studioDataStore.edit { it[Keys.DUAL_CONNECTION] = enabled }
    }

    suspend fun setInEarDetection(enabled: Boolean) {
        context.studioDataStore.edit { it[Keys.IN_EAR_DETECTION] = enabled }
    }

    suspend fun setLowLatencyMode(enabled: Boolean) {
        context.studioDataStore.edit { it[Keys.LOW_LATENCY] = enabled }
    }

    suspend fun setEqualizer(preset: EqPreset, bandsDb: List<Float>, bassBoost: Float) {
        context.studioDataStore.edit {
            it[Keys.EQ_PRESET] = preset.name
            it[Keys.EQ_BANDS] = bandsDb.joinToString(",")
            it[Keys.BASS_BOOST] = bassBoost
        }
    }

    suspend fun setControls(config: EarbudControlsConfig) {
        context.studioDataStore.edit {
            it[Keys.TAP_LEFT] = config.doubleTapLeft.name
            it[Keys.TAP_RIGHT] = config.doubleTapRight.name
            it[Keys.TRIPLE_TAP] = config.tripleTap.name
            it[Keys.PRESS_HOLD] = config.pressAndHold.name
        }
    }

    suspend fun setThemePreference(theme: ThemePreference) {
        context.studioDataStore.edit { it[Keys.THEME_PREF] = theme.name }
    }

    suspend fun setPopupStyle(style: PopupStyle) {
        context.studioDataStore.edit { it[Keys.POPUP_STYLE] = style.name }
    }

    suspend fun setAnimationStyle(style: AnimationStyle) {
        context.studioDataStore.edit { it[Keys.ANIMATION_STYLE] = style.name }
    }

    suspend fun setNotificationStyle(style: NotificationStyle) {
        context.studioDataStore.edit { it[Keys.NOTIFICATION_STYLE] = style.name }
    }

    suspend fun setGlassTransparency(alpha: Float) {
        context.studioDataStore.edit { it[Keys.GLASS_ALPHA] = alpha.coerceIn(0.45f, 0.96f) }
    }

    suspend fun setGlassBlurDp(blurDp: Float) {
        context.studioDataStore.edit { it[Keys.GLASS_BLUR_DP] = blurDp.coerceIn(0f, 28f) }
    }

    suspend fun setSilverReflection(intensity: Float) {
        context.studioDataStore.edit { it[Keys.SILVER_REFLECTION] = intensity.coerceIn(0.15f, 1.0f) }
    }

    suspend fun setCardCornerRadius(radiusDp: Float) {
        context.studioDataStore.edit { it[Keys.CARD_CORNER_RADIUS] = radiusDp.coerceIn(14f, 34f) }
    }

    suspend fun setAnimationSpeed(multiplier: Float) {
        context.studioDataStore.edit { it[Keys.ANIM_SPEED] = multiplier.coerceIn(0.6f, 1.5f) }
    }

    suspend fun setEnable3dAutoOrbit(enabled: Boolean) {
        context.studioDataStore.edit { it[Keys.ENABLE_3D_ORBIT] = enabled }
    }

    suspend fun setLayoutDensity(density: LayoutDensity) {
        context.studioDataStore.edit { it[Keys.LAYOUT_DENSITY] = density.name }
    }

    suspend fun setShowEqCardOnHome(show: Boolean) {
        context.studioDataStore.edit { it[Keys.SHOW_EQ_HOME] = show }
    }

    suspend fun setAutoConnectionPopupEnabled(enabled: Boolean) {
        context.studioDataStore.edit { it[Keys.AUTO_POPUP_ENABLED] = enabled }
    }

    suspend fun setBackgroundMonitoringEnabled(enabled: Boolean) {
        context.studioDataStore.edit { it[Keys.BG_MONITOR_ENABLED] = enabled }
    }

    suspend fun setLowBatteryThreshold(threshold: Int) {
        context.studioDataStore.edit { it[Keys.LOW_BATTERY_THRESHOLD] = threshold.coerceIn(10, 35) }
    }

    suspend fun recordConnectedDevice(address: String, name: String) {
        context.studioDataStore.edit { prefs ->
            prefs[Keys.LAST_DEVICE_ADDR] = address
            prefs[Keys.LAST_DEVICE_NAME] = name
            val current = prefs[Keys.REMEMBERED_ADDRESSES] ?: emptySet()
            prefs[Keys.REMEMBERED_ADDRESSES] = current + address
        }
    }
}
