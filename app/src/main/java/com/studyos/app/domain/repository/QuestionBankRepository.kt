package com.studyos.app.domain.repository

import com.studyos.app.domain.model.QuestionBankItem
import kotlinx.coroutines.flow.Flow

interface QuestionBankRepository {
    fun getQuestionsForSubject(subjectId: String): Flow<List<QuestionBankItem>>
    fun getQuestionsForChapter(chapterId: String): Flow<List<QuestionBankItem>>
    fun getQuestionsForTopic(topicId: String): Flow<List<QuestionBankItem>>
    suspend fun getQuestionById(id: String): QuestionBankItem?
    suspend fun getQuestionsForPaperSampling(subjectId: String, chapterIds: List<String>): List<QuestionBankItem>
    suspend fun saveQuestion(question: QuestionBankItem)
    suspend fun saveQuestions(questions: List<QuestionBankItem>)
    suspend fun incrementUsage(id: String)
    suspend fun deleteQuestion(question: QuestionBankItem)
    fun getTotalQuestionCount(): Flow<Int>
}
