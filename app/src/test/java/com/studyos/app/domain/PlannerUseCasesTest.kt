package com.studyos.app.domain

import com.studyos.app.core.util.DateTimeUtils
import com.studyos.app.domain.model.StudySession
import com.studyos.app.domain.model.StudySessionStatus
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.repository.PlannerRepository
import com.studyos.app.domain.usecase.CheckSessionOverlapUseCase
import com.studyos.app.domain.usecase.DeletePlannerSessionUseCase
import com.studyos.app.domain.usecase.GetUpcomingScheduleUseCase
import com.studyos.app.domain.usecase.GetWeekScheduleUseCase
import com.studyos.app.domain.usecase.MoveSessionUseCase
import com.studyos.app.domain.usecase.SavePlannerSessionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class FakePlannerRepository : PlannerRepository {
    val items = mutableListOf<StudySession>()
    private val sessionsFlow = MutableStateFlow<List<StudySession>>(emptyList())

    private fun sync() {
        sessionsFlow.value = items.toList()
    }

    override fun observeSessionsForDay(startOfDay: Long, endOfDay: Long): Flow<List<StudySession>> =
        sessionsFlow.map { list ->
            list.filter { it.scheduledStart != null && it.scheduledStart!! in startOfDay..endOfDay }
        }

    override fun observeSessionsForRange(startTime: Long, endTime: Long): Flow<List<StudySession>> =
        sessionsFlow.map { list ->
            list.filter { it.scheduledStart != null && it.scheduledStart!! in startTime..endTime }
        }

    override fun observeUpcomingSessions(): Flow<List<StudySession>> =
        sessionsFlow.map { list ->
            val now = System.currentTimeMillis()
            list.filter { (it.scheduledEnd ?: it.scheduledStart ?: 0) >= now }
        }

    override suspend fun getSessionsForDayOnce(startOfDay: Long, endOfDay: Long): List<StudySession> =
        items.filter { it.scheduledStart != null && it.scheduledStart!! in startOfDay..endOfDay }

    override suspend fun checkOverlap(startTime: Long, endTime: Long, excludeSessionId: String?): Boolean =
        items.any { session ->
            if (session.id == excludeSessionId) return@any false
            val sStart = session.scheduledStart ?: return@any false
            val sEnd = session.scheduledEnd ?: (sStart + session.plannedMinutes * 60_000L)
            startTime < sEnd && endTime > sStart
        }

    override suspend fun createSession(session: StudySession): StudySession {
        items.add(session)
        sync()
        return session
    }

    override suspend fun updateSession(session: StudySession) {
        val idx = items.indexOfFirst { it.id == session.id }
        if (idx != -1) {
            items[idx] = session
            sync()
        }
    }

    override suspend fun moveSession(sessionId: String, newScheduledStart: Long, newScheduledEnd: Long?) {
        val idx = items.indexOfFirst { it.id == sessionId }
        if (idx != -1) {
            val cur = items[idx]
            val end = newScheduledEnd ?: (newScheduledStart + cur.plannedMinutes * 60_000L)
            items[idx] = cur.copy(scheduledStart = newScheduledStart, scheduledEnd = end)
            sync()
        }
    }

    override suspend fun deleteSession(sessionId: String) {
        items.removeAll { it.id == sessionId }
        sync()
    }
}

class PlannerUseCasesTest {

    private lateinit var plannerRepository: FakePlannerRepository
    private lateinit var subjectRepository: FakeSubjectRepository
    private lateinit var chapterRepository: FakeChapterRepository

    private lateinit var getWeekScheduleUseCase: GetWeekScheduleUseCase
    private lateinit var getUpcomingScheduleUseCase: GetUpcomingScheduleUseCase
    private lateinit var checkSessionOverlapUseCase: CheckSessionOverlapUseCase
    private lateinit var moveSessionUseCase: MoveSessionUseCase
    private lateinit var savePlannerSessionUseCase: SavePlannerSessionUseCase
    private lateinit var deletePlannerSessionUseCase: DeletePlannerSessionUseCase

