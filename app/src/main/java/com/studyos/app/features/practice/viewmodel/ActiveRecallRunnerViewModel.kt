package com.studyos.app.features.practice.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.domain.model.ActiveRecallItem
import com.studyos.app.domain.model.ActiveRecallSessionSummary
import com.studyos.app.domain.model.ActiveRecallSessionType
import com.studyos.app.domain.usecase.CompleteActiveRecallSessionUseCase
import com.studyos.app.domain.usecase.StartActiveRecallSessionUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.studyos.app.domain.engine.RecallAlgorithm
import com.studyos.app.domain.model.RecallEvaluationResult
import com.studyos.app.domain.model.RecallEvaluationStatus

data class ActiveRecallRunnerUiState(
    val sessionType: ActiveRecallSessionType = ActiveRecallSessionType.DEEP_15,
    val items: List<ActiveRecallItem> = emptyList(),
    val currentIndex: Int = 0,
    val isAnswerRevealed: Boolean = false,
    val typedAnswer: String = "",
    val evaluationResult: RecallEvaluationResult? = null,
    val selectedOption: String? = null,
    val results: List<Pair<ActiveRecallItem, Boolean>> = emptyList(),
    val elapsedSeconds: Int = 0,
    val isFinished: Boolean = false,
    val summary: ActiveRecallSessionSummary? = null,
    val isLoading: Boolean = true,
    val isFeynmanMode: Boolean = false,
    val feynmanRemainingSeconds: Int = 180,
    val feynmanRubricSimple: Boolean = false,
    val feynmanRubricAccurate: Boolean = false,
    val feynmanRubricGapsIdentified: Boolean = false
) {
    val currentItem: ActiveRecallItem?
        get() = items.getOrNull(currentIndex)

    val progressFraction: Float
        get() = if (items.isNotEmpty()) (currentIndex.toFloat() / items.size.toFloat()) else 0f

    val feynmanTimerString: String
        get() {
            val mins = feynmanRemainingSeconds / 60
            val secs = feynmanRemainingSeconds % 60
            return String.format("%02d:%02d", mins, secs)
        }
}

class ActiveRecallRunnerViewModel(
    private val startActiveRecallSessionUseCase: StartActiveRecallSessionUseCase,
    private val completeActiveRecallSessionUseCase: CompleteActiveRecallSessionUseCase,
    private val initialSessionType: ActiveRecallSessionType = ActiveRecallSessionType.DEEP_15,
    private val chapterId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ActiveRecallRunnerUiState(sessionType = initialSessionType)
    )
    val uiState: StateFlow<ActiveRecallRunnerUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        startSession()
    }

    fun startSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val loadedItems = startActiveRecallSessionUseCase(
                sessionType = _uiState.value.sessionType,
                chapterId = chapterId
            )
            _uiState.update {
                it.copy(
                    items = loadedItems,
                    currentIndex = 0,
                    isAnswerRevealed = false,
                    typedAnswer = "",
                    evaluationResult = null,
                    selectedOption = null,
                    results = emptyList(),
                    elapsedSeconds = 0,
                    feynmanRemainingSeconds = 180,
                    feynmanRubricSimple = false,
                    feynmanRubricAccurate = false,
                    feynmanRubricGapsIdentified = false,
                    isFinished = false,
                    summary = null,
                    isLoading = false
                )
            }
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { current ->
                    val newElapsed = current.elapsedSeconds + 1
                    val newFeynman = if (current.isFeynmanMode && !current.isAnswerRevealed) {
                        (current.feynmanRemainingSeconds - 1).coerceAtLeast(0)
                    } else {
                        current.feynmanRemainingSeconds
                    }
                    current.copy(
                        elapsedSeconds = newElapsed,
                        feynmanRemainingSeconds = newFeynman
                    )
                }
            }
        }
    }

    fun toggleFeynmanMode() {
        _uiState.update { current ->
            current.copy(
                isFeynmanMode = !current.isFeynmanMode,
                feynmanRemainingSeconds = 180,
                feynmanRubricSimple = false,
                feynmanRubricAccurate = false,
                feynmanRubricGapsIdentified = false
            )
        }
    }

    fun toggleFeynmanRubric(criterion: String) {
        _uiState.update { state ->
            when (criterion) {
                "simple" -> state.copy(feynmanRubricSimple = !state.feynmanRubricSimple)
                "accurate" -> state.copy(feynmanRubricAccurate = !state.feynmanRubricAccurate)
                "gaps" -> state.copy(feynmanRubricGapsIdentified = !state.feynmanRubricGapsIdentified)
                else -> state
            }
        }
    }

    fun updateTypedAnswer(text: String) {
        _uiState.update { it.copy(typedAnswer = text) }
    }

    fun revealAnswer() {
        val current = _uiState.value.currentItem
        val eval = if (current != null) {
            RecallAlgorithm.evaluateAnswer(
                userAnswer = _uiState.value.typedAnswer,
                expectedAnswer = current.answer,
                explanation = current.explanation ?: ""
            )
        } else null

        _uiState.update {
            it.copy(
                isAnswerRevealed = true,
                evaluationResult = eval
            )
        }
    }

    fun selectOption(option: String) {
        val current = _uiState.value.currentItem ?: return
        val eval = RecallAlgorithm.evaluateAnswer(
            userAnswer = option,
            expectedAnswer = current.answer,
            explanation = current.explanation ?: ""
        )
        _uiState.update {
            it.copy(
                selectedOption = option,
                isAnswerRevealed = true,
                evaluationResult = eval
            )
        }
    }

    fun recordResult(wasCorrect: Boolean) {
        val current = _uiState.value.currentItem ?: return
        val updatedResults = _uiState.value.results + (current to wasCorrect)
        val nextIdx = _uiState.value.currentIndex + 1

        if (nextIdx >= _uiState.value.items.size) {
            finishSession(updatedResults)
        } else {
            _uiState.update {
                it.copy(
                    currentIndex = nextIdx,
                    isAnswerRevealed = false,
                    typedAnswer = "",
                    evaluationResult = null,
                    selectedOption = null,
                    feynmanRemainingSeconds = 180,
                    feynmanRubricSimple = false,
                    feynmanRubricAccurate = false,
                    feynmanRubricGapsIdentified = false,
                    results = updatedResults
                )
            }
        }
    }

    fun skipItem() {
        recordResult(wasCorrect = false)
    }

    fun finishSession(results: List<Pair<ActiveRecallItem, Boolean>> = _uiState.value.results) {
        timerJob?.cancel()
        viewModelScope.launch {
            val sessionType = _uiState.value.sessionType
            val duration = _uiState.value.elapsedSeconds
            val summary = completeActiveRecallSessionUseCase(
                sessionType = sessionType,
                durationSeconds = duration,
                reviewedItems = results
            )
            _uiState.update {
                it.copy(
                    isFinished = true,
                    summary = summary
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
