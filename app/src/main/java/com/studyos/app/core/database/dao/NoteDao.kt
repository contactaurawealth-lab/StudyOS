package com.studyos.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studyos.app.core.database.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE chapterId = :chapterId ORDER BY isPinned DESC, updatedAt DESC")
    fun observeNotesForChapter(chapterId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE subjectId = :subjectId ORDER BY isPinned DESC, updatedAt DESC")
    fun observeNotesForSubject(subjectId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    fun observeAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun observeNoteById(id: String): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteByIdOnce(id: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity)

    @Update
    suspend fun update(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE notes SET isPinned = :isPinned WHERE id = :id")
    suspend fun setPinned(id: String, isPinned: Boolean)

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchNotes(query: String): Flow<List<NoteEntity>>
}
