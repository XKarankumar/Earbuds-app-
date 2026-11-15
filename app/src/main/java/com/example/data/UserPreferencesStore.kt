package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.hometheatre.core.UserAudioPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.theatreDataStore: DataStore<Preferences> by preferencesDataStore(name = "theatre_pro_prefs")

class UserPreferencesStore(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val prefsKey = stringPreferencesKey("user_audio_preferences_json")

    val preferencesFlow: Flow<UserAudioPreferences> = context.theatreDataStore.data.map { prefs ->
        val raw = prefs[prefsKey]
        if (raw.isNullOrBlank()) {
            UserAudioPreferences()
        } else {
            try {
                json.decodeFromString<UserAudioPreferences>(raw)
            } catch (_: Throwable) {
                UserAudioPreferences()
            }
        }
    }

    suspend fun update(transform: (UserAudioPreferences) -> UserAudioPreferences) {
        context.theatreDataStore.edit { store ->
            val current = store[prefsKey]?.let {
                try {
                    json.decodeFromString<UserAudioPreferences>(it)
                } catch (_: Throwable) {
                    UserAudioPreferences()
                }
            } ?: UserAudioPreferences()
            val updated = transform(current)
            store[prefsKey] = json.encodeToString(updated)
        }
    }
}
