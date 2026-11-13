package com.example.security

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppLanguage
import com.example.model.AppThemeId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.finderDataStore by preferencesDataStore(name = "karan_finder_pro_prefs")

data class LocalAppPreferences(
    val themeId: AppThemeId = AppThemeId.DARK_3D_GLASS,
    val glassIntensity: Float = 0.88f,
    val language: AppLanguage = AppLanguage.BILINGUAL,
    val guestModeActive: Boolean = true,
    val saveSearchHistory: Boolean = true,
    val pinLockEnabled: Boolean = false,
    val encryptedPinHash: String = "",
    val encryptedPinSalt: String = "",
    val biometricLockEnabled: Boolean = false,
    val cameraScanFeatureEnabled: Boolean = true
)

class AppPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_ID = stringPreferencesKey("theme_id")
        val GLASS_INTENSITY = floatPreferencesKey("glass_intensity")
        val LANGUAGE = stringPreferencesKey("language")
        val GUEST_MODE = booleanPreferencesKey("guest_mode")
        val SAVE_HISTORY = booleanPreferencesKey("save_history")
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash_enc")
        val PIN_SALT = stringPreferencesKey("pin_salt_enc")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val CAMERA_SCAN_ENABLED = booleanPreferencesKey("camera_scan_enabled")
    }

    val preferencesFlow: Flow<LocalAppPreferences> = context.finderDataStore.data.map { prefs ->
        val rawHash = prefs[Keys.PIN_HASH].orEmpty()
        val rawSalt = prefs[Keys.PIN_SALT].orEmpty()
        val decryptedHash = if (rawHash.isNotEmpty()) CryptoVault.decryptLocalString(rawHash).orEmpty() else ""
        val decryptedSalt = if (rawSalt.isNotEmpty()) CryptoVault.decryptLocalString(rawSalt).orEmpty() else ""

        LocalAppPreferences(
            themeId = AppThemeId.fromKey(prefs[Keys.THEME_ID]),
            glassIntensity = prefs[Keys.GLASS_INTENSITY] ?: 0.88f,
            language = AppLanguage.fromKey(prefs[Keys.LANGUAGE]),
            guestModeActive = prefs[Keys.GUEST_MODE] ?: true,
            saveSearchHistory = prefs[Keys.SAVE_HISTORY] ?: true,
            pinLockEnabled = prefs[Keys.PIN_ENABLED] ?: false,
            encryptedPinHash = decryptedHash,
            encryptedPinSalt = decryptedSalt,
            biometricLockEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
            cameraScanFeatureEnabled = prefs[Keys.CAMERA_SCAN_ENABLED] ?: true
        )
    }

    suspend fun setTheme(themeId: AppThemeId) {
        context.finderDataStore.edit { it[Keys.THEME_ID] = themeId.name }
    }

    suspend fun setGlassIntensity(value: Float) {
        context.finderDataStore.edit { it[Keys.GLASS_INTENSITY] = value.coerceIn(0.35f, 1.0f) }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.finderDataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setSaveSearchHistory(enabled: Boolean) {
        context.finderDataStore.edit { it[Keys.SAVE_HISTORY] = enabled }
    }

    suspend fun setPinCode(pin: String) {
        val (hashB64, saltB64) = CryptoVault.hashPinWithSalt(pin)
        val encHash = CryptoVault.encryptLocalString(hashB64)
        val encSalt = CryptoVault.encryptLocalString(saltB64)
        context.finderDataStore.edit {
            it[Keys.PIN_ENABLED] = true
            it[Keys.PIN_HASH] = encHash
            it[Keys.PIN_SALT] = encSalt
        }
    }

    suspend fun disablePinCode() {
        context.finderDataStore.edit {
            it[Keys.PIN_ENABLED] = false
            it[Keys.PIN_HASH] = ""
            it[Keys.PIN_SALT] = ""
            it[Keys.BIOMETRIC_ENABLED] = false
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.finderDataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setCameraScanEnabled(enabled: Boolean) {
        context.finderDataStore.edit { it[Keys.CAMERA_SCAN_ENABLED] = enabled }
    }
}
