package com.studyos.app.domain.repository

import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.ChapterStatus
import kotlinx.coroutines.flow.Flow

interface ChapterRepository {
    fun observeChaptersForSubject(subjectId: String): Flow<List<Chapter>>
    suspend fun getChaptersForSubjectOnce(subjectId: String): List<Chapter>
    fun observeAllChapters(): Flow<List<Chapter>>
    suspend fun getAllChaptersOnce(): List<Chapter>
    fun getChapter(id: String): Flow<Chapter?>
    suspend fun getChapterById(id: String): Chapter?
    suspend fun addChapter(subjectId: String, name: String, description: String?): Chapter
    suspend fun updateChapter(chapter: Chapter)
    suspend fun updateChapterProgress(id: String, progress: Int)
    suspend fun updateChapterStatus(id: String, status: ChapterStatus)
    suspend fun deleteChapter(id: String)
    suspend fun reorderChapters(subjectId: String, chapters: List<Chapter>)
    suspend fun moveChapter(subjectId: String, chapterId: String, moveUp: Boolean)
}
