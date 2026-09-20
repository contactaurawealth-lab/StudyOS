package com.studyos.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.studyos.app.domain.model.Task
import com.studyos.app.domain.model.TaskPriority
import com.studyos.app.domain.model.TaskStatus
import java.util.UUID

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["subjectId"]),
        Index(value = ["chapterId"]),
        Index(value = ["dueAt"]),
        Index(value = ["status"])
    ]
)
data class TaskEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String? = null,
    val subjectId: String? = null,
    val chapterId: String? = null,
    val dueAt: Long? = null,
    val priority: String = TaskPriority.MEDIUM.name,
    val status: String = TaskStatus.TODO.name,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

fun TaskEntity.toDomain(): Task {
    val taskPriority = try {
        TaskPriority.valueOf(priority)
    } catch (_: Exception) {
        TaskPriority.MEDIUM
    }
    val taskStatus = try {
        TaskStatus.valueOf(status)
    } catch (_: Exception) {
        TaskStatus.TODO
    }
    return Task(
        id = id,
        title = title,
        description = description,
        subjectId = subjectId,
        chapterId = chapterId,
        dueAt = dueAt,
        priority = taskPriority,
        status = taskStatus,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = id,
        title = title,
        description = description,
        subjectId = subjectId,
        chapterId = chapterId,
        dueAt = dueAt,
        priority = priority.name,
        status = status.name,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
