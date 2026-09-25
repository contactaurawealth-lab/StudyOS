package com.studyos.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studyos.app.core.database.entity.AiConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiConversationDao {
    @Query("SELECT * FROM ai_conversations ORDER BY updatedAt DESC")
    fun observeConversations(): Flow<List<AiConversationEntity>>

    @Query("SELECT * FROM ai_conversations WHERE id = :id")
    fun getConversationById(id: String): Flow<AiConversationEntity?>

    @Query("SELECT * FROM ai_conversations WHERE id = :id")
    suspend fun getConversationByIdOnce(id: String): AiConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conversation: AiConversationEntity)

    @Update
    suspend fun update(conversation: AiConversationEntity)

    @Query("DELETE FROM ai_conversations WHERE id = :id")
    suspend fun deleteById(id: String)
}
