package com.studyos.app.features.practice.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.domain.model.ActiveQuizState
import com.studyos.app.domain.model.Quiz
import com.studyos.app.domain.model.QuizAttempt
import com.studyos.app.domain.model.QuizQuestion
import com.studyos.app.domain.usecase.GetActiveQuizStateUseCase
import com.studyos.app.domain.usecase.GetQuizWithQuestionsUseCase
import com.studyos.app.domain.usecase.SaveActiveQuizStateUseCase
import com.studyos.app.domain.usecase.SubmitQuizAttemptUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class QuizRunnerUiState(
    val quizId: String = "",
    val quiz: Quiz? = null,
    val questions: List<QuizQuestion> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val selectedAnswers: Map<String, String> = emptyMap(),
    val flaggedQuestionIds: Set<String> = emptySet(),
    val customTimeLimitMinutes: Int? = null,
    val showPaletteSheet: Boolean = false,
    val showReviewDialog: Boolean = false,
    val elapsedSeconds: Int = 0,
    val startedAt: Long = System.currentTimeMillis(),
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val isCompleted: Boolean = false,
    val attemptResult: QuizAttempt? = null,
    val infoMessage: String? = null
) {
    val currentQuestion: QuizQuestion? get() = questions.getOrNull(currentQuestionIndex)
    val totalQuestions: Int get() = questions.size
    val progress: Float get() = if (totalQuestions > 0) (currentQuestionIndex + 1).toFloat() / totalQuestions else 0f
    val answeredCount: Int get() = selectedAnswers.size
    val unansweredCount: Int get() = (totalQuestions - answeredCount).coerceAtLeast(0)
    val flaggedCount: Int get() = flaggedQuestionIds.size
    val allQuestionsAnswered: Boolean get() = questions.isNotEmpty() && selectedAnswers.size == questions.size

    val effectiveTimeLimitMinutes: Int? get() = customTimeLimitMinutes ?: quiz?.timeLimitMinutes
    val isTimed: Boolean get() = (effectiveTimeLimitMinutes ?: 0) > 0
    val totalTimeSeconds: Int get() = (effectiveTimeLimitMinutes ?: 0) * 60
    val remainingSeconds: Int get() = if (isTimed) maxOf(0, totalTimeSeconds - elapsedSeconds) else 0
    val isTimeExpiringSoon: Boolean get() = isTimed && remainingSeconds in 1..300
    val isTimeCritical: Boolean get() = isTimed && remainingSeconds in 1..60
}

