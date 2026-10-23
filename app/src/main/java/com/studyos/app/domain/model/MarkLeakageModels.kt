package com.studyos.app.domain.model

/**
 * Breakdown of marks lost for a specific category.
 */
data class CategoryLeakageSummary(
    val category: LossReasonCategory,
    val totalLostMarks: Double,
    val percentageOfTotal: Double,
    val isExecutionError: Boolean
)

/**
 * Diagnostic report containing aggregated mark leakage, bleed rates, and recoverable score potential.
 */
data class MarkLeakageReport(
    val totalMarksLost: Double = 0.0,
    val recoverableScorePotential: Double = 0.0,
    val bleedRatePerExam: Double = 0.0,
    val bleedRatePer100Marks: Double = 0.0,
    val categoryBreakdown: List<CategoryLeakageSummary> = emptyList(),
    val topLeakageCategory: LossReasonCategory? = null,
    val actionableAdvice: String = ""
)
