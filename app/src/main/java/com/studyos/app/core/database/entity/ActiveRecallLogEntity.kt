package com.studyos.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "active_recall_logs",
    indices = [
        Index("completedAt"),
        Index("sessionType")
    ]
)
data class ActiveRecallLogEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val sessionType: String,
    val durationSeconds: Int,
    val completedAt: Long = System.currentTimeMillis(),
    val itemsReviewedCount: Int,
    val correctCount: Int,
    val accuracyPercentage: Int,
    val streakDays: Int
)
