package com.example.hometheatre.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ultimate_theatre_theme_prefs"
)

@Serializable
data class ThemeState(
    val selectedPreset: ThemePreset = ThemePreset.PREMIUM_DARK,
    val customConfig: CustomThemeConfig = CustomThemeConfig()
)

/**
 * Persistent DataStore-backed manager in :core responsible for storing and restoring
 * the user's selected theme ('Dark', 'Monochrome', 'Silver Glass', 'RGB Neon', or 'Custom')
 * across app restarts.
 */
class ThemeManager private constructor(
    private val dataStore: DataStore<Preferences>
) {
    constructor(context: Context) : this(context.applicationContext.themeDataStore)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val safeDataFlow: Flow<Preferences> = dataStore.data
        .catch { err ->
            if (err is IOException) emit(emptyPreferences()) else throw err
        }

    val selectedThemeFlow: Flow<ThemePreset> = safeDataFlow.map { prefs ->
        decodeThemePreset(prefs[KEY_SELECTED_THEME])
    }

    val customThemeFlow: Flow<CustomThemeConfig> = safeDataFlow.map { prefs ->
        decodeCustomConfig(prefs[KEY_CUSTOM_THEME_JSON])
    }

    val themeStateFlow: Flow<ThemeState> = safeDataFlow.map { prefs ->
        ThemeState(
            selectedPreset = decodeThemePreset(prefs[KEY_SELECTED_THEME]),
            customConfig = decodeCustomConfig(prefs[KEY_CUSTOM_THEME_JSON])
        )
    }

    suspend fun selectTheme(preset: ThemePreset) {
        dataStore.edit { prefs ->
            prefs[KEY_SELECTED_THEME] = preset.name
        }
    }

    /**
     * Persists a theme by friendly label ('Dark', 'Monochrome', 'Silver Glass', 'RGB Neon', 'Custom')
     * or enum name. Returns true if recognized and saved.
     */
    suspend fun selectThemeByName(label: String): Boolean {
        val resolved = resolveThemeName(label) ?: return false
        selectTheme(resolved)
        return true
    }

    suspend fun updateCustomTheme(config: CustomThemeConfig) {
        dataStore.edit { prefs ->
            prefs[KEY_SELECTED_THEME] = ThemePreset.CUSTOM.name
            prefs[KEY_CUSTOM_THEME_JSON] = json.encodeToString(config)
        }
    }

    suspend fun getSelectedTheme(): ThemePreset = selectedThemeFlow.first()

    suspend fun resetToDefault() {
        dataStore.edit { prefs ->
            prefs[KEY_SELECTED_THEME] = ThemePreset.PREMIUM_DARK.name
            prefs.remove(KEY_CUSTOM_THEME_JSON)
        }
    }

    private fun decodeThemePreset(raw: String?): ThemePreset {
        if (raw.isNullOrBlank()) return ThemePreset.PREMIUM_DARK
        return resolveThemeName(raw) ?: ThemePreset.PREMIUM_DARK
    }

    private fun decodeCustomConfig(raw: String?): CustomThemeConfig {
        if (raw.isNullOrBlank()) return CustomThemeConfig()
        return try {
            json.decodeFromString<CustomThemeConfig>(raw)
        } catch (_: Throwable) {
            CustomThemeConfig()
        }
    }

    companion object {
        internal val KEY_SELECTED_THEME = stringPreferencesKey("selected_theme_preset")
        internal val KEY_CUSTOM_THEME_JSON = stringPreferencesKey("custom_theme_config_json")

        internal fun createForTesting(dataStore: DataStore<Preferences>): ThemeManager {
            return ThemeManager(dataStore)
        }

        fun resolveThemeName(name: String): ThemePreset? {
            return when (name.trim().lowercase()) {
                "dark", "premium dark", "premium_dark" -> ThemePreset.PREMIUM_DARK
                "monochrome", "monochrome b&w", "b&w", "black & white" -> ThemePreset.MONOCHROME
                "silver glass", "silver_glass", "silver" -> ThemePreset.SILVER_GLASS
                "rgb neon", "rgb_neon", "neon", "rgb" -> ThemePreset.RGB_NEON
                "custom", "custom studio" -> ThemePreset.CUSTOM
                else -> ThemePreset.entries.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
            }
        }
    }
}
