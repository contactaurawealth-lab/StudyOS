package com.studyos.app.features.today.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.domain.model.Student
import com.studyos.app.domain.model.StudyPreferences
import com.studyos.app.domain.usecase.GetStudentUseCase
import com.studyos.app.domain.usecase.GetStudyPreferencesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class TodayUiState(
    val studentName: String? = null,
    val greeting: String = "Good day.",
    val dailyGoalMinutes: Int = 60,
    val sessionLengthMinutes: Int = 45,
    val infoMessage: String? = null
)

class TodayViewModel(
    private val getStudentUseCase: GetStudentUseCase,
    private val getStudyPreferencesUseCase: GetStudyPreferencesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    init {
        updateGreeting()
        loadData()
    }

    private fun updateGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val timeGreeting = when (hour) {
            in 4..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
        _uiState.update { current ->
            val fullName = current.studentName
            val fullGreeting = if (!fullName.isNullOrBlank()) {
                "$timeGreeting, ${fullName.split(" ").first()}."
            } else {
                "$timeGreeting."
            }
            current.copy(greeting = fullGreeting)
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            getStudentUseCase().collect { student ->
                val name = student?.name
                _uiState.update { current ->
                    current.copy(studentName = name)
                }
                updateGreeting()
            }
        }

        viewModelScope.launch {
            getStudyPreferencesUseCase().collect { prefs ->
                if (prefs != null) {
                    _uiState.update {
                        it.copy(
                            dailyGoalMinutes = prefs.dailyStudyGoalMinutes,
                            sessionLengthMinutes = prefs.defaultSessionMinutes
                        )
                    }
                }
            }
        }
    }

    fun onPlanSessionClicked() {
        _uiState.update { it.copy(infoMessage = "Session planning will be available in Phase 3.") }
    }

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }
}
