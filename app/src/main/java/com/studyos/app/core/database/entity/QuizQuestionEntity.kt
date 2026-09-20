package com.studyos.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "quiz_questions",
    foreignKeys = [
        ForeignKey(
            entity = QuizEntity::class,
            parentColumns = ["id"],
            childColumns = ["quizId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["quizId"]),
        Index(value = ["orderIndex"])
    ]
)
data class QuizQuestionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val quizId: String,
    val question: String,
    val optionsJson: String,
    val correctAnswer: String,
    val explanation: String? = null,
    val questionType: String = "MCQ",
    val topic: String? = null,
    val orderIndex: Int = 0
)
