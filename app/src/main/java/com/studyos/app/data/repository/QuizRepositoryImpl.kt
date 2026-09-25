package com.studyos.app.data.repository

import com.studyos.app.core.database.dao.QuizDao
import com.studyos.app.core.database.entity.ActiveQuizStateEntity
import com.studyos.app.core.database.entity.QuestionResultEntity
import com.studyos.app.core.database.entity.QuizAttemptEntity
import com.studyos.app.core.database.entity.QuizEntity
import com.studyos.app.core.database.entity.QuizQuestionEntity
import com.studyos.app.domain.model.ActiveQuizState
import com.studyos.app.domain.model.QuestionResult
import com.studyos.app.domain.model.QuestionType
import com.studyos.app.domain.model.Quiz
import com.studyos.app.domain.model.QuizAttempt
import com.studyos.app.domain.model.QuizDifficulty
import com.studyos.app.domain.model.QuizQuestion
import com.studyos.app.domain.repository.QuizRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class QuizRepositoryImpl(
    private val quizDao: QuizDao
) : QuizRepository {

    override fun observeQuizzesForChapter(chapterId: String): Flow<List<Quiz>> =
        quizDao.observeQuizzesForChapter(chapterId).map { entities -> entities.map { it.toDomain() } }

    override fun observeQuizzesForSubject(subjectId: String): Flow<List<Quiz>> =
        quizDao.observeQuizzesForSubject(subjectId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getQuizById(id: String): Quiz? =
        quizDao.getQuizById(id)?.toDomain()

    override suspend fun getQuestionsForQuiz(quizId: String): List<QuizQuestion> =
        quizDao.getQuestionsForQuiz(quizId).map { it.toDomain() }

    override suspend fun saveQuiz(quiz: Quiz, questions: List<QuizQuestion>): Quiz {
        quizDao.insertQuiz(quiz.toEntity())
        quizDao.insertQuestions(questions.map { it.toEntity() })
        return quiz
    }

    override suspend fun deleteQuiz(quizId: String) {
        quizDao.deleteQuizById(quizId)
    }

    override suspend fun saveActiveState(state: ActiveQuizState) {
        val json = JSONObject()
        state.answers.forEach { (k, v) -> json.put(k, v) }
        quizDao.saveActiveState(
            ActiveQuizStateEntity(
                quizId = state.quizId,
                currentQuestionIndex = state.currentQuestionIndex,
                answersJson = json.toString(),
                elapsedSeconds = state.elapsedSeconds,
                updatedAt = state.updatedAt
            )
        )
    }

    override suspend fun getActiveState(quizId: String): ActiveQuizState? {
        val entity = quizDao.getActiveState(quizId) ?: return null
        val answersMap = mutableMapOf<String, String>()
        try {
            val json = JSONObject(entity.answersJson)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                answersMap[key] = json.getString(key)
            }
        } catch (_: Exception) {}

        return ActiveQuizState(
            quizId = entity.quizId,
            currentQuestionIndex = entity.currentQuestionIndex,
            answers = answersMap,
            elapsedSeconds = entity.elapsedSeconds,
            updatedAt = entity.updatedAt
        )
    }

    override suspend fun clearActiveState(quizId: String) {
        quizDao.deleteActiveState(quizId)
    }

    override suspend fun recordAttempt(attempt: QuizAttempt, results: List<QuestionResult>): QuizAttempt {
        quizDao.insertAttempt(attempt.toEntity())
        quizDao.insertQuestionResults(results.map { it.toEntity() })
        return attempt
    }

    override fun getAttemptsForQuiz(quizId: String): Flow<List<QuizAttempt>> =
        quizDao.observeAttemptsForQuiz(quizId).map { entities -> entities.map { it.toDomain() } }

    override fun getAttemptsForChapter(chapterId: String): Flow<List<QuizAttempt>> =
        quizDao.observeAttemptsForChapter(chapterId).map { entities -> entities.map { it.toDomain() } }

    override fun getAllAttempts(): Flow<List<QuizAttempt>> =
        quizDao.observeAllAttempts().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getAttemptResults(attemptId: String): List<QuestionResult> =
        quizDao.getQuestionResultsForAttempt(attemptId).map { it.toDomain() }

    // Mappers
    private fun QuizEntity.toDomain(): Quiz = Quiz(
        id = id,
        title = title,
        subjectId = subjectId,
        chapterId = chapterId,
        questionCount = questionCount,
        difficulty = try { QuizDifficulty.valueOf(difficulty) } catch (_: Exception) { QuizDifficulty.MEDIUM },
        timeLimitMinutes = timeLimitMinutes,
        createdAt = createdAt
    )

    private fun Quiz.toEntity(): QuizEntity = QuizEntity(
        id = id,
        title = title,
        subjectId = subjectId,
        chapterId = chapterId,
        questionCount = questionCount,
        difficulty = difficulty.name,
        timeLimitMinutes = timeLimitMinutes,
        createdAt = createdAt
    )

    private fun QuizQuestionEntity.toDomain(): QuizQuestion {
        val optionsList = mutableListOf<String>()
        try {
            val jsonArr = JSONArray(optionsJson)
            for (i in 0 until jsonArr.length()) {
                optionsList.add(jsonArr.getString(i))
            }
        } catch (_: Exception) {}

        return QuizQuestion(
            id = id,
            quizId = quizId,
            question = question,
            options = optionsList,
            correctAnswer = correctAnswer,
            explanation = explanation,
            type = try { QuestionType.valueOf(questionType) } catch (_: Exception) { QuestionType.MCQ },
            topic = topic,
            orderIndex = orderIndex
        )
    }

    private fun QuizQuestion.toEntity(): QuizQuestionEntity {
        val jsonArr = JSONArray()
        options.forEach { jsonArr.put(it) }
        return QuizQuestionEntity(
            id = id,
            quizId = quizId,
            question = question,
            optionsJson = jsonArr.toString(),
            correctAnswer = correctAnswer,
            explanation = explanation,
            questionType = type.name,
            topic = topic,
            orderIndex = orderIndex
        )
    }

    private fun QuizAttemptEntity.toDomain(): QuizAttempt = QuizAttempt(
        id = id,
        quizId = quizId,
        subjectId = subjectId,
        chapterId = chapterId,
        startedAt = startedAt,
        completedAt = completedAt,
        score = score,
        totalQuestions = totalQuestions,
        accuracyPercentage = accuracyPercentage,
        timeSpentSeconds = timeSpentSeconds,
        isCompleted = isCompleted
    )

    private fun QuizAttempt.toEntity(): QuizAttemptEntity = QuizAttemptEntity(
        id = id,
        quizId = quizId,
        subjectId = subjectId,
        chapterId = chapterId,
        startedAt = startedAt,
        completedAt = completedAt,
        score = score,
        totalQuestions = totalQuestions,
        accuracyPercentage = accuracyPercentage,
        timeSpentSeconds = timeSpentSeconds,
        isCompleted = isCompleted
    )

    private fun QuestionResultEntity.toDomain(): QuestionResult = QuestionResult(
        id = id,
        attemptId = attemptId,
        questionId = questionId,
        studentAnswer = studentAnswer,
        isCorrect = isCorrect,
        topic = topic
    )

    private fun QuestionResult.toEntity(): QuestionResultEntity = QuestionResultEntity(
        id = id,
        attemptId = attemptId,
        questionId = questionId,
        studentAnswer = studentAnswer,
        isCorrect = isCorrect,
        topic = topic
    )
}
