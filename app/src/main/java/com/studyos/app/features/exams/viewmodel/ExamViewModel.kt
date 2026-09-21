package com.studyos.app.features.exams.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.domain.model.Exam
import com.studyos.app.domain.model.ExamChapterRevisionItem
import com.studyos.app.domain.model.ExamDashboardItem
import com.studyos.app.domain.model.ExamRevisionCategory
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.repository.SubjectRepository
import com.studyos.app.domain.usecase.DeleteExamUseCase
import com.studyos.app.domain.usecase.GetExamDashboardUseCase
import com.studyos.app.domain.usecase.GetExamsUseCase
import com.studyos.app.domain.usecase.SaveExamUseCase
import com.studyos.app.core.notification.AlarmScheduler
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExamUiState(
    val exams: List<Exam> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val isLoading: Boolean = true,
    val selectedExamId: String? = null,
    val examDashboard: ExamDashboardItem? = null,
    val revisionChapters: List<ExamChapterRevisionItem> = emptyList(),
    val selectedCategoryFilter: ExamRevisionCategory? = null,
    val isLoadingDetail: Boolean = false,
    val isCreateExamSheetOpen: Boolean = false,
    val editingExam: Exam? = null,
    val infoMessage: String? = null
) {
    val filteredRevisionChapters: List<ExamChapterRevisionItem>
        get() = if (selectedCategoryFilter == null) {
            revisionChapters
        } else {
            revisionChapters.filter { it.category == selectedCategoryFilter }
        }

    val needRevisionCount: Int get() = revisionChapters.count { it.category == ExamRevisionCategory.NEED_REVISION }
    val practiceCount: Int get() = revisionChapters.count { it.category == ExamRevisionCategory.PRACTICE }
    val strongCount: Int get() = revisionChapters.count { it.category == ExamRevisionCategory.STRONG }
}

class ExamViewModel(
    private val getExamsUseCase: GetExamsUseCase,
    private val saveExamUseCase: SaveExamUseCase,
    private val deleteExamUseCase: DeleteExamUseCase,
    private val getExamDashboardUseCase: GetExamDashboardUseCase,
    private val subjectRepository: SubjectRepository,
    private val updateExamScoreUseCase: com.studyos.app.domain.usecase.UpdateExamScoreUseCase? = null,
    private val alarmScheduler: AlarmScheduler? = null,
    private val context: Context? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExamUiState())
    val uiState: StateFlow<ExamUiState> = _uiState.asStateFlow()

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
            getExamsUseCase().collect { exams ->
                _uiState.update { it.copy(exams = exams, isLoading = false) }

                // If currently viewing an exam, refresh detail
                val currentExamId = _uiState.value.selectedExamId
                if (currentExamId != null) {
                    loadExamDetail(currentExamId)
                }
            }
        }
    }

    fun loadExamDetail(examId: String) {
        _uiState.update { it.copy(selectedExamId = examId, isLoadingDetail = true) }
        viewModelScope.launch {
            val dashboard = getExamDashboardUseCase(examId)
            val chapters = getExamDashboardUseCase.getRevisionChapters(examId)
            _uiState.update {
                it.copy(
                    examDashboard = dashboard,
                    revisionChapters = chapters,
                    isLoadingDetail = false
                )
            }
        }
    }

    fun filterRevisionCategory(category: ExamRevisionCategory?) {
        _uiState.update { it.copy(selectedCategoryFilter = category) }
    }

    fun openCreateExamSheet() {
        _uiState.update { it.copy(isCreateExamSheetOpen = true, editingExam = null) }
    }

    fun openEditExamSheet(exam: Exam) {
        _uiState.update { it.copy(isCreateExamSheetOpen = true, editingExam = exam) }
    }

    fun closeExamSheet() {
        _uiState.update { it.copy(isCreateExamSheetOpen = false, editingExam = null) }
    }

    fun saveExam(
        name: String,
        targetDate: Long,
        subjectIds: List<String>,
        targetScore: Int? = null,
        notes: String? = null
    ) {
        if (name.isBlank() || subjectIds.isEmpty()) {
            _uiState.update { it.copy(infoMessage = "Please enter an exam title and select at least one subject.") }
            return
        }

        viewModelScope.launch {
            val editingId = _uiState.value.editingExam?.id
            val savedExam = saveExamUseCase(
                id = editingId,
                name = name,
                targetDate = targetDate,
                subjectIds = subjectIds,
                targetScore = targetScore,
                notes = notes
            )
            alarmScheduler?.scheduleExamReminder(savedExam.id, savedExam.name, savedExam.targetDate)
            context?.let {
                com.studyos.app.core.widget.ExamCountdownWidgetProvider.triggerUpdate(it)
            }
            _uiState.update {
                it.copy(
                    isCreateExamSheetOpen = false,
                    editingExam = null,
                    infoMessage = if (editingId != null) "Exam updated." else "Exam created!"
                )
            }
        }
    }

    fun deleteExam(id: String) {
        viewModelScope.launch {
            deleteExamUseCase(id)
            alarmScheduler?.cancelExamReminder(id)
            context?.let {
                com.studyos.app.core.widget.ExamCountdownWidgetProvider.triggerUpdate(it)
            }
            _uiState.update {
                it.copy(
                    selectedExamId = if (it.selectedExamId == id) null else it.selectedExamId,
                    examDashboard = if (it.selectedExamId == id) null else it.examDashboard,
                    revisionChapters = if (it.selectedExamId == id) emptyList() else it.revisionChapters,
                    infoMessage = "Exam deleted."
                )
            }
        }
    }

    fun logExamScore(examId: String, actualScore: Int) {
        viewModelScope.launch {
            updateExamScoreUseCase?.invoke(examId, actualScore, isCompleted = true)
            loadExamDetail(examId)
            _uiState.update { it.copy(infoMessage = "Score recorded: $actualScore%") }
        }
    }

    fun toggleExamCompleted(examId: String, completed: Boolean) {
        viewModelScope.launch {
            val currentScore = _uiState.value.examDashboard?.exam?.actualScore
            updateExamScoreUseCase?.invoke(examId, currentScore, isCompleted = completed)
            loadExamDetail(examId)
            _uiState.update { it.copy(infoMessage = if (completed) "Exam marked as completed." else "Exam moved to upcoming.") }
        }
    }

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }
}
