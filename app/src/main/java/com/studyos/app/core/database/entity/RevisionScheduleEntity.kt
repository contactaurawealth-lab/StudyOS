package com.studyos.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "revision_schedules",
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("chapterId", unique = true),
        Index("subjectId"),
        Index("nextRevisionDue")
    ]
)
data class RevisionScheduleEntity(
    @PrimaryKey
    val chapterId: String,
    val subjectId: String,
    val intervalDays: Int = 1,
    val revisionCount: Int = 0,
    val lastRevisedAt: Long? = null,
    val nextRevisionDue: Long,
    val easeFactor: Float = 2.5f
)
