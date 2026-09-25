package com.studyos.app.features.practice.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.domain.model.AiConfig
import com.studyos.app.domain.model.Flashcard
import com.studyos.app.domain.model.Note
import com.studyos.app.domain.model.Quiz
import com.studyos.app.domain.model.QuizDifficulty
import com.studyos.app.domain.model.QuizQuestion
import com.studyos.app.domain.usecase.AiPracticeToolsUseCase
import com.studyos.app.domain.usecase.DeleteNoteUseCase
import com.studyos.app.domain.usecase.GetAiConfigUseCase
import com.studyos.app.domain.usecase.GetNoteUseCase
import com.studyos.app.domain.usecase.SaveFlashcardUseCase
import com.studyos.app.domain.usecase.SaveNoteUseCase
import com.studyos.app.domain.repository.SubjectRepository
import com.studyos.app.domain.usecase.SaveQuizUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NoteEditorUiState(
    val noteId: String? = null,
    val title: String = "",
    val subjectId: String? = null,
    val chapterId: String? = null,
    val content: String = "",
    val isPinned: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isAiWorking: Boolean = false,
    val aiResultTitle: String? = null,
    val aiResultContent: String? = null,
    val generatedFlashcards: List<Flashcard> = emptyList(),
    val generatedQuizId: String? = null,
    val infoMessage: String? = null
)

