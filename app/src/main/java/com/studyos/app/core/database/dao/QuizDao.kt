package com.studyos.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studyos.app.core.database.entity.ActiveQuizStateEntity
import com.studyos.app.core.database.entity.QuestionResultEntity
import com.studyos.app.core.database.entity.QuizAttemptEntity
import com.studyos.app.core.database.entity.QuizEntity
import com.studyos.app.core.database.entity.QuizQuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizDao {
    @Query("SELECT * FROM quizzes WHERE chapterId = :chapterId ORDER BY createdAt DESC")
    fun observeQuizzesForChapter(chapterId: String): Flow<List<QuizEntity>>

    @Query("SELECT * FROM quizzes WHERE subjectId = :subjectId ORDER BY createdAt DESC")
    fun observeQuizzesForSubject(subjectId: String): Flow<List<QuizEntity>>

    @Query("SELECT * FROM quizzes WHERE id = :id LIMIT 1")
    suspend fun getQuizById(id: String): QuizEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuiz(quiz: QuizEntity)

    @Query("DELETE FROM quizzes WHERE id = :id")
    suspend fun deleteQuizById(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuizQuestionEntity>)

    @Query("SELECT * FROM quiz_questions WHERE quizId = :quizId ORDER BY orderIndex ASC")
    suspend fun getQuestionsForQuiz(quizId: String): List<QuizQuestionEntity>

    // Attempts
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: QuizAttemptEntity)

    @Update
    suspend fun updateAttempt(attempt: QuizAttemptEntity)

    @Query("SELECT * FROM quiz_attempts WHERE id = :attemptId LIMIT 1")
    suspend fun getAttemptById(attemptId: String): QuizAttemptEntity?

    @Query("SELECT * FROM quiz_attempts WHERE quizId = :quizId ORDER BY startedAt DESC")
    fun observeAttemptsForQuiz(quizId: String): Flow<List<QuizAttemptEntity>>

    @Query("SELECT * FROM quiz_attempts WHERE chapterId = :chapterId ORDER BY startedAt DESC")
    fun observeAttemptsForChapter(chapterId: String): Flow<List<QuizAttemptEntity>>

    @Query("SELECT * FROM quiz_attempts ORDER BY startedAt DESC")
    fun observeAllAttempts(): Flow<List<QuizAttemptEntity>>

    // Results
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestionResults(results: List<QuestionResultEntity>)

    @Query("SELECT * FROM question_results WHERE attemptId = :attemptId")
    suspend fun getQuestionResultsForAttempt(attemptId: String): List<QuestionResultEntity>

    // Active in-progress state
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveActiveState(state: ActiveQuizStateEntity)

    @Query("SELECT * FROM active_quiz_states WHERE quizId = :quizId LIMIT 1")
    suspend fun getActiveState(quizId: String): ActiveQuizStateEntity?

    @Query("DELETE FROM active_quiz_states WHERE quizId = :quizId")
    suspend fun deleteActiveState(quizId: String)
}
