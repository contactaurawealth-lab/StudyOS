package com.studyos.app.domain.repository

import com.studyos.app.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTasks(): Flow<List<Task>>
    fun observeTodayTasks(startOfDay: Long, endOfDay: Long): Flow<List<Task>>
    fun observeUpcomingTasks(endOfDay: Long): Flow<List<Task>>
    fun observeCompletedTasks(): Flow<List<Task>>
    fun observeRelevantTasks(startOfDay: Long, endOfDay: Long, now: Long, limit: Int = 5): Flow<List<Task>>
    fun getTaskById(id: String): Flow<Task?>
    suspend fun getTaskByIdOnce(id: String): Task?
    suspend fun createTask(task: Task): Task
    suspend fun updateTask(task: Task)
    suspend fun toggleTaskCompletion(id: String)
    suspend fun deleteTask(id: String)
}
