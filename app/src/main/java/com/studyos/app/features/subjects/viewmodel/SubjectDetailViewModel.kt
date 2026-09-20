package com.studyos.app.features.subjects.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.ChapterStatus
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.model.calculateSubjectProgress
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
    private val deleteSubjectUseCase: DeleteSubjectUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubjectDetailUiState())
    val uiState: StateFlow<SubjectDetailUiState> = _uiState.asStateFlow()

    init {
        observeSubjectAndChapters()
    }

    private fun observeSubjectAndChapters() {
        viewModelScope.launch {
            combine(
                getSubjectByIdUseCase(subjectId),
                getChaptersForSubjectUseCase(subjectId)
            ) { subject, chapters ->
                Pair(subject, chapters)
            }.collect { (subject, chapters) ->
                if (subject == null && !_uiState.value.isDeleted && !_uiState.value.isLoading) {
                    _uiState.update { it.copy(isDeleted = true) }
                    return@collect
                }
                val progress = calculateSubjectProgress(chapters)
                val completed = chapters.count { it.status == ChapterStatus.COMPLETED || it.progress == 100 }
                _uiState.update {
                    it.copy(
                        subject = subject,
                        chapters = chapters,
                        progress = progress,
                        completedCount = completed,
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
}
