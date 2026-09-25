package com.studyos.app.features

import com.studyos.app.domain.FakeChapterRepository
import com.studyos.app.domain.FakeSubjectRepository
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.usecase.AddChapterUseCase
import com.studyos.app.domain.usecase.DeleteChapterUseCase
import com.studyos.app.domain.usecase.DeleteSubjectUseCase
import com.studyos.app.domain.usecase.GetChaptersForSubjectUseCase
import com.studyos.app.domain.usecase.GetSubjectByIdUseCase
import com.studyos.app.domain.usecase.MoveChapterUseCase
import com.studyos.app.domain.usecase.RenameSubjectUseCase
import com.studyos.app.features.subjects.viewmodel.SubjectDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SubjectDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var subjectRepository: FakeSubjectRepository
    private lateinit var chapterRepository: FakeChapterRepository
    private lateinit var viewModel: SubjectDetailViewModel

    private val subject = Subject(id = "sub_math", name = "Mathematics")

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        subjectRepository = FakeSubjectRepository()
        chapterRepository = FakeChapterRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSubjectDetail_loadsDataAndCalculatesProgress() = runTest {
        subjectRepository.saveSubject(subject)

        val ch1 = chapterRepository.addChapter(subject.id, "Ch 1", null)
        val ch2 = chapterRepository.addChapter(subject.id, "Ch 2", null)
        chapterRepository.updateChapterProgress(ch1.id, 100)
        chapterRepository.updateChapterProgress(ch2.id, 50)

        viewModel = SubjectDetailViewModel(
            subjectId = subject.id,
            getSubjectByIdUseCase = GetSubjectByIdUseCase(subjectRepository),
            getChaptersForSubjectUseCase = GetChaptersForSubjectUseCase(chapterRepository),
            addChapterUseCase = AddChapterUseCase(chapterRepository),
            deleteChapterUseCase = DeleteChapterUseCase(chapterRepository),
            moveChapterUseCase = MoveChapterUseCase(chapterRepository),
            renameSubjectUseCase = RenameSubjectUseCase(subjectRepository),
            deleteSubjectUseCase = DeleteSubjectUseCase(subjectRepository)
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertNotNull(state.subject)
        assertEquals("Mathematics", state.subject?.name)
        assertEquals(2, state.chapters.size)
        assertEquals(75, state.progress) // (100 + 50) / 2 = 75%
        assertEquals(1, state.completedCount)
    }

    @Test
    fun testSubjectDetail_addChapter_updatesList() = runTest {
        subjectRepository.saveSubject(subject)

        viewModel = SubjectDetailViewModel(
            subjectId = subject.id,
            getSubjectByIdUseCase = GetSubjectByIdUseCase(subjectRepository),
            getChaptersForSubjectUseCase = GetChaptersForSubjectUseCase(chapterRepository),
            addChapterUseCase = AddChapterUseCase(chapterRepository),
            deleteChapterUseCase = DeleteChapterUseCase(chapterRepository),
            moveChapterUseCase = MoveChapterUseCase(chapterRepository),
            renameSubjectUseCase = RenameSubjectUseCase(subjectRepository),
            deleteSubjectUseCase = DeleteSubjectUseCase(subjectRepository)
        )
        advanceUntilIdle()

        val success = viewModel.addChapter("Probability", "Basic concepts")
        advanceUntilIdle()

        assertTrue(success)
        assertEquals(1, viewModel.uiState.value.chapters.size)
        assertEquals("Probability", viewModel.uiState.value.chapters.first().name)
    }

    @Test
    fun testSubjectDetail_deleteSubject_setsDeletedFlag() = runTest {
        subjectRepository.saveSubject(subject)

        viewModel = SubjectDetailViewModel(
            subjectId = subject.id,
            getSubjectByIdUseCase = GetSubjectByIdUseCase(subjectRepository),
            getChaptersForSubjectUseCase = GetChaptersForSubjectUseCase(chapterRepository),
            addChapterUseCase = AddChapterUseCase(chapterRepository),
            deleteChapterUseCase = DeleteChapterUseCase(chapterRepository),
            moveChapterUseCase = MoveChapterUseCase(chapterRepository),
            renameSubjectUseCase = RenameSubjectUseCase(subjectRepository),
            deleteSubjectUseCase = DeleteSubjectUseCase(subjectRepository)
        )
        advanceUntilIdle()

        viewModel.deleteSubject()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isDeleted)
    }
}
