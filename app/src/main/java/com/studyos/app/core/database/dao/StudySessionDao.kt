package com.studyos.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studyos.app.core.database.entity.StudySessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {

    @Query("SELECT * FROM study_sessions WHERE scheduledStart >= :startOfDay AND scheduledStart <= :endOfDay ORDER BY scheduledStart ASC")
    fun observeSessionsForDay(startOfDay: Long, endOfDay: Long): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE scheduledStart >= :startOfDay AND scheduledStart <= :endOfDay ORDER BY scheduledStart ASC")
    suspend fun getSessionsForDayOnce(startOfDay: Long, endOfDay: Long): List<StudySessionEntity>

    @Query("SELECT * FROM study_sessions WHERE scheduledStart >= :startTime AND scheduledStart <= :endTime ORDER BY scheduledStart ASC")
    fun observeSessionsBetween(startTime: Long, endTime: Long): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE scheduledStart >= :startTime AND scheduledStart <= :endTime ORDER BY scheduledStart ASC")
    suspend fun getSessionsBetweenOnce(startTime: Long, endTime: Long): List<StudySessionEntity>

    @Query("SELECT * FROM study_sessions WHERE (scheduledStart >= :now OR scheduledEnd >= :now) ORDER BY scheduledStart ASC")
    fun observeUpcomingSessions(now: Long): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE scheduledStart >= :now AND status = 'PLANNED' ORDER BY scheduledStart ASC")
    suspend fun getUpcomingSessionsOnce(now: Long): List<StudySessionEntity>

    @Query("SELECT * FROM study_sessions WHERE (id != :excludeSessionId OR :excludeSessionId IS NULL) AND scheduledStart < :end AND (CASE WHEN scheduledEnd IS NOT NULL THEN scheduledEnd ELSE scheduledStart + (plannedMinutes * 60000) END) > :start")
    suspend fun getConflictingSessionsOnce(start: Long, end: Long, excludeSessionId: String? = null): List<StudySessionEntity>

    @Query("SELECT * FROM study_sessions ORDER BY scheduledStart ASC")
    fun observeAllSessions(): Flow<List<StudySessionEntity>>

    @Query("SELECT SUM(actualMinutes) FROM study_sessions WHERE status = 'COMPLETED' AND scheduledStart >= :startOfDay AND scheduledStart <= :endOfDay")
    fun observeCompletedMinutesForDay(startOfDay: Long, endOfDay: Long): Flow<Int?>

    @Query("SELECT SUM(actualMinutes) FROM study_sessions WHERE status = 'COMPLETED' AND scheduledStart >= :startOfDay AND scheduledStart <= :endOfDay")
    suspend fun getCompletedMinutesForDayOnce(startOfDay: Long, endOfDay: Long): Int?

    @Query("SELECT * FROM study_sessions WHERE id = :id LIMIT 1")
    fun getSessionById(id: String): Flow<StudySessionEntity?>

    @Query("SELECT * FROM study_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionByIdOnce(id: String): StudySessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: StudySessionEntity)

    @Update
    suspend fun update(session: StudySessionEntity)

    @Delete
    suspend fun delete(session: StudySessionEntity)

    @Query("DELETE FROM study_sessions WHERE id = :id")
    suspend fun deleteById(id: String)
}
