package com.studyos.app.features

import com.studyos.app.domain.FakeChapterRepository
import com.studyos.app.domain.FakePlannerRepository
import com.studyos.app.domain.FakeSubjectRepository
import com.studyos.app.domain.model.PlannerViewMode
import com.studyos.app.domain.model.StudyPreferences
import com.studyos.app.domain.usecase.CheckSessionOverlapUseCase
import com.studyos.app.domain.usecase.DeletePlannerSessionUseCase
import com.studyos.app.domain.usecase.GetChaptersForSubjectUseCase
import com.studyos.app.domain.usecase.GetStudyPreferencesUseCase
import com.studyos.app.domain.usecase.GetSubjectsUseCase
import com.studyos.app.domain.usecase.GetUpcomingScheduleUseCase
import com.studyos.app.domain.usecase.GetWeekScheduleUseCase
import com.studyos.app.domain.usecase.MoveSessionUseCase
import com.studyos.app.domain.usecase.SavePlannerSessionUseCase
import com.studyos.app.features.planner.viewmodel.PlannerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class PlannerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var plannerRepository: FakePlannerRepository
    private lateinit var subjectRepository: FakeSubjectRepository
    private lateinit var chapterRepository: FakeChapterRepository
    private lateinit var studyPreferencesRepository: FakeStudyPreferencesRepository

    private lateinit var getWeekScheduleUseCase: GetWeekScheduleUseCase
    private lateinit var getUpcomingScheduleUseCase: GetUpcomingScheduleUseCase
    private lateinit var checkSessionOverlapUseCase: CheckSessionOverlapUseCase
    private lateinit var savePlannerSessionUseCase: SavePlannerSessionUseCase
    private lateinit var moveSessionUseCase: MoveSessionUseCase
    private lateinit var deletePlannerSessionUseCase: DeletePlannerSessionUseCase
    private lateinit var getSubjectsUseCase: GetSubjectsUseCase
    private lateinit var getChaptersForSubjectUseCase: GetChaptersForSubjectUseCase
    private lateinit var getStudyPreferencesUseCase: GetStudyPreferencesUseCase

    private lateinit var viewModel: PlannerViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        plannerRepository = FakePlannerRepository()
        subjectRepository = FakeSubjectRepository()
        chapterRepository = FakeChapterRepository()
        studyPreferencesRepository = FakeStudyPreferencesRepository().apply {
            savedPreferences = StudyPreferences()
        }

        getWeekScheduleUseCase = GetWeekScheduleUseCase(plannerRepository, subjectRepository, chapterRepository)
        getUpcomingScheduleUseCase = GetUpcomingScheduleUseCase(plannerRepository, subjectRepository, chapterRepository)
        checkSessionOverlapUseCase = CheckSessionOverlapUseCase(plannerRepository)
        savePlannerSessionUseCase = SavePlannerSessionUseCase(plannerRepository, subjectRepository, chapterRepository)
        moveSessionUseCase = MoveSessionUseCase(plannerRepository)
        deletePlannerSessionUseCase = DeletePlannerSessionUseCase(plannerRepository)
        getSubjectsUseCase = GetSubjectsUseCase(subjectRepository)
        getChaptersForSubjectUseCase = GetChaptersForSubjectUseCase(chapterRepository)
        getStudyPreferencesUseCase = GetStudyPreferencesUseCase(studyPreferencesRepository)

        viewModel = PlannerViewModel(
            getWeekScheduleUseCase = getWeekScheduleUseCase,
            getUpcomingScheduleUseCase = getUpcomingScheduleUseCase,
            checkSessionOverlapUseCase = checkSessionOverlapUseCase,
            savePlannerSessionUseCase = savePlannerSessionUseCase,
            moveSessionUseCase = moveSessionUseCase,
            deletePlannerSessionUseCase = deletePlannerSessionUseCase,
            getSubjectsUseCase = getSubjectsUseCase,
            getChaptersForSubjectUseCase = getChaptersForSubjectUseCase,
            getStudyPreferencesUseCase = getStudyPreferencesUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState_loadsWeek() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(LocalDate.now(), state.selectedDate)
        assertEquals(PlannerViewMode.WEEK, state.viewMode)
        assertNotNull(state.weekSchedule)
        assertEquals(7, state.weekSchedule?.weekDays?.size)
    }

    @Test
    fun testSelectDate() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()
        val futureDate = LocalDate.now().plusDays(5)
        viewModel.selectDate(futureDate)
        advanceUntilIdle()

        assertEquals(futureDate, viewModel.uiState.value.selectedDate)
    }

    @Test
    fun testSetViewMode() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()
        viewModel.setViewMode(PlannerViewMode.DAY)
        advanceUntilIdle()
        assertEquals(PlannerViewMode.DAY, viewModel.uiState.value.viewMode)

        viewModel.setViewMode(PlannerViewMode.LIST)
        advanceUntilIdle()
        assertEquals(PlannerViewMode.LIST, viewModel.uiState.value.viewMode)
    }

    @Test
    fun testJumpToToday() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()
        viewModel.selectDate(LocalDate.now().minusWeeks(2))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.selectedDate != LocalDate.now())

        viewModel.goToToday()
        advanceUntilIdle()
        assertEquals(LocalDate.now(), viewModel.uiState.value.selectedDate)
    }

    @Test
    fun testCreateSession_andDeleteSession() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()
        val startTime = System.currentTimeMillis() + 3600_000L

        viewModel.saveSession(
            sessionId = null,
            subjectId = null,
            chapterId = null,
            title = "Independent Study",
            scheduledStart = startTime,
            plannedMinutes = 45
        )
        advanceUntilIdle()

        assertEquals(1, plannerRepository.items.size)
        val created = plannerRepository.items.first()
        assertEquals("Independent Study", created.title)

        viewModel.deleteSession(created.id)
        advanceUntilIdle()
        assertEquals(0, plannerRepository.items.size)
    }
}
