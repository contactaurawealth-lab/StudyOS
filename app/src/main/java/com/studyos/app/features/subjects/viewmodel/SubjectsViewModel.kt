package com.studyos.app.features.subjects.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.usecase.AddSubjectResult
import com.studyos.app.domain.usecase.AddSubjectUseCase
import com.studyos.app.domain.usecase.DeleteSubjectUseCase
import com.studyos.app.domain.usecase.GetSubjectsUseCase
import com.studyos.app.domain.usecase.RenameSubjectUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SubjectsUiState(
    val subjects: List<Subject> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val actionMessage: String? = null
)

class SubjectsViewModel(
    private val getSubjectsUseCase: GetSubjectsUseCase,
    private val addSubjectUseCase: AddSubjectUseCase,
    private val renameSubjectUseCase: RenameSubjectUseCase,
    private val deleteSubjectUseCase: DeleteSubjectUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubjectsUiState())
    val uiState: StateFlow<SubjectsUiState> = _uiState.asStateFlow()

    init {
        loadSubjects()
    }

    private fun loadSubjects() {
        viewModelScope.launch {
            getSubjectsUseCase().collect { list ->
                _uiState.update { it.copy(subjects = list, isLoading = false) }
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
                    _uiState.update { it.copy(errorMessage = result.message) }
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
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
            }
        }
        return success
    }

    fun deleteSubject(subjectId: String) {
        viewModelScope.launch {
            deleteSubjectUseCase(subjectId)
            _uiState.update { it.copy(actionMessage = "Subject removed") }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, actionMessage = null) }
    }
}
