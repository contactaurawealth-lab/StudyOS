package com.studyos.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studyos.app.core.database.entity.ExamResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamResultDao {

    @Query("SELECT * FROM exam_results WHERE paperId = :paperId LIMIT 1")
    suspend fun getResultForPaper(paperId: String): ExamResultEntity?

    @Query("SELECT * FROM exam_results WHERE paperId = :paperId LIMIT 1")
    fun observeResultForPaper(paperId: String): Flow<ExamResultEntity?>

    @Query("SELECT * FROM exam_results ORDER BY examDate DESC")
    fun getAllResults(): Flow<List<ExamResultEntity>>

    @Query("SELECT * FROM exam_results WHERE id = :id")
    suspend fun getResultById(id: String): ExamResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: ExamResultEntity)

    @Update
    suspend fun updateResult(result: ExamResultEntity)

    @Delete
    suspend fun deleteResult(result: ExamResultEntity)

    @Query("SELECT AVG(percentage) FROM exam_results")
    fun getOverallAveragePercentage(): Flow<Double?>
}
