package com.studyos.app.features.planner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.PlannerViewMode
import com.studyos.app.domain.model.StudyPreferences
import com.studyos.app.domain.model.StudySession
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.usecase.CheckSessionOverlapUseCase
import com.studyos.app.domain.usecase.DeletePlannerSessionUseCase
import com.studyos.app.domain.usecase.GetChaptersForSubjectUseCase
import com.studyos.app.domain.usecase.GetStudyPreferencesUseCase
import com.studyos.app.domain.usecase.GetSubjectsUseCase
import com.studyos.app.domain.usecase.GetUpcomingScheduleUseCase
import com.studyos.app.domain.usecase.GetWeekScheduleUseCase
import com.studyos.app.domain.usecase.MoveSessionUseCase
import com.studyos.app.domain.usecase.SavePlannerSessionUseCase
import com.studyos.app.domain.usecase.UpcomingScheduleGroup
import com.studyos.app.domain.usecase.WeekScheduleData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PlannerUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val viewMode: PlannerViewMode = PlannerViewMode.WEEK,
    val weekSchedule: WeekScheduleData? = null,
    val upcomingGroups: List<UpcomingScheduleGroup> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val defaultSessionMinutes: Int = 45,
    val editingSession: StudySession? = null,
    val isCreateSessionSheetOpen: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class PlannerViewModel(
    private val getWeekScheduleUseCase: GetWeekScheduleUseCase,
    private val getUpcomingScheduleUseCase: GetUpcomingScheduleUseCase,
    private val checkSessionOverlapUseCase: CheckSessionOverlapUseCase,
    private val savePlannerSessionUseCase: SavePlannerSessionUseCase,
    private val moveSessionUseCase: MoveSessionUseCase,
    private val deletePlannerSessionUseCase: DeletePlannerSessionUseCase,
    private val getSubjectsUseCase: GetSubjectsUseCase,
    private val getChaptersForSubjectUseCase: GetChaptersForSubjectUseCase,
    private val getStudyPreferencesUseCase: GetStudyPreferencesUseCase
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _viewMode = MutableStateFlow(PlannerViewMode.WEEK)
    private val _editingSession = MutableStateFlow<StudySession?>(null)
    private val _isCreateSessionSheetOpen = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PlannerUiState> = combine(
        _selectedDate.flatMapLatest { date -> getWeekScheduleUseCase(date) },
        getUpcomingScheduleUseCase(),
        _viewMode,
        getSubjectsUseCase(),
        getStudyPreferencesUseCase(),
        _editingSession,
        _isCreateSessionSheetOpen,
        _errorMessage
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val weekSchedule = args[0] as? WeekScheduleData
        @Suppress("UNCHECKED_CAST")
        val upcoming = (args[1] as? List<UpcomingScheduleGroup>) ?: emptyList()
        val mode = (args[2] as? PlannerViewMode) ?: PlannerViewMode.WEEK
        @Suppress("UNCHECKED_CAST")
        val subjects = (args[3] as? List<Subject>) ?: emptyList()
        val prefs = args[4] as? StudyPreferences
        val editing = args[5] as? StudySession
        val isCreateOpen = (args[6] as? Boolean) ?: false
        val error = args[7] as? String

        PlannerUiState(
            selectedDate = weekSchedule?.selectedDate ?: _selectedDate.value,
            viewMode = mode,
            weekSchedule = weekSchedule,
            upcomingGroups = upcoming,
            subjects = subjects,
            defaultSessionMinutes = prefs?.defaultSessionMinutes ?: 45,
            editingSession = editing,
            isCreateSessionSheetOpen = isCreateOpen,
            isLoading = false,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlannerUiState()
    )

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun previousWeek() {
        _selectedDate.value = _selectedDate.value.minusWeeks(1)
    }

    fun nextWeek() {
        _selectedDate.value = _selectedDate.value.plusWeeks(1)
    }

    fun goToToday() {
        _selectedDate.value = LocalDate.now()
    }

    fun setViewMode(mode: PlannerViewMode) {
        _viewMode.value = mode
    }

    fun openCreateSessionSheet() {
        _isCreateSessionSheetOpen.value = true
    }

    fun closeCreateSessionSheet() {
        _isCreateSessionSheetOpen.value = false
    }

    fun openEditSession(session: StudySession) {
        _editingSession.value = session
    }

    fun closeEditSession() {
        _editingSession.value = null
    }

    suspend fun checkOverlap(startTime: Long, endTime: Long, excludeSessionId: String?): Boolean {
        return checkSessionOverlapUseCase(startTime, endTime, excludeSessionId)
    }

    fun saveSession(
        sessionId: String?,
        subjectId: String?,
        chapterId: String?,
        title: String?,
        scheduledStart: Long,
        plannedMinutes: Int
    ) {
        viewModelScope.launch {
            try {
                savePlannerSessionUseCase(
                    sessionId = sessionId,
                    subjectId = subjectId,
                    chapterId = chapterId,
                    title = title,
                    scheduledStart = scheduledStart,
                    plannedMinutes = plannedMinutes
                )
                _isCreateSessionSheetOpen.value = false
                _editingSession.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Couldn't save this session."
            }
        }
    }

    fun moveSession(sessionId: String, newDate: LocalDate) {
        viewModelScope.launch {
            try {
                moveSessionUseCase(sessionId, newDate)
            } catch (e: Exception) {
                _errorMessage.value = "Couldn't move this session."
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            try {
                deletePlannerSessionUseCase(sessionId)
                _editingSession.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Couldn't delete this session."
            }
        }
    }

    suspend fun getChaptersForSubject(subjectId: String): List<Chapter> {
        return getChaptersForSubjectUseCase.getOnce(subjectId)
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
