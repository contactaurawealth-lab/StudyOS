package com.studyos.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studyos.app.core.database.entity.RecallAttemptEntity
import com.studyos.app.core.database.entity.RecallItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecallDao {

    @Query("SELECT * FROM recall_items WHERE id = :id LIMIT 1")
    suspend fun getRecallItemById(id: String): RecallItemEntity?

    @Query("SELECT * FROM recall_items WHERE chapterId = :chapterId")
    fun observeRecallItemsForChapter(chapterId: String): Flow<List<RecallItemEntity>>

    @Query("SELECT * FROM recall_items WHERE chapterId = :chapterId")
    suspend fun getRecallItemsForChapterOnce(chapterId: String): List<RecallItemEntity>

    @Query("SELECT * FROM recall_items WHERE subjectId = :subjectId")
    suspend fun getRecallItemsForSubjectOnce(subjectId: String): List<RecallItemEntity>

    @Query("SELECT * FROM recall_items WHERE nextReviewTimestamp <= :currentTime ORDER BY nextReviewTimestamp ASC")
    fun observeDueRecallItems(currentTime: Long): Flow<List<RecallItemEntity>>

    @Query("SELECT * FROM recall_items WHERE nextReviewTimestamp <= :currentTime ORDER BY nextReviewTimestamp ASC")
    suspend fun getDueRecallItemsOnce(currentTime: Long): List<RecallItemEntity>

    @Query("SELECT * FROM recall_items WHERE recallState = 'WEAK'")
    fun observeWeakRecallItems(): Flow<List<RecallItemEntity>>

    @Query("SELECT * FROM recall_items WHERE recallState = 'WEAK'")
    suspend fun getWeakRecallItemsOnce(): List<RecallItemEntity>

    @Query("SELECT * FROM recall_items WHERE recallState = 'MASTERED'")
    fun observeMasteredRecallItems(): Flow<List<RecallItemEntity>>

    @Query("SELECT * FROM recall_items WHERE recallState = 'MASTERED'")
    suspend fun getMasteredRecallItemsOnce(): List<RecallItemEntity>

    @Query("SELECT * FROM recall_items")
    suspend fun getAllRecallItemsOnce(): List<RecallItemEntity>

    @Query("SELECT COUNT(*) FROM recall_items WHERE nextReviewTimestamp <= :currentTime")
    fun observeDueCount(currentTime: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM recall_items WHERE recallState = 'WEAK'")
    fun observeWeakCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM recall_items WHERE recallState = 'MASTERED'")
    fun observeMasteredCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM recall_items")
    fun observeTotalCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: RecallItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<RecallItemEntity>)

    @Update
    suspend fun update(item: RecallItemEntity)

    @Query("DELETE FROM recall_items WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM recall_items WHERE chapterId = :chapterId")
    suspend fun deleteForChapter(chapterId: String)

    // Attempts
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: RecallAttemptEntity)

    @Query("SELECT * FROM recall_attempts WHERE chapterId = :chapterId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentAttemptsForChapter(chapterId: String, limit: Int = 20): List<RecallAttemptEntity>

    @Query("SELECT * FROM recall_attempts ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentAttempts(limit: Int = 50): List<RecallAttemptEntity>
}
