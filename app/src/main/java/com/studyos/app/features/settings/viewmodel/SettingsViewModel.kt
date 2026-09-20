package com.studyos.app.features.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.core.datastore.StudyOSPreferencesDataSource
import com.studyos.app.core.model.AppTheme
import com.studyos.app.domain.model.Student
import com.studyos.app.domain.model.StudyPreferences
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.usecase.AddSubjectResult
import com.studyos.app.domain.usecase.AddSubjectUseCase
import com.studyos.app.domain.usecase.DeleteSubjectUseCase
import com.studyos.app.domain.usecase.GetStudentUseCase
import com.studyos.app.domain.usecase.GetStudyPreferencesUseCase
import com.studyos.app.domain.usecase.GetSubjectsUseCase
import com.studyos.app.domain.usecase.RenameSubjectUseCase
import com.studyos.app.domain.usecase.SaveStudentUseCase
import com.studyos.app.domain.usecase.SaveStudyPreferencesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val student: Student? = null,
    val subjects: List<Subject> = emptyList(),
    val preferences: StudyPreferences = StudyPreferences(),
    val currentTheme: AppTheme = AppTheme.SYSTEM,
    val studyRemindersEnabled: Boolean = true,
    val dailyReminderEnabled: Boolean = true,
    val dailyReminderTime: String = "19:00",
    val revisionRemindersEnabled: Boolean = true,
    val areSystemNotificationsEnabled: Boolean = true,
    val showPermissionRationaleDialog: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val notificationMessage: String? = null
)

