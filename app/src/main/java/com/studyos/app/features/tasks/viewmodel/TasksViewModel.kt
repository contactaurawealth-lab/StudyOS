package com.studyos.app.features.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.core.util.DateTimeUtils
import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.model.Task
import com.studyos.app.domain.model.TaskFilter
import com.studyos.app.domain.model.TaskItem
import com.studyos.app.domain.model.TaskPriority
import com.studyos.app.domain.usecase.CreateTaskUseCase
import com.studyos.app.domain.usecase.DeleteTaskUseCase
import com.studyos.app.domain.usecase.GetChaptersForSubjectUseCase
import com.studyos.app.domain.usecase.GetSubjectsUseCase
import com.studyos.app.domain.usecase.GetTasksUseCase
import com.studyos.app.domain.usecase.ToggleTaskCompletionUseCase
import com.studyos.app.domain.usecase.UpdateTaskUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TasksUiState(
    val tasks: List<TaskItem> = emptyList(),
    val filter: TaskFilter = TaskFilter.ALL,
    val subjects: List<Subject> = emptyList(),
    val editingTask: Task? = null,
    val isAddTaskSheetOpen: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModel(
    private val getTasksUseCase: GetTasksUseCase,
    private val createTaskUseCase: CreateTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val getSubjectsUseCase: GetSubjectsUseCase,
    private val getChaptersForSubjectUseCase: GetChaptersForSubjectUseCase
) : ViewModel() {

    private val _filter = MutableStateFlow(TaskFilter.ALL)
    private val _editingTask = MutableStateFlow<Task?>(null)
    private val _isAddTaskSheetOpen = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TasksUiState> = combine(
        _filter.flatMapLatest { filter -> getTasksUseCase(filter) },
        _filter,
        getSubjectsUseCase(),
        _editingTask,
        _isAddTaskSheetOpen,
        _errorMessage
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val tasks = (args[0] as? List<TaskItem>) ?: emptyList()
        val filter = (args[1] as? TaskFilter) ?: TaskFilter.ALL
        @Suppress("UNCHECKED_CAST")
        val subjects = (args[2] as? List<Subject>) ?: emptyList()
        val editingTask = args[3] as? Task
        val isAddOpen = (args[4] as? Boolean) ?: false
        val error = args[5] as? String

        TasksUiState(
            tasks = tasks,
            filter = filter,
            subjects = subjects,
            editingTask = editingTask,
            isAddTaskSheetOpen = isAddOpen,
            isLoading = false,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TasksUiState()
    )

    fun setFilter(filter: TaskFilter) {
        _filter.value = filter
    }

    fun openAddTaskSheet() {
        _isAddTaskSheetOpen.value = true
    }

    fun closeAddTaskSheet() {
        _isAddTaskSheetOpen.value = false
    }

    fun openEditTask(task: Task) {
        _editingTask.value = task
    }

    fun closeEditTask() {
        _editingTask.value = null
    }

    fun quickAddTask(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            try {
                val dueToday = DateTimeUtils.getEndOfDay(LocalDate.now())
                createTaskUseCase(
                    title = title,
                    dueAt = dueToday
                )
            } catch (e: Exception) {
                _errorMessage.value = "Couldn't save this task."
            }
        }
    }

    fun addTask(
        title: String,
        description: String?,
        subjectId: String?,
        chapterId: String?,
        dueAt: Long?,
        priority: TaskPriority
    ) {
        viewModelScope.launch {
            try {
                createTaskUseCase(
                    title = title,
                    description = description,
                    subjectId = subjectId,
                    chapterId = chapterId,
                    dueAt = dueAt,
                    priority = priority
                )
                _isAddTaskSheetOpen.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Couldn't save this task."
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            try {
                updateTaskUseCase(task)
                _editingTask.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Couldn't update this task."
            }
        }
    }

    fun toggleTaskCompletion(taskId: String) {
        viewModelScope.launch {
            try {
                toggleTaskCompletionUseCase(taskId)
            } catch (e: Exception) {
                _errorMessage.value = "Couldn't update task."
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                deleteTaskUseCase(taskId)
                _editingTask.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Couldn't delete this task."
            }
        }
    }

    suspend fun getChaptersForSubject(subjectId: String): List<Chapter> {
        return getChaptersForSubjectUseCase.getOnce(subjectId)
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
