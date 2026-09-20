package com.studyos.app.domain.usecase

import com.studyos.app.core.util.DateTimeUtils
import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.ChapterStatus
import com.studyos.app.domain.model.FocusItem
import com.studyos.app.domain.model.RecentChapterItem
import com.studyos.app.domain.model.Student
import com.studyos.app.domain.model.StudyPreferences
import com.studyos.app.domain.model.StudySession
import com.studyos.app.domain.model.StudySessionItem
import com.studyos.app.domain.model.StudySessionStatus
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.repository.ChapterRepository
import com.studyos.app.domain.repository.StudentRepository
import com.studyos.app.domain.repository.StudyPreferencesRepository
import com.studyos.app.domain.repository.StudySessionRepository
import com.studyos.app.domain.repository.SubjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

data class TodayData(
    val student: Student? = null,
    val preferences: StudyPreferences? = null,
    val focusItem: FocusItem? = null,
    val upcomingSessions: List<StudySessionItem> = emptyList(),
    val completedMinutesToday: Int = 0,
    val dailyGoalMinutes: Int = 60,
    val recentChapter: RecentChapterItem? = null
)

class GetTodayDataUseCase(
    private val studentRepository: StudentRepository,
    private val studyPreferencesRepository: StudyPreferencesRepository,
    private val subjectRepository: SubjectRepository,
    private val chapterRepository: ChapterRepository,
    private val studySessionRepository: StudySessionRepository
) {
    operator fun invoke(date: LocalDate = LocalDate.now()): Flow<TodayData> {
        val startOfDay = DateTimeUtils.getStartOfDay(date)
        val endOfDay = DateTimeUtils.getEndOfDay(date)

        return combine(
            studentRepository.getStudent(),
            studyPreferencesRepository.getPreferences(),
            subjectRepository.getAllSubjects(),
            chapterRepository.observeAllChapters(),
            studySessionRepository.observeSessionsForDay(startOfDay, endOfDay),
            chapterRepository.observeMostRecentChapter()
        ) { args: Array<Any?> ->
            @Suppress("UNCHECKED_CAST")
            val student = args[0] as? Student
            @Suppress("UNCHECKED_CAST")
            val preferences = args[1] as? StudyPreferences
            @Suppress("UNCHECKED_CAST")
            val subjects = (args[2] as? List<Subject>) ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val chapters = (args[3] as? List<Chapter>) ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val todaySessions = (args[4] as? List<StudySession>) ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val recentChapter = args[5] as? Chapter

            val now = System.currentTimeMillis()
            val subjectMap = subjects.associateBy { it.id }
            val chapterMap = chapters.associateBy { it.id }

            // 1. Calculate Today's Focus
            val focusItem = determineFocusItem(
                now = now,
                endOfDay = endOfDay,
                todaySessions = todaySessions,
                chapters = chapters,
                subjects = subjects
            )

            // 2. Calculate Next Up (upcoming sessions today, excluding sessions already completed or cancelled)
            val upcomingSessions = todaySessions
                .filter { it.status == StudySessionStatus.PLANNED || it.status == StudySessionStatus.IN_PROGRESS }
                // Exclude current focus session if it's already highlighted in Focus card
                .filter { session ->
                    val isCurrentFocus = focusItem is FocusItem.SessionFocus && focusItem.session.id == session.id
                    !isCurrentFocus
                }
                .sortedBy { it.scheduledStart ?: Long.MAX_VALUE }
                .take(3)
                .map { session ->
                    StudySessionItem(
                        session = session,
                        subjectName = session.subjectId?.let { subjectMap[it]?.name } ?: "General Study",
                        chapterName = session.chapterId?.let { chapterMap[it]?.name },
                        formattedTime = session.scheduledStart?.let { DateTimeUtils.formatTime(it) }
                            ?: "${session.plannedMinutes}m"
                    )
                }

            // 3. Completed minutes today
            val completedMinutesToday = todaySessions
                .filter { it.status == StudySessionStatus.COMPLETED }
                .sumOf { it.actualMinutes }

            val goalMinutes = preferences?.dailyStudyGoalMinutes?.takeIf { it > 0 } ?: 60

            // 4. Continue studying recent chapter
            val recentChapterItem = recentChapter
                ?.takeIf { it.lastOpenedAt != null }
                ?.let { chapter ->
                    val subjName = subjectMap[chapter.subjectId]?.name ?: ""
                    RecentChapterItem(chapter = chapter, subjectName = subjName)
                }

            TodayData(
                student = student,
                preferences = preferences,
                focusItem = focusItem,
                upcomingSessions = upcomingSessions,
                completedMinutesToday = completedMinutesToday,
                dailyGoalMinutes = goalMinutes,
                recentChapter = recentChapterItem
            )
        }
    }

    fun determineFocusItem(
        now: Long,
        endOfDay: Long,
        todaySessions: List<StudySession>,
        chapters: List<Chapter>,
        subjects: List<Subject>
    ): FocusItem? {
        val subjectMap = subjects.associateBy { it.id }
        val chapterMap = chapters.associateBy { it.id }

        // Priority 1: Scheduled session happening right now
        val currentSession = todaySessions.firstOrNull { session ->
            val start = session.scheduledStart
            if (start != null && (session.status == StudySessionStatus.PLANNED || session.status == StudySessionStatus.IN_PROGRESS)) {
                val end = session.scheduledEnd ?: (start + session.plannedMinutes * 60_000L)
                now in start..end
            } else {
                false
            }
        }
        if (currentSession != null) {
            return FocusItem.SessionFocus(
                session = currentSession,
                subjectName = currentSession.subjectId?.let { subjectMap[it]?.name } ?: "General Study",
                chapterName = currentSession.chapterId?.let { chapterMap[it]?.name }
            )
        }

        // Priority 2: Next scheduled session today
        val nextSession = todaySessions
            .filter { session ->
                val start = session.scheduledStart
                start != null && start > now && start <= endOfDay && session.status == StudySessionStatus.PLANNED
            }
            .minByOrNull { it.scheduledStart ?: Long.MAX_VALUE }

        if (nextSession != null) {
            return FocusItem.SessionFocus(
                session = nextSession,
                subjectName = nextSession.subjectId?.let { subjectMap[it]?.name } ?: "General Study",
                chapterName = nextSession.chapterId?.let { chapterMap[it]?.name }
            )
        }

        // Priority 3: Chapter currently in-progress
        val inProgressChapters = chapters.filter {
            (it.status == ChapterStatus.IN_PROGRESS || (it.progress in 1..99)) && it.status != ChapterStatus.COMPLETED
        }
        if (inProgressChapters.isNotEmpty()) {
            // Sort by most recently opened, then lowest progress
            val chapter = inProgressChapters.maxByOrNull { it.lastOpenedAt ?: 0L }
                ?: inProgressChapters.first()
            return FocusItem.ChapterFocus(
                chapter = chapter,
                subjectName = subjectMap[chapter.subjectId]?.name ?: "",
                isLowestProgress = false
            )
        }

        // Priority 4: Chapter with lowest progress (< 100)
        val incompleteChapters = chapters.filter { it.progress < 100 && it.status != ChapterStatus.COMPLETED }
        if (incompleteChapters.isNotEmpty()) {
            val lowestChapter = incompleteChapters.minByOrNull { it.progress } ?: incompleteChapters.first()
            return FocusItem.ChapterFocus(
                chapter = lowestChapter,
                subjectName = subjectMap[lowestChapter.subjectId]?.name ?: "",
                isLowestProgress = true
            )
        }

        // Priority 5: None
        return null
    }
}

