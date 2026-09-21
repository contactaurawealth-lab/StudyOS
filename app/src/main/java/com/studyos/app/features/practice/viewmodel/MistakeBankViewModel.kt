package com.studyos.app.features.practice.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.domain.model.AiConfig
import com.studyos.app.domain.model.Mistake
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.repository.SubjectRepository
import com.studyos.app.domain.usecase.AiPracticeToolsUseCase
import com.studyos.app.domain.usecase.ConvertMistakeToFlashcardUseCase
import com.studyos.app.domain.usecase.GetAiConfigUseCase
import com.studyos.app.domain.usecase.GetAllMistakesUseCase
import com.studyos.app.domain.usecase.ResolveMistakeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MistakeFilter(val label: String) {
    UNRESOLVED("Unresolved"),
    ALL("All Mistakes"),
    RESOLVED("Resolved")
}

data class MistakeBankUiState(
    val mistakes: List<Mistake> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val selectedFilter: MistakeFilter = MistakeFilter.UNRESOLVED,
    val selectedCategory: String? = null,
    val selectedSubjectId: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val selectedMistakeForAi: Mistake? = null,
    val aiExplanation: String? = null,
    val isExplaining: Boolean = false,
    val infoMessage: String? = null
) {
    val filteredMistakes: List<Mistake>
        get() {
            return mistakes.filter { mistake ->
                val matchesFilter = when (selectedFilter) {
                    MistakeFilter.UNRESOLVED -> !mistake.isResolved
                    MistakeFilter.RESOLVED -> mistake.isResolved
                    MistakeFilter.ALL -> true
                }
                val matchesSubject = selectedSubjectId == null || mistake.subjectId == selectedSubjectId
                val matchesCategory = selectedCategory == null || (mistake.topic?.contains(selectedCategory, ignoreCase = true) == true)
                val matchesQuery = searchQuery.isBlank() ||
                        mistake.question.contains(searchQuery, ignoreCase = true) ||
                        (mistake.topic?.contains(searchQuery, ignoreCase = true) == true)

                matchesFilter && matchesSubject && matchesCategory && matchesQuery
            }
        }

    val unresolvedCount: Int get() = mistakes.count { !it.isResolved }
    val resolvedCount: Int get() = mistakes.count { it.isResolved }
    val conceptCount: Int get() = mistakes.count { !it.isResolved && (it.topic?.contains("Concept", ignoreCase = true) == true) }
    val memoryCount: Int get() = mistakes.count { !it.isResolved && (it.topic?.contains("Memory", ignoreCase = true) == true) }
    val calculationCount: Int get() = mistakes.count { !it.isResolved && (it.topic?.contains("Calculation", ignoreCase = true) == true) }
    val carelessCount: Int get() = mistakes.count { !it.isResolved && (it.topic?.contains("Careless", ignoreCase = true) == true || it.topic?.contains("Misread", ignoreCase = true) == true) }
    val isReviewModalOpen: Boolean = false
    val currentReviewIndex: Int = 0
}

class MistakeBankViewModel(
    private val getAllMistakesUseCase: GetAllMistakesUseCase,
    private val resolveMistakeUseCase: ResolveMistakeUseCase,
    private val convertMistakeToFlashcardUseCase: ConvertMistakeToFlashcardUseCase,
    private val aiPracticeToolsUseCase: AiPracticeToolsUseCase,
    private val getAiConfigUseCase: GetAiConfigUseCase,
    private val subjectRepository: SubjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MistakeBankUiState())
    val uiState: StateFlow<MistakeBankUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            subjectRepository.getAllSubjects().collect { subjects ->
                _uiState.update { it.copy(subjects = subjects) }
            }
        }

        viewModelScope.launch {
            getAllMistakesUseCase().collect { mistakes ->
                _uiState.update { it.copy(mistakes = mistakes, isLoading = false) }
            }
        }
    }

    fun setFilter(filter: MistakeFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun setSubjectFilter(subjectId: String?) {
        _uiState.update { it.copy(selectedSubjectId = subjectId) }
    }

    fun setCategoryFilter(category: String?) {
        _uiState.update { it.copy(selectedCategory = if (it.selectedCategory == category) null else category) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleResolve(mistake: Mistake) {
        viewModelScope.launch {
            resolveMistakeUseCase(mistake.id)
            val action = if (mistake.isResolved) "unresolved" else "resolved"
            _uiState.update { it.copy(infoMessage = "Mistake marked as $action.") }
        }
    }

    fun convertToFlashcard(mistake: Mistake) {
        viewModelScope.launch {
            convertMistakeToFlashcardUseCase(mistake)
            _uiState.update { it.copy(infoMessage = "Converted mistake into flashcard!") }
        }
    }

    fun explainWithAi(mistake: Mistake) {
        _uiState.update {
            it.copy(
                selectedMistakeForAi = mistake,
                aiExplanation = "",
                isExplaining = true
            )
        }

        viewModelScope.launch {
            try {
                val config = getAiConfigUseCase().firstOrNull() ?: AiConfig()
                aiPracticeToolsUseCase.explainMistake(
                    question = mistake.question,
                    studentAnswer = mistake.studentAnswer,
                    correctAnswer = mistake.correctAnswer,
                    config = config
                ).collect { chunk ->
                    _uiState.update { it.copy(aiExplanation = chunk) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(infoMessage = e.message ?: "Failed to explain mistake.") }
            } finally {
                _uiState.update { it.copy(isExplaining = false) }
            }
        }
    }

    fun closeAiExplanation() {
        _uiState.update {
            it.copy(
                selectedMistakeForAi = null,
                aiExplanation = null,
                isExplaining = false
            )
        }
    }

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }
}
