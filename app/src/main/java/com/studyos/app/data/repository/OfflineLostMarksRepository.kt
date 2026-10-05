package com.studyos.app.data.repository

import com.studyos.app.core.database.dao.CategoryLostMarksTuple
import com.studyos.app.core.database.dao.LostMarksDao
import com.studyos.app.core.database.entity.toDomain
import com.studyos.app.core.database.entity.toEntity
import com.studyos.app.domain.model.LossReasonCategory
import com.studyos.app.domain.model.LostMark
import com.studyos.app.domain.repository.LostMarksRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineLostMarksRepository(
    private val lostMarksDao: LostMarksDao
) : LostMarksRepository {

    override fun getLostMarksForResult(resultId: String): Flow<List<LostMark>> =
        lostMarksDao.getLostMarksForResult(resultId).map { list -> list.map { it.toDomain() } }

    override suspend fun getLostMarksForResultOnce(resultId: String): List<LostMark> =
        lostMarksDao.getLostMarksForResultOnce(resultId).map { it.toDomain() }

    override fun getLostMarksForTopic(topicId: String): Flow<List<LostMark>> =
        lostMarksDao.getLostMarksForTopic(topicId).map { list -> list.map { it.toDomain() } }

    override fun getLostMarksByCategory(category: LossReasonCategory): Flow<List<LostMark>> =
        lostMarksDao.getLostMarksByCategory(category.name).map { list -> list.map { it.toDomain() } }

    override fun getAllLostMarks(): Flow<List<LostMark>> =
        lostMarksDao.getAllLostMarks().map { list -> list.map { it.toDomain() } }

    override fun getLostMarksCategoryBreakdown(): Flow<List<CategoryLostMarksTuple>> =
        lostMarksDao.getLostMarksCategoryBreakdown()

    override suspend fun saveLostMark(lostMark: LostMark) {
        lostMarksDao.insertLostMark(lostMark.toEntity())
    }

    override suspend fun saveLostMarks(lostMarks: List<LostMark>) {
        lostMarksDao.insertLostMarks(lostMarks.map { it.toEntity() })
    }

    override suspend fun updateRemediation(id: String, remediated: Boolean) {
        lostMarksDao.updateRemediation(id, remediated)
    }

    override suspend fun deleteLostMark(lostMark: LostMark) {
        lostMarksDao.deleteLostMark(lostMark.toEntity())
    }
}
