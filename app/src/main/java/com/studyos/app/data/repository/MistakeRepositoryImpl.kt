package com.studyos.app.data.repository

import com.studyos.app.core.database.dao.MistakeDao
import com.studyos.app.core.database.entity.MistakeEntity
import com.studyos.app.domain.model.Mistake
import com.studyos.app.domain.repository.MistakeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MistakeRepositoryImpl(
    private val mistakeDao: MistakeDao
) : MistakeRepository {

    override fun observeMistakesForChapter(chapterId: String): Flow<List<Mistake>> =
        mistakeDao.observeMistakesForChapter(chapterId).map { entities -> entities.map { it.toDomain() } }

    override fun observeMistakesForSubject(subjectId: String): Flow<List<Mistake>> =
        mistakeDao.observeMistakesForSubject(subjectId).map { entities -> entities.map { it.toDomain() } }

    override fun observeAllMistakes(): Flow<List<Mistake>> =
        mistakeDao.observeAllMistakes().map { entities -> entities.map { it.toDomain() } }

    override suspend fun recordMistake(mistake: Mistake): Mistake {
        val existing = mistakeDao.findExistingMistake(mistake.question, mistake.chapterId)
        if (existing != null) {
            val updated = existing.copy(
                missedCount = existing.missedCount + 1,
                lastMissedAt = System.currentTimeMillis(),
                studentAnswer = mistake.studentAnswer,
                isResolved = false
            )
            mistakeDao.update(updated)
            return updated.toDomain()
        } else {
            mistakeDao.insert(mistake.toEntity())
            return mistake
        }
    }

    override suspend fun resolveMistake(id: String) {
        mistakeDao.resolveMistake(id)
    }

    override suspend fun deleteMistake(id: String) {
        mistakeDao.deleteById(id)
    }

    override fun searchMistakes(query: String): Flow<List<Mistake>> =
        mistakeDao.searchMistakes(query).map { entities -> entities.map { it.toDomain() } }

    private fun MistakeEntity.toDomain(): Mistake = Mistake(
        id = id,
        question = question,
        studentAnswer = studentAnswer,
        correctAnswer = correctAnswer,
        explanation = explanation,
        subjectId = subjectId,
        chapterId = chapterId,
        topic = topic,
        missedCount = missedCount,
        lastMissedAt = lastMissedAt,
        isResolved = isResolved,
        createdAt = createdAt
    )

    private fun Mistake.toEntity(): MistakeEntity = MistakeEntity(
        id = id,
        question = question,
        studentAnswer = studentAnswer,
        correctAnswer = correctAnswer,
        explanation = explanation,
        subjectId = subjectId,
        chapterId = chapterId,
        topic = topic,
        missedCount = missedCount,
        lastMissedAt = lastMissedAt,
        isResolved = isResolved,
        createdAt = createdAt
    )
}
