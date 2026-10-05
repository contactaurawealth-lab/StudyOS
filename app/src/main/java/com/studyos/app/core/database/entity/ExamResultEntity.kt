package com.studyos.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.studyos.app.domain.model.ExamResult
import java.util.UUID

@Entity(
    tableName = "exam_results",
    foreignKeys = [
        ForeignKey(
            entity = PaperEntity::class,
            parentColumns = ["id"],
            childColumns = ["paperId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["paperId"]),
        Index(value = ["examDate"])
    ]
)
data class ExamResultEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val paperId: String,
    val marksObtained: Double,
    val totalMarks: Double,
    val percentage: Double,
    val timeTakenMinutes: Int,
    val notesOrFeedback: String? = null,
    val examDate: Long = System.currentTimeMillis()
)

fun ExamResultEntity.toDomain(): ExamResult {
    return ExamResult(
        id = id,
        paperId = paperId,
        marksObtained = marksObtained,
        totalMarks = totalMarks,
        percentage = percentage,
        timeTakenMinutes = timeTakenMinutes,
        notesOrFeedback = notesOrFeedback,
        examDate = examDate
    )
}

fun ExamResult.toEntity(): ExamResultEntity {
    return ExamResultEntity(
        id = id,
        paperId = paperId,
        marksObtained = marksObtained,
        totalMarks = totalMarks,
        percentage = percentage,
        timeTakenMinutes = timeTakenMinutes,
        notesOrFeedback = notesOrFeedback,
        examDate = examDate
    )
}
