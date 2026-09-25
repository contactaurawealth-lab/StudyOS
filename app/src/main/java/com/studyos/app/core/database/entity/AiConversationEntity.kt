package com.studyos.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.studyos.app.domain.model.AiConversation

@Entity(
    tableName = "ai_conversations",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("subjectId"),
        Index("chapterId"),
        Index("updatedAt")
    ]
)
data class AiConversationEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val subjectId: String?,
    val chapterId: String?,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): AiConversation = AiConversation(
        id = id,
        title = title,
        subjectId = subjectId,
        chapterId = chapterId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(domain: AiConversation): AiConversationEntity = AiConversationEntity(
            id = domain.id,
            title = domain.title,
            subjectId = domain.subjectId,
            chapterId = domain.chapterId,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}
