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
import com.studyos.app.core.assistant.AssistantCustomization
import com.studyos.app.core.assistant.AssistantPersona
import com.studyos.app.core.assistant.AssistantThemeGlow
import com.studyos.app.core.model.AppState
import com.studyos.app.core.model.AppTheme
import com.studyos.app.domain.model.AiConfig
import com.studyos.app.domain.model.NamedApiKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
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
    val appBlockerEnabled: Flow<Boolean> get() = flowOf(false)
    val blockedPackages: Flow<Set<String>> get() = flowOf(emptySet())
    val appBlockerPasscode: Flow<String> get() = flowOf("")
    val assistantCustomization: Flow<AssistantCustomization> get() = flowOf(AssistantCustomization())
    val customAccentHex: Flow<String> get() = flowOf("#D4A373")
    suspend fun setThemePreference(theme: AppTheme)
    suspend fun setCustomAccentHex(hex: String) {}
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun resetOnboarding()
    suspend fun resetAll()
    suspend fun saveAiConfig(config: AiConfig)
    suspend fun updateRevisionStreak(todayEpochDay: Long): Int
    suspend fun setDailyRevisionTargetMinutes(minutes: Int)
    suspend fun setStudyRemindersEnabled(enabled: Boolean)
    suspend fun setDailyReminderEnabled(enabled: Boolean)
    suspend fun setDailyReminderTime(time: String)
    suspend fun setRevisionRemindersEnabled(enabled: Boolean)
    suspend fun setAppBlockerEnabled(enabled: Boolean) {}
    suspend fun setBlockedPackages(packages: Set<String>) {}
    suspend fun setAppBlockerPasscode(passcode: String) {}
    suspend fun updateAssistantCustomization(customization: AssistantCustomization) {}
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
        val AI_SAVED_KEYS_JSON = stringPreferencesKey("ai_saved_keys_json")
        val AI_SAVED_MODELS_JSON = stringPreferencesKey("ai_saved_models_json")
        val REVISION_STREAK_DAYS = intPreferencesKey("revision_streak_days")
        val DAILY_REVISION_TARGET_MINUTES = intPreferencesKey("daily_revision_target_minutes")
        val LAST_REVISION_EPOCH_DAY = longPreferencesKey("last_revision_epoch_day")
        val STUDY_REMINDERS_ENABLED = booleanPreferencesKey("study_reminders_enabled")
        val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val DAILY_REMINDER_TIME = stringPreferencesKey("daily_reminder_time")
        val REVISION_REMINDERS_ENABLED = booleanPreferencesKey("revision_reminders_enabled")
        val APP_BLOCKER_ENABLED = booleanPreferencesKey("app_blocker_enabled")
        val BLOCKED_PACKAGES = androidx.datastore.preferences.core.stringSetPreferencesKey("blocked_packages")
        val APP_BLOCKER_PASSCODE = stringPreferencesKey("app_blocker_passcode")
        val ASSISTANT_THEME_GLOW = stringPreferencesKey("assistant_theme_glow")
        val ASSISTANT_PERSONA = stringPreferencesKey("assistant_persona")
        val ASSISTANT_AUTO_LISTEN = booleanPreferencesKey("assistant_auto_listen")
        val ASSISTANT_SPEAK_RESPONSES = booleanPreferencesKey("assistant_speak_responses")
        val ASSISTANT_AUTO_CAPTURE_SCREEN = booleanPreferencesKey("assistant_auto_capture_screen")
        val ASSISTANT_HAPTIC_FEEDBACK = booleanPreferencesKey("assistant_haptic_feedback")
        val CUSTOM_ACCENT_HEX = stringPreferencesKey("custom_accent_hex")
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
            val customAccent = preferences[PreferencesKeys.CUSTOM_ACCENT_HEX] ?: "#D4A373"
            val onboardingCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
            val firstLaunch = preferences[PreferencesKeys.FIRST_LAUNCH] ?: true

            AppState(
                isFirstLaunch = firstLaunch,
                isOnboardingCompleted = onboardingCompleted,
                theme = theme,
                customAccentHex = customAccent,
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

    override val customAccentHex: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.CUSTOM_ACCENT_HEX] ?: "#D4A373"
        }

    override suspend fun setCustomAccentHex(hex: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CUSTOM_ACCENT_HEX] = hex
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

    override suspend fun resetAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
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
            val apiKey = preferences[PreferencesKeys.AI_API_KEY] ?: ""
            val baseUrl = preferences[PreferencesKeys.AI_BASE_URL] ?: "https://api.openai.com/v1/"
            val model = preferences[PreferencesKeys.AI_MODEL] ?: "gpt-4o-mini"
            val systemPrompt = preferences[PreferencesKeys.AI_SYSTEM_PROMPT]
            val keysJson = preferences[PreferencesKeys.AI_SAVED_KEYS_JSON] ?: ""
            val modelsJson = preferences[PreferencesKeys.AI_SAVED_MODELS_JSON] ?: ""

            val savedKeys = mutableListOf<NamedApiKey>()
            if (keysJson.isNotBlank()) {
                try {
                    val array = JSONArray(keysJson)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        savedKeys.add(
                            NamedApiKey(
                                id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                                name = obj.optString("name", "Key ${i + 1}"),
                                key = obj.optString("key", "")
                            )
                        )
                    }
                } catch (ignored: Exception) {}
            }
            if (savedKeys.isEmpty() && apiKey.isNotBlank()) {
                savedKeys.add(NamedApiKey(name = "Primary Key", key = apiKey))
            }

            val savedModels = mutableListOf<String>()
            if (modelsJson.isNotBlank()) {
                try {
                    val array = JSONArray(modelsJson)
                    for (i in 0 until array.length()) {
                        val m = array.getString(i).trim()
                        if (m.isNotEmpty() && !savedModels.contains(m)) {
                            savedModels.add(m)
                        }
                    }
                } catch (ignored: Exception) {}
            }

            AiConfig(
                apiKey = apiKey,
                baseUrl = baseUrl,
                model = model,
                customSystemPrompt = systemPrompt,
                savedApiKeys = savedKeys,
                savedModels = savedModels
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

            // Save multiple API keys
            val keysArray = JSONArray()
            config.savedApiKeys.forEach { item ->
                if (item.key.isNotBlank()) {
                    val obj = JSONObject()
                    obj.put("id", item.id)
                    obj.put("name", item.name)
                    obj.put("key", item.key.trim())
                    keysArray.put(obj)
                }
            }
            preferences[PreferencesKeys.AI_SAVED_KEYS_JSON] = keysArray.toString()

            // Save multiple models
            val modelsArray = JSONArray()
            config.savedModels.forEach { m ->
                if (m.isNotBlank()) {
                    modelsArray.put(m.trim())
                }
            }
            preferences[PreferencesKeys.AI_SAVED_MODELS_JSON] = modelsArray.toString()
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

    override val appBlockerEnabled: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.APP_BLOCKER_ENABLED] ?: false
        }

    override val blockedPackages: Flow<Set<String>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.BLOCKED_PACKAGES] ?: emptySet()
        }

    override val appBlockerPasscode: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.APP_BLOCKER_PASSCODE] ?: ""
        }

    override suspend fun setAppBlockerEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_BLOCKER_ENABLED] = enabled
        }
        syncAppBlockerManager()
    }

    override suspend fun setBlockedPackages(packages: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BLOCKED_PACKAGES] = packages
        }
        syncAppBlockerManager()
    }

    override suspend fun setAppBlockerPasscode(passcode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_BLOCKER_PASSCODE] = passcode
        }
        syncAppBlockerManager()
    }

    override val assistantCustomization: Flow<AssistantCustomization> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            val themeStr = prefs[PreferencesKeys.ASSISTANT_THEME_GLOW] ?: AssistantThemeGlow.GEMINI_AURORA.name
            val personaStr = prefs[PreferencesKeys.ASSISTANT_PERSONA] ?: AssistantPersona.SOCRATIC_TUTOR.name
            val themeGlow = try {
                AssistantThemeGlow.valueOf(themeStr)
            } catch (_: Exception) {
                AssistantThemeGlow.GEMINI_AURORA
            }
            val persona = try {
                AssistantPersona.valueOf(personaStr)
            } catch (_: Exception) {
                AssistantPersona.SOCRATIC_TUTOR
            }

            AssistantCustomization(
                themeGlow = themeGlow,
                persona = persona,
                autoListenOnLaunch = prefs[PreferencesKeys.ASSISTANT_AUTO_LISTEN] ?: true,
                speakResponses = prefs[PreferencesKeys.ASSISTANT_SPEAK_RESPONSES] ?: false,
                autoCaptureScreen = prefs[PreferencesKeys.ASSISTANT_AUTO_CAPTURE_SCREEN] ?: true,
                hapticFeedback = prefs[PreferencesKeys.ASSISTANT_HAPTIC_FEEDBACK] ?: true
            )
        }

    override suspend fun updateAssistantCustomization(customization: AssistantCustomization) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.ASSISTANT_THEME_GLOW] = customization.themeGlow.name
            prefs[PreferencesKeys.ASSISTANT_PERSONA] = customization.persona.name
            prefs[PreferencesKeys.ASSISTANT_AUTO_LISTEN] = customization.autoListenOnLaunch
            prefs[PreferencesKeys.ASSISTANT_SPEAK_RESPONSES] = customization.speakResponses
            prefs[PreferencesKeys.ASSISTANT_AUTO_CAPTURE_SCREEN] = customization.autoCaptureScreen
            prefs[PreferencesKeys.ASSISTANT_HAPTIC_FEEDBACK] = customization.hapticFeedback
        }
    }

    private suspend fun syncAppBlockerManager() {
        try {
            val prefs = context.dataStore.data.firstOrNull() ?: return
            val enabled = prefs[PreferencesKeys.APP_BLOCKER_ENABLED] ?: false
            val pkgs = prefs[PreferencesKeys.BLOCKED_PACKAGES] ?: emptySet()
            val code = prefs[PreferencesKeys.APP_BLOCKER_PASSCODE] ?: ""
            com.studyos.app.core.blocker.AppBlockerManager.syncFromPreferences(enabled, pkgs, code)
        } catch (_: Exception) {}
    }
}
