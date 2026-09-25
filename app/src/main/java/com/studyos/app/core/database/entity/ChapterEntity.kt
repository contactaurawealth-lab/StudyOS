package com.studyos.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.ChapterStatus
import java.util.UUID

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["subjectId"])]
)
data class ChapterEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val subjectId: String,
    val name: String,
    val description: String? = null,
    val orderIndex: Int = 0,
    val status: String = ChapterStatus.NOT_STARTED.name,
    val progress: Int = 0,
    val lastOpenedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

fun ChapterEntity.toDomain(): Chapter {
    val chapterStatus = try {
        ChapterStatus.valueOf(status)
    } catch (_: Exception) {
        ChapterStatus.NOT_STARTED
    }
    return Chapter(
        id = id,
        subjectId = subjectId,
        name = name,
        description = description,
        orderIndex = orderIndex,
        status = chapterStatus,
        progress = progress.coerceIn(0, 100),
        lastOpenedAt = lastOpenedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Chapter.toEntity(): ChapterEntity {
    return ChapterEntity(
        id = id,
        subjectId = subjectId,
        name = name,
        description = description,
        orderIndex = orderIndex,
        status = status.name,
        progress = progress.coerceIn(0, 100),
        lastOpenedAt = lastOpenedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
