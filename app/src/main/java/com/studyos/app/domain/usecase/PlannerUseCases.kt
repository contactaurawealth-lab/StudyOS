package com.studyos.app.domain.usecase

import com.studyos.app.core.util.DateTimeUtils
import com.studyos.app.domain.model.CalendarDay
import com.studyos.app.domain.model.StudySession
import com.studyos.app.domain.model.StudySessionItem
import com.studyos.app.domain.model.StudySessionStatus
import com.studyos.app.domain.repository.ChapterRepository
import com.studyos.app.domain.repository.PlannerRepository
import com.studyos.app.domain.repository.SubjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class WeekScheduleData(
    val selectedDate: LocalDate,
    val weekStartDate: LocalDate,
    val weekDays: List<CalendarDay>,
    val sessionsForSelectedDate: List<StudySessionItem>
)

data class UpcomingScheduleGroup(
    val title: String,
    val date: LocalDate?,
    val sessions: List<StudySessionItem>
)

class GetWeekScheduleUseCase(
    private val plannerRepository: PlannerRepository,
    private val subjectRepository: SubjectRepository,
    private val chapterRepository: ChapterRepository
) {
    operator fun invoke(selectedDate: LocalDate): Flow<WeekScheduleData> {
        val weekStart = DateTimeUtils.getStartOfWeek(selectedDate)
        val weekEnd = DateTimeUtils.getEndOfWeek(selectedDate)
        val rangeStartMillis = DateTimeUtils.getStartOfDay(weekStart)
        val rangeEndMillis = DateTimeUtils.getEndOfDay(weekEnd)

        val selectedDayStart = DateTimeUtils.getStartOfDay(selectedDate)
        val selectedDayEnd = DateTimeUtils.getEndOfDay(selectedDate)

        return combine(
            plannerRepository.observeSessionsForRange(rangeStartMillis, rangeEndMillis),
            subjectRepository.getAllSubjects(),
            chapterRepository.observeAllChapters()
        ) { sessions, subjects, chapters ->
            val subjectMap = subjects.associateBy { it.id }
            val chapterMap = chapters.associateBy { it.id }
            val zoneId = ZoneId.systemDefault()
            val today = LocalDate.now()

            // Count sessions per day in week
            val sessionsByDate = sessions
                .filter { it.scheduledStart != null }
                .groupBy { session ->
                    Instant.ofEpochMilli(session.scheduledStart!!).atZone(zoneId).toLocalDate()
                }

            val weekDays = (0L..6L).map { dayOffset ->
                val date = weekStart.plusDays(dayOffset)
                val daySessions = sessionsByDate[date] ?: emptyList()
                CalendarDay(
                    date = date,
                    isSelected = date == selectedDate,
                    isToday = date == today,
                    sessionCount = daySessions.size
                )
            }

            val selectedSessions = sessions
                .filter { session ->
                    val start = session.scheduledStart
                    start != null && start >= selectedDayStart && start <= selectedDayEnd
                }
                .sortedBy { it.scheduledStart ?: Long.MAX_VALUE }
                .map { session ->
                    StudySessionItem(
                        session = session,
                        subjectName = session.subjectId?.let { subjectMap[it]?.name } ?: "General Study",
                        chapterName = session.chapterId?.let { chapterMap[it]?.name },
                        formattedTime = session.scheduledStart?.let { DateTimeUtils.formatTime(it) }
                            ?: "${session.plannedMinutes}m"
                    )
                }

            WeekScheduleData(
                selectedDate = selectedDate,
                weekStartDate = weekStart,
                weekDays = weekDays,
                sessionsForSelectedDate = selectedSessions
            )
        }
    }
}

