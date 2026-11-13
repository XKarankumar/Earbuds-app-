package com.example.security

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AnimationPreset
import com.example.model.CurrencyOption
import com.example.model.OceanEmeraldMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.smsCalcPrefs by preferencesDataStore(name = "simple_sms_calc_prefs")

data class LocalAppPreferences(
    val themeMode: OceanEmeraldMode = OceanEmeraldMode.DARK,
    val animationPreset: AnimationPreset = AnimationPreset.FULL_SPRING,
    val glassIntensity: Float = 0.88f,
    val hapticFeedbackEnabled: Boolean = true,
    val currency: CurrencyOption = CurrencyOption.INR,
    val businessSignature: String = "Hardik Kumar"
)

class AppPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("ocean_emerald_mode")
        val ANIMATION_PRESET = stringPreferencesKey("animation_preset")
        val GLASS_INTENSITY = floatPreferencesKey("glass_intensity")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val CURRENCY = stringPreferencesKey("currency_option")
        val BUSINESS_SIGNATURE = stringPreferencesKey("business_signature")
    }

    val preferencesFlow: Flow<LocalAppPreferences> = context.smsCalcPrefs.data.map { prefs ->
        LocalAppPreferences(
            themeMode = OceanEmeraldMode.fromKey(prefs[Keys.THEME_MODE]),
            animationPreset = AnimationPreset.fromKey(prefs[Keys.ANIMATION_PRESET]),
            glassIntensity = (prefs[Keys.GLASS_INTENSITY] ?: 0.88f).coerceIn(0.4f, 1.0f),
            hapticFeedbackEnabled = prefs[Keys.HAPTIC_ENABLED] ?: true,
            currency = CurrencyOption.fromKey(prefs[Keys.CURRENCY]),
            businessSignature = prefs[Keys.BUSINESS_SIGNATURE]?.takeIf { it.isNotBlank() } ?: "Hardik Kumar"
        )
    }

    suspend fun setThemeMode(mode: OceanEmeraldMode) {
        context.smsCalcPrefs.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAnimationPreset(preset: AnimationPreset) {
        context.smsCalcPrefs.edit { it[Keys.ANIMATION_PRESET] = preset.name }
    }

    suspend fun setGlassIntensity(value: Float) {
        context.smsCalcPrefs.edit { it[Keys.GLASS_INTENSITY] = value.coerceIn(0.4f, 1.0f) }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.smsCalcPrefs.edit { it[Keys.HAPTIC_ENABLED] = enabled }
    }

    suspend fun setCurrency(option: CurrencyOption) {
        context.smsCalcPrefs.edit { it[Keys.CURRENCY] = option.name }
    }

    suspend fun setBusinessSignature(signature: String) {
        context.smsCalcPrefs.edit {
            it[Keys.BUSINESS_SIGNATURE] = signature.trim().ifBlank { "Hardik Kumar" }
        }
    }

    suspend fun resetDefaults() {
        context.smsCalcPrefs.edit { it.clear() }
    }
}
