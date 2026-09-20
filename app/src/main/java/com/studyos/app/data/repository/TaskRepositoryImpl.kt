package com.studyos.app.data.repository

import com.studyos.app.core.database.dao.TaskDao
import com.studyos.app.core.database.entity.toDomain
import com.studyos.app.core.database.entity.toEntity
import com.studyos.app.domain.model.Task
import com.studyos.app.domain.model.TaskStatus
import com.studyos.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRepositoryImpl(
    private val taskDao: TaskDao
) : TaskRepository {

    override fun observeTasks(): Flow<List<Task>> {
        return taskDao.observeTasks().map { list -> list.map { it.toDomain() } }
    }

    override fun observeTodayTasks(startOfDay: Long, endOfDay: Long): Flow<List<Task>> {
        return taskDao.observeTodayTasks(startOfDay, endOfDay).map { list -> list.map { it.toDomain() } }
    }

    override fun observeUpcomingTasks(endOfDay: Long): Flow<List<Task>> {
        return taskDao.observeUpcomingTasks(endOfDay).map { list -> list.map { it.toDomain() } }
    }

    override fun observeCompletedTasks(): Flow<List<Task>> {
        return taskDao.observeCompletedTasks().map { list -> list.map { it.toDomain() } }
    }

    override fun observeRelevantTasks(startOfDay: Long, endOfDay: Long, now: Long, limit: Int): Flow<List<Task>> {
        return taskDao.observeRelevantTasks(startOfDay, endOfDay, now, limit).map { list -> list.map { it.toDomain() } }
    }

    override fun getTaskById(id: String): Flow<Task?> {
        return taskDao.getTaskById(id).map { it?.toDomain() }
    }

    override suspend fun getTaskByIdOnce(id: String): Task? {
        return taskDao.getTaskByIdOnce(id)?.toDomain()
    }

    override suspend fun createTask(task: Task): Task {
        taskDao.insert(task.toEntity())
        return task
    }

    override suspend fun updateTask(task: Task) {
        taskDao.update(task.copy(updatedAt = System.currentTimeMillis()).toEntity())
    }

    override suspend fun toggleTaskCompletion(id: String) {
        val task = taskDao.getTaskByIdOnce(id)?.toDomain() ?: return
        val newStatus = if (task.status == TaskStatus.COMPLETED) TaskStatus.TODO else TaskStatus.COMPLETED
        taskDao.updateStatus(id, newStatus.name, System.currentTimeMillis())
    }

    override suspend fun deleteTask(id: String) {
        taskDao.deleteById(id)
    }
}
