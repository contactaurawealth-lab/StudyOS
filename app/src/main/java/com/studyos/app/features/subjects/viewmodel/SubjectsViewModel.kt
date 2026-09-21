package com.studyos.app.features.subjects.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.domain.model.SubjectWithProgress
import com.studyos.app.domain.usecase.AddSubjectResult
import com.studyos.app.domain.usecase.AddSubjectUseCase
import com.studyos.app.domain.usecase.DeleteSubjectUseCase
import com.studyos.app.domain.usecase.GetSubjectReadinessUseCase
import com.studyos.app.domain.usecase.GetSubjectsWithProgressUseCase
import com.studyos.app.domain.usecase.LoadSampleDataUseCase
import com.studyos.app.domain.usecase.RenameSubjectUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SubjectFilter {
    ALL,
    NEEDS_ATTENTION,
    DUE_FOR_REVIEW,
    COMPLETED
}

data class SubjectsUiState(
    val subjects: List<SubjectWithProgress> = emptyList(),
    val filteredSubjects: List<SubjectWithProgress> = emptyList(),
    val selectedFilter: SubjectFilter = SubjectFilter.ALL,
    val selectedSubjectForDiagnostic: SubjectWithProgress? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val actionMessage: String? = null
)

class SubjectsViewModel(
    private val getSubjectsWithProgressUseCase: GetSubjectsWithProgressUseCase,
    private val addSubjectUseCase: AddSubjectUseCase,
    private val renameSubjectUseCase: RenameSubjectUseCase,
    private val deleteSubjectUseCase: DeleteSubjectUseCase,
    private val loadSampleDataUseCase: LoadSampleDataUseCase? = null,
    private val getSubjectReadinessUseCase: GetSubjectReadinessUseCase? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubjectsUiState())
    val uiState: StateFlow<SubjectsUiState> = _uiState.asStateFlow()

    private var loadJob: kotlinx.coroutines.Job? = null

    init {
        loadSubjects()
    }

    fun loadSubjects() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        loadJob = viewModelScope.launch {
            getSubjectsWithProgressUseCase()
                .catch {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = "Couldn't load your subjects. Try again."
                        )
                    }
                }
                .collect { list ->
                    val enhancedList = if (getSubjectReadinessUseCase != null) {
                        list.map { item ->
                            try {
                                val readiness = getSubjectReadinessUseCase(item.subject.id)
                                if (readiness != null) {
                                    item.copy(
                                        readinessScore = readiness.readinessPercentage,
                                        understandingPercentage = readiness.understandingPercentage,
                                        recallPercentage = readiness.recallPercentage,
                                        practicePercentage = readiness.practicePercentage,
                                        strongCount = readiness.strongChaptersCount,
                                        weakCount = readiness.weakChaptersCount,
                                        dueCount = readiness.dueChaptersCount,
                                        insight = readiness.insight
                                    )
                                } else item
                            } catch (e: Exception) {
                                item
                            }
                        }
                    } else list

                    _uiState.update { state ->
                        state.copy(
                            subjects = enhancedList,
                            filteredSubjects = filterSubjects(enhancedList, state.selectedFilter),
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    fun setFilter(filter: SubjectFilter) {
        _uiState.update { current ->
            current.copy(
                selectedFilter = filter,
                filteredSubjects = filterSubjects(current.subjects, filter)
            )
        }
    }

    fun showDiagnostic(subject: SubjectWithProgress) {
        _uiState.update { it.copy(selectedSubjectForDiagnostic = subject) }
    }

    fun dismissDiagnostic() {
        _uiState.update { it.copy(selectedSubjectForDiagnostic = null) }
    }

    private fun filterSubjects(list: List<SubjectWithProgress>, filter: SubjectFilter): List<SubjectWithProgress> {
        return when (filter) {
            SubjectFilter.ALL -> list
            SubjectFilter.NEEDS_ATTENTION -> list.filter { it.weakCount > 0 || it.readinessScore < 70 }
            SubjectFilter.DUE_FOR_REVIEW -> list.filter { it.dueCount > 0 }
            SubjectFilter.COMPLETED -> list.filter { it.progress == 100 }
        }
    }

    fun addSubject(name: String): Boolean {
        var success = false
        viewModelScope.launch {
            when (val result = addSubjectUseCase(name, isCustom = true)) {
                is AddSubjectResult.Success -> {
                    success = true
                    _uiState.update { it.copy(errorMessage = null, actionMessage = "Subject added") }
                }
                is AddSubjectResult.EmptyName -> {
                    _uiState.update { it.copy(errorMessage = "Subject name cannot be empty.") }
                }
                is AddSubjectResult.DuplicateName -> {
                    _uiState.update { it.copy(errorMessage = "That subject already exists.") }
                }
                is AddSubjectResult.Error -> {
                    _uiState.update { it.copy(errorMessage = "Couldn't save the subject. Try again.") }
                }
            }
        }
        return success
    }

    fun renameSubject(subjectId: String, newName: String): Boolean {
        var success = false
        viewModelScope.launch {
            when (val result = renameSubjectUseCase(subjectId, newName)) {
                is AddSubjectResult.Success -> {
                    success = true
                    _uiState.update { it.copy(errorMessage = null, actionMessage = "Subject renamed") }
                }
                is AddSubjectResult.EmptyName -> {
                    _uiState.update { it.copy(errorMessage = "Subject name cannot be empty.") }
                }
                is AddSubjectResult.DuplicateName -> {
                    _uiState.update { it.copy(errorMessage = "That subject already exists.") }
                }
                is AddSubjectResult.Error -> {
                    _uiState.update { it.copy(errorMessage = "Couldn't rename the subject. Try again.") }
                }
            }
        }
        return success
    }

    fun deleteSubject(subjectId: String) {
        viewModelScope.launch {
            try {
                deleteSubjectUseCase(subjectId)
                _uiState.update { it.copy(actionMessage = "Subject removed") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Couldn't remove the subject. Try again.") }
            }
        }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            try {
                loadSampleDataUseCase?.invoke()
                _uiState.update { it.copy(actionMessage = "Sample data loaded") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to load sample data.") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, actionMessage = null) }
    }
}
