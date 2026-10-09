package com.studyos.app.features.practice.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.domain.model.ChapterPracticeSummary
import com.studyos.app.domain.model.Flashcard
import com.studyos.app.domain.model.FlashcardDifficulty
import com.studyos.app.domain.model.Mistake
import com.studyos.app.domain.model.Note
import com.studyos.app.domain.model.Quiz
import com.studyos.app.domain.model.QuizAttempt
import com.studyos.app.domain.model.QuizDifficulty
import com.studyos.app.domain.model.QuizQuestion
import com.studyos.app.domain.usecase.AiPracticeToolsUseCase
import com.studyos.app.domain.usecase.ConvertMistakeToFlashcardUseCase
import com.studyos.app.domain.usecase.DeleteFlashcardUseCase
import com.studyos.app.domain.usecase.DeleteNoteUseCase
import com.studyos.app.domain.usecase.GetAiConfigUseCase
import com.studyos.app.domain.usecase.GetChapterPracticeSummaryUseCase
import com.studyos.app.domain.usecase.GetFlashcardsForChapterUseCase
import com.studyos.app.domain.usecase.GetMistakesForChapterUseCase
import com.studyos.app.domain.usecase.GetNotesForChapterUseCase
import com.studyos.app.domain.usecase.ResolveMistakeUseCase
import com.studyos.app.domain.usecase.SaveFlashcardUseCase
import com.studyos.app.domain.usecase.SaveQuizUseCase
import com.studyos.app.domain.usecase.ToggleNotePinUseCase
import com.studyos.app.core.database.dao.QuestionBankDao
import com.studyos.app.core.database.dao.TopicDao
import com.studyos.app.core.database.entity.TopicEntity
import com.studyos.app.domain.usecase.GenerateQuizFromQuestionBankUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PracticeHubTab(val label: String) {
    OVERVIEW("Overview"),
    NOTES("Notes"),
    FLASHCARDS("Flashcards"),
    QUIZ("Quiz"),
    MISTAKES("Mistakes")
}

data class ChapterPracticeHubUiState(
    val chapterId: String = "",
    val selectedTab: PracticeHubTab = PracticeHubTab.OVERVIEW,
    val summary: ChapterPracticeSummary? = null,
    val notes: List<Note> = emptyList(),
    val flashcards: List<Flashcard> = emptyList(),
    val mistakes: List<Mistake> = emptyList(),
    val availableTopics: List<TopicEntity> = emptyList(),
    val selectedTopicIdForQuiz: String? = null,
    val topicQuestionCounts: Map<String, Int> = emptyMap(),
    val chapterQuestionCount: Int = 0,
    val isLoading: Boolean = true,
    val isCreateFlashcardSheetOpen: Boolean = false,
    val isBulkImportSheetOpen: Boolean = false,
    val isCreateQuizSheetOpen: Boolean = false,
    val isGeneratingQuiz: Boolean = false,
    val isGeneratingAiQuiz: Boolean = false,
    val selectedMistakeForAi: Mistake? = null,
    val aiExplanation: String? = null,
    val isExplainingMistake: Boolean = false,
    val createdQuizId: String? = null,
    val infoMessage: String? = null
) {
    val selectedTopicQuestionCount: Int
        get() = if (selectedTopicIdForQuiz != null) {
            topicQuestionCounts[selectedTopicIdForQuiz] ?: 0
        } else {
            chapterQuestionCount
        }
}

