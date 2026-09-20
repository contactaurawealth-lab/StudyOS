package com.studyos.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "question_results",
    foreignKeys = [
        ForeignKey(
            entity = QuizAttemptEntity::class,
            parentColumns = ["id"],
            childColumns = ["attemptId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["attemptId"]),
        Index(value = ["questionId"])
    ]
)
data class QuestionResultEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val attemptId: String,
    val questionId: String,
    val studentAnswer: String,
    val isCorrect: Boolean,
    val topic: String? = null
)
