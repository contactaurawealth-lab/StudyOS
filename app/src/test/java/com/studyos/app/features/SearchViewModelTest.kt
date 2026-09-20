package com.studyos.app.features

import com.studyos.app.domain.FakeChapterRepository
import com.studyos.app.domain.FakeSubjectRepository
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.usecase.GetSubjectsUseCase
import com.studyos.app.features.search.viewmodel.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var subjectRepository: FakeSubjectRepository
    private lateinit var chapterRepository: FakeChapterRepository
    private lateinit var getSubjectsUseCase: GetSubjectsUseCase
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        subjectRepository = FakeSubjectRepository()
        chapterRepository = FakeChapterRepository()
        getSubjectsUseCase = GetSubjectsUseCase(subjectRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSearch_findsSubjectsAndChapters() = runTest {
        val math = Subject(id = "sub_math", name = "Mathematics")
        val sci = Subject(id = "sub_sci", name = "Science")
        subjectRepository.saveSubject(math)
        subjectRepository.saveSubject(sci)

        chapterRepository.addChapter(math.id, "Algebra Basics", "Equations and variables")
        chapterRepository.addChapter(math.id, "Geometry", "Triangles")
        chapterRepository.addChapter(sci.id, "Physics of Motion", "Kinematics")

        viewModel = SearchViewModel(getSubjectsUseCase, chapterRepository)
        advanceUntilIdle()

        // Search for "algebra"
        viewModel.onQueryChanged("algebra")
        advanceUntilIdle()

        val state1 = viewModel.uiState.value
        assertTrue(state1.hasSearched)
        assertEquals(0, state1.subjectResults.size)
        assertEquals(1, state1.chapterResults.size)
        assertEquals("Algebra Basics", state1.chapterResults.first().chapter.name)
        assertEquals("Mathematics", state1.chapterResults.first().subjectName)

        // Search for "math" -> matches Mathematics subject
        viewModel.onQueryChanged("math")
        advanceUntilIdle()

        val state2 = viewModel.uiState.value
        assertEquals(1, state2.subjectResults.size)
        assertEquals("Mathematics", state2.subjectResults.first().name)

        // Clear query
        viewModel.onQueryChanged("")
        advanceUntilIdle()

        val state3 = viewModel.uiState.value
        assertTrue(state3.subjectResults.isEmpty())
        assertTrue(state3.chapterResults.isEmpty())
        assertEquals(false, state3.hasSearched)
    }
}
