package com.studyos.app.data.repository

import com.studyos.app.core.database.dao.ExamResultDao
import com.studyos.app.core.database.entity.toDomain
import com.studyos.app.core.database.entity.toEntity
import com.studyos.app.domain.model.ExamResult
import com.studyos.app.domain.repository.ExamResultRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineExamResultRepository(
    private val examResultDao: ExamResultDao
) : ExamResultRepository {

    override suspend fun getResultForPaper(paperId: String): ExamResult? =
        examResultDao.getResultForPaper(paperId)?.toDomain()

    override fun observeResultForPaper(paperId: String): Flow<ExamResult?> =
        examResultDao.observeResultForPaper(paperId).map { it?.toDomain() }

    override fun getAllResults(): Flow<List<ExamResult>> =
        examResultDao.getAllResults().map { list -> list.map { it.toDomain() } }

    override suspend fun getResultById(id: String): ExamResult? =
        examResultDao.getResultById(id)?.toDomain()

    override suspend fun saveResult(result: ExamResult) {
        examResultDao.insertResult(result.toEntity())
    }

    override suspend fun deleteResult(result: ExamResult) {
        examResultDao.deleteResult(result.toEntity())
    }

    override fun getOverallAveragePercentage(): Flow<Double?> =
        examResultDao.getOverallAveragePercentage()
}
