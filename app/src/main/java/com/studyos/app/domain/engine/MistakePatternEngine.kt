package com.studyos.app.domain.engine

import com.studyos.app.domain.model.Mistake
import com.studyos.app.domain.model.Subject

data class MistakePatternInsight(
    val dominantCategory: String,
    val dominantCategoryPercentage: Int,
    val topSubjectName: String?,
    val resolutionRatePercentage: Int,
    val headline: String,
    val actionableTip: String
)

object MistakePatternEngine {

    fun analyze(mistakes: List<Mistake>, subjects: List<Subject>): MistakePatternInsight? {
        val unresolved = mistakes.filter { !it.isResolved }
        if (unresolved.size < 2) return null

        val totalUnresolved = unresolved.size.toDouble()

        // 1. Category Distribution
        var conceptCount = 0
        var calcCount = 0
        var memoryCount = 0
        var carelessCount = 0

        unresolved.forEach { m ->
            val t = (m.topic ?: "") + " " + (m.explanation ?: "")
            when {
                t.contains("Concept", ignoreCase = true) -> conceptCount++
                t.contains("Calc", ignoreCase = true) || t.contains("Math", ignoreCase = true) -> calcCount++
                t.contains("Memory", ignoreCase = true) || t.contains("Fact", ignoreCase = true) -> memoryCount++
                t.contains("Careless", ignoreCase = true) || t.contains("Misread", ignoreCase = true) -> carelessCount++
                else -> conceptCount++
            }
        }

        val categoryCounts = listOf(
            "Conceptual Gap" to conceptCount,
            "Calculation Error" to calcCount,
            "Memory / Recall" to memoryCount,
            "Careless Reading" to carelessCount
        ).sortedByDescending { it.second }

        val topCategory = categoryCounts.first()
        val topCategoryPct = ((topCategory.second / totalUnresolved) * 100).toInt()

        // 2. Subject Distribution
        val subjectMap = subjects.associateBy { it.id }
        val subjectMistakeCounts = unresolved.groupBy { it.subjectId }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }

        val topSubjectId = subjectMistakeCounts.firstOrNull()?.first
        val topSubjectName = topSubjectId?.let { subjectMap[it]?.name }

        // 3. Resolution Rate
        val totalMistakes = mistakes.size
        val resolvedCount = mistakes.count { it.isResolved }
        val resRate = if (totalMistakes > 0) ((resolvedCount.toDouble() / totalMistakes) * 100).toInt() else 0

        val headline = when {
            topCategoryPct >= 40 && topSubjectName != null ->
                "$topCategoryPct% of errors in $topSubjectName are ${topCategory.first}s"
            topCategoryPct >= 40 ->
                "$topCategoryPct% of your errors stem from ${topCategory.first}s"
            topSubjectName != null ->
                "$topSubjectName requires attention (${subjectMistakeCounts.first().second} unresolved errors)"
            else ->
                "Repeated error patterns identified across $totalMistakes items"
        }

        val tip = when (topCategory.first) {
            "Calculation Error" ->
                "Tip: Always write out formula steps and verify units on paper before picking final values."
            "Conceptual Gap" ->
                "Tip: Revisit core derivations in chapter notes or ask AI Tutor to explain fundamentals."
            "Memory / Recall" ->
                "Tip: Convert these items into flashcards and drill active recall twice daily."
            "Careless Reading" ->
                "Tip: Underline question keywords ('except', 'not', 'units') before selecting choices."
            else ->
                "Tip: Review and resolve your oldest mistakes first to reinforce long-term memory."
        }

        return MistakePatternInsight(
            dominantCategory = topCategory.first,
            dominantCategoryPercentage = topCategoryPct,
            topSubjectName = topSubjectName,
            resolutionRatePercentage = resRate,
            headline = headline,
            actionableTip = tip
        )
    }
}
