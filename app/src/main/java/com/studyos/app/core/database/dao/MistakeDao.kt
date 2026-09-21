package com.studyos.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studyos.app.core.database.entity.MistakeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MistakeDao {
    @Query("SELECT * FROM mistakes WHERE chapterId = :chapterId ORDER BY isResolved ASC, lastMissedAt DESC")
    fun observeMistakesForChapter(chapterId: String): Flow<List<MistakeEntity>>

    @Query("SELECT * FROM mistakes WHERE subjectId = :subjectId ORDER BY isResolved ASC, lastMissedAt DESC")
    fun observeMistakesForSubject(subjectId: String): Flow<List<MistakeEntity>>

    @Query("SELECT * FROM mistakes ORDER BY isResolved ASC, lastMissedAt DESC")
    fun observeAllMistakes(): Flow<List<MistakeEntity>>

    @Query("SELECT * FROM mistakes WHERE chapterId = :chapterId ORDER BY isResolved ASC, lastMissedAt DESC")
    suspend fun getMistakesForChapterOnce(chapterId: String): List<MistakeEntity>

    @Query("SELECT * FROM mistakes WHERE id = :id LIMIT 1")
    suspend fun getMistakeById(id: String): MistakeEntity?

    @Query("SELECT * FROM mistakes WHERE question = :question AND ((:chapterId IS NULL AND chapterId IS NULL) OR chapterId = :chapterId) LIMIT 1")
    suspend fun findExistingMistake(question: String, chapterId: String?): MistakeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mistake: MistakeEntity)

    @Update
    suspend fun update(mistake: MistakeEntity)

    @Query("DELETE FROM mistakes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE mistakes SET isResolved = 1 WHERE id = :id")
    suspend fun resolveMistake(id: String)

    @Query("SELECT * FROM mistakes WHERE question LIKE '%' || :query || '%' OR topic LIKE '%' || :query || '%' ORDER BY lastMissedAt DESC")
    fun searchMistakes(query: String): Flow<List<MistakeEntity>>
}
