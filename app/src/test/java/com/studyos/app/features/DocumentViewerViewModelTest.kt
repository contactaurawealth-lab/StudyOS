package com.studyos.app.features

import com.studyos.app.domain.FakeChapterRepository
import com.studyos.app.domain.FakeNoteRepository
import com.studyos.app.domain.FakeSubjectRepository
import com.studyos.app.domain.model.AiConfig
import com.studyos.app.domain.model.Note
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.usecase.AiPracticeToolsUseCase
import com.studyos.app.domain.usecase.GetAiConfigUseCase
import com.studyos.app.domain.usecase.GetNoteUseCase
import com.studyos.app.domain.usecase.SaveFlashcardUseCase
import com.studyos.app.domain.usecase.SaveQuizUseCase
import com.studyos.app.features.document.viewmodel.DocumentFontSize
import com.studyos.app.features.document.viewmodel.DocumentReadingTheme
import com.studyos.app.features.document.viewmodel.DocumentViewMode
import com.studyos.app.features.document.viewmodel.DocumentViewerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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

@OptIn(ExperimentalCoroutinesApi::class)
class DocumentViewerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var noteRepository: FakeNoteRepository
    private lateinit var subjectRepository: FakeSubjectRepository
    private lateinit var chapterRepository: FakeChapterRepository
    private lateinit var getNoteUseCase: GetNoteUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        noteRepository = FakeNoteRepository()
        subjectRepository = FakeSubjectRepository()
        chapterRepository = FakeChapterRepository()
        getNoteUseCase = GetNoteUseCase(noteRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        docUri: String? = null,
        noteId: String? = null,
        title: String? = null
    ): DocumentViewerViewModel {
        val fakeAiProvider = com.studyos.app.domain.FakeAiProvider()
        val fakePrefs = com.studyos.app.domain.FakeTestPreferencesDataSource()
        val flashcardRepo = com.studyos.app.domain.FakeFlashcardRepository()
        val quizRepo = com.studyos.app.domain.FakeQuizRepository()

        return DocumentViewerViewModel(
            context = null,
            initialDocumentUri = docUri,
            initialNoteId = noteId,
            initialTitle = title,
            noteRepository = noteRepository,
            subjectRepository = subjectRepository,
            chapterRepository = chapterRepository,
            saveFlashcardUseCase = SaveFlashcardUseCase(flashcardRepo),
            saveQuizUseCase = SaveQuizUseCase(quizRepo),
            aiPracticeToolsUseCase = AiPracticeToolsUseCase(fakeAiProvider),
            getAiConfigUseCase = GetAiConfigUseCase(fakePrefs),
            getNoteUseCase = getNoteUseCase
        )
    }

    @Test
    fun testLoadNote_populatesDocumentStateCorrectly() = runTest {
        val sampleNote = Note(
            id = "note_101",
            title = "Photosynthesis Lecture Notes",
            content = "Plants convert sunlight and carbon dioxide into glucose and oxygen through light-dependent reactions.",
            subjectId = "sub_bio"
        )
        noteRepository.saveNote(sampleNote)

        val viewModel = createViewModel(noteId = "note_101")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Photosynthesis Lecture Notes", state.documentTitle)
        assertEquals("NOTE", state.fileType)
        assertTrue(state.wordCount > 10)
        assertEquals(sampleNote.content, state.fullTextContent)
        assertEquals(DocumentViewMode.DOCS_READING, state.viewMode)
        assertFalse(state.isLoading)
    }

    @Test
    fun testFindInDocument_identifiesMatchesAndCyclesNextPrevious() = runTest {
        val sampleNote = Note(
            id = "note_search",
            title = "Mitochondria Guide",
            content = "The mitochondria is the powerhouse of the cell. Mitochondria generate ATP. The cell requires mitochondria.",
            subjectId = "sub_bio"
        )
        noteRepository.saveNote(sampleNote)

        val viewModel = createViewModel(noteId = "note_search")
        advanceUntilIdle()

        viewModel.toggleSearch(true)
        assertTrue(viewModel.uiState.value.isSearchActive)

        // Search for "mitochondria" (appears 3 times, case-insensitive)
        viewModel.onSearchQueryChange("mitochondria")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.searchResults.size)
        assertEquals(0, state.currentMatchIndex)

        // Next match
        viewModel.nextSearchMatch()
        assertEquals(1, viewModel.uiState.value.currentMatchIndex)

        // Next match
        viewModel.nextSearchMatch()
        assertEquals(2, viewModel.uiState.value.currentMatchIndex)

        // Wrap around
        viewModel.nextSearchMatch()
        assertEquals(0, viewModel.uiState.value.currentMatchIndex)

        // Previous match wraps to last
        viewModel.previousSearchMatch()
        assertEquals(2, viewModel.uiState.value.currentMatchIndex)

        // Close search
        viewModel.toggleSearch(false)
        assertFalse(viewModel.uiState.value.isSearchActive)
        assertEquals("", viewModel.uiState.value.searchQuery)
        assertTrue(viewModel.uiState.value.searchResults.isEmpty())
    }

    @Test
    fun testThemeAndTypographyOptions_updatesState() = runTest {
        val viewModel = createViewModel(title = "Chemistry Document")
        advanceUntilIdle()

        assertEquals(DocumentReadingTheme.GOOGLE_DOCS, viewModel.uiState.value.readingTheme)

        // Switch to Sepia theme
        viewModel.setReadingTheme(DocumentReadingTheme.SEPIA)
        assertEquals(DocumentReadingTheme.SEPIA, viewModel.uiState.value.readingTheme)

        // Switch to Midnight theme
        viewModel.setReadingTheme(DocumentReadingTheme.NIGHT)
        assertEquals(DocumentReadingTheme.NIGHT, viewModel.uiState.value.readingTheme)

        // Switch font size
        viewModel.setFontSize(DocumentFontSize.COMFORTABLE)
        assertEquals(DocumentFontSize.COMFORTABLE, viewModel.uiState.value.fontSize)

        // Switch view mode
        viewModel.setViewMode(DocumentViewMode.VISUAL_CANVAS)
        assertEquals(DocumentViewMode.VISUAL_CANVAS, viewModel.uiState.value.viewMode)
    }

    @Test
    fun testTtsSpeakingControls_updatesState() = runTest {
        val viewModel = createViewModel(title = "History Article")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isTtsSpeaking)
        assertEquals(1.0f, viewModel.uiState.value.ttsSpeechRate)

        viewModel.setTtsSpeaking(true)
        assertTrue(viewModel.uiState.value.isTtsSpeaking)

        viewModel.setTtsSpeechRate(1.25f)
        assertEquals(1.25f, viewModel.uiState.value.ttsSpeechRate)

        viewModel.setTtsSpeaking(false)
        assertFalse(viewModel.uiState.value.isTtsSpeaking)
    }

    @Test
    fun testSaveAsNote_convertsDocumentTextToStudyOSNote() = runTest {
        val mathSubject = Subject(id = "sub_math", name = "Calculus")
        subjectRepository.saveSubject(mathSubject)
        chapterRepository.addChapter(mathSubject.id, "Integrals", "Integration techniques")

        val note = Note(
            id = "doc_temp",
            title = "Fundamental Theorem of Calculus",
            content = "The fundamental theorem of calculus connects differentiation and integration.",
            subjectId = "sub_math"
        )
        noteRepository.saveNote(note)

        val viewModel = createViewModel(noteId = "doc_temp")
        advanceUntilIdle()

        var createdNoteId: String? = null
        viewModel.saveAsNote { noteId ->
            createdNoteId = noteId
        }
        advanceUntilIdle()

        assertNotNull(createdNoteId)
        val saved = noteRepository.getNoteByIdOnce(createdNoteId!!)
        assertNotNull(saved)
        assertEquals("Fundamental Theorem of Calculus", saved?.title)
        assertTrue(saved?.content?.contains("differentiation and integration") == true)
        assertEquals("Saved to StudyOS Notes!", viewModel.uiState.value.infoMessage)
    }
}
