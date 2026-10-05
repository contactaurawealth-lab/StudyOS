package com.studyos.app.domain.repository

import com.studyos.app.domain.model.ExamResult
import kotlinx.coroutines.flow.Flow

interface ExamResultRepository {
    suspend fun getResultForPaper(paperId: String): ExamResult?
    fun observeResultForPaper(paperId: String): Flow<ExamResult?>
    fun getAllResults(): Flow<List<ExamResult>>
    suspend fun getResultById(id: String): ExamResult?
    suspend fun saveResult(result: ExamResult)
    suspend fun deleteResult(result: ExamResult)
    fun getOverallAveragePercentage(): Flow<Double?>
}
