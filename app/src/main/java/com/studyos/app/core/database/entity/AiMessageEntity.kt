package com.studyos.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.studyos.app.domain.model.AiMessage
import com.studyos.app.domain.model.AiMessageRole
import com.studyos.app.domain.model.AiMessageStatus

@Entity(
    tableName = "ai_messages",
    foreignKeys = [
        ForeignKey(
            entity = AiConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("conversationId"),
        Index("createdAt")
    ]
)
data class AiMessageEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val status: String,
    val errorMessage: String?,
    val createdAt: Long
) {
    fun toDomain(): AiMessage = AiMessage(
        id = id,
        conversationId = conversationId,
        role = try { AiMessageRole.valueOf(role) } catch (e: Exception) { AiMessageRole.USER },
        content = content,
        status = try { AiMessageStatus.valueOf(status) } catch (e: Exception) { AiMessageStatus.SUCCESS },
        errorMessage = errorMessage,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(domain: AiMessage): AiMessageEntity = AiMessageEntity(
            id = domain.id,
            conversationId = domain.conversationId,
            role = domain.role.name,
            content = domain.content,
            status = domain.status.name,
            errorMessage = domain.errorMessage,
            createdAt = domain.createdAt
        )
    }
}
