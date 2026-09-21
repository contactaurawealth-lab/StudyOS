package com.studyos.app.features.practice.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.domain.model.Flashcard
import com.studyos.app.domain.model.FlashcardRating
import com.studyos.app.domain.usecase.GetDueFlashcardsUseCase
import com.studyos.app.domain.usecase.GetFlashcardsForChapterUseCase
import com.studyos.app.domain.usecase.ReviewFlashcardUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FlashcardStudyUiState(
    val cards: List<Flashcard> = emptyList(),
    val currentIndex: Int = 0,
    val isAnswerRevealed: Boolean = false,
    val isLoading: Boolean = true,
    val isFinished: Boolean = false,
    val reviewedCount: Int = 0,
    val againCount: Int = 0,
    val hardCount: Int = 0,
    val goodCount: Int = 0,
    val easyCount: Int = 0
) {
    val currentCard: Flashcard? get() = cards.getOrNull(currentIndex)
    val progress: Float get() = if (cards.isNotEmpty()) (currentIndex.toFloat() / cards.size) else 0f
}

class FlashcardStudyViewModel(
    private val chapterId: String?,
    private val isDueOnly: Boolean,
    private val getFlashcardsForChapterUseCase: GetFlashcardsForChapterUseCase,
    private val getDueFlashcardsUseCase: GetDueFlashcardsUseCase,
    private val reviewFlashcardUseCase: ReviewFlashcardUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FlashcardStudyUiState())
    val uiState: StateFlow<FlashcardStudyUiState> = _uiState.asStateFlow()

    init {
        loadDeck()
    }

    fun loadDeck() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val cardsList = if (isDueOnly) {
                val dueCards = getDueFlashcardsUseCase().first()
                if (!chapterId.isNullOrBlank()) {
                    dueCards.filter { it.chapterId == chapterId }
                } else {
                    dueCards
                }
            } else if (!chapterId.isNullOrBlank()) {
                getFlashcardsForChapterUseCase(chapterId).first()
            } else {
                getDueFlashcardsUseCase().first()
            }

            _uiState.update {
                it.copy(
                    cards = cardsList,
                    currentIndex = 0,
                    isAnswerRevealed = false,
                    isLoading = false,
                    isFinished = cardsList.isEmpty(),
                    reviewedCount = 0,
                    againCount = 0,
                    hardCount = 0,
                    goodCount = 0,
                    easyCount = 0
                )
            }
        }
    }

    fun revealAnswer() {
        _uiState.update { it.copy(isAnswerRevealed = true) }
    }

    private var isRatingInProgress = false

    fun rateCard(rating: FlashcardRating) {
        val currentCard = _uiState.value.currentCard ?: return
        if (isRatingInProgress || !_uiState.value.isAnswerRevealed) return
        isRatingInProgress = true

        viewModelScope.launch {
            try {
                reviewFlashcardUseCase(currentCard.id, rating)

                _uiState.update { state ->
                    val nextIdx = state.currentIndex + 1
                    val isDone = nextIdx >= state.cards.size

                    state.copy(
                        currentIndex = nextIdx,
                        isAnswerRevealed = false,
                        isFinished = isDone,
                        reviewedCount = state.reviewedCount + 1,
                        againCount = state.againCount + if (rating == FlashcardRating.AGAIN) 1 else 0,
                        hardCount = state.hardCount + if (rating == FlashcardRating.HARD) 1 else 0,
                        goodCount = state.goodCount + if (rating == FlashcardRating.GOOD) 1 else 0,
                        easyCount = state.easyCount + if (rating == FlashcardRating.EASY) 1 else 0
                    )
                }
            } finally {
                isRatingInProgress = false
            }
        }
    }

    fun restartDeck() {
        loadDeck()
    }
}
