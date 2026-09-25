package com.studyos.app.domain.repository

import com.studyos.app.domain.model.AiConversation
import com.studyos.app.domain.model.AiMessage
import kotlinx.coroutines.flow.Flow

interface AiConversationRepository {
    fun observeConversations(): Flow<List<AiConversation>>
    fun getConversationById(id: String): Flow<AiConversation?>
    suspend fun getConversationByIdOnce(id: String): AiConversation?
    suspend fun createConversation(conversation: AiConversation): AiConversation
    suspend fun updateConversation(conversation: AiConversation)
    suspend fun deleteConversation(id: String)
}

interface AiMessageRepository {
    fun observeMessagesForConversation(conversationId: String): Flow<List<AiMessage>>
    suspend fun getMessagesForConversationOnce(conversationId: String): List<AiMessage>
    suspend fun getMessageByIdOnce(id: String): AiMessage?
    suspend fun createMessage(message: AiMessage): AiMessage
    suspend fun updateMessage(message: AiMessage)
    suspend fun deleteMessage(id: String)
    suspend fun deleteMessagesForConversation(conversationId: String)
}