class NoteEditorViewModel(
    private val noteId: String?,
    private val initialSubjectId: String?,
    private val initialChapterId: String?,
    private val getNoteUseCase: GetNoteUseCase,
    private val saveNoteUseCase: SaveNoteUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val saveFlashcardUseCase: SaveFlashcardUseCase,
    private val saveQuizUseCase: SaveQuizUseCase,
    private val aiPracticeToolsUseCase: AiPracticeToolsUseCase,
    private val getAiConfigUseCase: GetAiConfigUseCase,
    private val subjectRepository: SubjectRepository? = null
) : ViewModel() {

    private val undoStack = ArrayDeque<String>()
    private val redoStack = ArrayDeque<String>()

    private val _uiState = MutableStateFlow(
        NoteEditorUiState(
            noteId = noteId,
            subjectId = initialSubjectId,
            chapterId = initialChapterId
        )
    )
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    init {
        if (!noteId.isNullOrBlank()) {
            loadNote(noteId)
        }
    }

    private fun loadNote(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val note = getNoteUseCase.getOnce(id)
            if (note != null) {
                undoStack.clear()
                redoStack.clear()
                _uiState.update {
                    it.copy(
                        noteId = note.id,
                        title = note.title,
                        content = note.content,
                        subjectId = note.subjectId ?: it.subjectId,
                        chapterId = note.chapterId ?: it.chapterId,
                        isPinned = note.isPinned,
                        canUndo = false,
                        canRedo = false,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
    }

    fun onContentChange(newContent: String) {
        val current = _uiState.value.content
        if (newContent != current) {
            if (undoStack.size > 50) {
                undoStack.removeFirst()
            }
            undoStack.addLast(current)
            redoStack.clear()
            _uiState.update {
                it.copy(
                    content = newContent,
                    canUndo = true,
                    canRedo = false
                )
            }
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val current = _uiState.value.content
        redoStack.addLast(current)
        val previous = undoStack.removeLast()
        _uiState.update {
            it.copy(
                content = previous,
                canUndo = undoStack.isNotEmpty(),
                canRedo = true
            )
        }
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val current = _uiState.value.content
        undoStack.addLast(current)
        val next = redoStack.removeLast()
        _uiState.update {
            it.copy(
                content = next,
                canUndo = true,
                canRedo = redoStack.isNotEmpty()
            )
        }
    }

    fun togglePin() {
        _uiState.update { it.copy(isPinned = !it.isPinned) }
    }

    fun insertMarkdown(prefix: String, suffix: String = "") {
        val current = _uiState.value.content
        undoStack.addLast(current)
        redoStack.clear()
        val newContent = if (current.isEmpty()) {
            "$prefix$suffix"
        } else if (current.endsWith("\n")) {
            "$current$prefix$suffix"
        } else {
            "$current\n$prefix$suffix"
        }
        _uiState.update {
            it.copy(
                content = newContent,
                canUndo = true,
                canRedo = false
            )
        }
    }

    fun saveNote() {
        val title = _uiState.value.title.trim()
        val content = _uiState.value.content.trim()

        if (title.isBlank()) {
            _uiState.update { it.copy(infoMessage = "Please enter a note title.") }
            return
        }

        viewModelScope.launch {
            saveNoteUseCase(
                id = _uiState.value.noteId,
                title = title,
                content = content,
                subjectId = _uiState.value.subjectId,
                chapterId = _uiState.value.chapterId,
                isPinned = _uiState.value.isPinned
            )
            _uiState.update { it.copy(isSaved = true, infoMessage = "Note saved.") }
        }
    }

    fun deleteNote() {
        val id = _uiState.value.noteId ?: return
        viewModelScope.launch {
            deleteNoteUseCase(id)
            _uiState.update { it.copy(isDeleted = true) }
        }
    }

    // AI Tools
    fun summarizeNote() {
        val content = _uiState.value.content.trim()
        val title = _uiState.value.title.trim().ifEmpty { "Study Note" }
        if (content.isBlank()) {
            _uiState.update { it.copy(infoMessage = "Cannot summarize an empty note.") }
            return
        }

        _uiState.update {
            it.copy(
                isAiWorking = true,
                aiResultTitle = "Summary of \"$title\"",
                aiResultContent = ""
            )
        }

        viewModelScope.launch {
            try {
                val config = getAiConfigUseCase().firstOrNull() ?: AiConfig()
                aiPracticeToolsUseCase.summarizeNote(title, content, config).collect { chunk ->
                    _uiState.update { it.copy(aiResultContent = chunk) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(infoMessage = e.message ?: "Failed to summarize.") }
            } finally {
                _uiState.update { it.copy(isAiWorking = false) }
            }
        }
    }

    fun explainNote() {
        val content = _uiState.value.content.trim()
        val title = _uiState.value.title.trim().ifEmpty { "Study Note" }
        if (content.isBlank()) {
            _uiState.update { it.copy(infoMessage = "Cannot explain an empty note.") }
            return
        }

        _uiState.update {
            it.copy(
                isAiWorking = true,
                aiResultTitle = "Explanation of \"$title\"",
                aiResultContent = ""
            )
        }

        viewModelScope.launch {
            try {
                val config = getAiConfigUseCase().firstOrNull() ?: AiConfig()
                aiPracticeToolsUseCase.explainNote(title, content, config).collect { chunk ->
                    _uiState.update { it.copy(aiResultContent = chunk) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(infoMessage = e.message ?: "Failed to explain.") }
            } finally {
                _uiState.update { it.copy(isAiWorking = false) }
            }
        }
    }

    private suspend fun resolveSubjectId(): String? {
        val current = _uiState.value.subjectId
        if (!current.isNullOrBlank()) return current
        return subjectRepository?.getAllSubjectsOnce()?.firstOrNull()?.id
    }

    fun convertToFlashcards() {
        val content = _uiState.value.content.trim()
        val title = _uiState.value.title.trim().ifEmpty { "Study Note" }
        val chapId = _uiState.value.chapterId

        if (content.isBlank()) {
            _uiState.update { it.copy(infoMessage = "Note is empty.") }
            return
        }

        viewModelScope.launch {
            val subId = resolveSubjectId()
            if (subId == null) {
                _uiState.update { it.copy(infoMessage = "Please create at least one subject to store flashcards.") }
                return@launch
            }

            _uiState.update { it.copy(isAiWorking = true) }
            try {
                val config = getAiConfigUseCase().firstOrNull() ?: AiConfig()
                val cards = aiPracticeToolsUseCase.generateFlashcardsFromNote(
                    noteTitle = title,
                    noteContent = content,
                    subjectId = subId,
                    chapterId = chapId,
                    config = config
                )
                if (cards.isNotEmpty()) {
                    saveFlashcardUseCase.saveBatch(cards)
                    _uiState.update {
                        it.copy(
                            isAiWorking = false,
                            generatedFlashcards = cards,
                            infoMessage = "Created ${cards.size} flashcards from note!"
                        )
                    }
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
                        infoMessage = e.message ?: "Failed to convert to flashcards."
                    )
                }
            }
        }
    }

    fun generateQuizFromNote() {
        val content = _uiState.value.content.trim()
        val title = _uiState.value.title.trim().ifEmpty { "Study Note" }
        val chapId = _uiState.value.chapterId

        if (content.isBlank()) {
            _uiState.update { it.copy(infoMessage = "Note is empty.") }
            return
        }

        viewModelScope.launch {
            val subId = resolveSubjectId()
            if (subId == null) {
                _uiState.update { it.copy(infoMessage = "Please create at least one subject to store quizzes.") }
                return@launch
            }

            _uiState.update { it.copy(isAiWorking = true) }
            try {
                val config = getAiConfigUseCase().firstOrNull() ?: AiConfig()
                val (quiz, questions) = aiPracticeToolsUseCase.generateQuizFromContext(
                    title = "$title Quiz",
                    topic = title,
                    content = content,
                    questionCount = 5,
                    difficulty = QuizDifficulty.MEDIUM,
                    subjectId = subId,
                    chapterId = chapId,
                    config = config
                )
                saveQuizUseCase(quiz, questions)
                _uiState.update {
                    it.copy(
                        isAiWorking = false,
                        generatedQuizId = quiz.id,
                        infoMessage = "Quiz generated successfully!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAiWorking = false,
                        infoMessage = e.message ?: "Failed to generate quiz."
                    )
                }
            }
        }
    }

    fun dismissAiResult() {
        _uiState.update {
            it.copy(
                aiResultTitle = null,
                aiResultContent = null,
                generatedFlashcards = emptyList(),
                generatedQuizId = null
            )
        }
    }

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }
}
