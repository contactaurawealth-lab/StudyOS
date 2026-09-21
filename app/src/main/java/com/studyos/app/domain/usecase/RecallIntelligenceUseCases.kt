package com.studyos.app.domain.usecase

import com.studyos.app.domain.engine.ChapterIntelligenceCalculator
import com.studyos.app.domain.engine.StudyRecommendationEngine
import com.studyos.app.domain.model.ChapterIntelligence
import com.studyos.app.domain.model.RecallDashboardSummary
import com.studyos.app.domain.model.RecallEvaluationResult
import com.studyos.app.domain.model.RecallItem
import com.studyos.app.domain.model.SmartStudyRecommendation
import com.studyos.app.domain.repository.ChapterRepository
import com.studyos.app.domain.repository.ExamRepository
import com.studyos.app.domain.repository.FlashcardRepository
import com.studyos.app.domain.repository.MistakeRepository
import com.studyos.app.domain.repository.NoteRepository
import com.studyos.app.domain.repository.QuizRepository
import com.studyos.app.domain.repository.RecallRepository
import com.studyos.app.domain.repository.StudyPreferencesRepository
import com.studyos.app.domain.repository.SubjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

/**
 * 1. "What should I study now?" Smart Recommendation Use Case
 */
class GetSmartStudyRecommendationUseCase(
    private val subjectRepository: SubjectRepository,
    private val chapterRepository: ChapterRepository,
    private val examRepository: ExamRepository,
    private val recallRepository: RecallRepository,
    private val mistakeRepository: MistakeRepository,
    private val quizRepository: QuizRepository,
    private val studyPreferencesRepository: StudyPreferencesRepository? = null
) {
    suspend operator fun invoke(): SmartStudyRecommendation? {
        val chapters = chapterRepository.getAllChaptersOnce()
        if (chapters.isEmpty()) return null

        val subjects = subjectRepository.getAllSubjectsOnce()
        val exams = examRepository.observeAllExams().firstOrNull() ?: emptyList()
        val recallItems = recallRepository.observeDueRecallItems().firstOrNull() ?: emptyList()
        val allRecallItems = recallRepository.getWeakRecallItems()
        val allMistakes = mistakeRepository.observeAllMistakes().firstOrNull() ?: emptyList()
        val allAttempts = quizRepository.getAllAttempts().firstOrNull() ?: emptyList()
        val preferences = studyPreferencesRepository?.getPreferencesOnce()

        // Seed recall items for chapters if none exist
        for (c in chapters.take(4)) {
            recallRepository.ensureRecallItemsSeededForChapter(
                chapterId = c.id,
                subjectId = c.subjectId,
                chapterName = c.name
            )
        }

        val recommendations = StudyRecommendationEngine.calculateRecommendations(
            chapters = chapters,
            subjects = subjects,
            exams = exams,
            recallItems = (recallItems + allRecallItems).distinctBy { it.id },
            mistakes = allMistakes,
            quizAttempts = allAttempts,
            defaultSessionMinutes = preferences?.defaultSessionMinutes ?: 45
        )

        return recommendations.firstOrNull()
    }
}

/**
 * 2. Chapter Intelligence Use Case (4-quadrant metrics, status, weak concepts, timeline)
 */
class GetChapterIntelligenceUseCase(
    private val chapterRepository: ChapterRepository,
    private val subjectRepository: SubjectRepository,
    private val noteRepository: NoteRepository,
    private val flashcardRepository: FlashcardRepository,
    private val quizRepository: QuizRepository,
    private val mistakeRepository: MistakeRepository,
    private val recallRepository: RecallRepository
) {
    suspend operator fun invoke(chapterId: String): ChapterIntelligence? {
        val chapter = chapterRepository.getChapterById(chapterId) ?: return null
        val subject = subjectRepository.getSubjectByIdOnce(chapter.subjectId) ?: return null

        // Ensure questions are seeded for recall
        recallRepository.ensureRecallItemsSeededForChapter(
            chapterId = chapter.id,
            subjectId = chapter.subjectId,
            chapterName = chapter.name
        )

        val notes = noteRepository.observeNotesForChapter(chapterId).firstOrNull() ?: emptyList()
        val flashcards = flashcardRepository.observeFlashcardsForChapter(chapterId).firstOrNull() ?: emptyList()
        val quizAttempts = quizRepository.getAttemptsForChapter(chapterId).firstOrNull() ?: emptyList()
        val mistakes = mistakeRepository.observeMistakesForChapter(chapterId).firstOrNull() ?: emptyList()
        val recallItems = recallRepository.getRecallItemsForChapter(chapterId)
        val quizAccuracy = if (quizAttempts.isNotEmpty()) {
            quizAttempts.map { it.accuracyPercentage }.average().toInt()
        } else null

        val flashcardsMastery = if (flashcards.isNotEmpty()) {
            ((flashcards.count { it.reviewCount >= 2 }.toDouble() / flashcards.size) * 100).toInt()
        } else null

        return ChapterIntelligenceCalculator.calculate(
            chapter = chapter,
            subject = subject,
            recallItems = recallItems,
            mistakes = mistakes,
            hasNotes = notes.isNotEmpty(),
            quizAccuracy = quizAccuracy,
            flashcardsMasteryPercentage = flashcardsMastery
        )
    }
}

/**
 * 3. Recall Dashboard Use Case
 */
class GetRecallDashboardUseCase(
    private val recallRepository: RecallRepository
) {
    fun observeSummary(): Flow<RecallDashboardSummary> {
        return recallRepository.getDashboardSummary()
    }

    suspend fun getSummaryOnce(): RecallDashboardSummary {
        return recallRepository.getDashboardSummaryOnce()
    }

    fun observeDueItems(): Flow<List<RecallItem>> {
        return recallRepository.observeDueRecallItems()
    }

    suspend fun getDueItemsOnce(): List<RecallItem> {
        return recallRepository.getDueRecallItems()
    }

    suspend fun getWeakItemsOnce(): List<RecallItem> {
        return recallRepository.getWeakRecallItems()
    }
}

/**
 * 4. Submit Recall Answer Use Case
 */
class SubmitRecallAnswerUseCase(
    private val recallRepository: RecallRepository
) {
    suspend operator fun invoke(
        recallItemId: String,
        userAnswer: String,
        confidenceRating: Int = 3
    ): RecallEvaluationResult {
        return recallRepository.recordAttemptAndEvaluate(
            recallItemId = recallItemId,
            userAnswer = userAnswer,
            confidenceRating = confidenceRating
        )
    }
}
