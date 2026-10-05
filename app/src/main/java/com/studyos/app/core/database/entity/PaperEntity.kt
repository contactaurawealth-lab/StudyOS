package com.studyos.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.studyos.app.domain.model.Paper
import com.studyos.app.domain.model.PaperStatus
import java.util.UUID

@Entity(
    tableName = "papers",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["subjectId"]),
        Index(value = ["createdAt"])
    ]
)
data class PaperEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val subjectId: String,
    val title: String,
    val totalMarks: Int,
    val durationMinutes: Int,
    val sectionsJson: String = "[]",
    val pdfUri: String? = null,
    val status: String = PaperStatus.GENERATED.name,
    val createdAt: Long = System.currentTimeMillis()
)

fun PaperEntity.toDomain(): Paper {
    val pStatus = try {
        PaperStatus.valueOf(status)
    } catch (_: Exception) {
        PaperStatus.GENERATED
    }
    return Paper(
        id = id,
        subjectId = subjectId,
        title = title,
        totalMarks = totalMarks,
        durationMinutes = durationMinutes,
        sectionsJson = sectionsJson,
        pdfUri = pdfUri,
        status = pStatus,
        createdAt = createdAt
    )
}

fun Paper.toEntity(): PaperEntity {
    return PaperEntity(
        id = id,
        subjectId = subjectId,
        title = title,
        totalMarks = totalMarks,
        durationMinutes = durationMinutes,
        sectionsJson = sectionsJson,
        pdfUri = pdfUri,
        status = status.name,
        createdAt = createdAt
    )
}
