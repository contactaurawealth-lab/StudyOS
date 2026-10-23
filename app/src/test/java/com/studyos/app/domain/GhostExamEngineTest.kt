package com.studyos.app.domain

import com.studyos.app.domain.engine.GhostExamEngine
import com.studyos.app.domain.model.LossReasonCategory
import com.studyos.app.domain.model.LostMark
import com.studyos.app.domain.model.Mistake
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GhostExamEngineTest {

    @Test
    fun testGhostExamPlan_partitionsExecutionSlipsAndConcepts() {
        val lostMarks = listOf(
            LostMark(
                id = "lm_1",
                resultId = "res_1",
                marksLost = 3.0,
                category = LossReasonCategory.CARELESS_MISTAKE,
                reflection = "Miscalculated signs in algebra step"
            ),
            LostMark(
                id = "lm_2",
                resultId = "res_1",
                marksLost = 5.0,
                category = LossReasonCategory.CONCEPT_ERROR,
                reflection = "Forgot integration by parts formula"
            )
        )

        val mistakes = listOf(
            Mistake(
                id = "mis_1",
                question = "What is Newton's third law?",
                studentAnswer = "Action without equal reaction",
                correctAnswer = "For every action there is an equal and opposite reaction",
                subjectId = "sub_math",
                marksLost = 2.0,
                lossCategory = "CONCEPT_ERROR"
            )
        )

        val plan = GhostExamEngine.generatePlan(
            subjectId = "sub_math",
            subjectName = "Mathematics",
            fallbackChapterId = "chap_1",
            unremediatedLostMarks = lostMarks,
            unresolvedMistakes = mistakes
        )

        assertEquals("Ghost Exam • Mathematics Remediation", plan.title)
        assertEquals(3, plan.questionItems.size)
        // Total marks: 3 + 5 + 2 = 10
        assertEquals(10, plan.totalMarks)
        // Duration: 10 * 1.5 = 15 mins
        assertEquals(15, plan.durationMinutes)

        // Section A check
        val secA = plan.questionItems.filter { it.sectionName.contains("Section A") }
        assertEquals(1, secA.size)
        assertEquals(3, secA.first().marksAllocated)

        // Section B check
        val secB = plan.questionItems.filter { it.sectionName.contains("Section B") }
        assertEquals(2, secB.size)

        // Remediated IDs
        assertTrue(plan.remediatedLostMarkIds.contains("lm_1"))
        assertTrue(plan.remediatedLostMarkIds.contains("lm_2"))
        assertTrue(plan.remediatedMistakeIds.contains("mis_1"))

        // Sections JSON valid
        assertTrue(plan.sectionsJson.contains("Section A: Execution & Calculation Verification"))
        assertTrue(plan.sectionsJson.contains("Section B: Conceptual Mastery Remediation"))
    }
}
