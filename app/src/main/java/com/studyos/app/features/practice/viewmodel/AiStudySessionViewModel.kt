package com.studyos.app.features.practice.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.RecallItem
import com.studyos.app.domain.model.StudySession
import com.studyos.app.domain.model.StudySessionStatus
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.repository.ChapterRepository
import com.studyos.app.domain.repository.RecallRepository
import com.studyos.app.domain.repository.StudySessionRepository
import com.studyos.app.domain.repository.SubjectRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class AiStudyStep(val title: String) {
    LEARN("1. Concept Review"),
    RECALL("2. Active Recall"),
    PRACTICE("3. Rapid Practice"),
    EVALUATE("4. Reflection"),
    COMPLETE("Session Complete")
}

data class AiStudySessionUiState(
    val step: AiStudyStep = AiStudyStep.LEARN,
    val subject: Subject? = null,
    val chapter: Chapter? = null,
    val elapsedSeconds: Int = 0,
    val totalPlannedSeconds: Int = 25 * 60,
    val isPaused: Boolean = false,
    val recallItems: List<RecallItem> = emptyList(),
    val currentRecallIndex: Int = 0,
    val isRecallAnswerRevealed: Boolean = false,
    val recallCorrectCount: Int = 0,
    val recallTotalAttempted: Int = 0,
    val practiceQuestions: List<String> = emptyList(),
    val practiceAnswers: MutableMap<Int, String> = mutableMapOf(),
    val practiceScore: Int = 0,
    val practiceTotal: Int = 5,
    val nextReviewDate: String = "Tomorrow",
    val isLogged: Boolean = false
)

class AiStudySessionViewModel(
    private val initialSubjectId: String?,
    private val initialChapterId: String?,
    private val subjectRepository: SubjectRepository,
    private val chapterRepository: ChapterRepository,
    private val recallRepository: RecallRepository,
    private val studySessionRepository: StudySessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiStudySessionUiState())
    val uiState: StateFlow<AiStudySessionUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadData()
        startTimer()
    }

    private fun loadData() {
        viewModelScope.launch {
            val subjects = subjectRepository.getAllSubjectsOnce()
            val subject = subjects.firstOrNull { it.id == initialSubjectId } ?: subjects.firstOrNull()
            val chapters = if (subject != null) {
                chapterRepository.getChaptersForSubjectOnce(subject.id)
            } else emptyList()
            val chapter = chapters.firstOrNull { it.id == initialChapterId } ?: chapters.firstOrNull()

            val recallItems = if (chapter != null) {
                recallRepository.getRecallItemsForChapter(chapter.id)
            } else emptyList()

            _uiState.update {
                it.copy(
                    subject = subject,
                    chapter = chapter,
                    recallItems = recallItems
                )
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                if (!_uiState.value.isPaused && _uiState.value.step != AiStudyStep.COMPLETE) {
                    _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
                }
            }
        }
    }

    fun togglePause() {
        _uiState.update { it.copy(isPaused = !it.isPaused) }
    }

    fun nextStep() {
        val next = when (_uiState.value.step) {
            AiStudyStep.LEARN -> AiStudyStep.RECALL
            AiStudyStep.RECALL -> AiStudyStep.PRACTICE
            AiStudyStep.PRACTICE -> AiStudyStep.EVALUATE
            AiStudyStep.EVALUATE -> {
                finalizeSession()
                AiStudyStep.COMPLETE
            }
            AiStudyStep.COMPLETE -> AiStudyStep.COMPLETE
        }
        _uiState.update { it.copy(step = next) }
    }

    fun revealRecallAnswer() {
        _uiState.update { it.copy(isRecallAnswerRevealed = true) }
    }

    fun recordRecallAttempt(isCorrect: Boolean) {
        val current = _uiState.value
        val items = current.recallItems
        val item = items.getOrNull(current.currentRecallIndex)

        if (item != null) {
            viewModelScope.launch {
                recallRepository.recordAttemptAndEvaluate(
                    recallItemId = item.id,
                    userAnswer = if (isCorrect) "Correct" else "Incorrect",
                    confidenceRating = if (isCorrect) 5 else 1
                )
            }
        }

        val nextIndex = current.currentRecallIndex + 1
        _uiState.update {
            it.copy(
                recallCorrectCount = it.recallCorrectCount + if (isCorrect) 1 else 0,
                recallTotalAttempted = it.recallTotalAttempted + 1,
                currentRecallIndex = nextIndex,
                isRecallAnswerRevealed = false
            )
        }

        if (nextIndex >= items.size || nextIndex >= 10) {
            nextStep()
        }
    }

    fun recordPracticeResult(score: Int, total: Int) {
        _uiState.update {
            it.copy(practiceScore = score, practiceTotal = total)
        }
        nextStep()
    }

    private fun finalizeSession() {
        val state = _uiState.value
        val minutes = (state.elapsedSeconds / 60).coerceAtLeast(1)
        val now = System.currentTimeMillis()
        val start = now - (state.elapsedSeconds * 1000L)

        viewModelScope.launch {
            try {
                val session = StudySession(
                    id = UUID.randomUUID().toString(),
                    subjectId = state.subject?.id,
                    chapterId = state.chapter?.id,
                    title = "AI Study Session: ${state.chapter?.name ?: state.subject?.name ?: "Deep Work"}",
                    scheduledStart = start,
                    scheduledEnd = now,
                    plannedMinutes = 25,
                    actualMinutes = minutes,
                    status = StudySessionStatus.COMPLETED,
                    createdAt = start,
                    updatedAt = now
                )
                studySessionRepository.createSession(session)
                _uiState.update { it.copy(isLogged = true) }
            } catch (e: Exception) {
                // Keep UI completion state
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