class QuizRunnerViewModel(
    private val quizId: String,
    private val getQuizWithQuestionsUseCase: GetQuizWithQuestionsUseCase,
    private val submitQuizAttemptUseCase: SubmitQuizAttemptUseCase,
    private val saveActiveQuizStateUseCase: SaveActiveQuizStateUseCase,
    private val getActiveQuizStateUseCase: GetActiveQuizStateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizRunnerUiState(quizId = quizId))
    val uiState: StateFlow<QuizRunnerUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadQuiz()
    }

    fun loadQuiz() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val (quiz, questions) = getQuizWithQuestionsUseCase(quizId)
            val sortedQuestions = questions.sortedBy { it.orderIndex }

            // Check if active state exists
            val activeState = getActiveQuizStateUseCase(quizId)

            val initialIndex = activeState?.currentQuestionIndex?.coerceIn(0, (sortedQuestions.size - 1).coerceAtLeast(0)) ?: 0
            val initialAnswers = activeState?.answers ?: emptyMap()
            val initialElapsed = activeState?.elapsedSeconds ?: 0

            _uiState.update {
                it.copy(
                    quiz = quiz,
                    questions = sortedQuestions,
                    currentQuestionIndex = initialIndex,
                    selectedAnswers = initialAnswers,
                    elapsedSeconds = initialElapsed,
                    isLoading = false,
                    isCompleted = false,
                    attemptResult = null
                )
            }

            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && !_uiState.value.isCompleted) {
                delay(1000L)
                val currentElapsed = _uiState.value.elapsedSeconds + 1
                val isTimed = _uiState.value.isTimed
                val totalLimit = _uiState.value.totalTimeSeconds

                if (isTimed && currentElapsed >= totalLimit) {
                    _uiState.update {
                        it.copy(
                            elapsedSeconds = totalLimit,
                            infoMessage = "Time is up! Your mock test has been submitted automatically."
                        )
                    }
                    submitQuiz()
                    break
                }

                _uiState.update { it.copy(elapsedSeconds = currentElapsed) }

                // Periodic autosave every 10 seconds
                if (currentElapsed % 10 == 0) {
                    persistActiveState()
                }
            }
        }
    }

    fun selectAnswer(questionId: String, answer: String) {
        val updated = _uiState.value.selectedAnswers.toMutableMap()
        updated[questionId] = answer
        _uiState.update { it.copy(selectedAnswers = updated) }
        persistActiveState()
    }

    fun toggleFlagCurrentQuestion() {
        val current = _uiState.value.currentQuestion ?: return
        toggleFlagQuestion(current.id)
    }

    fun toggleFlagQuestion(questionId: String) {
        val currentFlags = _uiState.value.flaggedQuestionIds.toMutableSet()
        if (currentFlags.contains(questionId)) {
            currentFlags.remove(questionId)
        } else {
            currentFlags.add(questionId)
        }
        _uiState.update { it.copy(flaggedQuestionIds = currentFlags) }
    }

    fun setCustomTimeLimit(minutes: Int?) {
        _uiState.update { it.copy(customTimeLimitMinutes = minutes) }
    }

    fun showPalette(show: Boolean) {
        _uiState.update { it.copy(showPaletteSheet = show) }
    }

    fun showReviewDialog(show: Boolean) {
        _uiState.update { it.copy(showReviewDialog = show) }
    }

    fun nextQuestion() {
        val nextIdx = _uiState.value.currentQuestionIndex + 1
        if (nextIdx < _uiState.value.questions.size) {
            _uiState.update { it.copy(currentQuestionIndex = nextIdx) }
            persistActiveState()
        }
    }

    fun previousQuestion() {
        val prevIdx = _uiState.value.currentQuestionIndex - 1
        if (prevIdx >= 0) {
            _uiState.update { it.copy(currentQuestionIndex = prevIdx) }
            persistActiveState()
        }
    }

    fun jumpToQuestion(index: Int) {
        if (index in 0 until _uiState.value.questions.size) {
            _uiState.update { it.copy(currentQuestionIndex = index, showPaletteSheet = false, showReviewDialog = false) }
            persistActiveState()
        }
    }

    fun jumpToFirstUnanswered() {
        val unansweredIdx = _uiState.value.questions.indexOfFirst {
            !_uiState.value.selectedAnswers.containsKey(it.id)
        }
        if (unansweredIdx != -1) {
            jumpToQuestion(unansweredIdx)
        }
    }

    fun jumpToFirstFlagged() {
        val flaggedIdx = _uiState.value.questions.indexOfFirst {
            _uiState.value.flaggedQuestionIds.contains(it.id)
        }
        if (flaggedIdx != -1) {
            jumpToQuestion(flaggedIdx)
        }
    }

    private var persistJob: Job? = null

    private fun persistActiveState() {
        val state = _uiState.value
        if (state.isCompleted || state.quiz == null) return
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            saveActiveQuizStateUseCase(
                ActiveQuizState(
                    quizId = quizId,
                    currentQuestionIndex = state.currentQuestionIndex,
                    answers = state.selectedAnswers,
                    elapsedSeconds = state.elapsedSeconds
                )
            )
        }
    }

    fun submitQuiz() {
        val state = _uiState.value
        val quiz = state.quiz ?: return
        if (state.isSubmitting || state.isCompleted) return

        timerJob?.cancel()
        _uiState.update { it.copy(isSubmitting = true, showReviewDialog = false, showPaletteSheet = false) }

        viewModelScope.launch {
            try {
                val attempt = submitQuizAttemptUseCase(
                    quizId = quizId,
                    subjectId = quiz.subjectId,
                    chapterId = quiz.chapterId,
                    startedAt = state.startedAt,
                    timeSpentSeconds = state.elapsedSeconds,
                    answers = state.selectedAnswers,
                    questions = state.questions
                )

                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        isCompleted = true,
                        attemptResult = attempt,
                        infoMessage = "Quiz submitted successfully! Incorrect answers saved to Mistake Bank."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        infoMessage = e.message ?: "Failed to submit quiz."
                    )
                }
            }
        }
    }

    fun retakeQuiz() {
        _uiState.update {
            it.copy(
                currentQuestionIndex = 0,
                selectedAnswers = emptyMap(),
                flaggedQuestionIds = emptySet(),
                elapsedSeconds = 0,
                startedAt = System.currentTimeMillis(),
                isCompleted = false,
                attemptResult = null,
                infoMessage = null,
                showPaletteSheet = false,
                showReviewDialog = false
            )
        }
        persistActiveState()
        startTimer()
    }

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
