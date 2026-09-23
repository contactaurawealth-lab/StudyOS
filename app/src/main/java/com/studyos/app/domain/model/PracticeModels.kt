package com.studyos.app.domain.model

import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

// ==========================================
// 1. NOTES
// ==========================================

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val subjectId: String? = null,
    val chapterId: String? = null,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// ==========================================
// 2. FLASHCARDS & SPACED REPETITION
// ==========================================

enum class FlashcardDifficulty {
    EASY, MEDIUM, HARD
}

enum class FlashcardCategory {
    NEW, LEARNING, DUE, MASTERED
}

enum class FlashcardRating {
    AGAIN, HARD, GOOD, EASY
}

data class Flashcard(
    val id: String = UUID.randomUUID().toString(),
    val question: String,
    val answer: String,
    val subjectId: String,
    val chapterId: String? = null,
    val difficulty: FlashcardDifficulty = FlashcardDifficulty.MEDIUM,
    val createdAt: Long = System.currentTimeMillis(),
    val lastReviewed: Long? = null,
    val nextReview: Long? = null,
    val reviewCount: Int = 0,
    val intervalDays: Int = 0,
    val easeFactor: Float = 2.5f
) {
    fun getCategory(currentTime: Long = System.currentTimeMillis()): FlashcardCategory {
        return when {
            reviewCount == 0 -> FlashcardCategory.NEW
            intervalDays >= 21 -> FlashcardCategory.MASTERED
            nextReview != null && nextReview <= currentTime -> FlashcardCategory.DUE
            else -> FlashcardCategory.LEARNING
        }
    }
}

data class FlashcardReview(
    val id: String = UUID.randomUUID().toString(),
    val flashcardId: String,
    val rating: FlashcardRating,
    val reviewedAt: Long = System.currentTimeMillis(),
    val intervalAfterDays: Int
)

object SpacedRepetitionAlgorithm {
    private const val ONE_DAY_MS = 86_400_000L

    /**
     * Calculates the next review parameters according to an SM-2 inspired algorithm:
     * - AGAIN: resets interval to 1 day, decreases easeFactor.
     * - HARD: interval grows slowly (1.2x), easeFactor slightly decreases.
     * - GOOD: standard progression (1 -> 6 -> interval * easeFactor).
     * - EASY: bonus interval (interval * easeFactor * 1.3), easeFactor increases.
     */
    fun calculateNextReview(
        card: Flashcard,
        rating: FlashcardRating,
        reviewTimestamp: Long = System.currentTimeMillis()
    ): Flashcard {
        var newEase = card.easeFactor
        var newInterval: Int
        val newReviewCount = card.reviewCount + 1

        when (rating) {
            FlashcardRating.AGAIN -> {
                newInterval = 1
                newEase = max(1.3f, newEase - 0.2f)
            }
            FlashcardRating.HARD -> {
                newInterval = if (card.intervalDays <= 1) 1 else (card.intervalDays * 1.2f).roundToInt()
                newEase = max(1.3f, newEase - 0.15f)
            }
            FlashcardRating.GOOD -> {
                newInterval = when (card.intervalDays) {
                    0 -> 1
                    1 -> 6
                    else -> (card.intervalDays * newEase).roundToInt()
                }
            }
            FlashcardRating.EASY -> {
                newInterval = when (card.intervalDays) {
                    0 -> 2
                    1 -> 8
                    else -> (card.intervalDays * newEase * 1.3f).roundToInt()
                }
                newEase += 0.15f
            }
        }

        val nextReviewTime = reviewTimestamp + (newInterval * ONE_DAY_MS)

        return card.copy(
            lastReviewed = reviewTimestamp,
            nextReview = nextReviewTime,
            reviewCount = newReviewCount,
            intervalDays = newInterval,
            easeFactor = newEase
        )
    }
}

// ==========================================
// 3. QUIZZES & QUESTIONS
// ==========================================

enum class QuizDifficulty {
    EASY, MEDIUM, HARD
}

enum class QuestionType {
    MCQ, TRUE_FALSE, SHORT_ANSWER
}

