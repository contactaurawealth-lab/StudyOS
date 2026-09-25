package com.studyos.app.data.repository

import com.studyos.app.core.database.dao.FlashcardDao
import com.studyos.app.core.database.entity.FlashcardEntity
import com.studyos.app.core.database.entity.FlashcardReviewEntity
import com.studyos.app.domain.model.Flashcard
import com.studyos.app.domain.model.FlashcardDifficulty
import com.studyos.app.domain.model.FlashcardRating
import com.studyos.app.domain.model.SpacedRepetitionAlgorithm
import com.studyos.app.domain.repository.FlashcardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FlashcardRepositoryImpl(
    private val flashcardDao: FlashcardDao
) : FlashcardRepository {

    override fun observeFlashcardsForChapter(chapterId: String): Flow<List<Flashcard>> =
        flashcardDao.observeFlashcardsForChapter(chapterId).map { entities -> entities.map { it.toDomain() } }

    override fun observeFlashcardsForSubject(subjectId: String): Flow<List<Flashcard>> =
        flashcardDao.observeFlashcardsForSubject(subjectId).map { entities -> entities.map { it.toDomain() } }

    override fun observeDueFlashcards(currentTime: Long): Flow<List<Flashcard>> =
        flashcardDao.observeDueFlashcards(currentTime).map { entities -> entities.map { it.toDomain() } }

    override fun observeAllFlashcards(): Flow<List<Flashcard>> =
        flashcardDao.observeAllFlashcards().map { entities -> entities.map { it.toDomain() } }

    override fun getFlashcardById(id: String): Flow<Flashcard?> =
        flashcardDao.observeFlashcardById(id).map { it?.toDomain() }

    override suspend fun getFlashcardByIdOnce(id: String): Flashcard? =
        flashcardDao.getFlashcardByIdOnce(id)?.toDomain()

    override suspend fun saveFlashcard(flashcard: Flashcard): Flashcard {
        flashcardDao.insert(flashcard.toEntity())
        return flashcard
    }

    override suspend fun saveFlashcards(flashcards: List<Flashcard>) {
        flashcardDao.insertAll(flashcards.map { it.toEntity() })
    }

    override suspend fun deleteFlashcard(id: String) {
        flashcardDao.deleteById(id)
    }

    override suspend fun recordReview(cardId: String, rating: FlashcardRating): Flashcard {
        val existing = flashcardDao.getFlashcardByIdOnce(cardId)
            ?: throw IllegalArgumentException("Flashcard with id $cardId not found")
        val currentCard = existing.toDomain()
        val updatedCard = SpacedRepetitionAlgorithm.calculateNextReview(currentCard, rating)
        flashcardDao.update(updatedCard.toEntity())
        flashcardDao.insertReview(
            FlashcardReviewEntity(
                flashcardId = cardId,
                rating = rating.name,
                reviewedAt = System.currentTimeMillis(),
                intervalAfterDays = updatedCard.intervalDays
            )
        )
        return updatedCard
    }

    override fun searchFlashcards(query: String): Flow<List<Flashcard>> =
        flashcardDao.searchFlashcards(query).map { entities -> entities.map { it.toDomain() } }

    private fun FlashcardEntity.toDomain(): Flashcard = Flashcard(
        id = id,
        subjectId = subjectId,
        chapterId = chapterId,
        question = question,
        answer = answer,
        difficulty = try { FlashcardDifficulty.valueOf(difficulty) } catch (_: Exception) { FlashcardDifficulty.MEDIUM },
        createdAt = createdAt,
        lastReviewed = lastReviewed,
        nextReview = nextReview,
        reviewCount = reviewCount,
        intervalDays = intervalDays,
        easeFactor = easeFactor
    )

    private fun Flashcard.toEntity(): FlashcardEntity = FlashcardEntity(
        id = id,
        subjectId = subjectId,
        chapterId = chapterId,
        question = question,
        answer = answer,
        difficulty = difficulty.name,
        createdAt = createdAt,
        lastReviewed = lastReviewed,
        nextReview = nextReview,
        reviewCount = reviewCount,
        intervalDays = intervalDays,
        easeFactor = easeFactor
    )
}
