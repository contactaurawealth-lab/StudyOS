package com.studyos.app.data.repository

import com.studyos.app.core.database.dao.AiConversationDao
import com.studyos.app.core.database.entity.AiConversationEntity
import com.studyos.app.domain.model.AiConversation
import com.studyos.app.domain.repository.AiConversationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AiConversationRepositoryImpl(
    private val aiConversationDao: AiConversationDao
) : AiConversationRepository {

    override fun observeConversations(): Flow<List<AiConversation>> =
        aiConversationDao.observeConversations().map { list ->
            list.map { it.toDomain() }
        }

    override fun getConversationById(id: String): Flow<AiConversation?> =
        aiConversationDao.getConversationById(id).map { it?.toDomain() }

    override suspend fun getConversationByIdOnce(id: String): AiConversation? =
        aiConversationDao.getConversationByIdOnce(id)?.toDomain()

    override suspend fun createConversation(conversation: AiConversation): AiConversation {
        aiConversationDao.insert(AiConversationEntity.fromDomain(conversation))
        return conversation
    }

    override suspend fun updateConversation(conversation: AiConversation) {
        aiConversationDao.update(AiConversationEntity.fromDomain(conversation))
    }

    override suspend fun deleteConversation(id: String) {
        aiConversationDao.deleteById(id)
    }
}
