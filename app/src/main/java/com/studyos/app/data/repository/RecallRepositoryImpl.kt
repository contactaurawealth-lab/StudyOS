package com.studyos.app.data.repository

import com.studyos.app.core.database.dao.FlashcardDao
import com.studyos.app.core.database.dao.MistakeDao
import com.studyos.app.core.database.dao.RecallDao
import com.studyos.app.core.database.entity.RecallAttemptEntity
import com.studyos.app.core.database.entity.toDomain
import com.studyos.app.core.database.entity.toEntity
import com.studyos.app.domain.engine.RecallAlgorithm
import com.studyos.app.domain.model.RecallAttempt
import com.studyos.app.domain.model.RecallDashboardSummary
import com.studyos.app.domain.model.RecallEvaluationResult
import com.studyos.app.domain.model.RecallEvaluationStatus
import com.studyos.app.domain.model.RecallItem
import com.studyos.app.domain.model.RecallQuestionType
import com.studyos.app.domain.model.RecallState
import com.studyos.app.domain.repository.RecallRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

class RecallRepositoryImpl(
    private val recallDao: RecallDao,
    private val flashcardDao: FlashcardDao? = null,
    private val mistakeDao: MistakeDao? = null
) : RecallRepository {

    override fun observeRecallItemsForChapter(chapterId: String): Flow<List<RecallItem>> {
        return recallDao.observeRecallItemsForChapter(chapterId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getRecallItemsForChapter(chapterId: String): List<RecallItem> {
        return recallDao.getRecallItemsForChapterOnce(chapterId).map { it.toDomain() }
    }

    override fun observeDueRecallItems(currentTime: Long): Flow<List<RecallItem>> {
        return recallDao.observeDueRecallItems(currentTime).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getDueRecallItems(currentTime: Long): List<RecallItem> {
        return recallDao.getDueRecallItemsOnce(currentTime).map { it.toDomain() }
    }

    override fun observeWeakRecallItems(): Flow<List<RecallItem>> {
        return recallDao.observeWeakRecallItems().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getWeakRecallItems(): List<RecallItem> {
        return recallDao.getWeakRecallItemsOnce().map { it.toDomain() }
    }

    override fun observeMasteredRecallItems(): Flow<List<RecallItem>> {
        return recallDao.observeMasteredRecallItems().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getMasteredRecallItems(): List<RecallItem> {
        return recallDao.getMasteredRecallItemsOnce().map { it.toDomain() }
    }

    override fun getDashboardSummary(): Flow<RecallDashboardSummary> {
        val now = System.currentTimeMillis()
        return combine(
            recallDao.observeDueCount(now),
            recallDao.observeWeakCount(),
            recallDao.observeMasteredCount(),
            recallDao.observeTotalCount()
        ) { due, weak, mastered, total ->
            RecallDashboardSummary(
                dueCount = due,
                weakCount = weak,
                masteredCount = mastered,
                totalCount = total
            )
        }
    }

    override suspend fun getDashboardSummaryOnce(): RecallDashboardSummary {
        val now = System.currentTimeMillis()
        val all = recallDao.getAllRecallItemsOnce()
        val due = all.count { it.nextReviewTimestamp <= now }
        val weak = all.count { it.recallState == RecallState.WEAK.name }
        val mastered = all.count { it.recallState == RecallState.MASTERED.name }
        return RecallDashboardSummary(
            dueCount = due,
            weakCount = weak,
            masteredCount = mastered,
            totalCount = all.size
        )
    }

    override suspend fun getRecentAttemptsForChapter(chapterId: String, limit: Int): List<RecallAttempt> {
        return recallDao.getRecentAttemptsForChapter(chapterId, limit).map { it.toDomain() }
    }

    override suspend fun recordAttemptAndEvaluate(
        recallItemId: String,
        userAnswer: String,
        confidenceRating: Int
    ): RecallEvaluationResult {
        val itemEntity = recallDao.getRecallItemById(recallItemId)
            ?: return RecallEvaluationResult(
                userAnswer = userAnswer,
                expectedAnswer = "",
                whyExplanation = "Question item not found.",
                status = RecallEvaluationStatus.INCORRECT,
                scorePercentage = 0,
                feedback = "Question item not found."
            )

        val itemDomain = itemEntity.toDomain()
        val evaluation = RecallAlgorithm.evaluateRecallAttempt(
            item = itemDomain,
            userAnswer = userAnswer,
            confidenceRating = confidenceRating
        )

        // Save Attempt
        recallDao.insertAttempt(
            RecallAttemptEntity(
                id = UUID.randomUUID().toString(),
                recallItemId = recallItemId,
                chapterId = itemEntity.chapterId,
                userAnswer = userAnswer,
                wasCorrect = evaluation.wasCorrect,
                confidenceRating = confidenceRating,
                timestamp = System.currentTimeMillis()
            )
        )

        evaluation.recallItem?.let { updated ->
            recallDao.update(updated.toEntity())
        }

        return evaluation
    }

    override suspend fun saveRecallItem(item: RecallItem) {
        recallDao.insertOrUpdate(item.toEntity())
    }

    override suspend fun saveRecallItems(items: List<RecallItem>) {
        recallDao.insertAll(items.map { it.toEntity() })
    }

    override suspend fun ensureRecallItemsSeededForChapter(
        chapterId: String,
        subjectId: String,
        chapterName: String
    ) {
        val existing = recallDao.getRecallItemsForChapterOnce(chapterId)
        if (existing.isNotEmpty()) return

        val seededList = mutableListOf<RecallItem>()

        // 1. Check flashcards for this chapter
        flashcardDao?.getFlashcardsForChapterOnce(chapterId)?.let { cards ->
            for (c in cards.take(6)) {
                seededList.add(
                    RecallItem(
                        id = UUID.randomUUID().toString(),
                        chapterId = chapterId,
                        subjectId = subjectId,
                        questionType = RecallQuestionType.FLASHCARD_QUESTION,
                        prompt = c.question,
                        expectedAnswer = c.answer,
                        explanation = "Core flashcard principle from ${chapterName}.",
                        options = emptyList(),
                        recallState = RecallState.NEW
                    )
                )
            }
        }

        // 2. Check mistakes for this chapter
        mistakeDao?.getMistakesForChapterOnce(chapterId)?.let { mistakes ->
            for (m in mistakes.filter { !it.isResolved }.take(4)) {
                seededList.add(
                    RecallItem(
                        id = UUID.randomUUID().toString(),
                        chapterId = chapterId,
                        subjectId = subjectId,
                        questionType = RecallQuestionType.COMMON_MISTAKE_REVERSAL,
                        prompt = "Previously missed: ${m.question}",
                        expectedAnswer = m.correctAnswer,
                        explanation = m.explanation ?: "Mistake reversal drill for ${m.topic ?: chapterName}",
                        options = emptyList(),
                        recallState = RecallState.WEAK
                    )
                )
            }
        }

        // 3. Synthesize foundational recall questions if needed
        if (seededList.size < 4) {
            val synthesis = createFoundationalQuestionsForChapter(chapterId, subjectId, chapterName)
            seededList.addAll(synthesis)
        }

        if (seededList.isNotEmpty()) {
            recallDao.insertAll(seededList.map { it.toEntity() })
        }
    }

    private fun createFoundationalQuestionsForChapter(
        chapterId: String,
        subjectId: String,
        chapterName: String
    ): List<RecallItem> {
        val now = System.currentTimeMillis()
        return listOf(
            RecallItem(
                id = UUID.randomUUID().toString(),
                chapterId = chapterId,
                subjectId = subjectId,
                questionType = RecallQuestionType.CONCEPT_EXPLANATION,
                prompt = "What is the primary governing mechanism or core principle of $chapterName?",
                expectedAnswer = "The core fundamental concept that establishes how $chapterName functions and produces outcomes.",
                explanation = "A complete understanding begins with defining the fundamental governing principle without relying on rote memorization.",
                options = emptyList(),
                recallState = RecallState.NEW,
                intervalDays = 1,
                nextReviewTimestamp = now
            ),
            RecallItem(
                id = UUID.randomUUID().toString(),
                chapterId = chapterId,
                subjectId = subjectId,
                questionType = RecallQuestionType.DIAGNOSTIC_QUESTION,
                prompt = "What are the key prerequisites or assumptions required when analyzing $chapterName?",
                expectedAnswer = "Key boundary conditions, fundamental variables, and initial assumptions.",
                explanation = "Identifying conditions under which $chapterName holds true prevents misapplication on complex exam problems.",
                options = emptyList(),
                recallState = RecallState.NEW,
                intervalDays = 1,
                nextReviewTimestamp = now
            ),
            RecallItem(
                id = UUID.randomUUID().toString(),
                chapterId = chapterId,
                subjectId = subjectId,
                questionType = RecallQuestionType.CONCEPT_LINKING,
                prompt = "How does $chapterName connect to or influence preceding topics in this subject?",
                expectedAnswer = "It integrates preceding foundations to form higher-order models and practical applications.",
                explanation = "Concept linking reinforces memory retention by attaching new knowledge to existing cognitive frameworks.",
                options = emptyList(),
                recallState = RecallState.NEW,
                intervalDays = 1,
                nextReviewTimestamp = now
            ),
            RecallItem(
                id = UUID.randomUUID().toString(),
                chapterId = chapterId,
                subjectId = subjectId,
                questionType = RecallQuestionType.FILL_IN_THE_BLANK,
                prompt = "In $chapterName, when input parameters double under steady-state conditions, the resulting change directly reflects _______ proportionality.",
                expectedAnswer = "direct",
                explanation = "Proportionality relationships are fundamental to rapid quantitative intuition.",
                options = listOf("direct", "inverse", "exponential", "logarithmic"),
                recallState = RecallState.NEW,
                intervalDays = 1,
                nextReviewTimestamp = now
            )
        )
    }
}
