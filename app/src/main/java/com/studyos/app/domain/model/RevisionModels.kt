package com.studyos.app.domain.model

import java.util.UUID

// ==========================================
// PHASE 10: AI STUDY ENGINE MODELS
// ==========================================

enum class AiTutorMode(val title: String, val description: String) {
    STUDY_PARTNER("Study Partner", "Ask any question about this chapter"),
    TEACH_ME("Teach Me", "Step-by-step guided teaching with check questions"),
    DOUBT_SOLVER("Doubt Solver", "Clarify confusing points and understand tricky concepts"),
    PRACTICE_DRILL("Practice Drill", "Interactive question drills with instant feedback")
}

enum class PracticeDifficulty(val label: String) {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard")
}

data class ChapterAiContext(
    val subjectId: String,
    val subjectName: String,
    val chapterId: String,
    val chapterName: String,
    val progress: Int,
    val masteryPercentage: Int = 0,
    val weakTopics: List<String> = emptyList(),
    val notesSummary: String = "",
    val recentMistakes: List<String> = emptyList(),
    val quizAccuracy: Int? = null
) {
    fun toSystemPromptAddendum(): String {
        val builder = StringBuilder()
        builder.append("\n\nACADEMIC CHAPTER CONTEXT:")
        builder.append("\nSubject: $subjectName")
        builder.append("\nChapter: $chapterName (Progress: $progress%, Mastery: $masteryPercentage%)")
        if (quizAccuracy != null) {
            builder.append("\nRecent Quiz Accuracy: $quizAccuracy%")
        }
        if (weakTopics.isNotEmpty()) {
            builder.append("\nStudent's Weak Topics in this chapter: ${weakTopics.joinToString(", ")}")
        }
        if (recentMistakes.isNotEmpty()) {
            builder.append("\nQuestions missed previously: ${recentMistakes.take(3).joinToString(" | ")}")
        }
        if (notesSummary.isNotBlank()) {
            builder.append("\nChapter Notes Summary:\n${notesSummary.take(500)}")
        }
        return builder.toString()
    }
}

data class RevisionRecommendation(
    val id: String = UUID.randomUUID().toString(),
    val chapterId: String,
    val chapterName: String,
    val subjectName: String,
    val reason: String,
    val recommendedAction: String,
    val priority: Int // 1 (highest) to 3
)

// ==========================================
// PHASE 11: REVISION & RECALL SYSTEM MODELS
// ==========================================

enum class ChapterMasteryLevel(val label: String) {
    NOVICE("Novice"),
    LEARNING("Learning"),
    PROFICIENT("Proficient"),
    MASTERED("Mastered")
}

data class ChapterMastery(
    val chapterId: String,
    val chapterName: String,
    val subjectId: String,
    val subjectName: String,
    val masteryPercentage: Int,
    val level: ChapterMasteryLevel,
    val progress: Int,
    val quizAccuracy: Int?,
    val flashcardsMastered: Int,
    val flashcardsTotal: Int,
    val mistakesCount: Int,
    val resolvedMistakesCount: Int,
    val lastRevisedAt: Long?,
    val nextRevisionDue: Long?,
    val isDueForRevision: Boolean
)

data class RevisionSchedule(
    val chapterId: String,
    val subjectId: String,
    val intervalDays: Int = 1,
    val revisionCount: Int = 0,
    val lastRevisedAt: Long? = null,
    val nextRevisionDue: Long = System.currentTimeMillis() + 86_400_000L,
    val easeFactor: Float = 2.5f
)

enum class ActiveRecallSessionType(
    val title: String,
    val targetMinutes: Int,
    val description: String
) {
    QUICK_5("5-Min Flash Recall", 5, "Rapid-fire due flashcard retention"),
    FOCUSED_10("10-Min Mistake Drill", 10, "Targeted review of previous mistakes"),
    DEEP_15("15-Min Complete Recall", 15, "Full memory consolidation: cards, mistakes & weak topics")
}

enum class ActiveRecallItemType {
    FLASHCARD,
    MISTAKE_RETRY,
    CONCEPT_QUIZ
}

data class ActiveRecallItem(
    val id: String = UUID.randomUUID().toString(),
    val type: ActiveRecallItemType,
    val title: String,
    val prompt: String,
    val answer: String,
    val options: List<String> = emptyList(),
    val explanation: String? = null,
    val subjectId: String,
    val chapterId: String?,
    val sourceReferenceId: String // flashcardId or mistakeId or questionId
)

data class ActiveRecallSessionSummary(
    val sessionType: ActiveRecallSessionType,
    val durationSeconds: Int,
    val itemsReviewedCount: Int,
    val correctCount: Int,
    val accuracyPercentage: Int,
    val streakDays: Int,
    val streakMaintained: Boolean,
    val masteryGainedText: String
)

data class DueRevisionDashboardData(
    val dueChaptersCount: Int = 0,
    val dueChapters: List<ChapterMastery> = emptyList(),
    val dueFlashcardsCount: Int = 0,
    val unresolvedWeakTopicsCount: Int = 0,
    val weakTopics: List<WeakTopic> = emptyList(),
    val recommendedSession: ActiveRecallSessionType = ActiveRecallSessionType.DEEP_15,
    val revisionStreakDays: Int = 0,
    val dailyRevisionTargetMinutes: Int = 15,
    val completedRevisionMinutesToday: Int = 0,
    val recommendations: List<RevisionRecommendation> = emptyList()
) {
    val isDailyTargetCompleted: Boolean
        get() = completedRevisionMinutesToday >= dailyRevisionTargetMinutes && dailyRevisionTargetMinutes > 0
}
