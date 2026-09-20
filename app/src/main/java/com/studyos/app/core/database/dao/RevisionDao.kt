package com.studyos.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.studyos.app.core.database.entity.ActiveRecallLogEntity
import com.studyos.app.core.database.entity.RevisionScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RevisionDao {

    @Query("SELECT * FROM revision_schedules WHERE chapterId = :chapterId LIMIT 1")
    fun getScheduleForChapter(chapterId: String): Flow<RevisionScheduleEntity?>

    @Query("SELECT * FROM revision_schedules WHERE chapterId = :chapterId LIMIT 1")
    suspend fun getScheduleForChapterOnce(chapterId: String): RevisionScheduleEntity?

    @Query("SELECT * FROM revision_schedules ORDER BY nextRevisionDue ASC")
    fun observeAllSchedules(): Flow<List<RevisionScheduleEntity>>

    @Query("SELECT * FROM revision_schedules WHERE nextRevisionDue <= :currentTime ORDER BY nextRevisionDue ASC")
    fun observeDueSchedules(currentTime: Long): Flow<List<RevisionScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSchedule(schedule: RevisionScheduleEntity)

    @Query("DELETE FROM revision_schedules WHERE chapterId = :chapterId")
    suspend fun deleteSchedule(chapterId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecallLog(log: ActiveRecallLogEntity)

    @Query("SELECT * FROM active_recall_logs ORDER BY completedAt DESC LIMIT :limit")
    fun observeRecentRecallLogs(limit: Int): Flow<List<ActiveRecallLogEntity>>

    @Query("SELECT SUM(durationSeconds) / 60 FROM active_recall_logs WHERE completedAt BETWEEN :startOfDay AND :endOfDay")
    fun getMinutesRevisedToday(startOfDay: Long, endOfDay: Long): Flow<Int?>
}