class GetUpcomingScheduleUseCase(
    private val plannerRepository: PlannerRepository,
    private val subjectRepository: SubjectRepository,
    private val chapterRepository: ChapterRepository
) {
    operator fun invoke(): Flow<List<UpcomingScheduleGroup>> {
        return combine(
            plannerRepository.observeUpcomingSessions(),
            subjectRepository.getAllSubjects(),
            chapterRepository.observeAllChapters()
        ) { sessions, subjects, chapters ->
            val subjectMap = subjects.associateBy { it.id }
            val chapterMap = chapters.associateBy { it.id }
            val zoneId = ZoneId.systemDefault()
            val today = LocalDate.now()
            val tomorrow = today.plusDays(1)

            val sessionsWithItems = sessions
                .filter { it.scheduledStart != null }
                .sortedBy { it.scheduledStart!! }
                .map { session ->
                    val date = Instant.ofEpochMilli(session.scheduledStart!!).atZone(zoneId).toLocalDate()
                    date to StudySessionItem(
                        session = session,
                        subjectName = session.subjectId?.let { subjectMap[it]?.name } ?: "General Study",
                        chapterName = session.chapterId?.let { chapterMap[it]?.name },
                        formattedTime = DateTimeUtils.formatTime(session.scheduledStart!!)
                    )
                }

            val groupedByDate = sessionsWithItems.groupBy({ it.first }, { it.second })

            val groups = mutableListOf<UpcomingScheduleGroup>()

            // 1. Today group
            val todaySessions = groupedByDate[today] ?: emptyList()
            if (todaySessions.isNotEmpty()) {
                groups.add(
                    UpcomingScheduleGroup(
                        title = "Today",
                        date = today,
                        sessions = todaySessions
                    )
                )
            }

            // 2. Tomorrow group
            val tomorrowSessions = groupedByDate[tomorrow] ?: emptyList()
            if (tomorrowSessions.isNotEmpty()) {
                groups.add(
                    UpcomingScheduleGroup(
                        title = "Tomorrow",
                        date = tomorrow,
                        sessions = tomorrowSessions
                    )
                )
            }

            // 3. Later dates
            groupedByDate.keys
                .filter { it.isAfter(tomorrow) }
                .sorted()
                .forEach { date ->
                    val daySessions = groupedByDate[date] ?: emptyList()
                    groups.add(
                        UpcomingScheduleGroup(
                            title = DateTimeUtils.formatDateHeader(date),
                            date = date,
                            sessions = daySessions
                        )
                    )
                }

            groups
        }
    }
}

class CheckSessionOverlapUseCase(
    private val plannerRepository: PlannerRepository
) {
    suspend operator fun invoke(
        startTime: Long,
        endTime: Long,
        excludeSessionId: String? = null
    ): Boolean {
        return plannerRepository.checkOverlap(startTime, endTime, excludeSessionId)
    }
}

class MoveSessionUseCase(
    private val plannerRepository: PlannerRepository
) {
    suspend operator fun invoke(
        sessionId: String,
        newDate: LocalDate,
        newHour: Int? = null,
        newMinute: Int? = null
    ) {
        val zoneId = ZoneId.systemDefault()
        val currentStart = DateTimeUtils.getStartOfDay(newDate, zoneId)
        val sessionStart = if (newHour != null && newMinute != null) {
            newDate.atTime(newHour, newMinute).atZone(zoneId).toInstant().toEpochMilli()
        } else {
            currentStart + 9 * 3600_000L // Default 9:00 AM
        }
        plannerRepository.moveSession(sessionId, sessionStart, null)
    }
}

class SavePlannerSessionUseCase(
    private val plannerRepository: PlannerRepository,
    private val subjectRepository: SubjectRepository,
    private val chapterRepository: ChapterRepository
) {
    suspend operator fun invoke(
        sessionId: String? = null,
        subjectId: String?,
        chapterId: String?,
        title: String?,
        scheduledStart: Long,
        plannedMinutes: Int
    ): StudySession {
        val subjectName = subjectId?.let { subjectRepository.getSubjectByIdOnce(it)?.name }
        val chapterName = chapterId?.let { chapterRepository.getChapterById(it)?.name }

        val finalTitle = when {
            !title.isNullOrBlank() -> title.trim()
            subjectName != null && chapterName != null -> "$subjectName • $chapterName"
            subjectName != null -> "$subjectName Study"
            else -> "Study Session"
        }

        val scheduledEnd = scheduledStart + plannedMinutes * 60_000L

        val session = if (sessionId != null) {
            StudySession(
                id = sessionId,
                subjectId = subjectId,
                chapterId = chapterId,
                title = finalTitle,
                scheduledStart = scheduledStart,
                scheduledEnd = scheduledEnd,
                plannedMinutes = plannedMinutes,
                actualMinutes = 0,
                status = StudySessionStatus.PLANNED,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            StudySession(
                subjectId = subjectId,
                chapterId = chapterId,
                title = finalTitle,
                scheduledStart = scheduledStart,
                scheduledEnd = scheduledEnd,
                plannedMinutes = plannedMinutes,
                actualMinutes = 0,
                status = StudySessionStatus.PLANNED,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }

        if (sessionId != null) {
            plannerRepository.updateSession(session)
        } else {
            plannerRepository.createSession(session)
        }
        return session
    }
}

class DeletePlannerSessionUseCase(
    private val plannerRepository: PlannerRepository
) {
    suspend operator fun invoke(sessionId: String) {
        plannerRepository.deleteSession(sessionId)
    }
}
