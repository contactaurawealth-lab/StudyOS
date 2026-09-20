package com.studyos.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "active_quiz_states")
data class ActiveQuizStateEntity(
    @PrimaryKey
    val quizId: String,
    val currentQuestionIndex: Int = 0,
    val answersJson: String = "{}",
    val elapsedSeconds: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)
