package com.studyos.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studyos.app.core.database.entity.TopicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {

    @Query("SELECT * FROM topics WHERE chapterId = :chapterId ORDER BY orderIndex ASC, createdAt ASC")
    fun getTopicsForChapter(chapterId: String): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE chapterId = :chapterId ORDER BY orderIndex ASC, createdAt ASC")
    suspend fun getTopicsForChapterOnce(chapterId: String): List<TopicEntity>

    @Query("SELECT * FROM topics WHERE id = :id")
    suspend fun getTopicById(id: String): TopicEntity?

    @Query("SELECT * FROM topics WHERE id = :id")
    fun observeTopicById(id: String): Flow<TopicEntity?>

    @Query("SELECT * FROM topics WHERE masteryState = :state")
    fun getTopicsByMasteryState(state: String): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics ORDER BY weaknessScore DESC LIMIT :limit")
    fun getWeakestTopics(limit: Int): Flow<List<TopicEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(topics: List<TopicEntity>)

    @Update
    suspend fun updateTopic(topic: TopicEntity)

    @Query("UPDATE topics SET masteryState = :masteryState, lastRevisedAt = :timestamp WHERE id = :id")
    suspend fun updateMasteryState(id: String, masteryState: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE topics SET weaknessScore = :score WHERE id = :id")
    suspend fun updateWeaknessScore(id: String, score: Float)

    @Delete
    suspend fun deleteTopic(topic: TopicEntity)

    @Query("DELETE FROM topics WHERE id = :id")
    suspend fun deleteTopicById(id: String)
}
