package com.studyos.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.studyos.app.core.model.AppState
import com.studyos.app.core.model.AppTheme
import com.studyos.app.domain.model.AiConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "studyos_preferences")

interface PreferencesDataSource {
    val appState: Flow<AppState>
    val themePreference: Flow<AppTheme>
    val isOnboardingCompleted: Flow<Boolean>
    val aiConfig: Flow<AiConfig>
    val revisionStreakDays: Flow<Int>
    val dailyRevisionTargetMinutes: Flow<Int>
    val lastRevisionEpochDay: Flow<Long>
    val studyRemindersEnabled: Flow<Boolean>
    val dailyReminderEnabled: Flow<Boolean>
    val dailyReminderTime: Flow<String>
    val revisionRemindersEnabled: Flow<Boolean>
    suspend fun setThemePreference(theme: AppTheme)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun resetOnboarding()
    suspend fun saveAiConfig(config: AiConfig)
    suspend fun updateRevisionStreak(todayEpochDay: Long): Int
    suspend fun setDailyRevisionTargetMinutes(minutes: Int)
    suspend fun setStudyRemindersEnabled(enabled: Boolean)
    suspend fun setDailyReminderEnabled(enabled: Boolean)
    suspend fun setDailyReminderTime(time: String)
    suspend fun setRevisionRemindersEnabled(enabled: Boolean)
}

class StudyOSPreferencesDataSource(private val context: Context) : PreferencesDataSource {

    private object PreferencesKeys {
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        val AI_API_KEY = stringPreferencesKey("ai_api_key")
        val AI_BASE_URL = stringPreferencesKey("ai_base_url")
        val AI_MODEL = stringPreferencesKey("ai_model")
        val AI_SYSTEM_PROMPT = stringPreferencesKey("ai_system_prompt")
        val REVISION_STREAK_DAYS = intPreferencesKey("revision_streak_days")
        val DAILY_REVISION_TARGET_MINUTES = intPreferencesKey("daily_revision_target_minutes")
        val LAST_REVISION_EPOCH_DAY = longPreferencesKey("last_revision_epoch_day")
        val STUDY_REMINDERS_ENABLED = booleanPreferencesKey("study_reminders_enabled")
        val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val DAILY_REMINDER_TIME = stringPreferencesKey("daily_reminder_time")
        val REVISION_REMINDERS_ENABLED = booleanPreferencesKey("revision_reminders_enabled")
    }

    override val appState: Flow<AppState> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val themeString = preferences[PreferencesKeys.THEME_PREFERENCE] ?: AppTheme.SYSTEM.name
            val theme = try {
                AppTheme.valueOf(themeString)
            } catch (e: IllegalArgumentException) {
                AppTheme.SYSTEM
            }
            val onboardingCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
            val firstLaunch = preferences[PreferencesKeys.FIRST_LAUNCH] ?: true

            AppState(
                isFirstLaunch = firstLaunch,
                isOnboardingCompleted = onboardingCompleted,
                theme = theme,
                isOffline = true,
                isLoading = false
            )
        }

    override val themePreference: Flow<AppTheme> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val themeString = preferences[PreferencesKeys.THEME_PREFERENCE] ?: AppTheme.SYSTEM.name
            try {
                AppTheme.valueOf(themeString)
            } catch (e: IllegalArgumentException) {
                AppTheme.SYSTEM
            }
        }

    override val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
        }

    override suspend fun setThemePreference(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_PREFERENCE] = theme.name
        }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
            preferences[PreferencesKeys.FIRST_LAUNCH] = false
        }
    }

    override suspend fun resetOnboarding() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = false
        }
    }

    override val aiConfig: Flow<AiConfig> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AiConfig(
                apiKey = preferences[PreferencesKeys.AI_API_KEY] ?: "",
                baseUrl = preferences[PreferencesKeys.AI_BASE_URL] ?: "https://api.openai.com/v1/",
                model = preferences[PreferencesKeys.AI_MODEL] ?: "gpt-4o-mini",
                customSystemPrompt = preferences[PreferencesKeys.AI_SYSTEM_PROMPT]
            )
        }

    override suspend fun saveAiConfig(config: AiConfig) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AI_API_KEY] = config.apiKey.trim()
            preferences[PreferencesKeys.AI_BASE_URL] = config.baseUrl.trim()
            preferences[PreferencesKeys.AI_MODEL] = config.model.trim()
            if (!config.customSystemPrompt.isNullOrBlank()) {
                preferences[PreferencesKeys.AI_SYSTEM_PROMPT] = config.customSystemPrompt.trim()
            } else {
                preferences.remove(PreferencesKeys.AI_SYSTEM_PROMPT)
            }
        }
    }

    override val revisionStreakDays: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.REVISION_STREAK_DAYS] ?: 0
        }

    override val dailyRevisionTargetMinutes: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.DAILY_REVISION_TARGET_MINUTES] ?: 15
        }

    override val lastRevisionEpochDay: Flow<Long> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.LAST_REVISION_EPOCH_DAY] ?: 0L
        }

    override suspend fun updateRevisionStreak(todayEpochDay: Long): Int {
        var updatedStreak = 1
        context.dataStore.edit { preferences ->
            val lastDay = preferences[PreferencesKeys.LAST_REVISION_EPOCH_DAY] ?: 0L
            val currentStreak = preferences[PreferencesKeys.REVISION_STREAK_DAYS] ?: 0

            updatedStreak = when {
                lastDay == todayEpochDay -> {
                    // Already logged today, keep existing streak (at least 1)
                    if (currentStreak > 0) currentStreak else 1
                }
                lastDay == todayEpochDay - 1L -> {
                    // Consecutive day
                    currentStreak + 1
                }
                else -> {
                    // Missed more than 1 day or first time
                    1
                }
            }

            preferences[PreferencesKeys.REVISION_STREAK_DAYS] = updatedStreak
            preferences[PreferencesKeys.LAST_REVISION_EPOCH_DAY] = todayEpochDay
        }
        return updatedStreak
    }

    override suspend fun setDailyRevisionTargetMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_REVISION_TARGET_MINUTES] = minutes.coerceAtLeast(5)
        }
    }

    override val studyRemindersEnabled: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.STUDY_REMINDERS_ENABLED] ?: true
        }

    override val dailyReminderEnabled: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.DAILY_REMINDER_ENABLED] ?: true
        }

    override val dailyReminderTime: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.DAILY_REMINDER_TIME] ?: "19:00"
        }

    override val revisionRemindersEnabled: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.REVISION_REMINDERS_ENABLED] ?: true
        }

    override suspend fun setStudyRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.STUDY_REMINDERS_ENABLED] = enabled
        }
    }

    override suspend fun setDailyReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_REMINDER_ENABLED] = enabled
        }
    }

    override suspend fun setDailyReminderTime(time: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_REMINDER_TIME] = time
        }
    }

    override suspend fun setRevisionRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REVISION_REMINDERS_ENABLED] = enabled
        }
    }
}
