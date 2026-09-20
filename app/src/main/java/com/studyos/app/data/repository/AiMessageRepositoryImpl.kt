package com.studyos.app.data.repository

import com.studyos.app.core.database.dao.AiMessageDao
import com.studyos.app.core.database.entity.AiMessageEntity
import com.studyos.app.domain.model.AiMessage
import com.studyos.app.domain.repository.AiMessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AiMessageRepositoryImpl(
    private val aiMessageDao: AiMessageDao
) : AiMessageRepository {

    override fun observeMessagesForConversation(conversationId: String): Flow<List<AiMessage>> =
        aiMessageDao.observeMessagesForConversation(conversationId).map { list ->
            list.map { it.toDomain() }
        }

    override suspend fun getMessagesForConversationOnce(conversationId: String): List<AiMessage> =
        aiMessageDao.getMessagesForConversationOnce(conversationId).map { it.toDomain() }

    override suspend fun getMessageByIdOnce(id: String): AiMessage? =
        aiMessageDao.getMessageByIdOnce(id)?.toDomain()

    override suspend fun createMessage(message: AiMessage): AiMessage {
        aiMessageDao.insert(AiMessageEntity.fromDomain(message))
        return message
    }

    override suspend fun updateMessage(message: AiMessage) {
        aiMessageDao.update(AiMessageEntity.fromDomain(message))
    }

    override suspend fun deleteMessage(id: String) {
        aiMessageDao.deleteById(id)
    }

    override suspend fun deleteMessagesForConversation(conversationId: String) {
        aiMessageDao.deleteForConversation(conversationId)
    }
}
