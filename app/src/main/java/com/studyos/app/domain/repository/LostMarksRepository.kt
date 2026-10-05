package com.studyos.app.domain.repository

import com.studyos.app.core.database.dao.CategoryLostMarksTuple
import com.studyos.app.domain.model.LossReasonCategory
import com.studyos.app.domain.model.LostMark
import kotlinx.coroutines.flow.Flow

interface LostMarksRepository {
    fun getLostMarksForResult(resultId: String): Flow<List<LostMark>>
    suspend fun getLostMarksForResultOnce(resultId: String): List<LostMark>
    fun getLostMarksForTopic(topicId: String): Flow<List<LostMark>>
    fun getLostMarksByCategory(category: LossReasonCategory): Flow<List<LostMark>>
    fun getAllLostMarks(): Flow<List<LostMark>>
    fun getLostMarksCategoryBreakdown(): Flow<List<CategoryLostMarksTuple>>
    suspend fun saveLostMark(lostMark: LostMark)
    suspend fun saveLostMarks(lostMarks: List<LostMark>)
    suspend fun updateRemediation(id: String, remediated: Boolean)
    suspend fun deleteLostMark(lostMark: LostMark)
}
