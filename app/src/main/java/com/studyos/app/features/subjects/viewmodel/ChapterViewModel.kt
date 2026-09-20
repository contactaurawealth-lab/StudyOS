package com.studyos.app.features.subjects.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.ChapterStatus
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.usecase.ChapterActionResult
import com.studyos.app.domain.usecase.DeleteChapterUseCase
import com.studyos.app.domain.usecase.GetChapterUseCase
import com.studyos.app.domain.usecase.GetSubjectByIdUseCase
import com.studyos.app.domain.usecase.RecordChapterOpenedUseCase
import com.studyos.app.domain.usecase.UpdateChapterProgressUseCase
import com.studyos.app.domain.usecase.UpdateChapterStatusUseCase
import com.studyos.app.domain.usecase.UpdateChapterUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChapterUiState(
    val chapter: Chapter? = null,
    val subject: Subject? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val isDeleted: Boolean = false
)

class ChapterViewModel(
    private val chapterId: String,
    private val getChapterUseCase: GetChapterUseCase,
    private val getSubjectByIdUseCase: GetSubjectByIdUseCase,
    private val updateChapterUseCase: UpdateChapterUseCase,
    private val updateChapterProgressUseCase: UpdateChapterProgressUseCase,
    private val updateChapterStatusUseCase: UpdateChapterStatusUseCase,
    private val deleteChapterUseCase: DeleteChapterUseCase,
    private val recordChapterOpenedUseCase: RecordChapterOpenedUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChapterUiState())
    val uiState: StateFlow<ChapterUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            recordChapterOpenedUseCase(chapterId)
        }
        observeChapter()
    }

    private fun observeChapter() {
        viewModelScope.launch {
            getChapterUseCase(chapterId).collect { chapter ->
                if (chapter == null && !_uiState.value.isDeleted && !_uiState.value.isLoading) {
                    _uiState.update { it.copy(isDeleted = true) }
                    return@collect
                }
                if (chapter != null) {
                    val subject = getSubjectByIdUseCase.getOnce(chapter.subjectId)
                    _uiState.update {
                        it.copy(
                            chapter = chapter,
                            subject = subject,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun updateProgress(progress: Int) {
        viewModelScope.launch {
            updateChapterProgressUseCase(chapterId, progress)
        }
    }

    fun updateStatus(status: ChapterStatus) {
        viewModelScope.launch {
            updateChapterStatusUseCase(chapterId, status)
        }
    }

    fun editChapter(name: String, description: String?, progress: Int): Boolean {
        var success = false
        viewModelScope.launch {
            when (val result = updateChapterUseCase(chapterId, name, description, progress)) {
                is ChapterActionResult.Success -> {
                    success = true
                    _uiState.update { it.copy(errorMessage = null, actionMessage = "Chapter updated") }
                }
                is ChapterActionResult.EmptyName -> {
                    _uiState.update { it.copy(errorMessage = "Chapter name cannot be empty.") }
                }
                is ChapterActionResult.DuplicateName -> {
                    _uiState.update { it.copy(errorMessage = "A chapter with this name already exists in this subject.") }
                }
                is ChapterActionResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
            }
        }
        return success
    }

    fun deleteChapter() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleted = true) }
            deleteChapterUseCase(chapterId)
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, actionMessage = null) }
    }
}
