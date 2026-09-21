package com.studyos.app.domain.model

import java.util.UUID

/**
 * Core states of the AI Recall Engine.
 */
enum class RecallState(val label: String) {
    NEW("New"),
    LEARNING("Learning"),
    WEAK("Weak"),
    DUE("Due"),
    MASTERED("Mastered")
}

/**
 * Question types supported by the AI Recall Engine.
 */
enum class RecallQuestionType(val label: String) {
    DEFINITION_RECALL("Definition"),
    CONCEPT_EXPLANATION("Concept"),
    FILL_IN_BLANK("Fill in Blank"),
    FILL_IN_THE_BLANK("Fill in Blank"),
    SHORT_ANSWER("Short Answer"),
    TRUE_FALSE("True/False"),
    COMPARE_CONCEPTS("Compare"),
    FORMULA_RECALL("Formula"),
    IMPORTANT_FACTS("Key Fact"),
    FLASHCARD_QUESTION("Flashcard Drill"),
    COMMON_MISTAKE_REVERSAL("Mistake Reversal"),
    DIAGNOSTIC_QUESTION("Diagnostic"),
    CONCEPT_LINKING("Concept Linking")
}

/**
 * Represents a single recallable item tracked by the spaced-recall system.
 */
data class RecallItem(
    val id: String = UUID.randomUUID().toString(),
    val chapterId: String,
    val subjectId: String,
    val questionType: RecallQuestionType,
    val prompt: String,
    val expectedAnswer: String,
    val explanation: String,
    val options: List<String> = emptyList(), // For multiple choice / true-false
    val recallState: RecallState = RecallState.NEW,
    val lastStudiedTimestamp: Long? = null,
    val lastRecalledTimestamp: Long? = null,
    val recallAccuracy: Int = 0, // 0..100
    val recallAttempts: Int = 0,
    val consecutiveCorrect: Int = 0,
    val consecutiveIncorrect: Int = 0,
    val confidence: Float = 0.5f, // 0.0f..1.0f
    val intervalDays: Int = 1,
    val nextReviewTimestamp: Long = System.currentTimeMillis()
) {
    val isDue: Boolean
        get() = System.currentTimeMillis() >= nextReviewTimestamp
}

/**
 * Record of a single recall attempt.
 */
data class RecallAttempt(
    val id: String = UUID.randomUUID().toString(),
    val recallItemId: String,
    val chapterId: String,
    val userAnswer: String,
    val wasCorrect: Boolean,
    val confidenceRating: Int = 3, // 1 (struggled) to 5 (effortless)
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Recall Dashboard minimal stats.
 */
data class RecallDashboardSummary(
    val dueCount: Int = 0,
    val weakCount: Int = 0,
    val masteredCount: Int = 0,
    val totalCount: Int = 0
)

enum class RecallEvaluationStatus {
    CORRECT,
    PARTIALLY_CORRECT,
    INCORRECT
}

/**
 * Result evaluation shown immediately after submitting an answer.
 */
data class RecallEvaluationResult(
    val recallItem: RecallItem? = null,
    val userAnswer: String = "",
    val expectedAnswer: String = "",
    val whyExplanation: String = "",
    val wasCorrect: Boolean = false,
    val status: RecallEvaluationStatus = if (wasCorrect) RecallEvaluationStatus.CORRECT else RecallEvaluationStatus.INCORRECT,
    val newState: RecallState = RecallState.LEARNING,
    val nextReviewText: String = "",
    val scorePercentage: Int = if (wasCorrect) 100 else 0,
    val feedback: String = ""
)

/**
 * Smart recommendation produced by the "What should I study now?" engine.
 */
data class SmartStudyRecommendation(
    val id: String = UUID.randomUUID().toString(),
    val subjectId: String,
    val subjectName: String,
    val chapterId: String,
    val chapterName: String,
    val priorityScore: Int, // 0..100 normalized
    val whyReasons: List<String>, // e.g. ["Recall is due", "Accuracy is low (45%)", "Exam in 3 days"]
    val recommendedDurationMinutes: Int = 25,
    val nextRecommendationPreview: String? = null // e.g. "Maths → Quadratic Equations"
) {
    val nextChapterPreview: String? get() = nextRecommendationPreview
}

/**
 * Intelligent status classification for chapters.
 */
enum class IntelligentChapterStatus(val label: String) {
    NOT_STARTED("Not Started"),
    LEARNING("Learning"),
    NEEDS_ATTENTION("Needs Attention"),
    REVISION_DUE("Revision Due"),
    STRONG("Strong"),
    MASTERED("Mastered")
}

/**
 * An actionable weak concept inside a chapter.
 */
data class ActionableWeakConcept(
    val id: String = UUID.randomUUID().toString(),
    val chapterId: String,
    val name: String,
    val accuracy: Int, // 0..100%
    val mistakeCount: Int = 0,
    val recommendedAction: WeakConceptAction = WeakConceptAction.RECALL
) {
    val concept: String get() = name
    val missCount: Int get() = mistakeCount
}

enum class WeakConceptAction {
    RECALL,
    REVIEW,
    PRACTICE
}

/**
 * Comprehensive Chapter Intelligence metrics.
 */
data class ChapterIntelligence(
    val chapterId: String,
    val chapterName: String,
    val subjectId: String,
    val subjectName: String,
    val overallPercentage: Int, // Weighted score 0..100
    val understandingPercentage: Int, // 0..100
    val recallPercentage: Int, // 0..100
    val practicePercentage: Int, // 0..100
    val completionPercentage: Int, // 0..100
    val status: IntelligentChapterStatus,
    val weakConcepts: List<ActionableWeakConcept> = emptyList(),
    val lastStudiedText: String, // e.g. "2 days ago" or "Never"
    val lastRecalledText: String, // e.g. "5 days ago" or "Never"
    val nextReviewText: String // e.g. "Today", "Tomorrow", "In 4 days"
) {
    val actionableWeakConcepts: List<ActionableWeakConcept> get() = weakConcepts
}

/**
 * Study session flow steps.
 */
enum class StudySessionStep {
    RECOMMENDATION,
    LEARN,
    RECALL,
    PRACTICE,
    EVALUATE,
    COMPLETED
}

/**
 * Summary shown upon completing an intelligent study session.
 */
data class IntelligentSessionSummary(
    val subjectName: String,
    val chapterName: String,
    val minutesStudied: Int,
    val recallScoreText: String, // e.g. "8/10"
    val practiceScoreText: String, // e.g. "7/10"
    val chapterStatus: IntelligentChapterStatus,
    val nextReviewText: String // e.g. "Tomorrow"
)
