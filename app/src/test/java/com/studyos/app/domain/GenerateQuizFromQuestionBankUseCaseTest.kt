package com.studyos.app.domain

import com.studyos.app.core.database.dao.QuestionBankDao
import com.studyos.app.core.database.entity.QuestionBankEntity
import com.studyos.app.domain.model.ActiveQuizState
import com.studyos.app.domain.model.QuestionResult
import com.studyos.app.domain.model.Quiz
import com.studyos.app.domain.model.QuizAttempt
import com.studyos.app.domain.model.QuizDifficulty
import com.studyos.app.domain.model.QuizQuestion
import com.studyos.app.domain.repository.QuizRepository
import com.studyos.app.domain.usecase.GenerateQuizFromQuestionBankUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class MockQuestionBankDaoForQb : QuestionBankDao {
    val questions = mutableListOf<QuestionBankEntity>()
    val incrementedUsageIds = mutableListOf<String>()

    override fun getQuestionsForSubject(subjectId: String): Flow<List<QuestionBankEntity>> =
        flowOf(questions.filter { it.subjectId == subjectId })

    override fun getQuestionsForChapter(chapterId: String): Flow<List<QuestionBankEntity>> =
        flowOf(questions.filter { it.chapterId == chapterId })

    override fun getQuestionsForTopic(topicId: String): Flow<List<QuestionBankEntity>> =
        flowOf(questions.filter { it.topicId == topicId })

    override suspend fun getQuestionsForTopicOnce(topicId: String): List<QuestionBankEntity> =
        questions.filter { it.topicId == topicId }

    override suspend fun getQuestionsForChapterOnce(chapterId: String): List<QuestionBankEntity> =
        questions.filter { it.chapterId == chapterId }

    override suspend fun getQuestionsForChapterTopic(chapterId: String, topicId: String?): List<QuestionBankEntity> =
        questions.filter { it.chapterId == chapterId && (topicId == null || it.topicId == topicId) }

    override suspend fun countQuestionsForTopic(topicId: String): Int =
        questions.count { it.topicId == topicId }

    override suspend fun countQuestionsForChapter(chapterId: String): Int =
        questions.count { it.chapterId == chapterId }

    override suspend fun getQuestionById(id: String): QuestionBankEntity? =
        questions.find { it.id == id }

    override suspend fun getQuestionsForPaperSampling(subjectId: String, chapterIds: List<String>): List<QuestionBankEntity> =
        questions.filter { it.subjectId == subjectId && it.chapterId in chapterIds }

    override suspend fun insertQuestion(question: QuestionBankEntity) {
        questions.add(question)
    }

    override suspend fun insertQuestions(questions: List<QuestionBankEntity>) {
        this.questions.addAll(questions)
    }

    override suspend fun updateQuestion(question: QuestionBankEntity) {
        val idx = questions.indexOfFirst { it.id == question.id }
        if (idx != -1) questions[idx] = question
    }

    override suspend fun incrementUsage(id: String, timestamp: Long) {
        incrementedUsageIds.add(id)
    }

    override suspend fun deleteQuestion(question: QuestionBankEntity) {
        questions.remove(question)
    }

    override fun getTotalQuestionCount(): Flow<Int> = flowOf(questions.size)

    override suspend fun getAllQuestionsOnce(): List<QuestionBankEntity> = questions

    override suspend fun getQuestionsForSubjectOnce(subjectId: String): List<QuestionBankEntity> =
        questions.filter { it.subjectId == subjectId }
}

private class MockQuizRepositoryForQb : QuizRepository {
    val savedQuizzes = mutableListOf<Quiz>()
    val savedQuestions = mutableListOf<QuizQuestion>()

    override fun observeQuizzesForChapter(chapterId: String): Flow<List<Quiz>> =
        flowOf(savedQuizzes.filter { it.chapterId == chapterId })

    override fun observeQuizzesForSubject(subjectId: String): Flow<List<Quiz>> =
        flowOf(savedQuizzes.filter { it.subjectId == subjectId })

    override suspend fun getQuizById(id: String): Quiz? = savedQuizzes.find { it.id == id }

    override suspend fun getQuestionsForQuiz(quizId: String): List<QuizQuestion> =
        savedQuestions.filter { it.quizId == quizId }

    override suspend fun saveQuiz(quiz: Quiz, questions: List<QuizQuestion>): Quiz {
        savedQuizzes.add(quiz)
        savedQuestions.addAll(questions)
        return quiz
    }

