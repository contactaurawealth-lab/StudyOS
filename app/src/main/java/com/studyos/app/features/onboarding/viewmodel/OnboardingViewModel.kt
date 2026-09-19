package com.studyos.app.features.onboarding.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.studyos.app.core.datastore.StudyOSPreferencesDataSource
import com.studyos.app.domain.model.Student
import com.studyos.app.domain.model.StudyPreferences
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.usecase.AddSubjectResult
import com.studyos.app.domain.usecase.AddSubjectUseCase
import com.studyos.app.domain.usecase.SaveStudentUseCase
import com.studyos.app.domain.usecase.SaveStudyPreferencesUseCase
import com.studyos.app.domain.usecase.SaveSubjectsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    // Student Profile
    val name: String = "",
    val nameError: String? = null,
    val classLevel: String = "Grade 9",
    val division: String = "",
    val schoolName: String = "",

    // Subject Selection
    val suggestedSubjects: List<String> = defaultSuggestedSubjects,
    val customSubjects: List<String> = emptyList(),
    val selectedSubjects: Set<String> = setOf("Mathematics", "Science", "English"),
    val subjectsError: String? = null,
    val customSubjectError: String? = null,

    // Study Preferences
    val dailyStudyGoalMinutes: Int = 60,
    val defaultSessionMinutes: Int = 45,
    val preferredStartMinutes: Int? = null,
    val preferredEndMinutes: Int? = null,

    // Status
    val isLoading: Boolean = false,
    val saveError: String? = null,
    val isCompleted: Boolean = false
) {
    val allAvailableSubjects: List<String>
        get() = suggestedSubjects + customSubjects
}

val defaultSuggestedSubjects = listOf(
    "Mathematics",
    "Science",
    "English",
    "History",
    "Geography",
    "Computer Science",
    "Hindi",
    "Marathi",
    "Commerce",
    "Physics",
    "Chemistry",
    "Biology"
)

val GradeOptions = listOf(
    "Grade 6",
    "Grade 7",
    "Grade 8",
    "Grade 9",
    "Grade 10",
    "Grade 11",
    "Grade 12",
    "Other"
)

val DailyGoalOptions = listOf(30, 60, 90, 120, 180)
val SessionLengthOptions = listOf(25, 45, 60, 90)

class OnboardingViewModel(
    private val saveStudentUseCase: SaveStudentUseCase,
    private val saveSubjectsUseCase: SaveSubjectsUseCase,
    private val addSubjectUseCase: AddSubjectUseCase,
    private val saveStudyPreferencesUseCase: SaveStudyPreferencesUseCase,
    private val preferencesDataSource: com.studyos.app.core.datastore.PreferencesDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onNameChanged(name: String) {
        _uiState.update {
            it.copy(
                name = name,
                nameError = if (name.isNotBlank()) null else it.nameError
            )
        }
    }

    fun onClassLevelChanged(grade: String) {
        _uiState.update { current ->
            // Adapt subjects where practical based on grade
            val adapted = if (grade == "Grade 11" || grade == "Grade 12") {
                listOf(
                    "Physics", "Chemistry", "Mathematics", "Biology",
                    "English", "Computer Science", "Commerce", "Economics", "History"
                )
            } else {
                defaultSuggestedSubjects
            }
            current.copy(classLevel = grade, suggestedSubjects = adapted)
        }
    }

    fun onDivisionChanged(division: String) {
        _uiState.update { it.copy(division = division) }
    }

    fun onSchoolNameChanged(school: String) {
        _uiState.update { it.copy(schoolName = school) }
    }

    fun onSubjectToggled(subjectName: String) {
        _uiState.update { current ->
            val updated = current.selectedSubjects.toMutableSet()
            if (updated.contains(subjectName)) {
                updated.remove(subjectName)
            } else {
                updated.add(subjectName)
            }
            current.copy(
                selectedSubjects = updated,
                subjectsError = if (updated.isNotEmpty()) null else current.subjectsError
            )
        }
    }

    fun addCustomSubject(subjectName: String): Boolean {
        val trimmed = subjectName.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(customSubjectError = "Subject name cannot be empty.") }
            return false
        }

        val allCurrent = _uiState.value.allAvailableSubjects
        if (allCurrent.any { it.equals(trimmed, ignoreCase = true) }) {
            _uiState.update { it.copy(customSubjectError = "That subject already exists.") }
            return false
        }

        _uiState.update { current ->
            val newCustom = current.customSubjects + trimmed
            val newSelected = current.selectedSubjects + trimmed
            current.copy(
                customSubjects = newCustom,
                selectedSubjects = newSelected,
                customSubjectError = null,
                subjectsError = null
            )
        }
        return true
    }

    fun clearCustomSubjectError() {
        _uiState.update { it.copy(customSubjectError = null) }
    }

    fun onDailyGoalChanged(minutes: Int) {
        _uiState.update { it.copy(dailyStudyGoalMinutes = minutes) }
    }

    fun onDefaultSessionChanged(minutes: Int) {
        _uiState.update { it.copy(defaultSessionMinutes = minutes) }
    }

    fun onPreferredHoursChanged(startMin: Int?, endMin: Int?) {
        _uiState.update {
            it.copy(
                preferredStartMinutes = startMin,
                preferredEndMinutes = endMin
            )
        }
    }

    fun validateProfile(): Boolean {
        val name = _uiState.value.name.trim()
        return if (name.isEmpty()) {
            _uiState.update { it.copy(nameError = "Please enter your name.") }
            false
        } else {
            _uiState.update { it.copy(nameError = null) }
            true
        }
    }

    fun validateSubjects(): Boolean {
        return if (_uiState.value.selectedSubjects.isEmpty()) {
            _uiState.update { it.copy(subjectsError = "Please select at least one subject.") }
            false
        } else {
            _uiState.update { it.copy(subjectsError = null) }
            true
        }
    }

    fun finishSetup(onSuccess: () -> Unit) {
        val state = _uiState.value
        _uiState.update { it.copy(isLoading = true, saveError = null) }

        viewModelScope.launch {
            try {
                val student = Student(
                    name = state.name.trim(),
                    classLevel = state.classLevel,
                    division = state.division.trim().ifEmpty { null },
                    schoolName = state.schoolName.trim().ifEmpty { null }
                )
                saveStudentUseCase(student)

                val subjectsToSave = state.selectedSubjects.map { name ->
                    val isCustom = state.customSubjects.contains(name)
                    Subject(name = name, isCustom = isCustom)
                }
                saveSubjectsUseCase(subjectsToSave)

                val preferences = StudyPreferences(
                    dailyStudyGoalMinutes = state.dailyStudyGoalMinutes,
                    defaultSessionMinutes = state.defaultSessionMinutes,
                    preferredStartMinutes = state.preferredStartMinutes,
                    preferredEndMinutes = state.preferredEndMinutes
                )
                saveStudyPreferencesUseCase(preferences)

                preferencesDataSource.setOnboardingCompleted(true)

                _uiState.update { it.copy(isLoading = false, isCompleted = true) }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        saveError = "Couldn't save your setup.\nTry again."
                    )
                }
            }
        }
    }
}
