package com.studyos.app.domain.repository

import com.studyos.app.domain.model.ActiveRecallSessionSummary
import com.studyos.app.domain.model.RevisionSchedule
import kotlinx.coroutines.flow.Flow

interface RevisionRepository {
    fun observeDueSchedules(currentTime: Long = System.currentTimeMillis()): Flow<List<RevisionSchedule>>
    fun observeAllSchedules(): Flow<List<RevisionSchedule>>
    fun observeScheduleForChapter(chapterId: String): Flow<RevisionSchedule?>
    suspend fun getScheduleForChapter(chapterId: String): RevisionSchedule?
    suspend fun saveSchedule(schedule: RevisionSchedule)
    suspend fun recordChapterRevisionCompleted(chapterId: String, subjectId: String, qualityScore: Int)
    suspend fun recordRecallSession(summary: ActiveRecallSessionSummary): Int
    fun observeMinutesRevisedToday(): Flow<Int>
    val revisionStreakDays: Flow<Int>
    val dailyRevisionTargetMinutes: Flow<Int>
    suspend fun setDailyRevisionTargetMinutes(minutes: Int)
}