class SettingsViewModel(
    private val getStudentUseCase: GetStudentUseCase,
    private val saveStudentUseCase: SaveStudentUseCase,
    private val getSubjectsUseCase: GetSubjectsUseCase,
    private val addSubjectUseCase: AddSubjectUseCase,
    private val renameSubjectUseCase: RenameSubjectUseCase,
    private val deleteSubjectUseCase: DeleteSubjectUseCase,
    private val getStudyPreferencesUseCase: GetStudyPreferencesUseCase,
    private val saveStudyPreferencesUseCase: SaveStudyPreferencesUseCase,
    private val preferencesDataSource: com.studyos.app.core.datastore.PreferencesDataSource,
    private val alarmScheduler: com.studyos.app.core.notification.AlarmScheduler? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            preferencesDataSource.themePreference.collect { theme ->
                _uiState.update { it.copy(currentTheme = theme) }
            }
        }

        viewModelScope.launch {
            preferencesDataSource.studyRemindersEnabled.collect { enabled ->
                _uiState.update { it.copy(studyRemindersEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            preferencesDataSource.dailyReminderEnabled.collect { enabled ->
                _uiState.update { it.copy(dailyReminderEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            preferencesDataSource.dailyReminderTime.collect { time ->
                _uiState.update { it.copy(dailyReminderTime = time) }
            }
        }

        viewModelScope.launch {
            preferencesDataSource.revisionRemindersEnabled.collect { enabled ->
                _uiState.update { it.copy(revisionRemindersEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            getStudentUseCase().collect { student ->
                _uiState.update { it.copy(student = student, isLoading = false) }
            }
        }

        viewModelScope.launch {
            getSubjectsUseCase().collect { subjects ->
                _uiState.update { it.copy(subjects = subjects) }
            }
        }

        viewModelScope.launch {
            getStudyPreferencesUseCase().collect { prefs ->
                if (prefs != null) {
                    _uiState.update { it.copy(preferences = prefs) }
                }
            }
        }
    }

    fun setSystemNotificationsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(areSystemNotificationsEnabled = enabled) }
    }

    fun setShowPermissionRationaleDialog(show: Boolean) {
        _uiState.update { it.copy(showPermissionRationaleDialog = show) }
    }

    fun toggleStudyReminders(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataSource.setStudyRemindersEnabled(enabled)
            _uiState.update {
                it.copy(notificationMessage = if (enabled) "Study reminders enabled." else "Study reminders paused.")
            }
        }
    }

    fun toggleDailyReminder(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataSource.setDailyReminderEnabled(enabled)
            if (enabled) {
                val time = _uiState.value.dailyReminderTime
                val parts = time.split(":")
                val hour = parts.getOrNull(0)?.toIntOrNull() ?: 19
                val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                alarmScheduler?.scheduleDailyReminder(hour, minute)
                _uiState.update { it.copy(notificationMessage = "Daily reminder active.") }
            } else {
                alarmScheduler?.cancelDailyReminder()
                _uiState.update { it.copy(notificationMessage = "Daily reminder disabled.") }
            }
        }
    }

    fun setDailyReminderTime(time: String) {
        viewModelScope.launch {
            preferencesDataSource.setDailyReminderTime(time)
            if (_uiState.value.dailyReminderEnabled) {
                val parts = time.split(":")
                val hour = parts.getOrNull(0)?.toIntOrNull() ?: 19
                val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                alarmScheduler?.scheduleDailyReminder(hour, minute)
            }
            _uiState.update { it.copy(notificationMessage = "Reminder time updated.") }
        }
    }

    fun toggleRevisionReminders(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataSource.setRevisionRemindersEnabled(enabled)
            _uiState.update {
                it.copy(notificationMessage = if (enabled) "Revision reminders enabled." else "Revision reminders disabled.")
            }
        }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            preferencesDataSource.setThemePreference(theme)
        }
    }

    fun updateProfile(
        name: String,
        classLevel: String?,
        division: String?,
        schoolName: String?,
        onSuccess: () -> Unit
    ): Boolean {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            _uiState.update { it.copy(error = "Please enter your name.") }
            return false
        }

        viewModelScope.launch {
            val existing = _uiState.value.student
            val updated = existing?.copy(
                name = trimmedName,
                classLevel = classLevel,
                division = division?.trim()?.ifEmpty { null },
                schoolName = schoolName?.trim()?.ifEmpty { null },
                updatedAt = System.currentTimeMillis()
            ) ?: Student(
                name = trimmedName,
                classLevel = classLevel,
                division = division?.trim()?.ifEmpty { null },
                schoolName = schoolName?.trim()?.ifEmpty { null }
            )
            saveStudentUseCase(updated)
            _uiState.update { it.copy(error = null, notificationMessage = "Profile updated") }
            onSuccess()
        }
        return true
    }

    fun addSubject(name: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            when (val result = addSubjectUseCase(name, isCustom = true)) {
                is AddSubjectResult.Success -> {
                    _uiState.update { it.copy(notificationMessage = "Subject added") }
                    onResult(true, null)
                }
                is AddSubjectResult.EmptyName -> {
                    onResult(false, "Subject name cannot be empty.")
                }
                is AddSubjectResult.DuplicateName -> {
                    onResult(false, "That subject already exists.")
                }
                is AddSubjectResult.Error -> {
                    onResult(false, result.message)
                }
            }
        }
    }

    fun renameSubject(id: String, newName: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            when (val result = renameSubjectUseCase(id, newName)) {
                is AddSubjectResult.Success -> {
                    _uiState.update { it.copy(notificationMessage = "Subject renamed") }
                    onResult(true, null)
                }
                is AddSubjectResult.EmptyName -> {
                    onResult(false, "Subject name cannot be empty.")
                }
                is AddSubjectResult.DuplicateName -> {
                    onResult(false, "That subject already exists.")
                }
                is AddSubjectResult.Error -> {
                    onResult(false, result.message)
                }
            }
        }
    }

    fun deleteSubject(id: String) {
        viewModelScope.launch {
            deleteSubjectUseCase(id)
            _uiState.update { it.copy(notificationMessage = "Subject removed") }
        }
    }

    fun updatePreferences(
        dailyGoalMinutes: Int,
        defaultSessionMinutes: Int,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val existing = _uiState.value.preferences
            val updated = existing.copy(
                dailyStudyGoalMinutes = dailyGoalMinutes,
                defaultSessionMinutes = defaultSessionMinutes,
                updatedAt = System.currentTimeMillis()
            )
            saveStudyPreferencesUseCase(updated)
            _uiState.update { it.copy(notificationMessage = "Preferences saved") }
            onSuccess()
        }
    }

    fun resetOnboarding(onReset: () -> Unit) {
        viewModelScope.launch {
            preferencesDataSource.resetOnboarding()
            onReset()
        }
    }

    fun clearNotificationMessage() {
        _uiState.update { it.copy(notificationMessage = null) }
    }
}
