package com.studyos.app.domain.engine

import com.studyos.app.domain.model.ActionableWeakConcept
import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.ChapterIntelligence
import com.studyos.app.domain.model.IntelligentChapterStatus
import com.studyos.app.domain.model.Mistake
import com.studyos.app.domain.model.RecallItem
import com.studyos.app.domain.model.RecallState
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.model.WeakConceptAction
import kotlin.math.roundToInt

/**
 * Intelligent analyzer that converts raw progress, recall logs, mistakes,
 * and notes into deep 4-dimensional Chapter Intelligence.
 */
object ChapterIntelligenceCalculator {

    private const val ONE_DAY_MILLIS = 86_400_000L

    fun calculate(
        chapter: Chapter,
        subject: Subject,
        recallItems: List<RecallItem> = emptyList(),
        mistakes: List<Mistake> = emptyList(),
        hasNotes: Boolean = false,
        quizAccuracy: Int? = null,
        flashcardsMasteryPercentage: Int? = null
    ): ChapterIntelligence {
        val now = System.currentTimeMillis()

        // 1. Completion Percentage (from syllabus checklist)
        val completion = chapter.progress.coerceIn(0, 100)

        // 2. Recall Percentage (active recall score + state weighting)
        val recall = if (recallItems.isNotEmpty()) {
            val total = recallItems.size
            val masteredWeight = recallItems.count { it.recallState == RecallState.MASTERED } * 100.0
            val learningWeight = recallItems.count { it.recallState == RecallState.LEARNING } * 70.0
            val weakWeight = recallItems.count { it.recallState == RecallState.WEAK } * 35.0
            val dueWeight = recallItems.count { it.recallState == RecallState.DUE } * 50.0
            val weightedRecall = (masteredWeight + learningWeight + weakWeight + dueWeight) / total
            weightedRecall.roundToInt().coerceIn(0, 100)
        } else {
            // If no discrete recall items yet, infer conservatively from completion
            (completion * 0.5).roundToInt()
        }

        // 3. Practice Percentage (quiz accuracy & flashcard mastery)
        val practice = when {
            quizAccuracy != null && flashcardsMasteryPercentage != null ->
                ((quizAccuracy * 0.6) + (flashcardsMasteryPercentage * 0.4)).roundToInt().coerceIn(0, 100)
            quizAccuracy != null -> quizAccuracy.coerceIn(0, 100)
            flashcardsMasteryPercentage != null -> flashcardsMasteryPercentage.coerceIn(0, 100)
            else -> (completion * 0.6).roundToInt().coerceIn(0, 100)
        }

        // 4. Understanding Percentage (concept synthesis, notes, error resolution)
        val unresolvedMistakes = mistakes.count { !it.isResolved }
        val mistakeDeduction = (unresolvedMistakes * 8).coerceAtMost(35)
        val notesBonus = if (hasNotes) 15 else 0
        val baseUnderstanding = ((completion * 0.4) + (practice * 0.4) + (recall * 0.2)).roundToInt()
        val understanding = (baseUnderstanding + notesBonus - mistakeDeduction).coerceIn(0, 100)

        // 5. Overall Percentage (Weighted composite)
        // 25% Understanding + 30% Recall + 25% Practice + 20% Completion
        val overall = (
            (understanding * 0.25) +
            (recall * 0.30) +
            (practice * 0.25) +
            (completion * 0.20)
        ).roundToInt().coerceIn(0, 100)

        // 6. Intelligent Status Classification
        // CRITICAL RULE: "100% complete + 45% recall = Needs Attention"
        val isDueForReview = recallItems.any { it.isDue }
        val status = when {
            completion == 0 -> IntelligentChapterStatus.NOT_STARTED
            understanding >= 85 && recall >= 85 && practice >= 80 && completion == 100 -> IntelligentChapterStatus.MASTERED
            understanding >= 75 && practice >= 70 && recall >= 70 -> IntelligentChapterStatus.STRONG
            // Disparity rule: High completion but failing recall or high unresolved mistakes
            (completion >= 70 && recall < 60) || unresolvedMistakes >= 3 -> IntelligentChapterStatus.NEEDS_ATTENTION
            isDueForReview -> IntelligentChapterStatus.REVISION_DUE
            else -> IntelligentChapterStatus.LEARNING
        }

        // 7. Actionable Weak Concepts
        val weakConcepts = mutableListOf<ActionableWeakConcept>()

        // Add from weak recall items
        recallItems.filter { it.recallState == RecallState.WEAK || it.recallAccuracy < 60 }.take(4).forEach { item ->
            weakConcepts.add(
                ActionableWeakConcept(
                    id = item.id,
                    chapterId = chapter.id,
                    name = item.prompt.take(45),
                    accuracy = item.recallAccuracy,
                    mistakeCount = item.consecutiveIncorrect,
                    recommendedAction = WeakConceptAction.RECALL
                )
            )
        }

        // Add from unresolved mistakes if not already covered
        mistakes.filter { !it.isResolved }.take(3).forEach { m ->
            weakConcepts.add(
                ActionableWeakConcept(
                    id = m.id,
                    chapterId = chapter.id,
                    name = m.question.take(45),
                    accuracy = 0,
                    mistakeCount = 1,
                    recommendedAction = WeakConceptAction.PRACTICE
                )
            )
        }

        // Fallback concept if status is NEEDS_ATTENTION but list empty
        if (status == IntelligentChapterStatus.NEEDS_ATTENTION && weakConcepts.isEmpty()) {
            weakConcepts.add(
                ActionableWeakConcept(
                    id = "review_core",
                    chapterId = chapter.id,
                    name = "Core Chapter Concepts",
                    accuracy = recall,
                    mistakeCount = 0,
                    recommendedAction = WeakConceptAction.REVIEW
                )
            )
        }

        // 8. Timelines
        val lastStudiedText = formatRelativeTime(chapter.lastOpenedAt)
        val lastRecalledTime = recallItems.mapNotNull { it.lastRecalledTimestamp }.maxOrNull()
        val lastRecalledText = formatRelativeTime(lastRecalledTime)

        val nextReviewTimestamp = recallItems.minOfOrNull { it.nextReviewTimestamp } ?: (now + ONE_DAY_MILLIS)
        val nextReviewText = RecallAlgorithm.formatNextReviewText(nextReviewTimestamp, now)

        return ChapterIntelligence(
            chapterId = chapter.id,
            chapterName = chapter.name,
            subjectId = subject.id,
            subjectName = subject.name,
            overallPercentage = overall,
            understandingPercentage = understanding,
            recallPercentage = recall,
            practicePercentage = practice,
            completionPercentage = completion,
            status = status,
            weakConcepts = weakConcepts.distinctBy { it.name }.take(4),
            lastStudiedText = lastStudiedText,
            lastRecalledText = lastRecalledText,
            nextReviewText = nextReviewText
        )
    }

    private fun formatRelativeTime(timestamp: Long?): String {
        if (timestamp == null || timestamp == 0L) return "Never"
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        if (diff < 0) return "Just now"

        val days = (diff / ONE_DAY_MILLIS).toInt()
        val hours = (diff / (3600 * 1000L)).toInt()

        return when {
            hours < 1 -> "Just now"
            hours < 24 -> "Today"
            days == 1 -> "1 day ago"
            days in 2..6 -> "$days days ago"
            days in 7..13 -> "1 week ago"
            days in 14..29 -> "${days / 7} weeks ago"
            else -> "Over a month ago"
        }
    }
}
