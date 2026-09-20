package com.studyos.app.data.repository

import com.studyos.app.core.database.dao.StudySessionDao
import com.studyos.app.core.database.entity.toDomain
import com.studyos.app.core.database.entity.toEntity
import com.studyos.app.domain.model.StudySession
import com.studyos.app.domain.repository.PlannerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlannerRepositoryImpl(
    private val studySessionDao: StudySessionDao
) : PlannerRepository {

    override fun observeSessionsForDay(startOfDay: Long, endOfDay: Long): Flow<List<StudySession>> {
        return studySessionDao.observeSessionsForDay(startOfDay, endOfDay).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeSessionsForRange(startTime: Long, endTime: Long): Flow<List<StudySession>> {
        return studySessionDao.observeSessionsBetween(startTime, endTime).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeUpcomingSessions(): Flow<List<StudySession>> {
        val now = System.currentTimeMillis()
        return studySessionDao.observeUpcomingSessions(now).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getSessionsForDayOnce(startOfDay: Long, endOfDay: Long): List<StudySession> {
        return studySessionDao.getSessionsForDayOnce(startOfDay, endOfDay).map { it.toDomain() }
    }

    override suspend fun checkOverlap(
        startTime: Long,
        endTime: Long,
        excludeSessionId: String?
    ): Boolean {
        val conflicts = studySessionDao.getConflictingSessionsOnce(startTime, endTime, excludeSessionId)
        return conflicts.isNotEmpty()
    }

    override suspend fun createSession(session: StudySession): StudySession {
        studySessionDao.insert(session.toEntity())
        return session
    }

    override suspend fun updateSession(session: StudySession) {
        studySessionDao.update(session.copy(updatedAt = System.currentTimeMillis()).toEntity())
    }

    override suspend fun moveSession(
        sessionId: String,
        newScheduledStart: Long,
        newScheduledEnd: Long?
    ) {
        val session = studySessionDao.getSessionByIdOnce(sessionId)?.toDomain() ?: return
        val updated = session.copy(
            scheduledStart = newScheduledStart,
            scheduledEnd = newScheduledEnd ?: (newScheduledStart + session.plannedMinutes * 60_000L),
            updatedAt = System.currentTimeMillis()
        )
        studySessionDao.update(updated.toEntity())
    }

    override suspend fun deleteSession(sessionId: String) {
        studySessionDao.deleteById(sessionId)
    }
}