class ChapterPracticeHubViewModel(
    private val chapterId: String,
    private val getChapterPracticeSummaryUseCase: GetChapterPracticeSummaryUseCase,
    private val getNotesForChapterUseCase: GetNotesForChapterUseCase,
    private val getFlashcardsForChapterUseCase: GetFlashcardsForChapterUseCase,
    private val getMistakesForChapterUseCase: GetMistakesForChapterUseCase,
    private val saveFlashcardUseCase: SaveFlashcardUseCase,
    private val deleteFlashcardUseCase: DeleteFlashcardUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val toggleNotePinUseCase: ToggleNotePinUseCase,
    private val resolveMistakeUseCase: ResolveMistakeUseCase,
    private val convertMistakeToFlashcardUseCase: ConvertMistakeToFlashcardUseCase,
    private val saveQuizUseCase: SaveQuizUseCase,
    private val aiPracticeToolsUseCase: AiPracticeToolsUseCase,
    private val getAiConfigUseCase: GetAiConfigUseCase,
    private val topicDao: TopicDao,
    private val questionBankDao: QuestionBankDao,
    private val generateQuizFromQuestionBankUseCase: GenerateQuizFromQuestionBankUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChapterPracticeHubUiState(chapterId = chapterId))
    val uiState: StateFlow<ChapterPracticeHubUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            getChapterPracticeSummaryUseCase(chapterId).collect { summary ->
                _uiState.update { it.copy(summary = summary, isLoading = false) }
            }
        }

        viewModelScope.launch {
            getNotesForChapterUseCase(chapterId).collect { notes ->
                _uiState.update { it.copy(notes = notes) }
            }
        }

        viewModelScope.launch {
            getFlashcardsForChapterUseCase(chapterId).collect { cards ->
                _uiState.update { it.copy(flashcards = cards) }
            }
        }

        viewModelScope.launch {
            getMistakesForChapterUseCase(chapterId).collect { mistakes ->
                _uiState.update { it.copy(mistakes = mistakes) }
            }
        }

        viewModelScope.launch {
            topicDao.getTopicsForChapter(chapterId).collect { topics ->
                val counts = mutableMapOf<String, Int>()
                for (t in topics) {
                    counts[t.id] = questionBankDao.countQuestionsForTopic(t.id)
                }
                val totalChapter = questionBankDao.countQuestionsForChapter(chapterId)
                _uiState.update {
                    it.copy(
                        availableTopics = topics,
                        topicQuestionCounts = counts,
                        chapterQuestionCount = totalChapter
                    )
                }
            }
        }

        viewModelScope.launch {
            questionBankDao.getQuestionsForChapter(chapterId).collect { chapterQuestions ->
                val topics = _uiState.value.availableTopics
                val counts = mutableMapOf<String, Int>()
                for (t in topics) {
                    counts[t.id] = chapterQuestions.count { it.topicId == t.id }
                }
                _uiState.update {
                    it.copy(
                        topicQuestionCounts = counts,
                        chapterQuestionCount = chapterQuestions.size
                    )
                }
            }
        }
    }

    fun selectTab(tab: PracticeHubTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    // Notes
    fun deleteNote(id: String) {
        viewModelScope.launch {
            deleteNoteUseCase(id)
            _uiState.update { it.copy(infoMessage = "Note deleted.") }
        }
    }

    fun toggleNotePin(id: String, currentPin: Boolean) {
        viewModelScope.launch {
            toggleNotePinUseCase(id, !currentPin)
        }
    }

    // Flashcards
    fun openCreateFlashcardSheet() {
        _uiState.update { it.copy(isCreateFlashcardSheetOpen = true) }
    }

    fun closeCreateFlashcardSheet() {
        _uiState.update { it.copy(isCreateFlashcardSheetOpen = false) }
    }

    fun openBulkImportSheet() {
        _uiState.update { it.copy(isBulkImportSheetOpen = true) }
    }

    fun closeBulkImportSheet() {
        _uiState.update { it.copy(isBulkImportSheetOpen = false) }
    }

    fun bulkImportFlashcards(
        parsedCards: List<com.studyos.app.core.util.ParsedCard>,
        difficulty: FlashcardDifficulty = FlashcardDifficulty.MEDIUM
    ) {
        val summary = _uiState.value.summary ?: return
        if (parsedCards.isEmpty()) return

        viewModelScope.launch {
            val cards = parsedCards.map { card ->
                Flashcard(
                    question = card.question.trim(),
                    answer = card.answer.trim(),
                    subjectId = summary.subjectId,
                    chapterId = chapterId,
                    difficulty = difficulty
                )
            }
            saveFlashcardUseCase.saveBatch(cards)
            _uiState.update {
                it.copy(
                    isBulkImportSheetOpen = false,
                    infoMessage = "Successfully imported ${cards.size} flashcard(s)!"
                )
            }
        }
    }

    fun createFlashcard(question: String, answer: String, difficulty: FlashcardDifficulty) {
        val summary = _uiState.value.summary ?: return
        if (question.isBlank() || answer.isBlank()) return

        viewModelScope.launch {
            saveFlashcardUseCase(
                question = question,
                answer = answer,
                subjectId = summary.subjectId,
                chapterId = chapterId,
                difficulty = difficulty
            )
            _uiState.update {
                it.copy(
                    isCreateFlashcardSheetOpen = false,
                    infoMessage = "Flashcard created."
                )
            }
        }
    }

    fun deleteFlashcard(cardId: String) {
        viewModelScope.launch {
            deleteFlashcardUseCase(cardId)
            _uiState.update { it.copy(infoMessage = "Flashcard deleted.") }
        }
    }

    // Mistakes
    fun resolveMistake(id: String) {
        viewModelScope.launch {
            resolveMistakeUseCase(id)
            _uiState.update { it.copy(infoMessage = "Mistake marked as resolved.") }
        }
    }

    fun convertMistakeToFlashcard(mistake: Mistake) {
        viewModelScope.launch {
            convertMistakeToFlashcardUseCase(mistake)
            _uiState.update { it.copy(infoMessage = "Flashcard created from mistake.") }
        }
    }

    fun explainMistakeWithAi(mistake: Mistake) {
        _uiState.update {
            it.copy(
                selectedMistakeForAi = mistake,
                aiExplanation = "",
                isExplainingMistake = true
            )
        }
        viewModelScope.launch {
            val config = getAiConfigUseCase().firstOrNull() ?: com.studyos.app.domain.model.AiConfig()
            aiPracticeToolsUseCase.explainMistake(
                question = mistake.question,
                studentAnswer = mistake.studentAnswer,
                correctAnswer = mistake.correctAnswer,
                config = config
            ).collect { explanation ->
                _uiState.update { it.copy(aiExplanation = explanation) }
            }
            _uiState.update { it.copy(isExplainingMistake = false) }
        }
    }

    fun closeMistakeAiExplanation() {
        _uiState.update {
            it.copy(
                selectedMistakeForAi = null,
                aiExplanation = null,
                isExplainingMistake = false
            )
        }
    }

    // Quizzes
    fun openCreateQuizSheet(preselectedTopicId: String? = null) {
        _uiState.update {
            it.copy(
                isCreateQuizSheetOpen = true,
                selectedTopicIdForQuiz = preselectedTopicId ?: it.selectedTopicIdForQuiz,
                createdQuizId = null
            )
        }
    }

    fun closeCreateQuizSheet() {
        _uiState.update { it.copy(isCreateQuizSheetOpen = false) }
    }

    fun selectTopicForQuiz(topicId: String?) {
        _uiState.update { it.copy(selectedTopicIdForQuiz = topicId) }
    }

    fun generateQuizFromQuestionBank(
        topicId: String? = _uiState.value.selectedTopicIdForQuiz,
        questionCount: Int = 5,
        difficulty: QuizDifficulty? = null
    ) {
        val summary = _uiState.value.summary ?: return
        val availableTopics = _uiState.value.availableTopics
        val selectedTopic = topicId?.let { tid -> availableTopics.find { it.id == tid } }
        val topicName = selectedTopic?.name ?: summary.chapterName
        val quizTitle = if (selectedTopic != null) {
            "${selectedTopic.name} Topic Quiz"
        } else {
            "${summary.chapterName} Quiz"
        }

        _uiState.update { it.copy(isGeneratingQuiz = true, isGeneratingAiQuiz = true) }

        viewModelScope.launch {
            try {
                val result = generateQuizFromQuestionBankUseCase(
                    title = quizTitle,
                    subjectId = summary.subjectId,
                    chapterId = chapterId,
                    topicId = topicId,
                    topicName = topicName,
                    questionCount = questionCount,
                    difficulty = difficulty
                )

                if (result == null) {
                    _uiState.update {
                        it.copy(
                            isGeneratingQuiz = false,
                            isGeneratingAiQuiz = false,
                            infoMessage = "No questions found in Question Bank for topic '$topicName'. Please import or add questions first."
                        )
                    }
                } else {
                    val (quiz, questions) = result
                    _uiState.update {
                        it.copy(
                            isGeneratingQuiz = false,
                            isGeneratingAiQuiz = false,
                            isCreateQuizSheetOpen = false,
                            createdQuizId = quiz.id,
                            infoMessage = "Generated ${questions.size}-question quiz from Question Bank for '$topicName'!"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGeneratingQuiz = false,
                        isGeneratingAiQuiz = false,
                        infoMessage = e.message ?: "Failed to generate quiz from Question Bank."
                    )
                }
            }
        }
    }

    fun generateAiQuiz(
        questionCount: Int = 5,
        difficulty: QuizDifficulty = QuizDifficulty.MEDIUM
    ) {
        // Strictly route through Question Bank for the currently selected topic or chapter
        generateQuizFromQuestionBank(
            topicId = _uiState.value.selectedTopicIdForQuiz,
            questionCount = questionCount,
            difficulty = difficulty
        )
    }

    fun clearCreatedQuizId() {
        _uiState.update { it.copy(createdQuizId = null) }
    }

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }
}
