package com.studyos.app.domain.engine

import com.studyos.app.domain.model.RecallEvaluationResult
import com.studyos.app.domain.model.RecallItem
import com.studyos.app.domain.model.RecallState
import kotlin.math.roundToInt

/**
 * Deterministic, offline adaptive spaced-recall algorithm for StudyOS.
 *
 * Interval Progression on consecutive correct answers:
 * 1 day -> 3 days -> 7 days -> 14 days -> 30 days
 *
 * When struggling / incorrect:
 * Resets consecutive streak, reduces interval, and schedules prompt for rapid review.
 */
object RecallAlgorithm {

    // Progressive interval tiers in days
    val INTERVAL_TIERS_DAYS = intArrayOf(1, 3, 7, 14, 30)

    private const val ONE_DAY_MILLIS = 86_400_000L
    private const val FOUR_HOURS_MILLIS = 14_400_000L

    /**
     * Evaluates a student's answer against the expected answer, updates the recall metrics,
     * adjusts confidence and intervals, and returns the evaluation result.
     */
    fun evaluateRecallAttempt(
        item: RecallItem,
        userAnswer: String,
        confidenceRating: Int = 3 // 1 (low) to 5 (high)
    ): RecallEvaluationResult {
        val now = System.currentTimeMillis()
        val isCorrect = checkAnswerMatch(userAnswer, item.expectedAnswer)

        val newAttempts = item.recallAttempts + 1
        val newConsecutiveCorrect = if (isCorrect) item.consecutiveCorrect + 1 else 0
        val newConsecutiveIncorrect = if (!isCorrect) item.consecutiveIncorrect + 1 else 0

        // Calculate cumulative accuracy percentage
        val previousCorrectApproximation = (item.recallAccuracy * item.recallAttempts) / 100.0
        val updatedCorrectCount = if (isCorrect) previousCorrectApproximation + 1.0 else previousCorrectApproximation
        val updatedAccuracy = ((updatedCorrectCount / newAttempts) * 100.0).roundToInt().coerceIn(0, 100)

        // Adjust confidence (0.0 to 1.0)
        val normalizedRating = (confidenceRating.coerceIn(1, 5) - 1) / 4.0f
        val updatedConfidence = if (isCorrect) {
            (item.confidence * 0.7f + normalizedRating * 0.3f).coerceIn(0.1f, 1.0f)
        } else {
            (item.confidence * 0.5f).coerceIn(0.05f, 0.6f)
        }

        // Calculate next interval and review timestamp
        val nextIntervalDays: Int
        val nextReviewTime: Long

        if (isCorrect) {
            val tierIndex = (newConsecutiveCorrect - 1).coerceIn(0, INTERVAL_TIERS_DAYS.lastIndex)
            nextIntervalDays = INTERVAL_TIERS_DAYS[tierIndex]
            nextReviewTime = now + (nextIntervalDays * ONE_DAY_MILLIS)
        } else {
            // Immediate review required: 4 hours or 1 day depending on failure severity
            nextIntervalDays = 1
            nextReviewTime = if (newConsecutiveIncorrect >= 2) {
                now + FOUR_HOURS_MILLIS // Review very soon
            } else {
                now + ONE_DAY_MILLIS
            }
        }

        // Determine the new Recall State
        val newState = when {
            newConsecutiveCorrect >= 4 && updatedAccuracy >= 85 -> RecallState.MASTERED
            !isCorrect || newConsecutiveIncorrect >= 2 || updatedAccuracy < 50 -> RecallState.WEAK
            newConsecutiveCorrect in 1..3 -> RecallState.LEARNING
            now >= nextReviewTime -> RecallState.DUE
            else -> RecallState.LEARNING
        }

        val updatedItem = item.copy(
            recallState = newState,
            lastRecalledTimestamp = now,
            lastStudiedTimestamp = now,
            recallAccuracy = updatedAccuracy,
            recallAttempts = newAttempts,
            consecutiveCorrect = newConsecutiveCorrect,
            consecutiveIncorrect = newConsecutiveIncorrect,
            confidence = updatedConfidence,
            intervalDays = nextIntervalDays,
            nextReviewTimestamp = nextReviewTime
        )

        val nextReviewText = formatNextReviewText(nextReviewTime, now)

        val evalStatus = if (isCorrect) com.studyos.app.domain.model.RecallEvaluationStatus.CORRECT else com.studyos.app.domain.model.RecallEvaluationStatus.INCORRECT

        return RecallEvaluationResult(
            recallItem = updatedItem,
            userAnswer = userAnswer,
            expectedAnswer = item.expectedAnswer,
            whyExplanation = item.explanation,
            wasCorrect = isCorrect,
            status = evalStatus,
            newState = newState,
            nextReviewText = nextReviewText
        )
    }