data class Quiz(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subjectId: String,
    val chapterId: String? = null,
    val questionCount: Int,
    val difficulty: QuizDifficulty = QuizDifficulty.MEDIUM,
    val timeLimitMinutes: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class QuizQuestion(
    val id: String = UUID.randomUUID().toString(),
    val quizId: String,
    val question: String,
    val options: List<String> = emptyList(),
    val correctAnswer: String,
    val explanation: String? = null,
    val type: QuestionType = QuestionType.MCQ,
    val topic: String? = null,
    val orderIndex: Int = 0
)

data class QuizAttempt(
    val id: String = UUID.randomUUID().toString(),
    val quizId: String,
    val subjectId: String,
    val chapterId: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val score: Int = 0,
    val totalQuestions: Int = 0,
    val accuracyPercentage: Int = 0,
    val timeSpentSeconds: Int = 0,
    val isCompleted: Boolean = false
)

data class QuestionResult(
    val id: String = UUID.randomUUID().toString(),
    val attemptId: String,
    val questionId: String,
    val studentAnswer: String,
    val isCorrect: Boolean,
    val topic: String? = null
)

data class ActiveQuizState(
    val quizId: String,
    val currentQuestionIndex: Int = 0,
    val answers: Map<String, String> = emptyMap(),
    val elapsedSeconds: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

// ==========================================
// 4. MISTAKES & WEAK TOPICS
// ==========================================

data class Mistake(
    val id: String = UUID.randomUUID().toString(),
    val question: String,
    val studentAnswer: String,
    val correctAnswer: String,
    val explanation: String? = null,
    val subjectId: String,
    val chapterId: String? = null,
    val topic: String? = null,
    val missedCount: Int = 1,
    val lastMissedAt: Long = System.currentTimeMillis(),
    val isResolved: Boolean = false,
    val photoUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class WeakTopicAction {
    REVISE, FLASHCARDS, PRACTICE_QUIZ
}

data class WeakTopic(
    val topic: String,
    val accuracyPercentage: Int,
    val questionsAttempted: Int,
    val subjectId: String? = null,
    val chapterId: String? = null,
    val recommendedAction: WeakTopicAction = WeakTopicAction.REVISE
)

data class ChapterPracticeSummary(
    val chapterId: String,
    val chapterName: String,
    val subjectId: String,
    val subjectName: String,
    val progress: Int,
    val quizAccuracy: Int?,
    val flashcardsDue: Int,
    val notesCount: Int,
    val mistakesCount: Int,
    val weakTopics: List<WeakTopic> = emptyList()
)

// ==========================================
// 5. EXAM PREPARATION
// ==========================================

fun calculateDaysRemaining(targetDateMillis: Long, nowMillis: Long = System.currentTimeMillis()): Long {
    val zone = java.time.ZoneId.systemDefault()
    val today = java.time.Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    val examDay = java.time.Instant.ofEpochMilli(targetDateMillis).atZone(zone).toLocalDate()
    return java.time.temporal.ChronoUnit.DAYS.between(today, examDay)
}

data class Exam(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val targetDate: Long,
    val targetScore: Int? = null,
    val actualScore: Int? = null,
    val isCompleted: Boolean = false,
    val notes: String? = null,
    val subjectIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getDaysRemaining(now: Long = System.currentTimeMillis()): Long =
        calculateDaysRemaining(targetDate, now)
}

data class ExamSubjectProgress(
    val subject: Subject,
    val progressPercentage: Int,
    val chaptersCount: Int,
    val completedChaptersCount: Int
)

enum class ExamRevisionCategory {
    NEED_REVISION, PRACTICE, STRONG
}

data class ExamChapterRevisionItem(
    val chapter: Chapter,
    val subject: Subject,
    val category: ExamRevisionCategory,
    val accuracyPercentage: Int?,
    val mistakesCount: Int,
    val flashcardsDueCount: Int
)

data class ExamDashboardItem(
    val exam: Exam,
    val daysRemaining: Long,
    val subjectProgresses: List<ExamSubjectProgress> = emptyList(),
    val chaptersRemaining: Int = 0,
    val notesAvailable: Int = 0,
    val flashcardsDue: Int = 0,
    val quizAccuracy: Int? = null,
    val weakTopics: List<WeakTopic> = emptyList()
)
