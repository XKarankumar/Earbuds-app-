package com.example.security

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppThemeId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appDataStore by preferencesDataStore(name = "simple_sms_calc_prefs")

data class LocalAppPreferences(
    val themeId: AppThemeId = AppThemeId.OBSIDIAN_EMERALD,
    val glassIntensity: Float = 0.85f,
    val pinLockEnabled: Boolean = false,
    val encryptedPinHash: String = "",
    val encryptedPinSalt: String = "",
    val biometricLockEnabled: Boolean = false,
    val directSmsFeatureEnabled: Boolean = false,
    val contactsFeatureEnabled: Boolean = true,
    val cameraScanFeatureEnabled: Boolean = true,
    val remindersFeatureEnabled: Boolean = true
)

class AppPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_ID = stringPreferencesKey("theme_id")
        val GLASS_INTENSITY = floatPreferencesKey("glass_intensity")
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash_enc")
        val PIN_SALT = stringPreferencesKey("pin_salt_enc")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val DIRECT_SMS_ENABLED = booleanPreferencesKey("direct_sms_enabled")
        val CONTACTS_ENABLED = booleanPreferencesKey("contacts_enabled")
        val CAMERA_SCAN_ENABLED = booleanPreferencesKey("camera_scan_enabled")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
    }

    val preferencesFlow: Flow<LocalAppPreferences> = context.appDataStore.data.map { prefs ->
        val rawHash = prefs[Keys.PIN_HASH].orEmpty()
        val rawSalt = prefs[Keys.PIN_SALT].orEmpty()
        val decryptedHash = if (rawHash.isNotEmpty()) CryptoVault.decryptLocalString(rawHash).orEmpty() else ""
        val decryptedSalt = if (rawSalt.isNotEmpty()) CryptoVault.decryptLocalString(rawSalt).orEmpty() else ""

        LocalAppPreferences(
            themeId = AppThemeId.fromKey(prefs[Keys.THEME_ID]),
            glassIntensity = prefs[Keys.GLASS_INTENSITY] ?: 0.85f,
            pinLockEnabled = prefs[Keys.PIN_ENABLED] ?: false,
            encryptedPinHash = decryptedHash,
            encryptedPinSalt = decryptedSalt,
            biometricLockEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
            directSmsFeatureEnabled = prefs[Keys.DIRECT_SMS_ENABLED] ?: false,
            contactsFeatureEnabled = prefs[Keys.CONTACTS_ENABLED] ?: true,
            cameraScanFeatureEnabled = prefs[Keys.CAMERA_SCAN_ENABLED] ?: true,
            remindersFeatureEnabled = prefs[Keys.REMINDERS_ENABLED] ?: true
        )
    }

    suspend fun setTheme(themeId: AppThemeId) {
        context.appDataStore.edit { it[Keys.THEME_ID] = themeId.name }
    }

    suspend fun setGlassIntensity(value: Float) {
        context.appDataStore.edit { it[Keys.GLASS_INTENSITY] = value.coerceIn(0.3f, 1.0f) }
    }

    suspend fun setPinCode(pin: String) {
        val (hashB64, saltB64) = CryptoVault.hashPinWithSalt(pin)
        val encHash = CryptoVault.encryptLocalString(hashB64)
        val encSalt = CryptoVault.encryptLocalString(saltB64)
        context.appDataStore.edit {
            it[Keys.PIN_ENABLED] = true
            it[Keys.PIN_HASH] = encHash
            it[Keys.PIN_SALT] = encSalt
        }
    }

    suspend fun disablePinCode() {
        context.appDataStore.edit {
            it[Keys.PIN_ENABLED] = false
            it[Keys.PIN_HASH] = ""
            it[Keys.PIN_SALT] = ""
            it[Keys.BIOMETRIC_ENABLED] = false
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setDirectSmsEnabled(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.DIRECT_SMS_ENABLED] = enabled }
    }

    suspend fun setContactsEnabled(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.CONTACTS_ENABLED] = enabled }
    }

    suspend fun setCameraScanEnabled(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.CAMERA_SCAN_ENABLED] = enabled }
    }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.REMINDERS_ENABLED] = enabled }
    }
}
