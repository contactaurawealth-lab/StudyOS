package com.studyos.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studyos.app.core.database.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = 'TODO' AND dueAt >= :startOfDay AND dueAt <= :endOfDay ORDER BY dueAt ASC")
    fun observeTodayTasks(startOfDay: Long, endOfDay: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = 'TODO' AND dueAt > :endOfDay ORDER BY dueAt ASC")
    fun observeUpcomingTasks(endOfDay: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = 'COMPLETED' ORDER BY updatedAt DESC")
    fun observeCompletedTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    fun getTaskById(id: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskByIdOnce(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE status = 'TODO' ORDER BY CASE WHEN dueAt < :now THEN 0 WHEN dueAt >= :startOfDay AND dueAt <= :endOfDay THEN 1 WHEN priority = 'HIGH' THEN 2 WHEN dueAt IS NOT NULL THEN 3 ELSE 4 END ASC, dueAt ASC, createdAt ASC LIMIT :limit")
    fun observeRelevantTasks(startOfDay: Long, endOfDay: Long, now: Long, limit: Int): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("UPDATE tasks SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: String)
}