class PlanSessionUseCase(
    private val studySessionRepository: StudySessionRepository,
    private val subjectRepository: SubjectRepository,
    private val chapterRepository: ChapterRepository
) {
    suspend operator fun invoke(
        subjectId: String?,
        chapterId: String?,
        title: String?,
        scheduledStart: Long,
        plannedMinutes: Int
    ): StudySession {
        require(plannedMinutes > 0) { "Planned duration must be positive" }

        val subjectName = subjectId?.let { subjectRepository.getSubjectByIdOnce(it)?.name }
        val chapterName = chapterId?.let { chapterRepository.getChapterById(it)?.name }

        val finalTitle = when {
            !title.isNullOrBlank() -> title.trim()
            subjectName != null && chapterName != null -> "$subjectName • $chapterName"
            subjectName != null -> "$subjectName Study"
            else -> "Study Session"
        }

        val scheduledEnd = scheduledStart + plannedMinutes * 60_000L

        val session = StudySession(
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

        return studySessionRepository.createSession(session)
    }
}

class DeleteSessionUseCase(
    private val studySessionRepository: StudySessionRepository
) {
    suspend operator fun invoke(sessionId: String) {
        studySessionRepository.deleteSession(sessionId)
    }
}

class UpdateSessionStatusUseCase(
    private val studySessionRepository: StudySessionRepository
) {
    suspend operator fun invoke(
        sessionId: String,
        status: StudySessionStatus,
        actualMinutes: Int? = null
    ) {
        studySessionRepository.updateSessionStatus(sessionId, status, actualMinutes)
    }
}
