package com.studyos.app.data.repository

import com.studyos.app.core.database.dao.NoteDao
import com.studyos.app.core.database.entity.NoteEntity
import com.studyos.app.domain.model.Note
import com.studyos.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NoteRepositoryImpl(
    private val noteDao: NoteDao
) : NoteRepository {

    override fun observeNotesForChapter(chapterId: String): Flow<List<Note>> =
        noteDao.observeNotesForChapter(chapterId).map { entities -> entities.map { it.toDomain() } }

    override fun observeNotesForSubject(subjectId: String): Flow<List<Note>> =
        noteDao.observeNotesForSubject(subjectId).map { entities -> entities.map { it.toDomain() } }

    override fun observeAllNotes(): Flow<List<Note>> =
        noteDao.observeAllNotes().map { entities -> entities.map { it.toDomain() } }

    override fun getNoteById(id: String): Flow<Note?> =
        noteDao.observeNoteById(id).map { it?.toDomain() }

    override suspend fun getNoteByIdOnce(id: String): Note? =
        noteDao.getNoteByIdOnce(id)?.toDomain()

    override suspend fun saveNote(note: Note): Note {
        noteDao.insert(note.toEntity())
        return note
    }

    override suspend fun updateNote(note: Note) {
        noteDao.update(note.toEntity())
    }

    override suspend fun deleteNote(id: String) {
        noteDao.deleteById(id)
    }

    override suspend fun togglePin(id: String, isPinned: Boolean) {
        noteDao.setPinned(id, isPinned)
    }

    override fun searchNotes(query: String): Flow<List<Note>> =
        noteDao.searchNotes(query).map { entities -> entities.map { it.toDomain() } }

    private fun NoteEntity.toDomain(): Note = Note(
        id = id,
        title = title,
        content = content,
        subjectId = subjectId,
        chapterId = chapterId,
        isPinned = isPinned,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun Note.toEntity(): NoteEntity = NoteEntity(
        id = id,
        subjectId = subjectId,
        chapterId = chapterId,
        title = title,
        content = content,
        isPinned = isPinned,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
