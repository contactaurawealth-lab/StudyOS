package com.studyos.app.features.document.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.core.util.DocumentTextExtractor
import com.studyos.app.core.util.PdfPageRenderer
import com.studyos.app.core.util.RenderedPdfPage
import com.studyos.app.domain.model.AiConfig
import com.studyos.app.domain.model.Note
import com.studyos.app.domain.model.QuizDifficulty
import com.studyos.app.domain.repository.ChapterRepository
import com.studyos.app.domain.repository.NoteRepository
import com.studyos.app.domain.repository.SubjectRepository
import com.studyos.app.domain.usecase.AiPracticeToolsUseCase
import com.studyos.app.domain.usecase.GetAiConfigUseCase
import com.studyos.app.domain.usecase.GetNoteUseCase
import com.studyos.app.domain.usecase.SaveFlashcardUseCase
import com.studyos.app.domain.usecase.SaveQuizUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class DocumentReadingTheme(
    val label: String,
    val paperColor: Color,
    val canvasColor: Color,
    val textColor: Color,
    val secondaryTextColor: Color,
    val accentColor: Color,
    val borderColor: Color
) {
    GOOGLE_DOCS(
        label = "Google Docs",
        paperColor = Color(0xFFFFFFFF),
        canvasColor = Color(0xFFEDF2F7),
        textColor = Color(0xFF1E293B),
        secondaryTextColor = Color(0xFF64748B),
        accentColor = Color(0xFF2563EB),
        borderColor = Color(0xFFCBD5E1)
    ),
    SEPIA(
        label = "Sepia Warm",
        paperColor = Color(0xFFFBF0D9),
        canvasColor = Color(0xFFE8DCB8),
        textColor = Color(0xFF433422),
        secondaryTextColor = Color(0xFF7A654C),
        accentColor = Color(0xFFB45309),
        borderColor = Color(0xFFDCC89E)
    ),
    NIGHT(
        label = "Midnight Dark",
        paperColor = Color(0xFF181A20),
        canvasColor = Color(0xFF0F1015),
        textColor = Color(0xFFF1F5F9),
        secondaryTextColor = Color(0xFF94A3B8),
        accentColor = Color(0xFF60A5FA),
        borderColor = Color(0xFF2D3748)
    ),
    SLATE(
        label = "Slate Cool",
        paperColor = Color(0xFF1E293B),
        canvasColor = Color(0xFF0F172A),
        textColor = Color(0xFFF8FAFC),
        secondaryTextColor = Color(0xFF94A3B8),
        accentColor = Color(0xFF38BDF8),
        borderColor = Color(0xFF334155)
    )
}

enum class DocumentFontSize(
    val label: String,
    val sizeSp: Int,
    val lineHeightSp: Int
) {
    COMPACT("Small", 13, 20),
    STANDARD("Normal", 16, 26),
    COMFORTABLE("Large", 19, 30),
    LARGE("Extra Large", 23, 36)
}

enum class DocumentViewMode {
    VISUAL_CANVAS, // Native high-DPI rendered PDF pages
    DOCS_READING   // Google Docs formatted paper sheets
}

data class DocumentSearchResult(
    val index: Int,
    val startOffset: Int,
    val endOffset: Int,
    val previewText: String,
    val pageNumber: Int = 1
)

data class DocumentViewerUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val documentUri: String? = null,
    val noteId: String? = null,
    val documentTitle: String = "Untitled Document",
    val fileType: String = "DOC",
    val wordCount: Int = 0,
    val characterCount: Int = 0,
    val fullTextContent: String = "",
    val pdfPages: List<RenderedPdfPage> = emptyList(),
    val totalPages: Int = 1,
    val currentPageIndex: Int = 0,
    val viewMode: DocumentViewMode = DocumentViewMode.DOCS_READING,
    val readingTheme: DocumentReadingTheme = DocumentReadingTheme.GOOGLE_DOCS,
    val fontSize: DocumentFontSize = DocumentFontSize.STANDARD,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<DocumentSearchResult> = emptyList(),
    val currentMatchIndex: Int = 0,
    val isTtsSpeaking: Boolean = false,
    val ttsSpeechRate: Float = 1.0f,
    val isAiWorking: Boolean = false
)

