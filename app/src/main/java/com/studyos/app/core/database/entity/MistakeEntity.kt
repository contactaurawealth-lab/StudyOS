package com.studyos.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "mistakes",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
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
        Index(value = ["isResolved"]),
        Index(value = ["lastMissedAt"])
    ]
)
data class MistakeEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val question: String,
    val studentAnswer: String,
    val correctAnswer: String,
    val explanation: String? = null,
    val subjectId: String,
    val chapterId: String? = null,
    val topic: String? = null,
    val missedCount: Int = 1,
    val lastMissedAt: Long = System.currentTimeMillis(),
    val isResolved: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
