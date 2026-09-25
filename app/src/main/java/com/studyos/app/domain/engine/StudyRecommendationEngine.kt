package com.studyos.app.domain.engine

import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.Exam
import com.studyos.app.domain.model.RecallItem
import com.studyos.app.domain.model.SmartStudyRecommendation
import com.studyos.app.domain.model.Subject
import kotlin.math.roundToInt

/**
 * Intelligent deterministic scoring engine for "What should I study now?".
 *
 * Scoring Formula:
 * Priority Score (0..100) =
 *   Exam Urgency (0..30 pts) +
 *   Recall Due Status (0..25 pts) +
 *   Weakness / Accuracy Deficit (0..20 pts) +
 *   Neglected Time (0..15 pts) +
 *   Unfinished Progress / Mistakes (0..10 pts)
 */
object StudyRecommendationEngine {

    private const val ONE_DAY_MILLIS = 86_400_000L

    data class ChapterEvaluationData(
        val chapter: Chapter,
        val subject: Subject,
        val recallItems: List<RecallItem> = emptyList(),
        val upcomingExams: List<Exam> = emptyList(),
        val mistakeCount: Int = 0,
        val quizAccuracy: Int? = null
    )

    fun calculateRecommendations(
        chapters: List<Chapter>,
        subjects: List<Subject>,
        exams: List<Exam> = emptyList(),
        recallItems: List<RecallItem> = emptyList(),
        mistakes: List<com.studyos.app.domain.model.Mistake> = emptyList(),
        quizAttempts: List<com.studyos.app.domain.model.QuizAttempt> = emptyList(),
        defaultSessionMinutes: Int = 25
    ): List<SmartStudyRecommendation> {
        val subjectMap = subjects.associateBy { it.id }
        val recallByChapter = recallItems.groupBy { it.chapterId }
        val mistakesByChapter = mistakes.filter { !it.isResolved }.groupBy { it.chapterId }
        val attemptsByChapter = quizAttempts.groupBy { it.chapterId }

        val candidates = chapters.mapNotNull { ch ->
            val sub = subjectMap[ch.subjectId] ?: return@mapNotNull null
            val chAttempts = attemptsByChapter[ch.id] ?: emptyList()
            val avgQuiz = if (chAttempts.isNotEmpty()) {
                chAttempts.map { it.accuracyPercentage }.average().roundToInt()
            } else null

            ChapterEvaluationData(
                chapter = ch,
                subject = sub,
                recallItems = recallByChapter[ch.id] ?: emptyList(),
                upcomingExams = exams,
                mistakeCount = (mistakesByChapter[ch.id] ?: emptyList()).size,
                quizAccuracy = avgQuiz
            )
        }

        val (top, next) = calculateRecommendations(candidates, defaultSessionMinutes)
        return listOfNotNull(top, next)
    }

    /**
     * Evaluates all chapters and returns the top recommendation and the runner-up ("Next").
     */
    fun calculateRecommendations(
        candidates: List<ChapterEvaluationData>,
        preferredDurationMinutes: Int = 25
    ): Pair<SmartStudyRecommendation?, SmartStudyRecommendation?> {
        if (candidates.isEmpty()) return Pair(null, null)

        val scored = candidates.map { data ->
            scoreChapter(data, preferredDurationMinutes)
        }.sortedByDescending { it.priorityScore }

        val top = scored.firstOrNull()
        val next = scored.getOrNull(1)

        val enrichedTop = if (top != null && next != null) {
            top.copy(nextRecommendationPreview = "${next.subjectName} → ${next.chapterName}")
        } else {
            top
        }

        return Pair(enrichedTop, next)
    }

