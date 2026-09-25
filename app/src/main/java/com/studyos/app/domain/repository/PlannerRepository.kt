package com.studyos.app.domain.repository

import com.studyos.app.domain.model.StudySession
import kotlinx.coroutines.flow.Flow

interface PlannerRepository {
    fun observeSessionsForDay(startOfDay: Long, endOfDay: Long): Flow<List<StudySession>>
    fun observeSessionsForRange(startTime: Long, endTime: Long): Flow<List<StudySession>>
    fun observeUpcomingSessions(): Flow<List<StudySession>>
    suspend fun getSessionsForDayOnce(startOfDay: Long, endOfDay: Long): List<StudySession>
    suspend fun checkOverlap(startTime: Long, endTime: Long, excludeSessionId: String? = null): Boolean
    suspend fun createSession(session: StudySession): StudySession
    suspend fun updateSession(session: StudySession)
    suspend fun moveSession(sessionId: String, newScheduledStart: Long, newScheduledEnd: Long?)
    suspend fun deleteSession(sessionId: String)
}
