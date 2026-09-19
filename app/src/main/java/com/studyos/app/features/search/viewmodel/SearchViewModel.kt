package com.studyos.app.features.search.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.usecase.GetSubjectsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val subjectResults: List<Subject> = emptyList(),
    val hasSearched: Boolean = false
)

class SearchViewModel(
    private val getSubjectsUseCase: GetSubjectsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var allSubjects: List<Subject> = emptyList()

    init {
        viewModelScope.launch {
            getSubjectsUseCase().collect { subjects ->
                allSubjects = subjects
                if (_uiState.value.query.isNotBlank()) {
                    performSearch(_uiState.value.query)
                }
            }
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
        performSearch(query)
    }

    private fun performSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(subjectResults = emptyList(), hasSearched = false) }
            return
        }

        val filtered = allSubjects.filter {
            it.name.contains(trimmed, ignoreCase = true)
        }

        _uiState.update {
            it.copy(
                subjectResults = filtered,
                hasSearched = true
            )
        }
    }

    fun clearSearch() {
        _uiState.update { SearchUiState() }
    }
}
