package com.studyos.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studyos.app.core.database.entity.AiMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiMessageDao {
    @Query("SELECT * FROM ai_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun observeMessagesForConversation(conversationId: String): Flow<List<AiMessageEntity>>

    @Query("SELECT * FROM ai_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    suspend fun getMessagesForConversationOnce(conversationId: String): List<AiMessageEntity>

    @Query("SELECT * FROM ai_messages WHERE id = :id")
    suspend fun getMessageByIdOnce(id: String): AiMessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: AiMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<AiMessageEntity>)

    @Update
    suspend fun update(message: AiMessageEntity)

    @Query("DELETE FROM ai_messages WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM ai_messages WHERE conversationId = :conversationId")
    suspend fun deleteForConversation(conversationId: String)
}