    /**
     * Evaluates an arbitrary student answer against the expected answer without requiring a full RecallItem.
     */
    fun evaluateAnswer(
        userAnswer: String,
        expectedAnswer: String,
        explanation: String = ""
    ): RecallEvaluationResult {
        val isCorrect = checkAnswerMatch(userAnswer, expectedAnswer)
        val status = when {
            isCorrect -> com.studyos.app.domain.model.RecallEvaluationStatus.CORRECT
            userAnswer.isNotBlank() && expectedAnswer.isNotBlank() && checkPartialMatch(userAnswer, expectedAnswer) ->
                com.studyos.app.domain.model.RecallEvaluationStatus.PARTIALLY_CORRECT
            else -> com.studyos.app.domain.model.RecallEvaluationStatus.INCORRECT
        }

        return RecallEvaluationResult(
            userAnswer = userAnswer,
            expectedAnswer = expectedAnswer,
            whyExplanation = explanation,
            wasCorrect = isCorrect,
            status = status
        )
    }

    private fun checkPartialMatch(userAnswer: String, expectedAnswer: String): Boolean {
        val cleanUser = normalize(userAnswer)
        val cleanExpected = normalize(expectedAnswer)
        if (cleanExpected.length in 3..25 && levenshteinDistance(cleanUser, cleanExpected) <= 3) return true
        val userTokens = cleanUser.split(" ").filter { it.length > 2 }.toSet()
        val expectedTokens = cleanExpected.split(" ").filter { it.length > 2 }.toSet()
        if (expectedTokens.isNotEmpty() && userTokens.isNotEmpty()) {
            val intersection = userTokens.intersect(expectedTokens).size
            val ratio = intersection.toDouble() / expectedTokens.size.toDouble()
            if (ratio >= 0.45) return true
        }
        return false
    }

    /**
     * Fuzzy matching for student answers (case-insensitive, trims whitespace, supports minor punctuation differences).
     */
    fun checkAnswerMatch(userAnswer: String, expectedAnswer: String): Boolean {
        val cleanUser = normalize(userAnswer)
        val cleanExpected = normalize(expectedAnswer)

        if (cleanUser == cleanExpected) return true
        if (cleanUser.isEmpty() || cleanExpected.isEmpty()) return false

        // True/False synonyms
        if ((cleanUser == "true" || cleanUser == "t" || cleanUser == "yes") && (cleanExpected == "true" || cleanExpected == "yes")) return true
        if ((cleanUser == "false" || cleanUser == "f" || cleanUser == "no") && (cleanExpected == "false" || cleanExpected == "no")) return true

        // For concise single/double word answers, allow close match if lengths are similar
        if (cleanExpected.length in 3..25) {
            val maxDist = when {
                cleanExpected.length > 10 -> 2
                cleanExpected.length > 4 -> 1
                else -> 0
            }
            if (levenshteinDistance(cleanUser, cleanExpected) <= maxDist) return true
        }

        // For multi-word answers, check token overlap (at least 70% of key words matched)
        val userTokens = cleanUser.split(" ").filter { it.length > 2 }.toSet()
        val expectedTokens = cleanExpected.split(" ").filter { it.length > 2 }.toSet()

        if (expectedTokens.isNotEmpty() && userTokens.isNotEmpty()) {
            val intersection = userTokens.intersect(expectedTokens).size
            val ratio = intersection.toDouble() / expectedTokens.size.toDouble()
            if (ratio >= 0.70) return true
        }

        return false
    }

    private fun normalize(text: String): String {
        return text.trim()
            .lowercase()
            .replace(Regex("[^a-z0-9 ]"), "")
            .replace(Regex("\\s+"), " ")
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val costs = IntArray(s2.length + 1) { it }
        for (i in 1..s1.length) {
            costs[0] = i
            var nw = i - 1
            for (j in 1..s2.length) {
                val cj = costs[j]
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                costs[j] = minOf(costs[j] + 1, costs[j - 1] + 1, nw + cost)
                nw = cj
            }
        }
        return costs[s2.length]
    }

    fun formatNextReviewText(nextReviewTimestamp: Long, currentTimestamp: Long = System.currentTimeMillis()): String {
        val diff = nextReviewTimestamp - currentTimestamp
        if (diff <= 0) return "Due Now"

        val days = (diff / ONE_DAY_MILLIS).toInt()
        val hours = (diff / (3600 * 1000L)).toInt()

        return when {
            hours < 24 -> "In $hours hours"
            days == 1 -> "Tomorrow"
            days in 2..6 -> "In $days days"
            days in 7..13 -> "In 1 week"
            days in 14..29 -> "In 2 weeks"
            else -> "In 1 month"
        }
    }
}