    @Before
    fun setup() {
        plannerRepository = FakePlannerRepository()
        subjectRepository = FakeSubjectRepository()
        chapterRepository = FakeChapterRepository()

        getWeekScheduleUseCase = GetWeekScheduleUseCase(plannerRepository, subjectRepository, chapterRepository)
        getUpcomingScheduleUseCase = GetUpcomingScheduleUseCase(plannerRepository, subjectRepository, chapterRepository)
        checkSessionOverlapUseCase = CheckSessionOverlapUseCase(plannerRepository)
        moveSessionUseCase = MoveSessionUseCase(plannerRepository)
        savePlannerSessionUseCase = SavePlannerSessionUseCase(plannerRepository, subjectRepository, chapterRepository)
        deletePlannerSessionUseCase = DeletePlannerSessionUseCase(plannerRepository)
    }

    @Test
    fun testSavePlannerSession_createNew() = runTest {
        val subject = Subject(id = "sub_1", name = "Physics", isCustom = true)
        subjectRepository.saveSubject(subject)

        val startTime = System.currentTimeMillis() + 3600_000L
        val session = savePlannerSessionUseCase(
            sessionId = null,
            subjectId = "sub_1",
            chapterId = null,
            title = null,
            scheduledStart = startTime,
            plannedMinutes = 45
        )

        assertNotNull(session.id)
        assertEquals("Physics Study", session.title)
        assertEquals(startTime + 45 * 60_000L, session.scheduledEnd)
        assertEquals(1, plannerRepository.items.size)
    }

    @Test
    fun testCheckSessionOverlap_detectsOverlap() = runTest {
        val baseStart = 1000000L
        val baseEnd = baseStart + 60 * 60_000L // 1 hour

        plannerRepository.createSession(
            StudySession(
                id = "sess_1",
                title = "Session 1",
                scheduledStart = baseStart,
                scheduledEnd = baseEnd,
                plannedMinutes = 60,
                status = StudySessionStatus.PLANNED
            )
        )

        // Inside the interval -> Overlap
        val overlaps = checkSessionOverlapUseCase(baseStart + 10_000L, baseEnd - 10_000L)
        assertTrue(overlaps)

        // Before the interval -> No overlap
        val noOverlapBefore = checkSessionOverlapUseCase(baseStart - 100_000L, baseStart)
        assertFalse(noOverlapBefore)

        // After the interval -> No overlap
        val noOverlapAfter = checkSessionOverlapUseCase(baseEnd, baseEnd + 100_000L)
        assertFalse(noOverlapAfter)

        // Exclude own ID during edit -> No overlap
        val excludeSelf = checkSessionOverlapUseCase(baseStart, baseEnd, excludeSessionId = "sess_1")
        assertFalse(excludeSelf)
    }

    @Test
    fun testMoveSession() = runTest {
        val baseStart = 1000000L
        val session = plannerRepository.createSession(
            StudySession(
                id = "sess_move",
                title = "Session To Move",
                scheduledStart = baseStart,
                scheduledEnd = baseStart + 3600_000L,
                plannedMinutes = 60,
                status = StudySessionStatus.PLANNED
            )
        )

        val newDate = LocalDate.now().plusDays(2)
        moveSessionUseCase("sess_move", newDate, 14, 30)

        val updated = plannerRepository.items.first { it.id == "sess_move" }
        assertNotNull(updated.scheduledStart)
        assertTrue(updated.scheduledStart != baseStart)
    }

    @Test
    fun testDeletePlannerSession() = runTest {
        val session = plannerRepository.createSession(
            StudySession(
                id = "sess_del",
                title = "To Delete",
                scheduledStart = 1000000L,
                plannedMinutes = 30,
                status = StudySessionStatus.PLANNED
            )
        )
        assertEquals(1, plannerRepository.items.size)

        deletePlannerSessionUseCase("sess_del")
        assertEquals(0, plannerRepository.items.size)
    }

    @Test
    fun testGetWeekSchedule_returns7Days() = runTest {
        val today = LocalDate.now()
        val weekSchedule = getWeekScheduleUseCase(today).first()

        assertEquals(7, weekSchedule.weekDays.size)
        assertEquals(today, weekSchedule.selectedDate)
        assertTrue(weekSchedule.weekDays.any { it.isSelected })
        assertTrue(weekSchedule.weekDays.any { it.isToday })
    }
}