class DocumentViewerViewModel(
    private val context: Context? = null,
    private val initialDocumentUri: String? = null,
    private val initialNoteId: String? = null,
    private val initialTitle: String? = null,
    private val noteRepository: NoteRepository,
    private val subjectRepository: SubjectRepository,
    private val chapterRepository: ChapterRepository,
    private val saveFlashcardUseCase: SaveFlashcardUseCase,
    private val saveQuizUseCase: SaveQuizUseCase,
    private val aiPracticeToolsUseCase: AiPracticeToolsUseCase,
    private val getAiConfigUseCase: GetAiConfigUseCase,
    private val getNoteUseCase: GetNoteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DocumentViewerUiState(
            documentUri = initialDocumentUri,
            noteId = initialNoteId,
            documentTitle = initialTitle?.ifBlank { null } ?: "Document"
        )
    )
    val uiState: StateFlow<DocumentViewerUiState> = _uiState.asStateFlow()

    init {
        loadInitialContent()
    }

    private fun loadInitialContent() {
        if (!initialNoteId.isNullOrBlank()) {
            loadNote(initialNoteId)
        } else if (!initialDocumentUri.isNullOrBlank()) {
            loadUri(Uri.parse(initialDocumentUri))
        }
    }

    fun loadUri(uri: Uri) {
        val currentContext = context
        if (currentContext == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Cannot open file: Context not available."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    documentUri = uri.toString(),
                    errorMessage = null
                )
            }

            try {
                // 1. Extract structured text using DocumentTextExtractor
                val extracted = withContext(Dispatchers.IO) {
                    DocumentTextExtractor.extract(currentContext, uri, fallbackTitle = _uiState.value.documentTitle)
                }

                val isPdf = extracted.fileType.equals("PDF", ignoreCase = true)
                var pdfPagesList: List<RenderedPdfPage> = emptyList()

                // 2. If PDF, render high-DPI page bitmaps
                if (isPdf) {
                    pdfPagesList = try {
                        PdfPageRenderer.renderPdfPages(currentContext, uri)
                    } catch (e: Exception) {
                        emptyList()
                    }
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        documentTitle = extracted.title.ifBlank { "Document" },
                        fileType = extracted.fileType,
                        wordCount = extracted.wordCount,
                        characterCount = extracted.characterCount,
                        fullTextContent = extracted.content,
                        pdfPages = pdfPagesList,
                        totalPages = if (isPdf && pdfPagesList.isNotEmpty()) pdfPagesList.size else 1,
                        viewMode = if (isPdf && pdfPagesList.isNotEmpty()) DocumentViewMode.VISUAL_CANVAS else DocumentViewMode.DOCS_READING,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load document: ${e.message}"
                    )
                }
            }
        }
    }

    fun loadNote(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val note = getNoteUseCase.getOnce(id)
            if (note != null) {
                val words = note.content.split("\\s+".toRegex()).count { it.isNotBlank() }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        noteId = note.id,
                        documentTitle = note.title.ifBlank { "Study Note" },
                        fileType = "NOTE",
                        wordCount = words,
                        characterCount = note.content.length,
                        fullTextContent = note.content,
                        pdfPages = emptyList(),
                        totalPages = 1,
                        viewMode = DocumentViewMode.DOCS_READING,
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not find requested note."
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        val trimmed = query.trim()
        val text = _uiState.value.fullTextContent

        if (trimmed.isBlank() || text.isBlank()) {
            _uiState.update {
                it.copy(
                    searchQuery = query,
                    searchResults = emptyList(),
                    currentMatchIndex = 0
                )
            }
            return
        }

        val results = mutableListOf<DocumentSearchResult>()
        var searchIndex = 0
        var matchCount = 0

        while (searchIndex < text.length) {
            val foundAt = text.indexOf(trimmed, startIndex = searchIndex, ignoreCase = true)
            if (foundAt == -1) break

            val previewStart = (foundAt - 30).coerceAtLeast(0)
            val previewEnd = (foundAt + trimmed.length + 30).coerceAtMost(text.length)
            val snippet = text.substring(previewStart, previewEnd)
                .replace("\n", " ")

            results.add(
                DocumentSearchResult(
                    index = matchCount,
                    startOffset = foundAt,
                    endOffset = foundAt + trimmed.length,
                    previewText = snippet
                )
            )

            matchCount++
            searchIndex = foundAt + trimmed.length
        }

        _uiState.update {
            it.copy(
                searchQuery = query,
                searchResults = results,
                currentMatchIndex = if (results.isNotEmpty()) 0 else 0
            )
        }
    }

    fun nextSearchMatch() {
        val state = _uiState.value
        if (state.searchResults.isEmpty()) return
        val nextIdx = (state.currentMatchIndex + 1) % state.searchResults.size
        _uiState.update { it.copy(currentMatchIndex = nextIdx) }
    }

    fun previousSearchMatch() {
        val state = _uiState.value
        if (state.searchResults.isEmpty()) return
        val prevIdx = if (state.currentMatchIndex > 0) state.currentMatchIndex - 1 else state.searchResults.lastIndex
        _uiState.update { it.copy(currentMatchIndex = prevIdx) }
    }

    fun toggleSearch(active: Boolean) {
        _uiState.update {
            it.copy(
                isSearchActive = active,
                searchQuery = if (!active) "" else it.searchQuery,
                searchResults = if (!active) emptyList() else it.searchResults,
                currentMatchIndex = 0
            )
        }
    }

    fun setReadingTheme(theme: DocumentReadingTheme) {
        _uiState.update { it.copy(readingTheme = theme) }
    }

    fun setFontSize(size: DocumentFontSize) {
        _uiState.update { it.copy(fontSize = size) }
    }

    fun setViewMode(mode: DocumentViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun setTtsSpeaking(speaking: Boolean) {
        _uiState.update { it.copy(isTtsSpeaking = speaking) }
    }

    fun setTtsSpeechRate(rate: Float) {
        _uiState.update { it.copy(ttsSpeechRate = rate) }
    }

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    fun saveAsNote(onSuccess: (noteId: String) -> Unit) {
        val state = _uiState.value
        if (state.fullTextContent.isBlank()) {
            _uiState.update { it.copy(infoMessage = "Document has no text to convert.") }
            return
        }

        viewModelScope.launch {
            try {
                // Find first subject/chapter or create general note
                val subjects = subjectRepository.getAllSubjectsOnce()
                val targetSubject = subjects.firstOrNull()
                val targetChapter = targetSubject?.let {
                    chapterRepository.getChaptersForSubjectOnce(it.id).firstOrNull()
                }

                val newNote = Note(
                    title = state.documentTitle.ifBlank { "Imported Document" },
                    content = state.fullTextContent,
                    subjectId = targetSubject?.id,
                    chapterId = targetChapter?.id,
                    isPinned = false
                )

                val savedNote = noteRepository.saveNote(newNote)
                _uiState.update {
                    it.copy(
                        noteId = savedNote.id,
                        infoMessage = "Saved to StudyOS Notes!"
                    )
                }
                onSuccess(savedNote.id)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(infoMessage = "Failed to convert note: ${e.message}")
                }
            }
        }
    }

    fun generateFlashcards(onSuccess: (chapterId: String) -> Unit) {
        val state = _uiState.value
        val text = state.fullTextContent.trim()
        if (text.isBlank()) {
            _uiState.update { it.copy(infoMessage = "Document text is empty.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAiWorking = true) }
            try {
                val subjects = subjectRepository.getAllSubjectsOnce()
                val targetSubject = subjects.firstOrNull()
                if (targetSubject == null) {
                    _uiState.update {
                        it.copy(
                            isAiWorking = false,
                            infoMessage = "Create at least one subject to store flashcards."
                        )
                    }
                    return@launch
                }
                val targetChapter = chapterRepository.getChaptersForSubjectOnce(targetSubject.id).firstOrNull()
                val config = getAiConfigUseCase().firstOrNull() ?: AiConfig()

                val cards = aiPracticeToolsUseCase.generateFlashcardsFromNote(
                    noteTitle = state.documentTitle,
                    noteContent = text.take(6000), // optimal prompt context window
                    subjectId = targetSubject.id,
                    chapterId = targetChapter?.id,
                    config = config
                )

                if (cards.isNotEmpty()) {
                    saveFlashcardUseCase.saveBatch(cards)
                    _uiState.update {
                        it.copy(
                            isAiWorking = false,
                            infoMessage = "Generated ${cards.size} flashcards!"
                        )
                    }
                    val chapId = targetChapter?.id ?: ""
                    onSuccess(chapId)
                } else {
                    _uiState.update {
                        it.copy(
                            isAiWorking = false,
                            infoMessage = "Could not generate flashcards from this text."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAiWorking = false,
                        infoMessage = "Error generating flashcards: ${e.message}"
                    )
                }
            }
        }
    }

    fun generateQuiz(onSuccess: (quizId: String) -> Unit) {
        val state = _uiState.value
        val text = state.fullTextContent.trim()
        if (text.isBlank()) {
            _uiState.update { it.copy(infoMessage = "Document text is empty.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAiWorking = true) }
            try {
                val subjects = subjectRepository.getAllSubjectsOnce()
                val targetSubject = subjects.firstOrNull()
                if (targetSubject == null) {
                    _uiState.update {
                        it.copy(
                            isAiWorking = false,
                            infoMessage = "Create at least one subject to store quizzes."
                        )
                    }
                    return@launch
                }
                val targetChapter = chapterRepository.getChaptersForSubjectOnce(targetSubject.id).firstOrNull()
                val config = getAiConfigUseCase().firstOrNull() ?: AiConfig()

                val (quiz, questions) = aiPracticeToolsUseCase.generateQuizFromContext(
                    title = "${state.documentTitle} Quiz",
                    topic = state.documentTitle,
                    content = text.take(6000),
                    questionCount = 5,
                    difficulty = QuizDifficulty.MEDIUM,
                    subjectId = targetSubject.id,
                    chapterId = targetChapter?.id,
                    config = config
                )

                saveQuizUseCase(quiz, questions)
                _uiState.update {
                    it.copy(
                        isAiWorking = false,
                        infoMessage = "Quiz generated successfully!"
                    )
                }
                onSuccess(quiz.id)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAiWorking = false,
                        infoMessage = "Error generating quiz: ${e.message}"
                    )
                }
            }
        }
    }
}
