package com.studyos.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.studyos.app.core.database.entity.NotificationEntity
import com.studyos.app.core.database.entity.ResourceEntity
import com.studyos.app.core.database.entity.StudyPlanEntity
import com.studyos.app.core.database.entity.TestAttemptEntity
import com.studyos.app.core.database.entity.TestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResourceDao {
    @Query("SELECT * FROM resources ORDER BY createdAt DESC")
    fun getAllResources(): Flow<List<ResourceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(resource: ResourceEntity)

    @Delete
    suspend fun delete(resource: ResourceEntity)
}

@Dao
interface TestDao {
    @Query("SELECT * FROM tests WHERE subjectId = :subjectId")
    fun getTestsForSubject(subjectId: String): Flow<List<TestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(test: TestEntity)
}

@Dao
interface TestAttemptDao {
    @Query("SELECT * FROM test_attempts WHERE testId = :testId")
    fun getAttemptsForTest(testId: String): Flow<List<TestAttemptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attempt: TestAttemptEntity)
}

@Dao
interface StudyPlanDao {
    @Query("SELECT * FROM study_plans WHERE isActive = 1 LIMIT 1")
    fun getActiveStudyPlan(): Flow<StudyPlanEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: StudyPlanEntity)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY scheduledTime DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM notifications")
    suspend fun clearAll()
}
