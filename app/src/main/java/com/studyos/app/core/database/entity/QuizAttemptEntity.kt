package com.studyos.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "quiz_attempts",
    foreignKeys = [
        ForeignKey(
            entity = QuizEntity::class,
            parentColumns = ["id"],
            childColumns = ["quizId"],
            onDelete = ForeignKey.CASCADE
        ),
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
        Index(value = ["quizId"]),
        Index(value = ["subjectId"]),
        Index(value = ["chapterId"]),
        Index(value = ["startedAt"])
    ]
)
data class QuizAttemptEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val quizId: String,
    val subjectId: String,
    val chapterId: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val score: Int = 0,
    val totalQuestions: Int = 0,
    val accuracyPercentage: Int = 0,
    val timeSpentSeconds: Int = 0,
    val isCompleted: Boolean = false
)
