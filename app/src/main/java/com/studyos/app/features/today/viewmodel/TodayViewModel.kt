package com.studyos.app.features.today.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.core.util.DateTimeUtils
import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.FocusItem
import com.studyos.app.domain.model.RecentChapterItem
import com.studyos.app.domain.model.StudySessionItem
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.model.TaskItem
import com.studyos.app.domain.usecase.DeleteSessionUseCase
import com.studyos.app.domain.usecase.GetChaptersForSubjectUseCase
import com.studyos.app.domain.usecase.GetSubjectsUseCase
import com.studyos.app.domain.usecase.GetTodayDataUseCase
import com.studyos.app.domain.usecase.GetTodayTasksUseCase
import com.studyos.app.domain.usecase.PlanSessionUseCase
import com.studyos.app.domain.usecase.ToggleTaskCompletionUseCase
import com.studyos.app.domain.usecase.UpdateSessionStatusUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.studyos.app.domain.model.Exam
import com.studyos.app.domain.usecase.GetDueFlashcardsUseCase
import com.studyos.app.domain.usecase.GetExamsUseCase

data class TodayUiState(
    val studentName: String? = null,
    val greeting: String = "Good day.",
    val dateHeader: String = "",
    val focusItem: FocusItem? = null,
    val upcomingSessions: List<StudySessionItem> = emptyList(),
    val todayTasks: List<TaskItem> = emptyList(),
    val completedMinutesToday: Int = 0,
    val dailyGoalMinutes: Int = 60,
    val sessionLengthMinutes: Int = 45,
    val recentChapter: RecentChapterItem? = null,
    val subjects: List<Subject> = emptyList(),
    val dueFlashcardsCount: Int = 0,
    val upcomingExam: Exam? = null,
    val isLoading: Boolean = true,
    val isPlanSessionSheetOpen: Boolean = false,
    val infoMessage: String? = null
) {
    val dailyProgressPercentage: Int
        get() = if (dailyGoalMinutes > 0) {
            ((completedMinutesToday.coerceAtLeast(0) * 100) / dailyGoalMinutes).coerceIn(0, 100)
        } else {
            0
        }

    val isDailyGoalReached: Boolean
        get() = completedMinutesToday >= dailyGoalMinutes && dailyGoalMinutes > 0
}

class TodayViewModel(
    private val getTodayDataUseCase: GetTodayDataUseCase,
    private val planSessionUseCase: PlanSessionUseCase,
    private val deleteSessionUseCase: DeleteSessionUseCase,
    private val updateSessionStatusUseCase: UpdateSessionStatusUseCase,
    private val getSubjectsUseCase: GetSubjectsUseCase,
    private val getChaptersForSubjectUseCase: GetChaptersForSubjectUseCase,
    private val getTodayTasksUseCase: GetTodayTasksUseCase,
    private val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase,
    private val getDueFlashcardsUseCase: GetDueFlashcardsUseCase? = null,
    private val getExamsUseCase: GetExamsUseCase? = null,
    private val alarmScheduler: com.studyos.app.core.notification.AlarmScheduler? = null,
    private val preferencesDataSource: com.studyos.app.core.datastore.PreferencesDataSource? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    init {
        updateDateAndGreeting(null)
        loadData()
    }

    private fun updateDateAndGreeting(name: String?) {
        val timeGreeting = DateTimeUtils.getGreeting()
        val firstName = name?.trim()?.split(" ")?.firstOrNull()?.ifBlank { null }
        val greetingText = if (firstName != null) {
            "$timeGreeting, $firstName."
        } else {
            "$timeGreeting."
        }
        val dateText = DateTimeUtils.formatDateHeader()

        _uiState.update {
            it.copy(
                greeting = greetingText,
                dateHeader = dateText
            )
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            getTodayDataUseCase().collect { todayData ->
                val name = todayData.student?.name
                updateDateAndGreeting(name)

                _uiState.update { current ->
                    current.copy(
                        studentName = name,
                        focusItem = todayData.focusItem,
                        upcomingSessions = todayData.upcomingSessions,
                        completedMinutesToday = todayData.completedMinutesToday,
                        dailyGoalMinutes = todayData.dailyGoalMinutes,
                        sessionLengthMinutes = todayData.preferences?.defaultSessionMinutes ?: 45,
                        recentChapter = todayData.recentChapter,
                        isLoading = false
                    )
                }
            }
        }

        viewModelScope.launch {
            getTodayTasksUseCase().collect { tasksList ->
                _uiState.update { it.copy(todayTasks = tasksList) }
            }
        }

        viewModelScope.launch {
            getSubjectsUseCase().collect { subjectsList ->
                _uiState.update { it.copy(subjects = subjectsList) }
            }
        }

        getDueFlashcardsUseCase?.let { useCase ->
            viewModelScope.launch {
                useCase().collect { cards ->
                    _uiState.update { it.copy(dueFlashcardsCount = cards.size) }
                }
            }
        }

        getExamsUseCase?.let { useCase ->
            viewModelScope.launch {
                useCase().collect { exams ->
                    val now = System.currentTimeMillis()
                    val nextExam = exams.filter { it.targetDate >= now }.minByOrNull { it.targetDate }
                    _uiState.update { it.copy(upcomingExam = nextExam) }
                }
            }
        }
    }

    fun toggleTaskCompletion(taskId: String) {
        viewModelScope.launch {
            toggleTaskCompletionUseCase(taskId)
        }
    }

    fun openPlanSessionSheet() {
        _uiState.update { it.copy(isPlanSessionSheetOpen = true) }
    }

    fun closePlanSessionSheet() {
        _uiState.update { it.copy(isPlanSessionSheetOpen = false) }
    }

    suspend fun getChaptersForSubject(subjectId: String): List<Chapter> {
        return getChaptersForSubjectUseCase.getOnce(subjectId)
    }

    fun planSession(
        subjectId: String?,
        chapterId: String?,
        title: String?,
        scheduledStart: Long,
        plannedMinutes: Int
    ) {
        viewModelScope.launch {
            try {
                val createdSession = planSessionUseCase(
                    subjectId = subjectId,
                    chapterId = chapterId,
                    title = title,
                    scheduledStart = scheduledStart,
                    plannedMinutes = plannedMinutes
                )
                val remindersEnabled = preferencesDataSource?.studyRemindersEnabled?.firstOrNull() ?: true
                if (remindersEnabled && alarmScheduler != null) {
                    val subjectName = subjectId?.let { id -> _uiState.value.subjects.find { it.id == id }?.name }
                    alarmScheduler.scheduleSessionReminder(createdSession, subjectName, null)
                }
                _uiState.update {
                    it.copy(
                        isPlanSessionSheetOpen = false,
                        infoMessage = "Session scheduled."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(infoMessage = e.message ?: "Could not schedule session.")
                }
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            alarmScheduler?.cancelSessionReminder(sessionId)
            deleteSessionUseCase(sessionId)
            _uiState.update { it.copy(infoMessage = "Session deleted.") }
        }
    }

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }
}
