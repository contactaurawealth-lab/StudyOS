package com.studyos.app.domain.model

import java.time.LocalDate
import java.util.UUID

enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH
}

enum class TaskStatus {
    TODO,
    COMPLETED
}

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String? = null,
    val subjectId: String? = null,
    val chapterId: String? = null,
    val dueAt: Long? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.TODO,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class TaskItem(
    val task: Task,
    val subjectName: String? = null,
    val chapterName: String? = null,
    val isOverdue: Boolean = false,
    val isDueToday: Boolean = false
)

enum class TaskFilter {
    ALL,
    TODAY,
    UPCOMING,
    COMPLETED
}

enum class PlannerViewMode {
    WEEK,
    DAY,
    LIST
}

data class CalendarDay(
    val date: LocalDate,
    val isSelected: Boolean,
    val isToday: Boolean,
    val sessionCount: Int = 0
)
