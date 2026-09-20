package com.studyos.app.features.search.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.ChapterSearchResult
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.repository.ChapterRepository
import com.studyos.app.domain.usecase.GetSubjectsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val subjectResults: List<Subject> = emptyList(),
    val chapterResults: List<ChapterSearchResult> = emptyList(),
    val hasSearched: Boolean = false
)

class SearchViewModel(
    private val getSubjectsUseCase: GetSubjectsUseCase,
    private val chapterRepository: ChapterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var allSubjects: List<Subject> = emptyList()
    private var allChapters: List<Chapter> = emptyList()

    init {
        viewModelScope.launch {
            combine(
                getSubjectsUseCase(),
                chapterRepository.observeAllChapters()
            ) { subjects, chapters ->
                Pair(subjects, chapters)
            }.collect { (subjects, chapters) ->
                allSubjects = subjects
                allChapters = chapters
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
            _uiState.update {
                it.copy(
                    subjectResults = emptyList(),
                    chapterResults = emptyList(),
                    hasSearched = false
                )
            }
            return
        }

        val matchingSubjects = allSubjects.filter {
            it.name.contains(trimmed, ignoreCase = true)
        }

        val subjectMap = allSubjects.associateBy { it.id }
        val matchingChapters = allChapters.filter { chapter ->
            chapter.name.contains(trimmed, ignoreCase = true) ||
                (chapter.description?.contains(trimmed, ignoreCase = true) == true)
        }.map { chapter ->
            val subjectName = subjectMap[chapter.subjectId]?.name ?: "Subject"
            ChapterSearchResult(
                chapter = chapter,
                subjectName = subjectName
            )
        }

        _uiState.update {
            it.copy(
                subjectResults = matchingSubjects,
                chapterResults = matchingChapters,
                hasSearched = true
            )
        }
    }

    fun clearSearch() {
        _uiState.update { SearchUiState() }
    }
}
