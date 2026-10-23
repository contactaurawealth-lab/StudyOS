package com.studyos.app.domain

import com.studyos.app.domain.engine.RecallAlgorithm
import com.studyos.app.domain.engine.SmartRevisionQueueEngine
import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.Exam
import com.studyos.app.domain.model.RecallItem
import com.studyos.app.domain.model.Subject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeadlineCompressedSpacedRepetitionTest {

    @Test
    fun testCompressIntervalForExam_scalesDeterministically() {
        // Standard interval 14 days, exam in 6 days -> max feasible = 6 / 2 = 3 days
        assertEquals(3, RecallAlgorithm.compressIntervalForExam(14, 6))

        // Standard interval 30 days, exam in 10 days -> max feasible = 10 / 2 = 5 days
        assertEquals(5, RecallAlgorithm.compressIntervalForExam(30, 10))

        // Standard interval 7 days, exam in 2 days -> final 72h clamps to 1 day
        assertEquals(1, RecallAlgorithm.compressIntervalForExam(7, 2))

        // Standard interval 3 days, exam in 50 days -> uncompressed, remains 3 days
        assertEquals(3, RecallAlgorithm.compressIntervalForExam(3, 50))
    }

    @Test
    fun testEvaluateRecallAttempt_withApproachingExamDeadline() {
        val item = RecallItem(
            id = "rec_1",
            chapterId = "chap_1",
            subjectId = "sub_physics",
            questionType = com.studyos.app.domain.model.RecallQuestionType.FORMULA_RECALL,
            prompt = "State Ohm's Law",
            expectedAnswer = "V = IR",
            explanation = "Ohm's law relation",
            consecutiveCorrect = 3, // Next correct answer normally triggers tier index 3 -> 14 days!
            intervalDays = 7
        )

        val result = RecallAlgorithm.evaluateRecallAttempt(
            item = item,
            userAnswer = "V = IR",
            confidenceRating = 5,
            daysUntilExam = 8 // Exam in 8 days
        )

        // Standard 14 days compressed to 8 / 2 = 4 days
        assertEquals(4, result.recallItem?.intervalDays)
        assertTrue((result.recallItem?.intervalDays ?: 0) < 8)
    }

    @Test
    fun testSmartRevisionQueue_activatesExamDayCompression() {
        val now = System.currentTimeMillis()
        val subject = Subject(id = "sub_chem", name = "Chemistry")
        val chapter = Chapter(id = "chap_organic", subjectId = "sub_chem", name = "Organic Chemistry", progress = 40)
        val upcomingExam = Exam(
            id = "exam_chem",
            name = "Final Board Exam",
            subjectIds = listOf("sub_chem"),
            targetDate = now + (6 * 86_400_000L) // 6 days left
        )

        val queue = SmartRevisionQueueEngine.buildQueue(
            chapters = listOf(chapter),
            subjects = listOf(subject),
            intelligences = emptyList(),
            exams = listOf(upcomingExam)
        )

        assertEquals(1, queue.size)
        val item = queue.first()
        assertTrue(item.isExamDayCompressed)
        assertTrue(item.reason.contains("6d left"))
    }
}
