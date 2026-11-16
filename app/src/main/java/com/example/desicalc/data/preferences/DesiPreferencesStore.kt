package com.example.desicalc.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.desicalc.model.AiEngineMode
import com.example.desicalc.model.AppLanguage
import com.example.desicalc.model.AppSettingsState
import com.example.desicalc.model.AppThemePreset
import com.example.desicalc.model.CustomBackgroundStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.desiDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "desi_calculator_prefs"
)

class DesiPreferencesStore(private val context: Context) {

    private object Keys {
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val CUSTOM_ACCENT = longPreferencesKey("custom_accent")
        val CUSTOM_BG = stringPreferencesKey("custom_bg")
        val LANGUAGE = stringPreferencesKey("language")
        val BUSINESS_NAME = stringPreferencesKey("business_name")
        val OWNER_MOBILE = stringPreferencesKey("owner_mobile")
        val PROFILE_PHOTO = stringPreferencesKey("profile_photo")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val NOTIFY_TX = booleanPreferencesKey("notify_tx")
        val NOTIFY_REMINDER = booleanPreferencesKey("notify_reminder")
        val NOTIFY_DAILY_SUMMARY = booleanPreferencesKey("notify_daily_summary")
        val AI_ENGINE_MODE = stringPreferencesKey("ai_engine_mode")
        val REMINDER_TEMPLATE = stringPreferencesKey("reminder_template")
        val UDHAR_TEMPLATE = stringPreferencesKey("udhar_template")
        val JAMA_TEMPLATE = stringPreferencesKey("jama_template")
        val SUMMARY_TEMPLATE = stringPreferencesKey("summary_template")
        val SCHEDULED_DAILY_SUMMARY = booleanPreferencesKey("scheduled_daily_summary")
    }

    val settingsFlow: Flow<AppSettingsState> = context.desiDataStore.data.map { prefs ->
        val defaultState = AppSettingsState()
        AppSettingsState(
            themePreset = AppThemePreset.fromId(prefs[Keys.THEME_PRESET] ?: defaultState.themePreset.id),
            customAccentColor = prefs[Keys.CUSTOM_ACCENT] ?: defaultState.customAccentColor,
            customBackgroundStyle = CustomBackgroundStyle.fromId(
                prefs[Keys.CUSTOM_BG] ?: defaultState.customBackgroundStyle.id
            ),
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE] ?: defaultState.language.code),
            businessName = prefs[Keys.BUSINESS_NAME] ?: defaultState.businessName,
            ownerMobile = prefs[Keys.OWNER_MOBILE] ?: defaultState.ownerMobile,
            profilePhotoPath = prefs[Keys.PROFILE_PHOTO],
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: defaultState.notificationsEnabled,
            notifyOnTransaction = prefs[Keys.NOTIFY_TX] ?: defaultState.notifyOnTransaction,
            notifyPaymentReminder = prefs[Keys.NOTIFY_REMINDER] ?: defaultState.notifyPaymentReminder,
            notifyDailySummary = prefs[Keys.NOTIFY_DAILY_SUMMARY] ?: defaultState.notifyDailySummary,
            aiEngineMode = AiEngineMode.entries.firstOrNull {
                it.id == prefs[Keys.AI_ENGINE_MODE]
            } ?: defaultState.aiEngineMode,
            reminderTemplate = prefs[Keys.REMINDER_TEMPLATE] ?: defaultState.reminderTemplate,
            udharTemplate = prefs[Keys.UDHAR_TEMPLATE] ?: defaultState.udharTemplate,
            jamaTemplate = prefs[Keys.JAMA_TEMPLATE] ?: defaultState.jamaTemplate,
            summaryTemplate = prefs[Keys.SUMMARY_TEMPLATE] ?: defaultState.summaryTemplate,
            scheduledDailySummaryEnabled = prefs[Keys.SCHEDULED_DAILY_SUMMARY]
                ?: defaultState.scheduledDailySummaryEnabled
        )
    }

    suspend fun setThemePreset(preset: AppThemePreset) {
        context.desiDataStore.edit { it[Keys.THEME_PRESET] = preset.id }
    }

    suspend fun setCustomAccentColor(colorArgb: Long) {
        context.desiDataStore.edit {
            it[Keys.CUSTOM_ACCENT] = colorArgb
            it[Keys.THEME_PRESET] = AppThemePreset.CUSTOM.id
        }
    }

    suspend fun setCustomBackgroundStyle(style: CustomBackgroundStyle) {
        context.desiDataStore.edit {
            it[Keys.CUSTOM_BG] = style.id
            it[Keys.THEME_PRESET] = AppThemePreset.CUSTOM.id
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.desiDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun updateProfile(businessName: String, ownerMobile: String) {
        context.desiDataStore.edit {
            it[Keys.BUSINESS_NAME] = businessName.ifBlank { "Desi Vyapar Kendra" }
            it[Keys.OWNER_MOBILE] = ownerMobile
        }
    }

    suspend fun setProfilePhotoPath(path: String?) {
        context.desiDataStore.edit {
            if (path.isNullOrBlank()) it.remove(Keys.PROFILE_PHOTO)
            else it[Keys.PROFILE_PHOTO] = path
        }
    }

    suspend fun updateNotificationSettings(
        enabled: Boolean,
        onTransaction: Boolean,
        paymentReminder: Boolean,
        dailySummary: Boolean,
        scheduledDailySummary: Boolean
    ) {
        context.desiDataStore.edit {
            it[Keys.NOTIFICATIONS_ENABLED] = enabled
            it[Keys.NOTIFY_TX] = onTransaction
            it[Keys.NOTIFY_REMINDER] = paymentReminder
            it[Keys.NOTIFY_DAILY_SUMMARY] = dailySummary
            it[Keys.SCHEDULED_DAILY_SUMMARY] = scheduledDailySummary
        }
    }

    suspend fun setAiEngineMode(mode: AiEngineMode) {
        context.desiDataStore.edit { it[Keys.AI_ENGINE_MODE] = mode.id }
    }

    suspend fun updateSmsTemplates(
        reminder: String,
        udhar: String,
        jama: String,
        summary: String
    ) {
        context.desiDataStore.edit {
            it[Keys.REMINDER_TEMPLATE] = reminder
            it[Keys.UDHAR_TEMPLATE] = udhar
            it[Keys.JAMA_TEMPLATE] = jama
            it[Keys.SUMMARY_TEMPLATE] = summary
        }
    }
}
