package com.studyos.app.domain

import com.studyos.app.core.util.DateTimeUtils
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.model.Task
import com.studyos.app.domain.model.TaskFilter
import com.studyos.app.domain.model.TaskPriority
import com.studyos.app.domain.model.TaskStatus
import com.studyos.app.domain.repository.TaskRepository
import com.studyos.app.domain.usecase.CreateTaskUseCase
import com.studyos.app.domain.usecase.DeleteTaskUseCase
import com.studyos.app.domain.usecase.GetTasksUseCase
import com.studyos.app.domain.usecase.GetTodayTasksUseCase
import com.studyos.app.domain.usecase.ToggleTaskCompletionUseCase
import com.studyos.app.domain.usecase.UpdateTaskUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class FakeTaskRepository : TaskRepository {
    val items = mutableListOf<Task>()
    private val tasksFlow = MutableStateFlow<List<Task>>(emptyList())

    private fun sync() {
        tasksFlow.value = items.toList()
    }

    override fun observeTasks(): Flow<List<Task>> = tasksFlow

    override fun observeTodayTasks(startOfDay: Long, endOfDay: Long): Flow<List<Task>> =
        tasksFlow.map { list ->
            list.filter { it.status == TaskStatus.TODO && it.dueAt != null && it.dueAt in startOfDay..endOfDay }
        }

    override fun observeUpcomingTasks(endOfDay: Long): Flow<List<Task>> =
        tasksFlow.map { list ->
            list.filter { it.status == TaskStatus.TODO && it.dueAt != null && it.dueAt > endOfDay }
        }

    override fun observeCompletedTasks(): Flow<List<Task>> =
        tasksFlow.map { list -> list.filter { it.status == TaskStatus.COMPLETED } }

    override fun observeRelevantTasks(startOfDay: Long, endOfDay: Long, now: Long, limit: Int): Flow<List<Task>> =
        tasksFlow.map { list ->
            list.filter { it.status == TaskStatus.TODO }
                .take(limit)
        }

    override fun getTaskById(id: String): Flow<Task?> =
        tasksFlow.map { it.find { t -> t.id == id } }

    override suspend fun getTaskByIdOnce(id: String): Task? =
        items.find { it.id == id }

    override suspend fun createTask(task: Task): Task {
        items.add(task)
        sync()
        return task
    }

    override suspend fun updateTask(task: Task) {
        val index = items.indexOfFirst { it.id == task.id }
        if (index != -1) {
            items[index] = task
            sync()
        }
    }

    override suspend fun toggleTaskCompletion(id: String) {
        val index = items.indexOfFirst { it.id == id }
        if (index != -1) {
            val cur = items[index]
            val newStatus = if (cur.status == TaskStatus.COMPLETED) TaskStatus.TODO else TaskStatus.COMPLETED
            items[index] = cur.copy(status = newStatus, updatedAt = System.currentTimeMillis())
            sync()
        }
    }

    override suspend fun deleteTask(id: String) {
        items.removeAll { it.id == id }
        sync()
    }
}

class TaskUseCasesTest {

    private lateinit var taskRepository: FakeTaskRepository
    private lateinit var subjectRepository: FakeSubjectRepository
    private lateinit var chapterRepository: FakeChapterRepository

    private lateinit var createTaskUseCase: CreateTaskUseCase
    private lateinit var updateTaskUseCase: UpdateTaskUseCase
    private lateinit var toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase
    private lateinit var deleteTaskUseCase: DeleteTaskUseCase
    private lateinit var getTasksUseCase: GetTasksUseCase
    private lateinit var getTodayTasksUseCase: GetTodayTasksUseCase

    @Before
    fun setup() {
        taskRepository = FakeTaskRepository()
        subjectRepository = FakeSubjectRepository()
        chapterRepository = FakeChapterRepository()

        createTaskUseCase = CreateTaskUseCase(taskRepository)
        updateTaskUseCase = UpdateTaskUseCase(taskRepository)
        toggleTaskCompletionUseCase = ToggleTaskCompletionUseCase(taskRepository)
        deleteTaskUseCase = DeleteTaskUseCase(taskRepository)
        getTasksUseCase = GetTasksUseCase(taskRepository, subjectRepository, chapterRepository)
        getTodayTasksUseCase = GetTodayTasksUseCase(taskRepository, subjectRepository, chapterRepository)
    }

