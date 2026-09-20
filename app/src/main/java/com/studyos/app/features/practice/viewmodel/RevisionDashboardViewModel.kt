package com.studyos.app.features.practice.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.domain.model.ActiveRecallSessionType
import com.studyos.app.domain.model.DueRevisionDashboardData
import com.studyos.app.domain.repository.RevisionRepository
import com.studyos.app.domain.usecase.GetDueRevisionDashboardUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RevisionDashboardUiState(
    val dashboardData: DueRevisionDashboardData = DueRevisionDashboardData(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class RevisionDashboardViewModel(
    private val getDueRevisionDashboardUseCase: GetDueRevisionDashboardUseCase,
    private val revisionRepository: RevisionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RevisionDashboardUiState())
    val uiState: StateFlow<RevisionDashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
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

    fun setDailyTargetMinutes(minutes: Int) {
        viewModelScope.launch {
            revisionRepository.setDailyRevisionTargetMinutes(minutes)
        }
    }
}
