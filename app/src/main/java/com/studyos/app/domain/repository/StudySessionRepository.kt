package com.studyos.app.domain.repository

import com.studyos.app.domain.model.StudySession
import com.studyos.app.domain.model.StudySessionStatus
import kotlinx.coroutines.flow.Flow

interface StudySessionRepository {
    fun observeSessionsForDay(startOfDay: Long, endOfDay: Long): Flow<List<StudySession>>
    suspend fun getSessionsForDayOnce(startOfDay: Long, endOfDay: Long): List<StudySession>
    fun observeAllSessions(): Flow<List<StudySession>>
    fun observeCompletedMinutesForDay(startOfDay: Long, endOfDay: Long): Flow<Int>
    suspend fun getCompletedMinutesForDayOnce(startOfDay: Long, endOfDay: Long): Int
    fun getSessionById(id: String): Flow<StudySession?>
    suspend fun getSessionByIdOnce(id: String): StudySession?
    suspend fun createSession(session: StudySession): StudySession
    suspend fun updateSession(session: StudySession)
    suspend fun updateSessionStatus(id: String, status: StudySessionStatus, actualMinutes: Int? = null)
    suspend fun deleteSession(id: String)
}
