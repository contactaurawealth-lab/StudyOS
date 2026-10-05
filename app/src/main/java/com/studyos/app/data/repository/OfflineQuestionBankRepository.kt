package com.studyos.app.data.repository

import com.studyos.app.core.database.dao.QuestionBankDao
import com.studyos.app.core.database.entity.toDomain
import com.studyos.app.core.database.entity.toEntity
import com.studyos.app.domain.model.QuestionBankItem
import com.studyos.app.domain.repository.QuestionBankRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineQuestionBankRepository(
    private val questionBankDao: QuestionBankDao
) : QuestionBankRepository {

    override fun getQuestionsForSubject(subjectId: String): Flow<List<QuestionBankItem>> =
        questionBankDao.getQuestionsForSubject(subjectId).map { list -> list.map { it.toDomain() } }

    override fun getQuestionsForChapter(chapterId: String): Flow<List<QuestionBankItem>> =
        questionBankDao.getQuestionsForChapter(chapterId).map { list -> list.map { it.toDomain() } }

    override fun getQuestionsForTopic(topicId: String): Flow<List<QuestionBankItem>> =
        questionBankDao.getQuestionsForTopic(topicId).map { list -> list.map { it.toDomain() } }

    override suspend fun getQuestionById(id: String): QuestionBankItem? =
        questionBankDao.getQuestionById(id)?.toDomain()

    override suspend fun getQuestionsForPaperSampling(
        subjectId: String,
        chapterIds: List<String>
    ): List<QuestionBankItem> =
        questionBankDao.getQuestionsForPaperSampling(subjectId, chapterIds).map { it.toDomain() }

    override suspend fun saveQuestion(question: QuestionBankItem) {
        questionBankDao.insertQuestion(question.toEntity())
    }

    override suspend fun saveQuestions(questions: List<QuestionBankItem>) {
        questionBankDao.insertQuestions(questions.map { it.toEntity() })
    }

    override suspend fun incrementUsage(id: String) {
        questionBankDao.incrementUsage(id)
    }

    override suspend fun deleteQuestion(question: QuestionBankItem) {
        questionBankDao.deleteQuestion(question.toEntity())
    }

    override fun getTotalQuestionCount(): Flow<Int> =
        questionBankDao.getTotalQuestionCount()
}
