package com.example.security

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.ActivityLevel
import com.example.model.FitnessGoalType
import com.example.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.fitnessHubPrefs by preferencesDataStore(name = "fitness_hub_secure_prefs")

data class FitnessPreferences(
    val themeMode: ThemeMode = ThemeMode.AMOLED_DARK,
    val gymName: String = "FITNESS HUB",
    val ownerSignature: String = "Hardik Kumar",
    val attendanceDeadlineHour: Int = 21,
    val attendanceDeadlineMinute: Int = 0,
    val workoutReminderEnabled: Boolean = false,
    val workoutReminderHour: Int = 7,
    val workoutReminderMinute: Int = 30,
    val smsGatewayEnabled: Boolean = false,
    val smsGatewayUrl: String = "",
    val encryptedSmsApiKey: String = "",
    val smsSenderId: String = "FITHUB",
    val tokenBackendUrl: String = "",
    val offlineLeaseHours: Int = 24,
    val encryptedCachedAuthLease: String = "",
    val failedTokenAttempts: Int = 0,
    val tokenLockoutUntilMillis: Long = 0L,
    val userAge: Int = 25,
    val userIsMale: Boolean = true,
    val userHeightCm: Float = 175f,
    val userWeightKg: Float = 72f,
    val targetWeightKg: Float = 75f,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val fitnessGoal: FitnessGoalType = FitnessGoalType.MUSCLE_GAIN,
    val dailyWaterTargetMl: Int = 3500,
    val dailyCalorieTarget: Int = 2650,
    val dailyProteinTargetG: Int = 155,
    val weeklyWorkoutTargetDays: Int = 5,
    val activeMemberCode: String = "FH-MEM-1001"
)

class AppPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val GYM_NAME = stringPreferencesKey("gym_name")
        val OWNER_SIGNATURE = stringPreferencesKey("owner_signature")
        val DEADLINE_HOUR = intPreferencesKey("deadline_hour")
        val DEADLINE_MINUTE = intPreferencesKey("deadline_minute")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val SMS_GATEWAY_ENABLED = booleanPreferencesKey("sms_gateway_enabled")
        val SMS_GATEWAY_URL = stringPreferencesKey("sms_gateway_url")
        val SMS_API_KEY_ENC = stringPreferencesKey("sms_api_key_enc")
        val SMS_SENDER_ID = stringPreferencesKey("sms_sender_id")
        val TOKEN_BACKEND_URL = stringPreferencesKey("token_backend_url")
        val OFFLINE_LEASE_HOURS = intPreferencesKey("offline_lease_hours")
        val CACHED_AUTH_LEASE_ENC = stringPreferencesKey("cached_auth_lease_enc")
        val FAILED_TOKEN_ATTEMPTS = intPreferencesKey("failed_token_attempts")
        val TOKEN_LOCKOUT_UNTIL = longPreferencesKey("token_lockout_until")
        val USER_AGE = intPreferencesKey("user_age")
        val USER_IS_MALE = booleanPreferencesKey("user_is_male")
        val USER_HEIGHT_CM = floatPreferencesKey("user_height_cm")
        val USER_WEIGHT_KG = floatPreferencesKey("user_weight_kg")
        val TARGET_WEIGHT_KG = floatPreferencesKey("target_weight_kg")
        val ACTIVITY_LEVEL = stringPreferencesKey("activity_level")
        val FITNESS_GOAL = stringPreferencesKey("fitness_goal")
        val WATER_TARGET_ML = intPreferencesKey("water_target_ml")
        val CALORIE_TARGET = intPreferencesKey("calorie_target")
        val PROTEIN_TARGET_G = intPreferencesKey("protein_target_g")
        val WEEKLY_WORKOUT_DAYS = intPreferencesKey("weekly_workout_days")
        val ACTIVE_MEMBER_CODE = stringPreferencesKey("active_member_code")
    }

    val preferencesFlow: Flow<FitnessPreferences> = context.fitnessHubPrefs.data.map { prefs ->
        FitnessPreferences(
            themeMode = ThemeMode.fromKey(prefs[Keys.THEME_MODE]),
            gymName = prefs[Keys.GYM_NAME]?.takeIf { it.isNotBlank() } ?: "FITNESS HUB",
            ownerSignature = prefs[Keys.OWNER_SIGNATURE]?.takeIf { it.isNotBlank() } ?: "Hardik Kumar",
            attendanceDeadlineHour = (prefs[Keys.DEADLINE_HOUR] ?: 21).coerceIn(0, 23),
            attendanceDeadlineMinute = (prefs[Keys.DEADLINE_MINUTE] ?: 0).coerceIn(0, 59),
            workoutReminderEnabled = prefs[Keys.REMINDER_ENABLED] ?: false,
            workoutReminderHour = (prefs[Keys.REMINDER_HOUR] ?: 7).coerceIn(0, 23),
            workoutReminderMinute = (prefs[Keys.REMINDER_MINUTE] ?: 30).coerceIn(0, 59),
            smsGatewayEnabled = prefs[Keys.SMS_GATEWAY_ENABLED] ?: false,
            smsGatewayUrl = prefs[Keys.SMS_GATEWAY_URL].orEmpty(),
            encryptedSmsApiKey = prefs[Keys.SMS_API_KEY_ENC].orEmpty(),
            smsSenderId = prefs[Keys.SMS_SENDER_ID]?.takeIf { it.isNotBlank() } ?: "FITHUB",
            tokenBackendUrl = prefs[Keys.TOKEN_BACKEND_URL].orEmpty(),
            offlineLeaseHours = (prefs[Keys.OFFLINE_LEASE_HOURS] ?: 24).coerceIn(1, 72),
            encryptedCachedAuthLease = prefs[Keys.CACHED_AUTH_LEASE_ENC].orEmpty(),
            failedTokenAttempts = prefs[Keys.FAILED_TOKEN_ATTEMPTS] ?: 0,
            tokenLockoutUntilMillis = prefs[Keys.TOKEN_LOCKOUT_UNTIL] ?: 0L,
            userAge = (prefs[Keys.USER_AGE] ?: 25).coerceIn(12, 95),
            userIsMale = prefs[Keys.USER_IS_MALE] ?: true,
            userHeightCm = (prefs[Keys.USER_HEIGHT_CM] ?: 175f).coerceIn(110f, 230f),
            userWeightKg = (prefs[Keys.USER_WEIGHT_KG] ?: 72f).coerceIn(30f, 250f),
            targetWeightKg = (prefs[Keys.TARGET_WEIGHT_KG] ?: 75f).coerceIn(30f, 250f),
            activityLevel = ActivityLevel.fromKey(prefs[Keys.ACTIVITY_LEVEL]),
            fitnessGoal = FitnessGoalType.fromKey(prefs[Keys.FITNESS_GOAL]),
            dailyWaterTargetMl = (prefs[Keys.WATER_TARGET_ML] ?: 3500).coerceIn(1000, 8000),
            dailyCalorieTarget = (prefs[Keys.CALORIE_TARGET] ?: 2650).coerceIn(1000, 6000),
            dailyProteinTargetG = (prefs[Keys.PROTEIN_TARGET_G] ?: 155).coerceIn(30, 400),
            weeklyWorkoutTargetDays = (prefs[Keys.WEEKLY_WORKOUT_DAYS] ?: 5).coerceIn(1, 7),
            activeMemberCode = prefs[Keys.ACTIVE_MEMBER_CODE] ?: "FH-MEM-1001"
        )
    }

    suspend fun current(): FitnessPreferences = preferencesFlow.first()

    suspend fun setThemeMode(mode: ThemeMode) {
        context.fitnessHubPrefs.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setGymBranding(gymName: String, owner: String) {
        context.fitnessHubPrefs.edit {
            it[Keys.GYM_NAME] = gymName.trim().ifBlank { "FITNESS HUB" }
            it[Keys.OWNER_SIGNATURE] = owner.trim().ifBlank { "Hardik Kumar" }
        }
    }

    suspend fun setAttendanceDeadline(hour: Int, minute: Int) {
        context.fitnessHubPrefs.edit {
            it[Keys.DEADLINE_HOUR] = hour.coerceIn(0, 23)
            it[Keys.DEADLINE_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setWorkoutReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.fitnessHubPrefs.edit {
            it[Keys.REMINDER_ENABLED] = enabled
            it[Keys.REMINDER_HOUR] = hour.coerceIn(0, 23)
            it[Keys.REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setSmsGatewayConfig(enabled: Boolean, url: String, encryptedApiKey: String, senderId: String) {
        context.fitnessHubPrefs.edit {
            it[Keys.SMS_GATEWAY_ENABLED] = enabled
            it[Keys.SMS_GATEWAY_URL] = url.trim()
            if (encryptedApiKey.isNotBlank()) it[Keys.SMS_API_KEY_ENC] = encryptedApiKey
            it[Keys.SMS_SENDER_ID] = senderId.trim().ifBlank { "FITHUB" }
        }
    }

    suspend fun setTokenBackendConfig(backendUrl: String, leaseHours: Int) {
        context.fitnessHubPrefs.edit {
            it[Keys.TOKEN_BACKEND_URL] = backendUrl.trim()
            it[Keys.OFFLINE_LEASE_HOURS] = leaseHours.coerceIn(1, 72)
        }
    }

    suspend fun setCachedAuthLease(encryptedPayload: String) {
        context.fitnessHubPrefs.edit { it[Keys.CACHED_AUTH_LEASE_ENC] = encryptedPayload }
    }

    suspend fun setTokenLockout(failedAttempts: Int, lockoutUntilMillis: Long) {
        context.fitnessHubPrefs.edit {
            it[Keys.FAILED_TOKEN_ATTEMPTS] = failedAttempts
            it[Keys.TOKEN_LOCKOUT_UNTIL] = lockoutUntilMillis
        }
    }

    suspend fun updatePersonalGoals(
        age: Int,
        isMale: Boolean,
        heightCm: Float,
        weightKg: Float,
        targetWeightKg: Float,
        activityLevel: ActivityLevel,
        goalType: FitnessGoalType,
        waterTargetMl: Int,
        calorieTarget: Int,
        proteinTargetG: Int,
        weeklyWorkoutDays: Int
    ) {
        context.fitnessHubPrefs.edit {
            it[Keys.USER_AGE] = age.coerceIn(12, 95)
            it[Keys.USER_IS_MALE] = isMale
            it[Keys.USER_HEIGHT_CM] = heightCm.coerceIn(110f, 230f)
            it[Keys.USER_WEIGHT_KG] = weightKg.coerceIn(30f, 250f)
            it[Keys.TARGET_WEIGHT_KG] = targetWeightKg.coerceIn(30f, 250f)
            it[Keys.ACTIVITY_LEVEL] = activityLevel.name
            it[Keys.FITNESS_GOAL] = goalType.name
            it[Keys.WATER_TARGET_ML] = waterTargetMl.coerceIn(1000, 8000)
            it[Keys.CALORIE_TARGET] = calorieTarget.coerceIn(1000, 6000)
            it[Keys.PROTEIN_TARGET_G] = proteinTargetG.coerceIn(30, 400)
            it[Keys.WEEKLY_WORKOUT_DAYS] = weeklyWorkoutDays.coerceIn(1, 7)
        }
    }

    suspend fun wipePersonalPreferences() {
        context.fitnessHubPrefs.edit { it.clear() }
    }
}
