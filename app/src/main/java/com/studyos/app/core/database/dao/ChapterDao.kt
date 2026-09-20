package com.studyos.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studyos.app.core.database.entity.ChapterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {

    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId ORDER BY orderIndex ASC")
    fun observeChaptersForSubject(subjectId: String): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId ORDER BY orderIndex ASC")
    fun getChaptersForSubject(subjectId: String): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId ORDER BY orderIndex ASC")
    suspend fun getChaptersForSubjectOnce(subjectId: String): List<ChapterEntity>

    @Query("SELECT * FROM chapters ORDER BY orderIndex ASC")
    fun observeAllChapters(): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters ORDER BY orderIndex ASC")
    suspend fun getAllChaptersOnce(): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE id = :id LIMIT 1")
    fun getChapter(id: String): Flow<ChapterEntity?>

    @Query("SELECT * FROM chapters WHERE id = :id LIMIT 1")
    fun getChapterById(id: String): Flow<ChapterEntity?>

    @Query("SELECT * FROM chapters WHERE id = :id LIMIT 1")
    suspend fun getChapterByIdOnce(id: String): ChapterEntity?

    @Query("SELECT * FROM chapters WHERE lastOpenedAt IS NOT NULL ORDER BY lastOpenedAt DESC LIMIT 1")
    fun observeMostRecentChapter(): Flow<ChapterEntity?>

    @Query("UPDATE chapters SET lastOpenedAt = :timestamp WHERE id = :id")
    suspend fun recordChapterOpened(id: String, timestamp: Long)

    @Query("SELECT MAX(orderIndex) FROM chapters WHERE subjectId = :subjectId")
    suspend fun getMaxOrderIndex(subjectId: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chapter: ChapterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chapters: List<ChapterEntity>)

    @Update
    suspend fun update(chapter: ChapterEntity)

    @Update
    suspend fun updateChapter(chapter: ChapterEntity)

    @Update
    suspend fun updateAll(chapters: List<ChapterEntity>)

    @Delete
    suspend fun delete(chapter: ChapterEntity)

    @Delete
    suspend fun deleteChapter(chapter: ChapterEntity)

    @Query("DELETE FROM chapters WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM chapters WHERE subjectId = :subjectId")
    suspend fun deleteForSubject(subjectId: String)
}
