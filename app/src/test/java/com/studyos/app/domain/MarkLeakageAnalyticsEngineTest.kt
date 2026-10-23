package com.studyos.app.domain

import com.studyos.app.domain.engine.MarkLeakageAnalyticsEngine
import com.studyos.app.domain.model.LossReasonCategory
import com.studyos.app.domain.model.LostMark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkLeakageAnalyticsEngineTest {

    @Test
    fun testEmptyLostMarks_returnsZeroReport() {
        val report = MarkLeakageAnalyticsEngine.analyze(
            lostMarks = emptyList(),
            completedExamsCount = 0,
            totalMarksAttempted = 0.0
        )

        assertEquals(0.0, report.totalMarksLost, 0.001)
        assertEquals(0.0, report.recoverableScorePotential, 0.001)
        assertEquals(0.0, report.bleedRatePerExam, 0.001)
        assertEquals(0.0, report.bleedRatePer100Marks, 0.001)
        assertTrue(report.categoryBreakdown.isEmpty())
        assertTrue(report.actionableAdvice.isNotEmpty())
    }

    @Test
    fun testRecoverablePotential_andBleedRates() {
        val lostMarks = listOf(
            LostMark(
                id = "1",
                resultId = "r1",
                marksLost = 4.0,
                category = LossReasonCategory.CARELESS_MISTAKE
            ),
            LostMark(
                id = "2",
                resultId = "r1",
                marksLost = 3.5,
                category = LossReasonCategory.CALCULATION_ERROR
            ),
            LostMark(
                id = "3",
                resultId = "r2",
                marksLost = 2.0,
                category = LossReasonCategory.MISREAD
            ),
            LostMark(
                id = "4",
                resultId = "r2",
                marksLost = 5.0,
                category = LossReasonCategory.CONCEPT_ERROR
            )
        )

        val report = MarkLeakageAnalyticsEngine.analyze(
            lostMarks = lostMarks,
            completedExamsCount = 2,
            totalMarksAttempted = 100.0
        )

        assertEquals(14.5, report.totalMarksLost, 0.001)
        // Recoverable: Careless (4.0) + Calculation (3.5) + Misread (2.0) = 9.5
        assertEquals(9.5, report.recoverableScorePotential, 0.001)
        // Bleed rate per exam: 14.5 / 2 = 7.25
        assertEquals(7.25, report.bleedRatePerExam, 0.001)
        // Bleed rate per 100 marks: (14.5 / 100) * 100 = 14.5
        assertEquals(14.5, report.bleedRatePer100Marks, 0.001)
        assertEquals(LossReasonCategory.CONCEPT_ERROR, report.topLeakageCategory)

        // Category summaries
        val conceptSummary = report.categoryBreakdown.first { it.category == LossReasonCategory.CONCEPT_ERROR }
        assertEquals(5.0, conceptSummary.totalLostMarks, 0.001)
        assertEquals(false, conceptSummary.isExecutionError)

        val carelessSummary = report.categoryBreakdown.first { it.category == LossReasonCategory.CARELESS_MISTAKE }
        assertEquals(4.0, carelessSummary.totalLostMarks, 0.001)
        assertEquals(true, carelessSummary.isExecutionError)

        assertNotNull(report.actionableAdvice)
        assertTrue(report.actionableAdvice.contains("conceptual"))
    }
}