    private fun scoreChapter(
        data: ChapterEvaluationData,
        preferredDurationMinutes: Int
    ): SmartStudyRecommendation {
        val now = System.currentTimeMillis()
        val chapter = data.chapter
        val subject = data.subject
        val reasons = mutableListOf<String>()

        var score = 0.0

        // 1. Exam Urgency (Up to 30 points)
        val nearestExam = data.upcomingExams
            .filter { it.targetDate >= now }
            .minByOrNull { it.targetDate }

        if (nearestExam != null) {
            val daysUntilExam = ((nearestExam.targetDate - now) / ONE_DAY_MILLIS).toInt()
            when {
                daysUntilExam <= 2 -> {
                    score += 30.0
                    reasons.add("Exam '${nearestExam.name}' in $daysUntilExam days")
                }
                daysUntilExam <= 7 -> {
                    score += 22.0
                    reasons.add("Exam '${nearestExam.name}' is approaching this week")
                }
                daysUntilExam <= 14 -> {
                    score += 14.0
                    reasons.add("Upcoming exam within 2 weeks")
                }
                else -> {
                    score += 6.0
                }
            }
        }

        // 2. Recall Due Status (Up to 25 points)
        val dueRecalls = data.recallItems.filter { it.isDue }
        val weakRecalls = data.recallItems.filter { it.recallState == com.studyos.app.domain.model.RecallState.WEAK }

        when {
            dueRecalls.size >= 5 -> {
                score += 25.0
                reasons.add("${dueRecalls.size} recall items due for spaced revision")
            }
            dueRecalls.isNotEmpty() -> {
                score += 18.0
                reasons.add("Recall review is due today")
            }
            weakRecalls.isNotEmpty() -> {
                score += 12.0
                reasons.add("${weakRecalls.size} weak recall topics need reinforcement")
            }
        }

        // 3. Weakness / Low Accuracy (Up to 20 points)
        val accuracy = data.quizAccuracy
        val avgRecallAccuracy = if (data.recallItems.isNotEmpty()) {
            data.recallItems.map { it.recallAccuracy }.average().roundToInt()
        } else null

        val effectiveAccuracy = avgRecallAccuracy ?: accuracy

        if (effectiveAccuracy != null) {
            when {
                effectiveAccuracy < 50 -> {
                    score += 20.0
                    reasons.add("Accuracy is critically low ($effectiveAccuracy%)")
                }
                effectiveAccuracy < 70 -> {
                    score += 14.0
                    reasons.add("Topic performance has room for mastery ($effectiveAccuracy%)")
                }
                effectiveAccuracy < 85 -> {
                    score += 6.0
                }
            }
        }

        // 4. Neglected Time (Up to 15 points)
        val lastTime = chapter.lastOpenedAt ?: chapter.createdAt
        val daysSinceStudied = ((now - lastTime) / ONE_DAY_MILLIS).toInt()

        when {
            daysSinceStudied >= 10 -> {
                score += 15.0
                reasons.add("Not studied in $daysSinceStudied days")
            }
            daysSinceStudied >= 5 -> {
                score += 10.0
                reasons.add("Last studied $daysSinceStudied days ago")
            }
            daysSinceStudied >= 2 -> {
                score += 5.0
            }
        }

        // 5. Unfinished Completion & Mistakes (Up to 10 points)
        if (data.mistakeCount >= 3) {
            score += 6.0
            reasons.add("${data.mistakeCount} unresolved mistakes in practice bank")
        }

        if (chapter.progress in 1..99) {
            score += 4.0
            reasons.add("In-progress topic (${chapter.progress}% completed)")
        } else if (chapter.progress == 0) {
            score += 2.0
            reasons.add("Not yet started")
        }

        // Fallback reason if none triggered
        if (reasons.isEmpty()) {
            reasons.add("Maintain consistency and review core concepts")
        }

        val normalizedScore = score.roundToInt().coerceIn(1, 100)

        // Calculate session length (20 - 45 min based on difficulty/urgency)
        val sessionDuration = when {
            normalizedScore >= 80 -> 35
            normalizedScore >= 60 -> 25
            else -> preferredDurationMinutes.coerceIn(15, 30)
        }

        return SmartStudyRecommendation(
            subjectId = subject.id,
            subjectName = subject.name,
            chapterId = chapter.id,
            chapterName = chapter.name,
            priorityScore = normalizedScore,
            whyReasons = reasons.take(3),
            recommendedDurationMinutes = sessionDuration
        )
    }
}
