package com.studyos.app.features.subjects.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.core.database.dao.ResourceDao
import com.studyos.app.core.database.entity.ResourceEntity
import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.ChapterStatus
import com.studyos.app.domain.model.Note
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.model.calculateSubjectProgress
import com.studyos.app.domain.repository.NoteRepository
import com.studyos.app.domain.usecase.AddChapterUseCase
import com.studyos.app.domain.usecase.AddSubjectResult
import com.studyos.app.domain.usecase.ChapterActionResult
import com.studyos.app.domain.usecase.DeleteChapterUseCase
import com.studyos.app.domain.usecase.DeleteSubjectUseCase
import com.studyos.app.domain.usecase.GetChapterUseCase
import com.studyos.app.domain.usecase.GetChaptersForSubjectUseCase
import com.studyos.app.domain.usecase.GetSubjectByIdUseCase
import com.studyos.app.domain.usecase.MoveChapterUseCase
import com.studyos.app.domain.usecase.RenameSubjectUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SubjectDetailUiState(
    val subject: Subject? = null,
    val chapters: List<Chapter> = emptyList(),
    val notes: List<Note> = emptyList(),
    val resources: List<ResourceEntity> = emptyList(),
    val progress: Int = 0,
    val completedCount: Int = 0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val isDeleted: Boolean = false
)

class SubjectDetailViewModel(
    private val subjectId: String,
    private val getSubjectByIdUseCase: GetSubjectByIdUseCase,
    private val getChaptersForSubjectUseCase: GetChaptersForSubjectUseCase,
    private val addChapterUseCase: AddChapterUseCase,
    private val deleteChapterUseCase: DeleteChapterUseCase,
    private val moveChapterUseCase: MoveChapterUseCase,
    private val renameSubjectUseCase: RenameSubjectUseCase,
    private val deleteSubjectUseCase: DeleteSubjectUseCase,
    private val noteRepository: NoteRepository? = null,
    private val resourceDao: ResourceDao? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubjectDetailUiState())
    val uiState: StateFlow<SubjectDetailUiState> = _uiState.asStateFlow()

    init {
        observeSubjectAndChapters()
    }

    private fun observeSubjectAndChapters() {
        viewModelScope.launch {
            val subjectFlow = getSubjectByIdUseCase(subjectId)
            val chaptersFlow = getChaptersForSubjectUseCase(subjectId)
            val notesFlow = noteRepository?.observeNotesForSubject(subjectId) ?: kotlinx.coroutines.flow.flowOf(emptyList())
            val resourcesFlow = resourceDao?.getResourcesForSubject(subjectId) ?: kotlinx.coroutines.flow.flowOf(emptyList())

            combine(
                subjectFlow,
                chaptersFlow,
                notesFlow,
                resourcesFlow
            ) { subject, chapters, notes, resources ->
                val progress = calculateSubjectProgress(chapters)
                val completed = chapters.count { it.status == ChapterStatus.COMPLETED || it.progress == 100 }
                SubjectDetailUiState(
                    subject = subject,
                    chapters = chapters,
                    notes = notes,
                    resources = resources,
                    progress = progress,
                    completedCount = completed,
                    isLoading = false,
                    isDeleted = subject == null && !_uiState.value.isLoading
                )
            }.collect { newState ->
                if (newState.isDeleted && !_uiState.value.isDeleted) {
                    _uiState.update { it.copy(isDeleted = true) }
                    return@collect
                }
                _uiState.update { current ->
                    current.copy(
                        subject = newState.subject,
                        chapters = newState.chapters,
                        notes = newState.notes,
                        resources = newState.resources,
                        progress = newState.progress,
                        completedCount = newState.completedCount,
                        isLoading = false
                    )
                }
            }
        }
    }

    suspend fun addChapter(name: String, description: String?): Boolean {
        return when (val result = addChapterUseCase(subjectId, name, description)) {
            is ChapterActionResult.Success -> {
                _uiState.update { it.copy(errorMessage = null, actionMessage = "Chapter added") }
                true
            }
            is ChapterActionResult.EmptyName -> {
                _uiState.update { it.copy(errorMessage = "Chapter name cannot be empty.") }
                false
            }
            is ChapterActionResult.DuplicateName -> {
                _uiState.update { it.copy(errorMessage = "A chapter with this name already exists in this subject.") }
                false
            }
            is ChapterActionResult.Error -> {
                _uiState.update { it.copy(errorMessage = result.message) }
                false
            }
        }
    }

    suspend fun renameSubject(newName: String): Boolean {
        return when (val result = renameSubjectUseCase(subjectId, newName)) {
            is AddSubjectResult.Success -> {
                _uiState.update { it.copy(errorMessage = null, actionMessage = "Subject renamed") }
                true
            }
            is AddSubjectResult.EmptyName -> {
                _uiState.update { it.copy(errorMessage = "Subject name cannot be empty.") }
                false
            }
            is AddSubjectResult.DuplicateName -> {
                _uiState.update { it.copy(errorMessage = "That subject already exists.") }
                false
            }
            is AddSubjectResult.Error -> {
                _uiState.update { it.copy(errorMessage = result.message) }
                false
            }
        }
    }

    fun deleteSubject() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleted = true) }
            deleteSubjectUseCase(subjectId)
        }
    }

    fun deleteChapter(chapterId: String) {
        viewModelScope.launch {
            try {
                deleteChapterUseCase(chapterId)
                _uiState.update { it.copy(actionMessage = "Chapter deleted") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Couldn't delete chapter. Try again.") }
            }
        }
    }

    fun moveChapter(chapterId: String, moveUp: Boolean) {
        viewModelScope.launch {
            moveChapterUseCase(subjectId, chapterId, moveUp)
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, actionMessage = null) }
    }

    fun addOrUploadNote(title: String, content: String, chapterId: String? = null) {
        viewModelScope.launch {
            if (title.isBlank() && content.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Note title and content cannot both be empty.") }
                return@launch
            }
            if (noteRepository == null) {
                _uiState.update { it.copy(errorMessage = "Note repository not initialized.") }
                return@launch
            }
            try {
                val newNote = Note(
                    title = title.trim().ifBlank { "Subject Note" },
                    content = content.trim(),
                    subjectId = subjectId,
                    chapterId = chapterId
                )
                noteRepository.saveNote(newNote)
                _uiState.update { it.copy(actionMessage = "Note uploaded & available for AI context") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to save note: ${e.message}") }
            }
        }
    }

    fun addResource(title: String, type: String, uriOrPath: String) {
        viewModelScope.launch {
            if (resourceDao == null) return@launch
            try {
                val resource = ResourceEntity(
                    subjectId = subjectId,
                    title = title.trim().ifBlank { "Subject Document" },
                    type = type,
                    uriOrPath = uriOrPath
                )
                resourceDao.insert(resource)
            } catch (_: Exception) {
            }
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            if (noteRepository == null) return@launch
            try {
                noteRepository.deleteNote(noteId)
                _uiState.update { it.copy(actionMessage = "Note removed") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to delete note.") }
            }
        }
    }

    fun deleteResource(resource: ResourceEntity) {
        viewModelScope.launch {
            if (resourceDao == null) return@launch
            try {
                resourceDao.delete(resource)
                _uiState.update { it.copy(actionMessage = "Resource removed") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to remove resource.") }
            }
        }
    }
}
