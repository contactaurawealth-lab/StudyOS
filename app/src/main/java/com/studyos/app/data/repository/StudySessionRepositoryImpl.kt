package com.studyos.app.data.repository

import com.studyos.app.core.database.dao.StudySessionDao
import com.studyos.app.core.database.entity.toDomain
import com.studyos.app.core.database.entity.toEntity
import com.studyos.app.domain.model.StudySession
import com.studyos.app.domain.model.StudySessionStatus
import com.studyos.app.domain.repository.StudySessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StudySessionRepositoryImpl(
    private val studySessionDao: StudySessionDao
) : StudySessionRepository {

    override fun observeSessionsForDay(startOfDay: Long, endOfDay: Long): Flow<List<StudySession>> {
        return studySessionDao.observeSessionsForDay(startOfDay, endOfDay).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getSessionsForDayOnce(startOfDay: Long, endOfDay: Long): List<StudySession> {
        return studySessionDao.getSessionsForDayOnce(startOfDay, endOfDay).map { it.toDomain() }
    }

    override fun observeAllSessions(): Flow<List<StudySession>> {
        return studySessionDao.observeAllSessions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeCompletedMinutesForDay(startOfDay: Long, endOfDay: Long): Flow<Int> {
        return studySessionDao.observeCompletedMinutesForDay(startOfDay, endOfDay).map { it ?: 0 }
    }

    override suspend fun getCompletedMinutesForDayOnce(startOfDay: Long, endOfDay: Long): Int {
        return studySessionDao.getCompletedMinutesForDayOnce(startOfDay, endOfDay) ?: 0
    }

    override fun getSessionById(id: String): Flow<StudySession?> {
        return studySessionDao.getSessionById(id).map { it?.toDomain() }
    }

    override suspend fun getSessionByIdOnce(id: String): StudySession? {
        return studySessionDao.getSessionByIdOnce(id)?.toDomain()
    }

    override suspend fun createSession(session: StudySession): StudySession {
        studySessionDao.insert(session.toEntity())
        return session
    }

    override suspend fun updateSession(session: StudySession) {
        val updated = session.copy(updatedAt = System.currentTimeMillis())
        studySessionDao.update(updated.toEntity())
    }

    override suspend fun updateSessionStatus(
        id: String,
        status: StudySessionStatus,
        actualMinutes: Int?
    ) {
        val existing = studySessionDao.getSessionByIdOnce(id)?.toDomain() ?: return
        val updated = existing.copy(
            status = status,
            actualMinutes = actualMinutes ?: existing.actualMinutes,
            updatedAt = System.currentTimeMillis()
        )
        studySessionDao.update(updated.toEntity())
    }

    override suspend fun deleteSession(id: String) {
        studySessionDao.deleteById(id)
    }
}
