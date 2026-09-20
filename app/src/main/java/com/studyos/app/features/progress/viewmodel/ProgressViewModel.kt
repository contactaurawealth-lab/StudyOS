package com.studyos.app.features.progress.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.domain.usecase.AcademicProgress
import com.studyos.app.domain.usecase.GetAcademicProgressUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProgressUiState(
    val academicProgress: AcademicProgress = AcademicProgress(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class ProgressViewModel(
    private val getAcademicProgressUseCase: GetAcademicProgressUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        loadProgress()
    }

    private fun loadProgress() {
        viewModelScope.launch {
            getAcademicProgressUseCase()
                .catch {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = "Couldn't load progress. Try again."
                        )
                    }
                }
                .collect { progress ->
                    _uiState.update {
                        it.copy(
                            academicProgress = progress,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }
}
