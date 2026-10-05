package com.studyos.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.studyos.app.domain.model.ExamRelevance
import com.studyos.app.domain.model.Topic
import com.studyos.app.domain.model.TopicMasteryState
import java.util.UUID

@Entity(
    tableName = "topics",
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["chapterId"]),
        Index(value = ["masteryState"])
    ]
)
data class TopicEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val chapterId: String,
    val name: String,
    val masteryState: String = TopicMasteryState.NOT_STARTED.name,
    val examRelevance: String = ExamRelevance.MEDIUM.name,
    val weaknessScore: Float = 0.0f,
    val lastRevisedAt: Long? = null,
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

fun TopicEntity.toDomain(): Topic {
    val state = try {
        TopicMasteryState.valueOf(masteryState)
    } catch (_: Exception) {
        TopicMasteryState.NOT_STARTED
    }
    val relevance = try {
        ExamRelevance.valueOf(examRelevance)
    } catch (_: Exception) {
        ExamRelevance.MEDIUM
    }
    return Topic(
        id = id,
        chapterId = chapterId,
        name = name,
        masteryState = state,
        examRelevance = relevance,
        weaknessScore = weaknessScore,
        lastRevisedAt = lastRevisedAt,
        orderIndex = orderIndex,
        createdAt = createdAt
    )
}

fun Topic.toEntity(): TopicEntity {
    return TopicEntity(
        id = id,
        chapterId = chapterId,
        name = name,
        masteryState = masteryState.name,
        examRelevance = examRelevance.name,
        weaknessScore = weaknessScore,
        lastRevisedAt = lastRevisedAt,
        orderIndex = orderIndex,
        createdAt = createdAt
    )
}
