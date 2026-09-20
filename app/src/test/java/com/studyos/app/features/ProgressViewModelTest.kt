package com.studyos.app.features

import com.studyos.app.domain.FakeChapterRepository
import com.studyos.app.domain.FakeSubjectRepository
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.usecase.GetAcademicProgressUseCase
import com.studyos.app.features.progress.viewmodel.ProgressViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var subjectRepository: FakeSubjectRepository
    private lateinit var chapterRepository: FakeChapterRepository
    private lateinit var getAcademicProgressUseCase: GetAcademicProgressUseCase
    private lateinit var viewModel: ProgressViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        subjectRepository = FakeSubjectRepository()
        chapterRepository = FakeChapterRepository()
        getAcademicProgressUseCase = GetAcademicProgressUseCase(subjectRepository, chapterRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testProgress_emptyState() = runTest {
        val math = Subject(name = "Mathematics")
        subjectRepository.saveSubject(math)

        viewModel = ProgressViewModel(getAcademicProgressUseCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(0, state.academicProgress.overallProgress)
        assertEquals(0, state.academicProgress.totalChapters)
        assertEquals(0, state.academicProgress.completedChapters)
        assertEquals(1, state.academicProgress.subjectBreakdowns.size)
        assertEquals(0, state.academicProgress.subjectBreakdowns.first().progress)
    }

    @Test
    fun testProgress_withChaptersAndCompletion() = runTest {
        val math = Subject(id = "math_id", name = "Mathematics")
        val science = Subject(id = "sci_id", name = "Science")
        subjectRepository.saveSubject(math)
        subjectRepository.saveSubject(science)

        // Math: 2 chapters (100% and 50%) -> 75%
        val m1 = chapterRepository.addChapter(math.id, "M1", null)
        val m2 = chapterRepository.addChapter(math.id, "M2", null)
        chapterRepository.updateChapterProgress(m1.id, 100)
        chapterRepository.updateChapterProgress(m2.id, 50)

        // Science: 2 chapters (100% and 0%) -> 50%
        val s1 = chapterRepository.addChapter(science.id, "S1", null)
        val s2 = chapterRepository.addChapter(science.id, "S2", null)
        chapterRepository.updateChapterProgress(s1.id, 100)
        chapterRepository.updateChapterProgress(s2.id, 0)

        viewModel = ProgressViewModel(getAcademicProgressUseCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val progress = state.academicProgress

        assertEquals(4, progress.totalChapters)
        assertEquals(2, progress.completedChapters)
        // Overall: (100 + 50 + 100 + 0) / 4 = 250 / 4 = 62%
        assertEquals(62, progress.overallProgress)

        val mathBreakdown = progress.subjectBreakdowns.find { it.subjectId == math.id }!!
        assertEquals(75, mathBreakdown.progress)
        assertEquals(1, mathBreakdown.completedChapters)
        assertEquals(2, mathBreakdown.totalChapters)

        val sciBreakdown = progress.subjectBreakdowns.find { it.subjectId == science.id }!!
        assertEquals(50, sciBreakdown.progress)
        assertEquals(1, sciBreakdown.completedChapters)
        assertEquals(2, sciBreakdown.totalChapters)
    }
}
