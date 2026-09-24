package com.studyos.app.features.search.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.ChapterSearchResult
import com.studyos.app.domain.model.Exam
import com.studyos.app.domain.model.Flashcard
import com.studyos.app.domain.model.Mistake
import com.studyos.app.domain.model.Note
import com.studyos.app.domain.model.RecallItem
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.repository.ChapterRepository
import com.studyos.app.domain.repository.ExamRepository
import com.studyos.app.domain.repository.FlashcardRepository
import com.studyos.app.domain.repository.MistakeRepository
import com.studyos.app.domain.repository.NoteRepository
import com.studyos.app.domain.repository.RecallRepository
import com.studyos.app.domain.usecase.GetSubjectsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val subjectResults: List<Subject> = emptyList(),
    val chapterResults: List<ChapterSearchResult> = emptyList(),
    val noteResults: List<Note> = emptyList(),
    val flashcardResults: List<Flashcard> = emptyList(),
    val mistakeResults: List<Mistake> = emptyList(),
    val recallResults: List<RecallItem> = emptyList(),
    val examResults: List<Exam> = emptyList(),
    val hasSearched: Boolean = false
) {
    val totalResultsCount: Int
        get() = subjectResults.size + chapterResults.size + noteResults.size +
                flashcardResults.size + mistakeResults.size + recallResults.size +
                examResults.size
}

class SearchViewModel(
    private val getSubjectsUseCase: GetSubjectsUseCase,
    private val chapterRepository: ChapterRepository,
    private val noteRepository: NoteRepository? = null,
    private val flashcardRepository: FlashcardRepository? = null,
    private val mistakeRepository: MistakeRepository? = null,
    private val recallRepository: RecallRepository? = null,
    private val examRepository: ExamRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var allSubjects: List<Subject> = emptyList()
    private var allChapters: List<Chapter> = emptyList()
    private var allNotes: List<Note> = emptyList()
    private var allFlashcards: List<Flashcard> = emptyList()
    private var allMistakes: List<Mistake> = emptyList()
    private var allRecallItems: List<RecallItem> = emptyList()
    private var allExams: List<Exam> = emptyList()

    init {
        // Collect Subjects & Chapters
        viewModelScope.launch {
            combine(
                getSubjectsUseCase(),
                chapterRepository.observeAllChapters()
            ) { subjects, chapters ->
                Pair(subjects, chapters)
            }.collect { (subjects, chapters) ->
                allSubjects = subjects
                allChapters = chapters
                if (_uiState.value.query.isNotBlank()) {
                    performSearch(_uiState.value.query)
                }
            }
        }

        // Collect Notes
        noteRepository?.let { repo ->
            viewModelScope.launch {
                repo.observeAllNotes().collect { notes ->
                    allNotes = notes
                    if (_uiState.value.query.isNotBlank()) {
                        performSearch(_uiState.value.query)
                    }
                }
            }
        }

        // Collect Flashcards
        flashcardRepository?.let { repo ->
            viewModelScope.launch {
                repo.observeAllFlashcards().collect { cards ->
                    allFlashcards = cards
                    if (_uiState.value.query.isNotBlank()) {
                        performSearch(_uiState.value.query)
                    }
                }
            }
        }

        // Collect Mistakes
        mistakeRepository?.let { repo ->
            viewModelScope.launch {
                repo.observeAllMistakes().collect { mistakes ->
                    allMistakes = mistakes
                    if (_uiState.value.query.isNotBlank()) {
                        performSearch(_uiState.value.query)
                    }
                }
            }
        }

        // Collect Recall Items
        recallRepository?.let { repo ->
            viewModelScope.launch {
                repo.observeDueRecallItems(System.currentTimeMillis() + 365L * 86400000L).collect { items ->
                    allRecallItems = items
                    if (_uiState.value.query.isNotBlank()) {
                        performSearch(_uiState.value.query)
                    }
                }
            }
        }

        // Collect Exams & Mock Tests
        examRepository?.let { repo ->
            viewModelScope.launch {
                repo.observeAllExams().collect { exams ->
                    allExams = exams
                    if (_uiState.value.query.isNotBlank()) {
                        performSearch(_uiState.value.query)
                    }
                }
            }
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
        performSearch(query)
    }

    private fun performSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _uiState.update {
                it.copy(
                    subjectResults = emptyList(),
                    chapterResults = emptyList(),
                    noteResults = emptyList(),
                    flashcardResults = emptyList(),
                    mistakeResults = emptyList(),
                    recallResults = emptyList(),
                    examResults = emptyList(),
                    hasSearched = false
                )
            }
            return
        }

        val matchingSubjects = allSubjects.filter {
            it.name.contains(trimmed, ignoreCase = true)
        }

        val subjectMap = allSubjects.associateBy { it.id }
        val matchingChapters = allChapters.filter { chapter ->
            chapter.name.contains(trimmed, ignoreCase = true) ||
                (chapter.description?.contains(trimmed, ignoreCase = true) == true)
        }.map { chapter ->
            val subjectName = subjectMap[chapter.subjectId]?.name ?: "Subject"
            ChapterSearchResult(
                chapter = chapter,
                subjectName = subjectName
            )
        }

        val matchingNotes = allNotes.filter { note ->
            note.title.contains(trimmed, ignoreCase = true) ||
                note.content.contains(trimmed, ignoreCase = true)
        }

        val matchingFlashcards = allFlashcards.filter { card ->
            card.question.contains(trimmed, ignoreCase = true) ||
                card.answer.contains(trimmed, ignoreCase = true)
        }

        val matchingMistakes = allMistakes.filter { mistake ->
            mistake.question.contains(trimmed, ignoreCase = true) ||
                mistake.studentAnswer.contains(trimmed, ignoreCase = true) ||
                mistake.correctAnswer.contains(trimmed, ignoreCase = true) ||
                (mistake.topic?.contains(trimmed, ignoreCase = true) == true)
        }

        val matchingRecall = allRecallItems.filter { item ->
            item.prompt.contains(trimmed, ignoreCase = true) ||
                item.expectedAnswer.contains(trimmed, ignoreCase = true)
        }

        val matchingExams = allExams.filter { exam ->
            exam.name.contains(trimmed, ignoreCase = true) ||
                (exam.notes?.contains(trimmed, ignoreCase = true) == true)
        }

        _uiState.update {
            it.copy(
                subjectResults = matchingSubjects,
                chapterResults = matchingChapters,
                noteResults = matchingNotes,
                flashcardResults = matchingFlashcards,
                mistakeResults = matchingMistakes,
                recallResults = matchingRecall,
                examResults = matchingExams,
                hasSearched = true
            )
        }
    }

    fun clearSearch() {
        _uiState.update { SearchUiState() }
    }
}
