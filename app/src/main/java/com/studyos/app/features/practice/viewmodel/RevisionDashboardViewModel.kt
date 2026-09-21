package com.studyos.app.features.practice.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.domain.engine.ChapterIntelligenceCalculator
import com.studyos.app.domain.engine.PriorityQueueItem
import com.studyos.app.domain.engine.SmartRevisionQueueEngine
import com.studyos.app.domain.model.ActiveRecallSessionType
import com.studyos.app.domain.model.DueRevisionDashboardData
import com.studyos.app.domain.repository.ChapterRepository
import com.studyos.app.domain.repository.ExamRepository
import com.studyos.app.domain.repository.MistakeRepository
import com.studyos.app.domain.repository.RecallRepository
import com.studyos.app.domain.repository.RevisionRepository
import com.studyos.app.domain.repository.SubjectRepository
import com.studyos.app.domain.usecase.GetDueRevisionDashboardUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RevisionDashboardUiState(
    val dashboardData: DueRevisionDashboardData = DueRevisionDashboardData(),
    val priorityQueue: List<PriorityQueueItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class RevisionDashboardViewModel(
    private val getDueRevisionDashboardUseCase: GetDueRevisionDashboardUseCase,
    private val revisionRepository: RevisionRepository,
    private val chapterRepository: ChapterRepository? = null,
    private val subjectRepository: SubjectRepository? = null,
    private val mistakeRepository: MistakeRepository? = null,
    private val recallRepository: RecallRepository? = null,
    private val examRepository: ExamRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(RevisionDashboardUiState())
    val uiState: StateFlow<RevisionDashboardUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var queueJob: Job? = null

    init {
        loadDashboard()
        loadPriorityQueue()
    }

    fun loadDashboard() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        loadJob = viewModelScope.launch {
            getDueRevisionDashboardUseCase()
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Failed to load revision dashboard"
                        )
                    }
                }
                .collect { data ->
                    _uiState.update {
                        it.copy(
                            dashboardData = data,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun loadPriorityQueue() {
        if (chapterRepository == null || subjectRepository == null) return

        queueJob?.cancel()
        queueJob = viewModelScope.launch {
            try {
                val chapters = chapterRepository.observeAllChapters().firstOrNull() ?: emptyList()
                val subjects = subjectRepository.observeSubjects().firstOrNull() ?: emptyList()
                val exams = examRepository?.observeAllExams()?.firstOrNull() ?: emptyList()
                val mistakes = mistakeRepository?.observeAllMistakes()?.firstOrNull() ?: emptyList()
                val recallItems = recallRepository?.getDueRecallItems(System.currentTimeMillis() + 86400000L) ?: emptyList()

                val subjectMap = subjects.associateBy { it.id }
                val intelligences = chapters.mapNotNull { chapter ->
                    val subject = subjectMap[chapter.subjectId] ?: return@mapNotNull null
                    val chMistakes = mistakes.filter { it.chapterId == chapter.id }
                    val chRecall = recallItems.filter { it.chapterId == chapter.id }
                    ChapterIntelligenceCalculator.calculate(
                        chapter = chapter,
                        subject = subject,
                        recallItems = chRecall,
                        mistakes = chMistakes
                    )
                }

                val queue = SmartRevisionQueueEngine.buildQueue(
                    chapters = chapters,
                    subjects = subjects,
                    intelligences = intelligences,
                    exams = exams,
                    recallItems = recallItems,
                    mistakes = mistakes
                )

                _uiState.update { it.copy(priorityQueue = queue) }
            } catch (e: Exception) {
                // Keep existing priority queue on background error
            }
        }
    }

    fun setDailyTargetMinutes(minutes: Int) {
        viewModelScope.launch {
            revisionRepository.setDailyRevisionTargetMinutes(minutes)
        }
    }
}
