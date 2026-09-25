package com.studyos.app.features.planner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.ChapterStatus
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
import kotlinx.coroutines.flow.firstOrNull
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
    val errorMessage: String? = null,
    val snackbarMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class PlannerViewModel(
    private val getWeekScheduleUseCase: GetWeekScheduleUseCase,
    private val getUpcomingScheduleUseCase: GetUpcomingScheduleUseCase,
    private val checkSessionOverlapUseCase: CheckSessionOverlapUseCase,
    private val moveSessionUseCase: MoveSessionUseCase,
    private val savePlannerSessionUseCase: SavePlannerSessionUseCase,
    private val deletePlannerSessionUseCase: DeletePlannerSessionUseCase,
    private val getSubjectsUseCase: GetSubjectsUseCase,
    private val getChaptersForSubjectUseCase: GetChaptersForSubjectUseCase,
    private val getStudyPreferencesUseCase: GetStudyPreferencesUseCase,
    private val alarmScheduler: com.studyos.app.core.notification.AlarmScheduler? = null,
    private val preferencesDataSource: com.studyos.app.core.datastore.PreferencesDataSource? = null,
    private val chapterRepository: com.studyos.app.domain.repository.ChapterRepository? = null
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _viewMode = MutableStateFlow(PlannerViewMode.WEEK)
    private val _editingSession = MutableStateFlow<StudySession?>(null)
    private val _isCreateSessionSheetOpen = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _snackbarMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PlannerUiState> = combine(
        _selectedDate.flatMapLatest { date -> getWeekScheduleUseCase(date) },
        getUpcomingScheduleUseCase(),
        _viewMode,
        getSubjectsUseCase(),
        getStudyPreferencesUseCase(),
        _editingSession,
        _isCreateSessionSheetOpen,
        _errorMessage,
        _snackbarMessage
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
        val snackbar = args[8] as? String

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
            errorMessage = error,
            snackbarMessage = snackbar
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
                if (sessionId != null) {
                    alarmScheduler?.cancelSessionReminder(sessionId)
                }
                val session = savePlannerSessionUseCase(
                    sessionId = sessionId,
                    subjectId = subjectId,
                    chapterId = chapterId,
                    title = title,
                    scheduledStart = scheduledStart,
                    plannedMinutes = plannedMinutes
                )
                val remindersEnabled = preferencesDataSource?.studyRemindersEnabled?.firstOrNull() ?: true
                if (remindersEnabled && alarmScheduler != null) {
                    val subjectName = subjectId?.let { id -> uiState.value.subjects.find { it.id == id }?.name }
                    alarmScheduler.scheduleSessionReminder(session, subjectName, null)
                }
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
                alarmScheduler?.cancelSessionReminder(sessionId)
                moveSessionUseCase(sessionId, newDate)
            } catch (e: Exception) {
                _errorMessage.value = "Couldn't move this session."
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            try {
                alarmScheduler?.cancelSessionReminder(sessionId)
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

    fun autoScheduleDueReviews(durationMinutes: Int = 25) {
        viewModelScope.launch {
            try {
                val chapters = chapterRepository?.observeAllChapters()?.firstOrNull() ?: emptyList()
                if (chapters.isEmpty()) {
                    _snackbarMessage.value = "No chapters found to schedule reviews for."
                    return@launch
                }

                // Prioritize completed or lowest progress chapters
                val candidateChapters = chapters
                    .sortedWith(compareBy({ it.status != ChapterStatus.COMPLETED }, { it.progress }, { it.lastOpenedAt ?: 0L }))
                    .take(3)

                val today = LocalDate.now()
                var scheduledCount = 0
                val candidateHours = listOf(10, 14, 16, 19, 21)

                for (chapter in candidateChapters) {
                    for (hour in candidateHours) {
                        val startLdt = today.atTime(hour, 0)
                        val startMillis = startLdt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                        val endMillis = startMillis + (durationMinutes * 60 * 1000L)

                        if (startMillis <= System.currentTimeMillis()) continue

                        val hasOverlap = checkSessionOverlapUseCase(startMillis, endMillis, null)
                        if (!hasOverlap) {
                            val session = savePlannerSessionUseCase(
                                sessionId = null,
                                subjectId = chapter.subjectId,
                                chapterId = chapter.id,
                                title = "Due Review: ${chapter.name}",
                                scheduledStart = startMillis,
                                plannedMinutes = durationMinutes
                            )
                            val remindersEnabled = preferencesDataSource?.studyRemindersEnabled?.firstOrNull() ?: true
                            if (remindersEnabled && alarmScheduler != null) {
                                val subjectName = uiState.value.subjects.find { it.id == chapter.subjectId }?.name
                                alarmScheduler.scheduleSessionReminder(session, subjectName, chapter.name)
                            }
                            scheduledCount++
                            break
                        }
                    }
                }

                if (scheduledCount > 0) {
                    _snackbarMessage.value = "Auto-scheduled $scheduledCount revision session(s) for today!"
                } else {
                    _snackbarMessage.value = "All review slots for today are filled or in the past."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Couldn't auto-schedule reviews: ${e.message}"
            }
        }
    }

    fun exportIcsCalendar(): String {
        val state = uiState.value
        val allSessions = mutableListOf<StudySession>()

        state.weekSchedule?.sessionsForSelectedDate?.forEach { item ->
            allSessions.add(item.session)
        }
        state.upcomingGroups.forEach { group ->
            group.sessions.forEach { item ->
                allSessions.add(item.session)
            }
        }

        val uniqueSessions = allSessions.distinctBy { it.id }

        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(java.time.ZoneOffset.UTC)

        val sb = StringBuilder()
        sb.appendLine("BEGIN:VCALENDAR")
        sb.appendLine("VERSION:2.0")
        sb.appendLine("PRODID:-//StudyOS//Personal Learning OS//EN")
        sb.appendLine("CALSCALE:GREGORIAN")
        sb.appendLine("METHOD:PUBLISH")

        val nowStr = formatter.format(java.time.Instant.now())

        for (session in uniqueSessions) {
            val startMillis = session.scheduledStart ?: continue
            val startInstant = java.time.Instant.ofEpochMilli(startMillis)
            val endInstant = session.scheduledEnd?.let { java.time.Instant.ofEpochMilli(it) }
                ?: startInstant.plusMillis(session.plannedMinutes * 60 * 1000L)

            val subjectName = state.subjects.find { it.id == session.subjectId }?.name ?: "Study"
            val title = session.title.ifBlank { "$subjectName Session" }

            sb.appendLine("BEGIN:VEVENT")
            sb.appendLine("UID:${session.id}@studyos.app")
            sb.appendLine("DTSTAMP:$nowStr")
            sb.appendLine("DTSTART:${formatter.format(startInstant)}")
            sb.appendLine("DTEND:${formatter.format(endInstant)}")
            sb.appendLine("SUMMARY:$title")
            sb.appendLine("DESCRIPTION:Scheduled with StudyOS ($subjectName)")
            sb.appendLine("STATUS:CONFIRMED")
            sb.appendLine("END:VEVENT")
        }

        sb.appendLine("END:VCALENDAR")
        return sb.toString()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }
}
