package com.studyos.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.studyos.app.domain.model.StudySession
import com.studyos.app.domain.model.StudySessionStatus
import java.util.UUID

@Entity(
    tableName = "study_sessions",
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
        Index(value = ["scheduledStart"])
    ]
)
data class StudySessionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val subjectId: String? = null,
    val chapterId: String? = null,
    val title: String,
    val scheduledStart: Long? = null,
    val scheduledEnd: Long? = null,
    val plannedMinutes: Int,
    val actualMinutes: Int = 0,
    val status: String = StudySessionStatus.PLANNED.name,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

fun StudySessionEntity.toDomain(): StudySession {
    val sessionStatus = try {
        StudySessionStatus.valueOf(status)
    } catch (_: Exception) {
        StudySessionStatus.PLANNED
    }
    return StudySession(
        id = id,
        subjectId = subjectId,
        chapterId = chapterId,
        title = title,
        scheduledStart = scheduledStart,
        scheduledEnd = scheduledEnd,
        plannedMinutes = plannedMinutes.coerceAtLeast(1),
        actualMinutes = actualMinutes.coerceAtLeast(0),
        status = sessionStatus,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun StudySession.toEntity(): StudySessionEntity {
    return StudySessionEntity(
        id = id,
        subjectId = subjectId,
        chapterId = chapterId,
        title = title,
        scheduledStart = scheduledStart,
        scheduledEnd = scheduledEnd,
        plannedMinutes = plannedMinutes.coerceAtLeast(1),
        actualMinutes = actualMinutes.coerceAtLeast(0),
        status = status.name,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
