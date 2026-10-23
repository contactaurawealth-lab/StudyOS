package com.studyos.app.domain.engine

import com.studyos.app.domain.model.CategoryLeakageSummary
import com.studyos.app.domain.model.LossReasonCategory
import com.studyos.app.domain.model.LostMark
import com.studyos.app.domain.model.MarkLeakageReport
import java.util.Locale

object MarkLeakageAnalyticsEngine {

    private val EXECUTION_ERROR_CATEGORIES = setOf(
        LossReasonCategory.CARELESS_MISTAKE,
        LossReasonCategory.CALCULATION_ERROR,
        LossReasonCategory.MISREAD,
        LossReasonCategory.TIME_SHORTAGE,
        LossReasonCategory.POOR_PRESENTATION
    )

    private val RECOVERABLE_SLIP_CATEGORIES = setOf(
        LossReasonCategory.CARELESS_MISTAKE,
        LossReasonCategory.CALCULATION_ERROR,
        LossReasonCategory.MISREAD
    )

    fun analyze(
        lostMarks: List<LostMark>,
        completedExamsCount: Int = 0,
        totalMarksAttempted: Double = 0.0
    ): MarkLeakageReport {
        if (lostMarks.isEmpty()) {
            return MarkLeakageReport(
                totalMarksLost = 0.0,
                recoverableScorePotential = 0.0,
                bleedRatePerExam = 0.0,
                bleedRatePer100Marks = 0.0,
                categoryBreakdown = emptyList(),
                topLeakageCategory = null,
                actionableAdvice = "No mark leakage recorded yet. Complete exam papers to track your diagnostic bleed rate."
            )
        }

        val totalMarksLost = lostMarks.sumOf { it.marksLost }

        // Category breakdown
        val marksByCategory = lostMarks.groupBy { it.category }
        val categoryBreakdown = LossReasonCategory.values().map { cat ->
            val catMarks = marksByCategory[cat]?.sumOf { it.marksLost } ?: 0.0
            val pct = if (totalMarksLost > 0) (catMarks / totalMarksLost) * 100.0 else 0.0
            CategoryLeakageSummary(
                category = cat,
                totalLostMarks = catMarks,
                percentageOfTotal = pct,
                isExecutionError = cat in EXECUTION_ERROR_CATEGORIES
            )
        }.sortedByDescending { it.totalLostMarks }

        // Recoverable potential (careless slips, calculation errors, misread)
        val recoverablePotential = lostMarks
            .filter { it.category in RECOVERABLE_SLIP_CATEGORIES }
            .sumOf { it.marksLost }

        // Bleed rates
        val bleedRatePerExam = if (completedExamsCount > 0) {
            totalMarksLost / completedExamsCount
        } else {
            totalMarksLost
        }

        val bleedRatePer100Marks = if (totalMarksAttempted > 0) {
            (totalMarksLost / totalMarksAttempted) * 100.0
        } else {
            0.0
        }

        val topCategory = categoryBreakdown.firstOrNull { it.totalLostMarks > 0 }?.category

        val advice = generateAdvice(
            topCategory = topCategory,
            recoverablePotential = recoverablePotential,
            totalMarksLost = totalMarksLost
        )

        return MarkLeakageReport(
            totalMarksLost = totalMarksLost,
            recoverableScorePotential = recoverablePotential,
            bleedRatePerExam = bleedRatePerExam,
            bleedRatePer100Marks = bleedRatePer100Marks,
            categoryBreakdown = categoryBreakdown,
            topLeakageCategory = topCategory,
            actionableAdvice = advice
        )
    }

    private fun generateAdvice(
        topCategory: LossReasonCategory?,
        recoverablePotential: Double,
        totalMarksLost: Double
    ): String {
        if (topCategory == null || totalMarksLost <= 0.0) {
            return "Solid consistency. Keep testing to maintain zero leakage."
        }

        val recoverableFormatted = String.format(Locale.US, "%.1f", recoverablePotential)

        return when (topCategory) {
            LossReasonCategory.CARELESS_MISTAKE,
            LossReasonCategory.CALCULATION_ERROR,
            LossReasonCategory.MISREAD -> {
                "Highest leakage is from preventable execution slips ($recoverableFormatted marks recoverable). Implement a mandatory 5-minute pre-submission double-check protocol to immediately close your 95% gap."
            }
            LossReasonCategory.TIME_SHORTAGE -> {
                "Time shortage is causing mark bleed. Practice with strict 1.2 min/mark pacing sprints to train faster question triage."
            }
            LossReasonCategory.CONCEPT_ERROR,
            LossReasonCategory.DIDNT_KNOW -> {
                "Core conceptual gaps are your main leakage source. Revisit fundamental derivations and test with focused Ghost Exam re-tests."
            }
            LossReasonCategory.FORGOT -> {
                "Retention decay detected. Leverage Spaced Repetition micro-sprints to reinforce memory before the forgetting curve takes hold."
            }
            LossReasonCategory.POOR_PRESENTATION,
            LossReasonCategory.INCOMPLETE_ANSWER -> {
                "Presentation and incomplete step deductions detected. Ensure step-by-step working and final units are clearly highlighted."
            }
            LossReasonCategory.OTHER -> {
                "Review your exam reflections to identify patterns and generate a Ghost Exam to recover lost marks."
            }
        }
    }
}
