package com.studyos.app.domain.repository

import com.studyos.app.domain.model.RecallAttempt
import com.studyos.app.domain.model.RecallDashboardSummary
import com.studyos.app.domain.model.RecallEvaluationResult
import com.studyos.app.domain.model.RecallItem
import kotlinx.coroutines.flow.Flow

interface RecallRepository {
    fun observeRecallItemsForChapter(chapterId: String): Flow<List<RecallItem>>
    suspend fun getRecallItemsForChapter(chapterId: String): List<RecallItem>
    fun observeDueRecallItems(currentTime: Long = System.currentTimeMillis()): Flow<List<RecallItem>>
    suspend fun getDueRecallItems(currentTime: Long = System.currentTimeMillis()): List<RecallItem>
    fun observeWeakRecallItems(): Flow<List<RecallItem>>
    suspend fun getWeakRecallItems(): List<RecallItem>
    fun observeMasteredRecallItems(): Flow<List<RecallItem>>
    suspend fun getMasteredRecallItems(): List<RecallItem>
    fun getDashboardSummary(): Flow<RecallDashboardSummary>
    suspend fun getDashboardSummaryOnce(): RecallDashboardSummary
    suspend fun getRecentAttemptsForChapter(chapterId: String, limit: Int = 20): List<RecallAttempt>
    suspend fun recordAttemptAndEvaluate(recallItemId: String, userAnswer: String, confidenceRating: Int = 3): RecallEvaluationResult
    suspend fun saveRecallItem(item: RecallItem)
    suspend fun saveRecallItems(items: List<RecallItem>)
    suspend fun ensureRecallItemsSeededForChapter(chapterId: String, subjectId: String, chapterName: String)
}
