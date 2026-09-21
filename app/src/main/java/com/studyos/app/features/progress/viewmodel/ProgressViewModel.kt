package com.studyos.app.features.progress.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.domain.engine.WeeklyReportEngine
import com.studyos.app.domain.engine.WeeklyStudyReport
import com.studyos.app.domain.repository.MistakeRepository
import com.studyos.app.domain.repository.QuizRepository
import com.studyos.app.domain.repository.RecallRepository
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
import java.time.Instant
import java.time.ZoneId

enum class ProgressTimeWindow(val label: String) {
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    ALL_TIME("All Time")
}

data class CognitiveReadiness(
    val overallIndex: Int = 0,
    val syllabusCompletionPct: Int = 0,
    val recallRetentionPct: Int = 0,
    val quizAccuracyPct: Int = 0,
    val consistencyPct: Int = 0,
    val readinessTier: String = "Developing"
)

data class ProgressUiState(
    val academicProgress: AcademicProgress = AcademicProgress(),
    val weeklyReport: WeeklyStudyReport? = null,
    val selectedTimeWindow: ProgressTimeWindow = ProgressTimeWindow.THIS_WEEK,
    val cognitiveReadiness: CognitiveReadiness = CognitiveReadiness(),
    val filteredStudyMinutes: Int = 0,
    val activeDaysCount: Int = 0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class ProgressViewModel(
    private val getAcademicProgressUseCase: GetAcademicProgressUseCase,
    private val studySessionRepository: StudySessionRepository? = null,
    private val subjectRepository: SubjectRepository? = null,
    private val mistakeRepository: MistakeRepository? = null,
    private val recallRepository: RecallRepository? = null,
    private val quizRepository: QuizRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    private val _timeWindow = MutableStateFlow(ProgressTimeWindow.THIS_WEEK)

    init {
        loadProgress()
        loadAnalyticsAndReadiness()
    }

    fun setTimeWindowFilter(filter: ProgressTimeWindow) {
        _timeWindow.value = filter
        _uiState.update { it.copy(selectedTimeWindow = filter) }
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

    private fun loadAnalyticsAndReadiness() {
        val sessionFlow = studySessionRepository?.observeAllSessions() ?: flowOf(emptyList())
        val subjectFlow = subjectRepository?.getAllSubjects() ?: flowOf(emptyList())
        val mistakeFlow = mistakeRepository?.observeAllMistakes() ?: flowOf(emptyList())
        val quizFlow = quizRepository?.getAllAttempts() ?: flowOf(emptyList())
        val recallFlow = recallRepository?.getDashboardSummary() ?: flowOf(null)

        viewModelScope.launch {
            combine(sessionFlow, subjectFlow, mistakeFlow, quizFlow, recallFlow, _timeWindow) { args ->
                @Suppress("UNCHECKED_CAST")
                val sessions = args[0] as List<com.studyos.app.domain.model.StudySession>
                @Suppress("UNCHECKED_CAST")
                val subjects = args[1] as List<com.studyos.app.domain.model.Subject>
                @Suppress("UNCHECKED_CAST")
                val mistakes = args[2] as List<com.studyos.app.domain.model.Mistake>
                @Suppress("UNCHECKED_CAST")
                val quizAttempts = args[3] as List<com.studyos.app.domain.model.QuizAttempt>
                val recallSummary = args[4] as? com.studyos.app.domain.model.RecallDashboardSummary
                val window = args[5] as ProgressTimeWindow

                val report = WeeklyReportEngine.generateReport(sessions, subjects, mistakes)

                // Filter sessions by window
                val now = System.currentTimeMillis()
                val windowMillis = when (window) {
                    ProgressTimeWindow.THIS_WEEK -> 7 * 86400000L
                    ProgressTimeWindow.THIS_MONTH -> 30 * 86400000L
                    ProgressTimeWindow.ALL_TIME -> Long.MAX_VALUE
                }
                val cutoff = if (windowMillis == Long.MAX_VALUE) 0L else now - windowMillis

                val filteredSessions = sessions.filter { (it.scheduledStart ?: 0L) >= cutoff }
                val filteredMinutes = filteredSessions.sumOf { it.plannedMinutes }
                val activeDays = filteredSessions.mapNotNull { it.scheduledStart }.map {
                    Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                }.distinct().size

                val windowDays = when (window) {
                    ProgressTimeWindow.THIS_WEEK -> 7
                    ProgressTimeWindow.THIS_MONTH -> 30
                    ProgressTimeWindow.ALL_TIME -> 30
                }
                val consistencyPct = ((activeDays.toFloat() / windowDays.toFloat()) * 100f).toInt().coerceIn(0, 100)

                val syllabusPct = _uiState.value.academicProgress.overallProgress

                val recallRetentionPct = if (recallSummary != null && recallSummary.totalCount > 0) {
                    ((recallSummary.masteredCount.toFloat() / recallSummary.totalCount.toFloat()) * 100f).toInt().coerceIn(0, 100)
                } else {
                    if (syllabusPct > 0) (syllabusPct * 0.85f).toInt().coerceIn(40, 95) else 60
                }

                val quizAccuracyPct = if (quizAttempts.isNotEmpty()) {
                    quizAttempts.map { it.accuracyPercentage }.average().toInt().coerceIn(0, 100)
                } else {
                    if (syllabusPct > 0) (syllabusPct * 0.80f).toInt().coerceIn(35, 90) else 70
                }

                val overallIndex = (
                    syllabusPct * 0.35f +
                    recallRetentionPct * 0.25f +
                    quizAccuracyPct * 0.20f +
                    consistencyPct * 0.20f
                ).toInt().coerceIn(0, 100)

                val tier = when {
                    overallIndex >= 80 -> "Exam Ready"
                    overallIndex >= 65 -> "On Track"
                    overallIndex >= 45 -> "Developing"
                    else -> "Needs Focus"
                }

                val readiness = CognitiveReadiness(
                    overallIndex = overallIndex,
                    syllabusCompletionPct = syllabusPct,
                    recallRetentionPct = recallRetentionPct,
                    quizAccuracyPct = quizAccuracyPct,
                    consistencyPct = consistencyPct,
                    readinessTier = tier
                )

                Triple(report, readiness, Pair(filteredMinutes, activeDays))
            }.collect { (report, readiness, stats) ->
                _uiState.update {
                    it.copy(
                        weeklyReport = report,
                        cognitiveReadiness = readiness,
                        filteredStudyMinutes = stats.first,
                        activeDaysCount = stats.second
                    )
                }
            }
        }
    }
}

