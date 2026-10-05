package com.studyos.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.studyos.app.domain.model.LossReasonCategory
import com.studyos.app.domain.model.LostMark
import java.util.UUID

@Entity(
    tableName = "lost_marks",
    foreignKeys = [
        ForeignKey(
            entity = ExamResultEntity::class,
            parentColumns = ["id"],
            childColumns = ["resultId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = QuestionBankEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["resultId"]),
        Index(value = ["questionId"]),
        Index(value = ["topicId"]),
        Index(value = ["category"])
    ]
)
data class LostMarksEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val resultId: String,
    val questionId: String? = null,
    val topicId: String? = null,
    val marksLost: Double,
    val category: String = LossReasonCategory.CONCEPT_ERROR.name,
    val reflection: String? = null,
    val isRemediated: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

fun LostMarksEntity.toDomain(): LostMark {
    val cat = try {
        LossReasonCategory.valueOf(category)
    } catch (_: Exception) {
        LossReasonCategory.CONCEPT_ERROR
    }
    return LostMark(
        id = id,
        resultId = resultId,
        questionId = questionId,
        topicId = topicId,
        marksLost = marksLost,
        category = cat,
        reflection = reflection,
        isRemediated = isRemediated,
        createdAt = createdAt
    )
}

fun LostMark.toEntity(): LostMarksEntity {
    return LostMarksEntity(
        id = id,
        resultId = resultId,
        questionId = questionId,
        topicId = topicId,
        marksLost = marksLost,
        category = category.name,
        reflection = reflection,
        isRemediated = isRemediated,
        createdAt = createdAt
    )
}
