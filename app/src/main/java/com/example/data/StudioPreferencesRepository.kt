package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AncStrength
import com.example.model.EarbudControlsConfig
import com.example.model.EqPreset
import com.example.model.GestureAction
import com.example.model.NoiseControlMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.studioDataStore by preferencesDataStore(name = "karan_buds_studio_prefs")

data class PersistedStudioPrefs(
    val noiseControlMode: NoiseControlMode = NoiseControlMode.ANC,
    val ancStrength: AncStrength = AncStrength.HIGH,
    val personalisedAnc: Boolean = true,
    val windNoiseReduction: Boolean = false,
    val transparencyVoiceFocus: Boolean = true,
    val dualConnection: Boolean = false,
    val inEarDetection: Boolean = true,
    val lowLatencyMode: Boolean = false,
    val autoReconnectEnabled: Boolean = true,
    val eqPreset: EqPreset = EqPreset.BALANCED,
    val eqBandsDb: List<Float> = EqPreset.BALANCED.bandsDb,
    val bassBoostStrength: Float = 0.25f,
    val controls: EarbudControlsConfig = EarbudControlsConfig(),
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
        val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
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
            autoReconnectEnabled = prefs[Keys.AUTO_RECONNECT] ?: true,
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

    suspend fun setAutoReconnectEnabled(enabled: Boolean) {
        context.studioDataStore.edit { it[Keys.AUTO_RECONNECT] = enabled }
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

    suspend fun recordConnectedDevice(address: String, name: String) {
        context.studioDataStore.edit { prefs ->
            prefs[Keys.LAST_DEVICE_ADDR] = address
            prefs[Keys.LAST_DEVICE_NAME] = name
            val current = prefs[Keys.REMEMBERED_ADDRESSES] ?: emptySet()
            prefs[Keys.REMEMBERED_ADDRESSES] = current + address
        }
    }

    suspend fun forgetDevice(address: String) {
        context.studioDataStore.edit { prefs ->
            val current = prefs[Keys.REMEMBERED_ADDRESSES] ?: emptySet()
            prefs[Keys.REMEMBERED_ADDRESSES] = current - address
        }
    }
}
