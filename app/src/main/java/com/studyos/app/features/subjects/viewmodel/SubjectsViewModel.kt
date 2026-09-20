package com.studyos.app.features.subjects.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.domain.model.SubjectWithProgress
import com.studyos.app.domain.usecase.AddSubjectResult
import com.studyos.app.domain.usecase.AddSubjectUseCase
import com.studyos.app.domain.usecase.DeleteSubjectUseCase
import com.studyos.app.domain.usecase.GetSubjectsWithProgressUseCase
import com.studyos.app.domain.usecase.LoadSampleDataUseCase
import com.studyos.app.domain.usecase.RenameSubjectUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SubjectsUiState(
    val subjects: List<SubjectWithProgress> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val actionMessage: String? = null
)

class SubjectsViewModel(
    private val getSubjectsWithProgressUseCase: GetSubjectsWithProgressUseCase,
    private val addSubjectUseCase: AddSubjectUseCase,
    private val renameSubjectUseCase: RenameSubjectUseCase,
    private val deleteSubjectUseCase: DeleteSubjectUseCase,
    private val loadSampleDataUseCase: LoadSampleDataUseCase? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubjectsUiState())
    val uiState: StateFlow<SubjectsUiState> = _uiState.asStateFlow()

    init {
        loadSubjects()
    }

    fun loadSubjects() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
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
                    _uiState.update {
                        it.copy(subjects = list, isLoading = false, errorMessage = null)
                    }
                }
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
