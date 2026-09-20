package com.studyos.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studyos.app.core.database.entity.ExamEntity
import com.studyos.app.core.database.entity.ExamSubjectCrossRefEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams ORDER BY date ASC")
    fun observeAllExams(): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE id = :id LIMIT 1")
    fun observeExamById(id: String): Flow<ExamEntity?>

    @Query("SELECT * FROM exams WHERE id = :id LIMIT 1")
    suspend fun getExamByIdOnce(id: String): ExamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity)

    @Update
    suspend fun updateExam(exam: ExamEntity)

    @Query("DELETE FROM exams WHERE id = :id")
    suspend fun deleteExamById(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamSubjects(crossRefs: List<ExamSubjectCrossRefEntity>)

    @Query("SELECT subjectId FROM exam_subjects WHERE examId = :examId")
    suspend fun getSubjectIdsForExam(examId: String): List<String>

    @Query("SELECT subjectId FROM exam_subjects WHERE examId = :examId")
    fun observeSubjectIdsForExam(examId: String): Flow<List<String>>

    @Query("DELETE FROM exam_subjects WHERE examId = :examId")
    suspend fun deleteExamSubjects(examId: String)
}
