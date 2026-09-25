package com.studyos.app.features.today.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.StudySession
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.repository.ChapterRepository
import com.studyos.app.domain.repository.StudySessionRepository
import com.studyos.app.domain.repository.SubjectRepository
import com.studyos.app.domain.usecase.DeleteSessionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudySessionDetailUiState(
    val session: StudySession? = null,
    val subject: Subject? = null,
    val chapter: Chapter? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false
)

class StudySessionViewModel(
    private val sessionId: String,
    private val studySessionRepository: StudySessionRepository,
    private val subjectRepository: SubjectRepository,
    private val chapterRepository: ChapterRepository,
    private val deleteSessionUseCase: DeleteSessionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudySessionDetailUiState())
    val uiState: StateFlow<StudySessionDetailUiState> = _uiState.asStateFlow()

    init {
        loadSession()
    }

    private fun loadSession() {
        viewModelScope.launch {
            studySessionRepository.getSessionById(sessionId).collect { session ->
                if (session == null && !_uiState.value.isLoading && !_uiState.value.isDeleted) {
                    _uiState.update { it.copy(isDeleted = true) }
                    return@collect
                }
                if (session != null) {
                    val subject = session.subjectId?.let { subjectRepository.getSubjectByIdOnce(it) }
                    val chapter = session.chapterId?.let { chapterRepository.getChapterById(it) }
                    _uiState.update {
                        it.copy(
                            session = session,
                            subject = subject,
                            chapter = chapter,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun deleteSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleted = true) }
            deleteSessionUseCase(sessionId)
        }
    }
}