    override suspend fun deleteQuiz(quizId: String) {
        savedQuizzes.removeAll { it.id == quizId }
    }

    override suspend fun saveActiveState(state: ActiveQuizState) {}
    override suspend fun getActiveState(quizId: String): ActiveQuizState? = null
    override suspend fun clearActiveState(quizId: String) {}
    override suspend fun recordAttempt(attempt: QuizAttempt, results: List<QuestionResult>): QuizAttempt = attempt
    override suspend fun getAttemptResults(attemptId: String): List<QuestionResult> = emptyList()
    override fun getAttemptsForQuiz(quizId: String): Flow<List<QuizAttempt>> = flowOf(emptyList())
    override fun getAttemptsForChapter(chapterId: String): Flow<List<QuizAttempt>> = flowOf(emptyList())
    override fun getAllAttempts(): Flow<List<QuizAttempt>> = flowOf(emptyList())
}

class GenerateQuizFromQuestionBankUseCaseTest {

    @Test
    fun generateQuiz_fromTopic_generatesQuizStrictlyFromGivenQuestionsInQuestionBank() = runTest {
        val qbDao = MockQuestionBankDaoForQb()
        val quizRepo = MockQuizRepositoryForQb()
        val useCase = GenerateQuizFromQuestionBankUseCase(qbDao, quizRepo)

        // Seed questions for topic1 and topic2
        qbDao.insertQuestion(
            QuestionBankEntity(
                id = "q1",
                subjectId = "sub1",
                chapterId = "chap1",
                topicId = "topic1",
                questionText = "What is the unit of work? (A) Newton (B) Joule (C) Watt (D) Pascal",
                markingScheme = "Joule",
                marks = 1,
                questionType = "MCQ",
                difficulty = "EASY"
            )
        )
        qbDao.insertQuestion(
            QuestionBankEntity(
                id = "q2",
                subjectId = "sub1",
                chapterId = "chap1",
                topicId = "topic1",
                questionText = "State work energy theorem.",
                markingScheme = "W_net = Delta K",
                marks = 2,
                questionType = "SHORT_ANSWER",
                difficulty = "MEDIUM"
            )
        )
        qbDao.insertQuestion(
            QuestionBankEntity(
                id = "q3_other",
                subjectId = "sub1",
                chapterId = "chap1",
                topicId = "topic2",
                questionText = "Unrelated question for topic2",
                markingScheme = "Other",
                marks = 1,
                questionType = "SHORT_ANSWER",
                difficulty = "EASY"
            )
        )

        val result = useCase(
            title = "Work and Energy Topic Quiz",
            subjectId = "sub1",
            chapterId = "chap1",
            topicId = "topic1",
            topicName = "Work & Energy",
            questionCount = 5,
            difficulty = QuizDifficulty.MEDIUM
        )

        assertNotNull(result)
        val (quiz, questions) = result!!

        // Must ONLY contain questions from topic1
        assertEquals(2, questions.size)
        assertTrue(questions.all { it.topic == "Work & Energy" })
        assertTrue(questions.any { it.question.contains("unit of work") })
        assertTrue(questions.any { it.question.contains("work energy theorem") })
        assertFalse(questions.any { it.question.contains("Unrelated") })

        // Quiz saved in repository
        assertEquals(1, quizRepo.savedQuizzes.size)
        assertEquals(quiz.id, quizRepo.savedQuizzes.first().id)

        // Usage incremented on Question Bank entities
        assertTrue(qbDao.incrementedUsageIds.contains("q1"))
        assertTrue(qbDao.incrementedUsageIds.contains("q2"))
        assertFalse(qbDao.incrementedUsageIds.contains("q3_other"))
    }

    @Test
    fun generateQuiz_emptyTopic_returnsNullGracefully() = runTest {
        val qbDao = MockQuestionBankDaoForQb()
        val quizRepo = MockQuizRepositoryForQb()
        val useCase = GenerateQuizFromQuestionBankUseCase(qbDao, quizRepo)

        val result = useCase(
            title = "Empty Topic Quiz",
            subjectId = "sub1",
            chapterId = "chap1",
            topicId = "topic_with_zero_questions",
            topicName = "Empty Topic",
            questionCount = 5
        )

        assertNull(result)
        assertEquals(0, quizRepo.savedQuizzes.size)
    }
}
