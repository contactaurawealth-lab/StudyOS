package com.studyos.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.studyos.app.core.model.AppState
import com.studyos.app.core.model.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "studyos_preferences")

interface PreferencesDataSource {
    val appState: Flow<AppState>
    val themePreference: Flow<AppTheme>
    val isOnboardingCompleted: Flow<Boolean>
    suspend fun setThemePreference(theme: AppTheme)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun resetOnboarding()
}

class StudyOSPreferencesDataSource(private val context: Context) : PreferencesDataSource {

    private object PreferencesKeys {
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
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
}
