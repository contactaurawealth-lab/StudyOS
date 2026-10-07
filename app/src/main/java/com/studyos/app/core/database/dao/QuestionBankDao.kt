package com.studyos.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studyos.app.core.database.entity.QuestionBankEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionBankDao {

    @Query("SELECT * FROM question_bank WHERE subjectId = :subjectId ORDER BY createdAt DESC")
    fun getQuestionsForSubject(subjectId: String): Flow<List<QuestionBankEntity>>

    @Query("SELECT * FROM question_bank WHERE chapterId = :chapterId ORDER BY createdAt DESC")
    fun getQuestionsForChapter(chapterId: String): Flow<List<QuestionBankEntity>>

    @Query("SELECT * FROM question_bank WHERE topicId = :topicId ORDER BY createdAt DESC")
    fun getQuestionsForTopic(topicId: String): Flow<List<QuestionBankEntity>>

    @Query("SELECT * FROM question_bank WHERE id = :id")
    suspend fun getQuestionById(id: String): QuestionBankEntity?

    @Query("SELECT * FROM question_bank WHERE subjectId = :subjectId AND chapterId IN (:chapterIds)")
    suspend fun getQuestionsForPaperSampling(subjectId: String, chapterIds: List<String>): List<QuestionBankEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionBankEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionBankEntity>)

    @Update
    suspend fun updateQuestion(question: QuestionBankEntity)

    @Query("UPDATE question_bank SET usageCount = usageCount + 1, lastTestedAt = :timestamp WHERE id = :id")
    suspend fun incrementUsage(id: String, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteQuestion(question: QuestionBankEntity)

    @Query("SELECT COUNT(*) FROM question_bank")
    fun getTotalQuestionCount(): Flow<Int>

    @Query("SELECT * FROM question_bank ORDER BY createdAt DESC")
    suspend fun getAllQuestionsOnce(): List<QuestionBankEntity>

    @Query("SELECT * FROM question_bank WHERE subjectId = :subjectId ORDER BY createdAt DESC")
    suspend fun getQuestionsForSubjectOnce(subjectId: String): List<QuestionBankEntity>

    @Query("SELECT * FROM question_bank WHERE chapterId = :chapterId ORDER BY createdAt DESC")
    suspend fun getQuestionsForChapterOnce(chapterId: String): List<QuestionBankEntity>
}

