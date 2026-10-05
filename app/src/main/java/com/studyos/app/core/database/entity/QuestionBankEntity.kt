package com.studyos.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.studyos.app.domain.model.QuestionDifficulty
import com.studyos.app.domain.model.ExamQuestionType
import com.studyos.app.domain.model.QuestionBankItem
import java.util.UUID

@Entity(
    tableName = "question_bank",
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
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["subjectId"]),
        Index(value = ["chapterId"]),
        Index(value = ["topicId"]),
        Index(value = ["difficulty"])
    ]
)
data class QuestionBankEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val subjectId: String,
    val chapterId: String,
    val topicId: String? = null,
    val questionText: String,
    val markingScheme: String,
    val marks: Int,
    val questionType: String = ExamQuestionType.SHORT_ANSWER.name,
    val difficulty: String = QuestionDifficulty.MEDIUM.name,
    val usageCount: Int = 0,
    val lastTestedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

fun QuestionBankEntity.toDomain(): QuestionBankItem {
    val qType = try {
        ExamQuestionType.valueOf(questionType)
    } catch (_: Exception) {
        ExamQuestionType.SHORT_ANSWER
    }
    val diff = try {
        QuestionDifficulty.valueOf(difficulty)
    } catch (_: Exception) {
        QuestionDifficulty.MEDIUM
    }
    return QuestionBankItem(
        id = id,
        subjectId = subjectId,
        chapterId = chapterId,
        topicId = topicId,
        questionText = questionText,
        markingScheme = markingScheme,
        marks = marks,
        questionType = qType,
        difficulty = diff,
        usageCount = usageCount,
        lastTestedAt = lastTestedAt,
        createdAt = createdAt
    )
}

fun QuestionBankItem.toEntity(): QuestionBankEntity {
    return QuestionBankEntity(
        id = id,
        subjectId = subjectId,
        chapterId = chapterId,
        topicId = topicId,
        questionText = questionText,
        markingScheme = markingScheme,
        marks = marks,
        questionType = questionType.name,
        difficulty = difficulty.name,
        usageCount = usageCount,
        lastTestedAt = lastTestedAt,
        createdAt = createdAt
    )
}
