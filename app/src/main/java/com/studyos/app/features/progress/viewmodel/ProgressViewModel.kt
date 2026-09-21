package com.studyos.app.features.progress.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.domain.engine.WeeklyReportEngine
import com.studyos.app.domain.engine.WeeklyStudyReport
import com.studyos.app.domain.repository.MistakeRepository
import com.studyos.app.domain.repository.StudySessionRepository
import com.studyos.app.domain.repository.SubjectRepository
import com.studyos.app.domain.usecase.AcademicProgress
import com.studyos.app.domain.usecase.GetAcademicProgressUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProgressUiState(
    val academicProgress: AcademicProgress = AcademicProgress(),
    val weeklyReport: WeeklyStudyReport? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class ProgressViewModel(
    private val getAcademicProgressUseCase: GetAcademicProgressUseCase,
    private val studySessionRepository: StudySessionRepository? = null,
    private val subjectRepository: SubjectRepository? = null,
    private val mistakeRepository: MistakeRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        loadProgress()
        loadWeeklyReport()
    }

    private fun loadProgress() {
        viewModelScope.launch {
            getAcademicProgressUseCase()
                .catch {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = "Couldn't load progress. Try again."
                        )
                    }
                }
                .collect { progress ->
                    _uiState.update {
                        it.copy(
                            academicProgress = progress,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    private fun loadWeeklyReport() {
        val sessionFlow = studySessionRepository?.observeAllSessions() ?: flowOf(emptyList())
        val subjectFlow = subjectRepository?.getAllSubjects() ?: flowOf(emptyList())
        val mistakeFlow = mistakeRepository?.observeAllMistakes() ?: flowOf(emptyList())

        viewModelScope.launch {
            combine(sessionFlow, subjectFlow, mistakeFlow) { sessions, subjects, mistakes ->
                WeeklyReportEngine.generateReport(sessions, subjects, mistakes)
            }.collect { report ->
                _uiState.update { it.copy(weeklyReport = report) }
            }
        }
    }
}

