package com.studyos.app.domain

import com.studyos.app.domain.engine.LeverageMatrixEngine
import com.studyos.app.domain.model.Chapter
import com.studyos.app.domain.model.ExamRelevance
import com.studyos.app.domain.model.LeverageQuadrant
import com.studyos.app.domain.model.Subject
import com.studyos.app.domain.model.Topic
import com.studyos.app.domain.model.TopicMasteryState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LeverageMatrixEngineTest {

    @Test
    fun testEmptyTopics_returnsEmptyReport() {
        val report = LeverageMatrixEngine.buildMatrix(
            topics = emptyList(),
            chapters = emptyList(),
            subjects = emptyList()
        )
        assertEquals(0, report.items.size)
        assertTrue(report.strategicSummary.contains("Add topics"))
    }

    @Test
    fun testQuadrantClassification_andPrioritySorting() {
        val subject = Subject(id = "sub_1", name = "Physics")
        val chapter = Chapter(id = "chap_1", subjectId = "sub_1", name = "Thermodynamics")

        val highImpactTopic = Topic(
            id = "t1",
            chapterId = "chap_1",
            name = "Carnot Engine",
            masteryState = TopicMasteryState.LEARNING,
            examRelevance = ExamRelevance.HIGH,
            weaknessScore = 0.8f // Low mastery, High relevance
        )

        val secureTopic = Topic(
            id = "t2",
            chapterId = "chap_1",
            name = "First Law of Thermodynamics",
            masteryState = TopicMasteryState.MASTERED,
            examRelevance = ExamRelevance.HIGH,
            weaknessScore = 0.0f // High mastery, High relevance
        )

        val quickWinTopic = Topic(
            id = "t3",
            chapterId = "chap_1",
            name = "Thermal Equilibrium",
            masteryState = TopicMasteryState.NOT_STARTED,
            examRelevance = ExamRelevance.LOW,
            weaknessScore = 0.2f // Low mastery, Low relevance
        )

        val overstudiedTopic = Topic(
            id = "t4",
            chapterId = "chap_1",
            name = "Temperature Scales",
            masteryState = TopicMasteryState.MASTERED,
            examRelevance = ExamRelevance.LOW,
            weaknessScore = 0.0f // High mastery, Low relevance
        )

        val report = LeverageMatrixEngine.buildMatrix(
            topics = listOf(secureTopic, highImpactTopic, quickWinTopic, overstudiedTopic),
            chapters = listOf(chapter),
            subjects = listOf(subject)
        )

        assertEquals(4, report.items.size)
        assertEquals(1, report.highImpactCount)
        assertEquals(1, report.secureCount)
        assertEquals(1, report.quickWinsCount)
        assertEquals(1, report.overstudiedCount)

        // Carnot Engine should have highest leverage score and be #1
        val topItem = report.items.first()
        assertEquals("Carnot Engine", topItem.topicName)
        assertEquals(LeverageQuadrant.HIGH_IMPACT, topItem.quadrant)

        val secureItem = report.items.find { it.topicId == "t2" }!!
        assertEquals(LeverageQuadrant.SECURE, secureItem.quadrant)

        val quickWinItem = report.items.find { it.topicId == "t3" }!!
        assertEquals(LeverageQuadrant.QUICK_WINS, quickWinItem.quadrant)

        val overstudiedItem = report.items.find { it.topicId == "t4" }!!
        assertEquals(LeverageQuadrant.OVERSTUDIED, overstudiedItem.quadrant)

        assertTrue(report.estimatedTotalMarksGainTo95 > 0)
        assertNotNull(report.topLeverageTopic)
        assertEquals("Carnot Engine", report.topLeverageTopic?.topicName)
    }
}
