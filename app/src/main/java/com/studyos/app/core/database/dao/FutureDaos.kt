package com.studyos.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studyos.app.core.database.entity.ExamEntity
import com.studyos.app.core.database.entity.FlashcardEntity
import com.studyos.app.core.database.entity.NoteEntity
import com.studyos.app.core.database.entity.NotificationEntity
import com.studyos.app.core.database.entity.ResourceEntity
import com.studyos.app.core.database.entity.StudyPlanEntity
import com.studyos.app.core.database.entity.StudySessionEntity
import com.studyos.app.core.database.entity.TaskEntity
import com.studyos.app.core.database.entity.TestAttemptEntity
import com.studyos.app.core.database.entity.TestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY dueDate ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)
}

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<StudySessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: StudySessionEntity)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)
}

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
interface FlashcardDao {
    @Query("SELECT * FROM flashcards WHERE subjectId = :subjectId")
    fun getFlashcardsForSubject(subjectId: String): Flow<List<FlashcardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(flashcard: FlashcardEntity)

    @Delete
    suspend fun delete(flashcard: FlashcardEntity)
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
interface ExamDao {
    @Query("SELECT * FROM exams ORDER BY examDate ASC")
    fun getAllExams(): Flow<List<ExamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exam: ExamEntity)
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)
}
