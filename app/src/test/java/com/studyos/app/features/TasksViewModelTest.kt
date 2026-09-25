package com.studyos.app.features

import com.studyos.app.domain.FakeChapterRepository
import com.studyos.app.domain.FakeSubjectRepository
import com.studyos.app.domain.FakeTaskRepository
import com.studyos.app.domain.model.TaskFilter
import com.studyos.app.domain.model.TaskPriority
import com.studyos.app.domain.model.TaskStatus
import com.studyos.app.domain.usecase.CreateTaskUseCase
import com.studyos.app.domain.usecase.DeleteTaskUseCase
import com.studyos.app.domain.usecase.GetChaptersForSubjectUseCase
import com.studyos.app.domain.usecase.GetSubjectsUseCase
import com.studyos.app.domain.usecase.GetTasksUseCase
import com.studyos.app.domain.usecase.ToggleTaskCompletionUseCase
import com.studyos.app.domain.usecase.UpdateTaskUseCase
import com.studyos.app.features.tasks.viewmodel.TasksViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var taskRepository: FakeTaskRepository
    private lateinit var subjectRepository: FakeSubjectRepository
    private lateinit var chapterRepository: FakeChapterRepository

    private lateinit var getTasksUseCase: GetTasksUseCase
    private lateinit var createTaskUseCase: CreateTaskUseCase
    private lateinit var updateTaskUseCase: UpdateTaskUseCase
    private lateinit var toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase
    private lateinit var deleteTaskUseCase: DeleteTaskUseCase
    private lateinit var getSubjectsUseCase: GetSubjectsUseCase
    private lateinit var getChaptersForSubjectUseCase: GetChaptersForSubjectUseCase

    private lateinit var viewModel: TasksViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        taskRepository = FakeTaskRepository()
        subjectRepository = FakeSubjectRepository()
        chapterRepository = FakeChapterRepository()

        getTasksUseCase = GetTasksUseCase(taskRepository, subjectRepository, chapterRepository)
        createTaskUseCase = CreateTaskUseCase(taskRepository)
        updateTaskUseCase = UpdateTaskUseCase(taskRepository)
        toggleTaskCompletionUseCase = ToggleTaskCompletionUseCase(taskRepository)
        deleteTaskUseCase = DeleteTaskUseCase(taskRepository)
        getSubjectsUseCase = GetSubjectsUseCase(subjectRepository)
        getChaptersForSubjectUseCase = GetChaptersForSubjectUseCase(chapterRepository)

        viewModel = TasksViewModel(
            getTasksUseCase = getTasksUseCase,
            createTaskUseCase = createTaskUseCase,
            updateTaskUseCase = updateTaskUseCase,
            toggleTaskCompletionUseCase = toggleTaskCompletionUseCase,
            deleteTaskUseCase = deleteTaskUseCase,
            getSubjectsUseCase = getSubjectsUseCase,
            getChaptersForSubjectUseCase = getChaptersForSubjectUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState_empty() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(0, state.tasks.size)
        assertEquals(TaskFilter.ALL, state.filter)
    }

    @Test
    fun testQuickAddTask() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()
        viewModel.quickAddTask("Review calculus notes")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.tasks.size)
        assertEquals("Review calculus notes", state.tasks.first().task.title)
        assertEquals(TaskStatus.TODO, state.tasks.first().task.status)
    }

    @Test
    fun testToggleTaskCompletion() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()
        viewModel.quickAddTask("Complete practice exam")
        advanceUntilIdle()

        val taskId = viewModel.uiState.value.tasks.first().task.id
        viewModel.toggleTaskCompletion(taskId)
        advanceUntilIdle()

        val updated = viewModel.uiState.value.tasks.first()
        assertEquals(TaskStatus.COMPLETED, updated.task.status)
    }

    @Test
    fun testSetFilter() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()
        viewModel.quickAddTask("Task 1")
        advanceUntilIdle()

        val taskId = viewModel.uiState.value.tasks.first().task.id
        viewModel.toggleTaskCompletion(taskId)
        advanceUntilIdle()

        // Filter COMPLETED
        viewModel.setFilter(TaskFilter.COMPLETED)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.tasks.size)

        // Filter TODAY (has no due date, so not in today)
        viewModel.setFilter(TaskFilter.TODAY)
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.tasks.size)
    }

    @Test
    fun testDeleteTask() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()
        viewModel.quickAddTask("Temp task")
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.tasks.size)

        val taskId = viewModel.uiState.value.tasks.first().task.id
        viewModel.deleteTask(taskId)
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.tasks.size)
    }
}
