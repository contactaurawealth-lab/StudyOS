package com.studyos.app.domain.usecase

import com.studyos.app.core.util.DateTimeUtils
import com.studyos.app.domain.model.Task
import com.studyos.app.domain.model.TaskFilter
import com.studyos.app.domain.model.TaskItem
import com.studyos.app.domain.model.TaskPriority
import com.studyos.app.domain.model.TaskStatus
import com.studyos.app.domain.repository.ChapterRepository
import com.studyos.app.domain.repository.SubjectRepository
import com.studyos.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

class GetTasksUseCase(
    private val taskRepository: TaskRepository,
    private val subjectRepository: SubjectRepository,
    private val chapterRepository: ChapterRepository
) {
    operator fun invoke(
        filter: TaskFilter = TaskFilter.ALL,
        date: LocalDate = LocalDate.now()
    ): Flow<List<TaskItem>> {
        val startOfDay = DateTimeUtils.getStartOfDay(date)
        val endOfDay = DateTimeUtils.getEndOfDay(date)

        return combine(
            taskRepository.observeTasks(),
            subjectRepository.getAllSubjects(),
            chapterRepository.observeAllChapters()
        ) { tasks, subjects, chapters ->
            val now = System.currentTimeMillis()
            val subjectMap = subjects.associateBy { it.id }
            val chapterMap = chapters.associateBy { it.id }

            val filteredTasks = when (filter) {
                TaskFilter.ALL -> tasks
                TaskFilter.TODAY -> tasks.filter { task ->
                    task.status == TaskStatus.TODO && task.dueAt != null &&
                        (task.dueAt in startOfDay..endOfDay || task.dueAt < now)
                }
                TaskFilter.UPCOMING -> tasks.filter { task ->
                    task.status == TaskStatus.TODO && task.dueAt != null && task.dueAt > endOfDay
                }
                TaskFilter.COMPLETED -> tasks.filter { task ->
                    task.status == TaskStatus.COMPLETED
                }
            }

            filteredTasks
                .sortedWith { t1, t2 ->
                    val rank1 = taskSortRank(t1, now, startOfDay, endOfDay)
                    val rank2 = taskSortRank(t2, now, startOfDay, endOfDay)
                    if (rank1 != rank2) {
                        rank1.compareTo(rank2)
                    } else {
                        val due1 = t1.dueAt ?: Long.MAX_VALUE
                        val due2 = t2.dueAt ?: Long.MAX_VALUE
                        if (due1 != due2) {
                            due1.compareTo(due2)
                        } else {
                            t1.createdAt.compareTo(t2.createdAt)
                        }
                    }
                }
                .map { task ->
                    val isOverdue = task.status != TaskStatus.COMPLETED &&
                        task.dueAt != null && task.dueAt < now
                    val isDueToday = task.status != TaskStatus.COMPLETED &&
                        task.dueAt != null && task.dueAt in startOfDay..endOfDay

                    TaskItem(
                        task = task,
                        subjectName = task.subjectId?.let { subjectMap[it]?.name },
                        chapterName = task.chapterId?.let { chapterMap[it]?.name },
                        isOverdue = isOverdue,
                        isDueToday = isDueToday
                    )
                }
        }
    }

    private fun taskSortRank(task: Task, now: Long, startOfDay: Long, endOfDay: Long): Int {
        if (task.status == TaskStatus.COMPLETED) return 6
        val dueAt = task.dueAt
        return when {
            dueAt != null && dueAt < now -> 1 // 1. Overdue
            dueAt != null && dueAt in startOfDay..endOfDay -> 2 // 2. Due today
            task.priority == TaskPriority.HIGH -> 3 // 3. High priority
            dueAt != null && dueAt > endOfDay -> 4 // 4. Upcoming
            else -> 5 // 5. No due date
        }
    }
}

class GetTodayTasksUseCase(
    private val taskRepository: TaskRepository,
    private val subjectRepository: SubjectRepository,
    private val chapterRepository: ChapterRepository
) {
    operator fun invoke(limit: Int = 4, date: LocalDate = LocalDate.now()): Flow<List<TaskItem>> {
        val startOfDay = DateTimeUtils.getStartOfDay(date)
        val endOfDay = DateTimeUtils.getEndOfDay(date)

        return combine(
            taskRepository.observeTasks(),
            subjectRepository.getAllSubjects(),
            chapterRepository.observeAllChapters()
        ) { tasks, subjects, chapters ->
            val now = System.currentTimeMillis()
            val subjectMap = subjects.associateBy { it.id }
            val chapterMap = chapters.associateBy { it.id }

            tasks
                .filter { it.status == TaskStatus.TODO }
                .sortedWith { t1, t2 ->
                    val rank1 = rankForToday(t1, now, startOfDay, endOfDay)
                    val rank2 = rankForToday(t2, now, startOfDay, endOfDay)
                    if (rank1 != rank2) {
                        rank1.compareTo(rank2)
                    } else {
                        val due1 = t1.dueAt ?: Long.MAX_VALUE
                        val due2 = t2.dueAt ?: Long.MAX_VALUE
                        if (due1 != due2) due1.compareTo(due2) else t1.createdAt.compareTo(t2.createdAt)
                    }
                }
                .take(limit)
                .map { task ->
                    val isOverdue = task.dueAt != null && task.dueAt < now
                    val isDueToday = task.dueAt != null && task.dueAt in startOfDay..endOfDay

                    TaskItem(
                        task = task,
                        subjectName = task.subjectId?.let { subjectMap[it]?.name },
                        chapterName = task.chapterId?.let { chapterMap[it]?.name },
                        isOverdue = isOverdue,
                        isDueToday = isDueToday
                    )
                }
        }
    }

    private fun rankForToday(task: Task, now: Long, startOfDay: Long, endOfDay: Long): Int {
        val dueAt = task.dueAt
        return when {
            dueAt != null && dueAt < now -> 1
            dueAt != null && dueAt in startOfDay..endOfDay -> 2
            task.priority == TaskPriority.HIGH -> 3
            dueAt != null -> 4
            else -> 5
        }
    }
}

class CreateTaskUseCase(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(
        title: String,
        description: String? = null,
        subjectId: String? = null,
        chapterId: String? = null,
        dueAt: Long? = null,
        priority: TaskPriority = TaskPriority.MEDIUM
    ): Task {
        val trimmed = title.trim()
        require(trimmed.isNotBlank()) { "Task title cannot be empty" }

        val task = Task(
            title = trimmed,
            description = description?.trim()?.ifBlank { null },
            subjectId = subjectId,
            chapterId = chapterId,
            dueAt = dueAt,
            priority = priority,
            status = TaskStatus.TODO
        )
        return taskRepository.createTask(task)
    }
}

class UpdateTaskUseCase(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(task: Task) {
        val trimmed = task.title.trim()
        require(trimmed.isNotBlank()) { "Task title cannot be empty" }
        taskRepository.updateTask(task.copy(title = trimmed))
    }
}

class ToggleTaskCompletionUseCase(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(taskId: String) {
        taskRepository.toggleTaskCompletion(taskId)
    }
}

class DeleteTaskUseCase(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(taskId: String) {
        taskRepository.deleteTask(taskId)
    }
}