    @Test
    fun testCreateTask_success() = runTest {
        val task = createTaskUseCase(
            title = "Read chapter 3",
            description = "Pages 45-60",
            priority = TaskPriority.HIGH
        )

        assertNotNull(task.id)
        assertEquals("Read chapter 3", task.title)
        assertEquals("Pages 45-60", task.description)
        assertEquals(TaskPriority.HIGH, task.priority)
        assertEquals(TaskStatus.TODO, task.status)
        assertEquals(1, taskRepository.items.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testCreateTask_blankTitle_throws() = runTest {
        createTaskUseCase(title = "   ")
    }

    @Test
    fun testUpdateTask_success() = runTest {
        val created = createTaskUseCase(title = "Homework 1")
        updateTaskUseCase(created.copy(title = "Homework 1 - updated", priority = TaskPriority.HIGH))

        val updated = taskRepository.getTaskByIdOnce(created.id)
        assertNotNull(updated)
        assertEquals("Homework 1 - updated", updated?.title)
        assertEquals(TaskPriority.HIGH, updated?.priority)
    }

    @Test
    fun testToggleTaskCompletion() = runTest {
        val created = createTaskUseCase(title = "Essay outline")
        assertEquals(TaskStatus.TODO, created.status)

        toggleTaskCompletionUseCase(created.id)
        var current = taskRepository.getTaskByIdOnce(created.id)
        assertEquals(TaskStatus.COMPLETED, current?.status)

        toggleTaskCompletionUseCase(created.id)
        current = taskRepository.getTaskByIdOnce(created.id)
        assertEquals(TaskStatus.TODO, current?.status)
    }

    @Test
    fun testDeleteTask() = runTest {
        val created = createTaskUseCase(title = "Math problem set")
        assertEquals(1, taskRepository.items.size)

        deleteTaskUseCase(created.id)
        assertEquals(0, taskRepository.items.size)
    }

    @Test
    fun testGetTasks_filtering() = runTest {
        val today = LocalDate.now()
        val startOfDay = DateTimeUtils.getStartOfDay(today)
        val endOfDay = DateTimeUtils.getEndOfDay(today)

        val taskToday = createTaskUseCase(title = "Task Today", dueAt = startOfDay + 1000)
        val taskUpcoming = createTaskUseCase(title = "Task Upcoming", dueAt = endOfDay + 100000)
        val taskCompleted = createTaskUseCase(title = "Task Completed")
        toggleTaskCompletionUseCase(taskCompleted.id)

        // ALL filter
        val allTasks = getTasksUseCase(TaskFilter.ALL, today).first()
        assertEquals(3, allTasks.size)

        // TODAY filter
        val todayTasks = getTasksUseCase(TaskFilter.TODAY, today).first()
        assertEquals(1, todayTasks.size)
        assertEquals(taskToday.id, todayTasks.first().task.id)

        // UPCOMING filter
        val upcomingTasks = getTasksUseCase(TaskFilter.UPCOMING, today).first()
        assertEquals(1, upcomingTasks.size)
        assertEquals(taskUpcoming.id, upcomingTasks.first().task.id)

        // COMPLETED filter
        val completedTasks = getTasksUseCase(TaskFilter.COMPLETED, today).first()
        assertEquals(1, completedTasks.size)
        assertEquals(taskCompleted.id, completedTasks.first().task.id)
    }

    @Test
    fun testGetTasks_deterministicSorting() = runTest {
        val today = LocalDate.now()
        val startOfDay = DateTimeUtils.getStartOfDay(today)
        val endOfDay = DateTimeUtils.getEndOfDay(today)
        val now = System.currentTimeMillis()

        // 1. Overdue
        val overdue = createTaskUseCase(title = "Overdue task", dueAt = now - 50000)
        // 2. Due today
        val dueToday = createTaskUseCase(title = "Due today", dueAt = endOfDay - 1000)
        // 3. High priority without due date
        val highPriority = createTaskUseCase(title = "Urgent no due", priority = TaskPriority.HIGH)
        // 4. Upcoming
        val upcoming = createTaskUseCase(title = "Upcoming next week", dueAt = endOfDay + 500000)
        // 5. No due date, normal priority
        val noDue = createTaskUseCase(title = "Normal no due", priority = TaskPriority.MEDIUM)
        // 6. Completed
        val completed = createTaskUseCase(title = "Done")
        toggleTaskCompletionUseCase(completed.id)

        val items = getTasksUseCase(TaskFilter.ALL, today).first()
        assertEquals(6, items.size)
        assertEquals(overdue.id, items[0].task.id)
        assertEquals(dueToday.id, items[1].task.id)
        assertEquals(highPriority.id, items[2].task.id)
        assertEquals(upcoming.id, items[3].task.id)
        assertEquals(noDue.id, items[4].task.id)
        assertEquals(completed.id, items[5].task.id)
    }

    @Test
    fun testGetTodayTasks_respectsLimit() = runTest {
        val today = LocalDate.now()
        val startOfDay = DateTimeUtils.getStartOfDay(today)

        for (i in 1..6) {
            createTaskUseCase(title = "Task $i", dueAt = startOfDay + (i * 100))
        }

        val todayTasks = getTodayTasksUseCase(limit = 4, date = today).first()
        assertEquals(4, todayTasks.size)
    }
}
