package com.studyos.app.domain

import com.studyos.app.core.database.entity.toDomain
import com.studyos.app.core.database.entity.toEntity
import com.studyos.app.domain.model.ExamRelevance
import com.studyos.app.domain.model.LossReasonCategory
import com.studyos.app.domain.model.LostMark
import com.studyos.app.domain.model.Paper
import com.studyos.app.domain.model.PaperStatus
import com.studyos.app.domain.model.QuestionBankItem
import com.studyos.app.domain.model.QuestionDifficulty
import com.studyos.app.domain.model.ExamQuestionType
import com.studyos.app.domain.model.Topic
import com.studyos.app.domain.model.TopicMasteryState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopicAndPaperPilotModelTest {

    @Test
    fun testTopicEntityMapping() {
        val topic = Topic(
            id = "topic-1",
            chapterId = "chap-1",
            name = "Newton's Second Law",
            masteryState = TopicMasteryState.LEARNING,
            examRelevance = ExamRelevance.HIGH,
            weaknessScore = 0.45f
        )
        val entity = topic.toEntity()
        assertEquals("topic-1", entity.id)
        assertEquals("chap-1", entity.chapterId)
        assertEquals("Newton's Second Law", entity.name)
        assertEquals("LEARNING", entity.masteryState)
        assertEquals("HIGH", entity.examRelevance)
        assertEquals(0.45f, entity.weaknessScore, 0.001f)

        val domainBack = entity.toDomain()
        assertEquals(topic, domainBack)
    }

    @Test
    fun testQuestionBankItemMapping() {
        val item = QuestionBankItem(
            id = "q-1",
            subjectId = "sub-1",
            chapterId = "chap-1",
            topicId = "topic-1",
            questionText = "State and derive Newton's Second Law of Motion.",
            markingScheme = "Statement (1m) + Formula F=ma derivation (2m)",
            marks = 3,
            questionType = ExamQuestionType.SHORT_ANSWER,
            difficulty = QuestionDifficulty.MEDIUM
        )
        val entity = item.toEntity()
        assertEquals("q-1", entity.id)
        assertEquals(3, entity.marks)
        assertEquals("SHORT_ANSWER", entity.questionType)
        assertEquals("MEDIUM", entity.difficulty)

        val domainBack = entity.toDomain()
        assertEquals(item, domainBack)
    }

    @Test
    fun testPaperMapping() {
        val paper = Paper(
            id = "paper-1",
            subjectId = "sub-1",
            title = "Physics Mid-Term Mock",
            totalMarks = 50,
            durationMinutes = 90,
            status = PaperStatus.GENERATED
        )
        val entity = paper.toEntity()
        assertEquals("paper-1", entity.id)
        assertEquals(50, entity.totalMarks)
        assertEquals("GENERATED", entity.status)

        val domainBack = entity.toDomain()
        assertEquals(paper, domainBack)
    }

    @Test
    fun testLostMarkMapping() {
        val lostMark = LostMark(
            id = "lm-1",
            resultId = "res-1",
            questionId = "q-1",
            topicId = "topic-1",
            marksLost = 2.0,
            category = LossReasonCategory.CARELESS_MISTAKE,
            reflection = "Forgot unit conversion in final step",
            isRemediated = false
        )
        val entity = lostMark.toEntity()
        assertEquals("lm-1", entity.id)
        assertEquals(2.0, entity.marksLost, 0.001)
        assertEquals("CARELESS_MISTAKE", entity.category)
        assertFalse(entity.isRemediated)

        val domainBack = entity.toDomain()
        assertEquals(lostMark, domainBack)
    }
}
