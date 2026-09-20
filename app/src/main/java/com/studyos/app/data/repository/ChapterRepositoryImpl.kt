package com.studyos.app.data.repository

import com.studyos.app.core.database.dao.ChapterDao
import com.studyos.app.core.database.entity.toDomain
import com.studyos.app.core.database.entity.toEntity
import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.ChapterStatus
import com.studyos.app.domain.model.progressForStatus
import com.studyos.app.domain.model.statusForProgress
import com.studyos.app.domain.repository.ChapterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChapterRepositoryImpl(
    private val chapterDao: ChapterDao
) : ChapterRepository {

    override fun observeChaptersForSubject(subjectId: String): Flow<List<Chapter>> {
        return chapterDao.observeChaptersForSubject(subjectId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getChaptersForSubjectOnce(subjectId: String): List<Chapter> {
        return chapterDao.getChaptersForSubjectOnce(subjectId).map { it.toDomain() }
    }

    override fun observeAllChapters(): Flow<List<Chapter>> {
        return chapterDao.observeAllChapters().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAllChaptersOnce(): List<Chapter> {
        return chapterDao.getAllChaptersOnce().map { it.toDomain() }
    }

    override fun getChapter(id: String): Flow<Chapter?> {
        return chapterDao.getChapter(id).map { it?.toDomain() }
    }

    override suspend fun getChapterById(id: String): Chapter? {
        return chapterDao.getChapterByIdOnce(id)?.toDomain()
    }

    override suspend fun addChapter(subjectId: String, name: String, description: String?): Chapter {
        val maxIndex = chapterDao.getMaxOrderIndex(subjectId) ?: -1
        val newOrderIndex = maxIndex + 1
        val chapter = Chapter(
            subjectId = subjectId,
            name = name.trim(),
            description = description?.trim()?.ifEmpty { null },
            orderIndex = newOrderIndex,
            status = ChapterStatus.NOT_STARTED,
            progress = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        chapterDao.insert(chapter.toEntity())
        return chapter
    }

    override suspend fun updateChapter(chapter: Chapter) {
        val clampedProgress = chapter.progress.coerceIn(0, 100)
        val derivedStatus = statusForProgress(clampedProgress)
        val updated = chapter.copy(
            name = chapter.name.trim(),
            description = chapter.description?.trim()?.ifEmpty { null },
            progress = clampedProgress,
            status = derivedStatus,
            updatedAt = System.currentTimeMillis()
        )
        chapterDao.update(updated.toEntity())
    }

    override suspend fun updateChapterProgress(id: String, progress: Int) {
        val existing = chapterDao.getChapterByIdOnce(id)?.toDomain() ?: return
        val clamped = progress.coerceIn(0, 100)
        val derivedStatus = statusForProgress(clamped)
        val updated = existing.copy(
            progress = clamped,
            status = derivedStatus,
            updatedAt = System.currentTimeMillis()
        )
        chapterDao.update(updated.toEntity())
    }

    override suspend fun updateChapterStatus(id: String, status: ChapterStatus) {
        val existing = chapterDao.getChapterByIdOnce(id)?.toDomain() ?: return
        val newProgress = progressForStatus(status, existing.progress)
        val updated = existing.copy(
            status = status,
            progress = newProgress,
            updatedAt = System.currentTimeMillis()
        )
        chapterDao.update(updated.toEntity())
    }

    override suspend fun deleteChapter(id: String) {
        val existing = chapterDao.getChapterByIdOnce(id) ?: return
        val subjectId = existing.subjectId
        chapterDao.deleteById(id)

        // Normalize order indexes for remaining chapters
        val remaining = chapterDao.getChaptersForSubjectOnce(subjectId)
        val reordered = remaining.mapIndexed { index, chapterEntity ->
            chapterEntity.copy(orderIndex = index)
        }
        chapterDao.updateAll(reordered)
    }

    override suspend fun reorderChapters(subjectId: String, chapters: List<Chapter>) {
        val updatedEntities = chapters.mapIndexed { index, chapter ->
            chapter.copy(orderIndex = index, updatedAt = System.currentTimeMillis()).toEntity()
        }
        chapterDao.updateAll(updatedEntities)
    }

    override suspend fun moveChapter(subjectId: String, chapterId: String, moveUp: Boolean) {
        val currentChapters = chapterDao.getChaptersForSubjectOnce(subjectId).map { it.toDomain() }
        val index = currentChapters.indexOfFirst { it.id == chapterId }
        if (index == -1) return

        val targetIndex = if (moveUp) index - 1 else index + 1
        if (targetIndex !in currentChapters.indices) return

        val mutableList = currentChapters.toMutableList()
        val item = mutableList.removeAt(index)
        mutableList.add(targetIndex, item)

        val updatedEntities = mutableList.mapIndexed { idx, chapter ->
            chapter.copy(orderIndex = idx, updatedAt = System.currentTimeMillis()).toEntity()
        }
        chapterDao.updateAll(updatedEntities)
    }

    override fun observeMostRecentChapter(): Flow<Chapter?> {
        return chapterDao.observeMostRecentChapter().map { it?.toDomain() }
    }

    override suspend fun recordChapterOpened(id: String) {
        chapterDao.recordChapterOpened(id, System.currentTimeMillis())
    }
}
