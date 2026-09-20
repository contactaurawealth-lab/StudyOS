package com.studyos.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "flashcard_reviews",
    foreignKeys = [
        ForeignKey(
            entity = FlashcardEntity::class,
            parentColumns = ["id"],
            childColumns = ["flashcardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["flashcardId"]),
        Index(value = ["reviewedAt"])
    ]
)
data class FlashcardReviewEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val flashcardId: String,
    val rating: String,
    val reviewedAt: Long = System.currentTimeMillis(),
    val intervalAfterDays: Int
)
