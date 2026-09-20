package com.studyos.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "flashcards",
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
        Index(value = ["nextReview"])
    ]
)
data class FlashcardEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val subjectId: String,
    val chapterId: String? = null,
    val question: String,
    val answer: String,
    val difficulty: String = "MEDIUM",
    val createdAt: Long = System.currentTimeMillis(),
    val lastReviewed: Long? = null,
    val nextReview: Long? = null,
    val reviewCount: Int = 0,
    val intervalDays: Int = 0,
    val easeFactor: Float = 2.5f
)
