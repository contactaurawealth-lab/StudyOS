package com.studyos.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.studyos.app.core.database.dao.ChapterDao
import com.studyos.app.core.database.dao.ExamDao
import com.studyos.app.core.database.dao.FlashcardDao
import com.studyos.app.core.database.dao.NoteDao
import com.studyos.app.core.database.dao.NotificationDao
import com.studyos.app.core.database.dao.ResourceDao
import com.studyos.app.core.database.dao.StudentDao
import com.studyos.app.core.database.dao.StudyPlanDao
import com.studyos.app.core.database.dao.StudyPreferencesDao
import com.studyos.app.core.database.dao.StudySessionDao
import com.studyos.app.core.database.dao.SubjectDao
import com.studyos.app.core.database.dao.TaskDao
import com.studyos.app.core.database.dao.TestAttemptDao
import com.studyos.app.core.database.dao.TestDao
import com.studyos.app.core.database.entity.ChapterEntity
import com.studyos.app.core.database.entity.ExamEntity
import com.studyos.app.core.database.entity.FlashcardEntity
import com.studyos.app.core.database.entity.NoteEntity
import com.studyos.app.core.database.entity.NotificationEntity
import com.studyos.app.core.database.entity.ResourceEntity
import com.studyos.app.core.database.entity.StudentEntity
import com.studyos.app.core.database.entity.StudyPlanEntity
import com.studyos.app.core.database.entity.StudyPreferencesEntity
import com.studyos.app.core.database.entity.StudySessionEntity
import com.studyos.app.core.database.entity.SubjectEntity
import com.studyos.app.core.database.entity.TaskEntity
import com.studyos.app.core.database.entity.TestAttemptEntity
import com.studyos.app.core.database.entity.TestEntity

@Database(
    entities = [
        StudentEntity::class,
        SubjectEntity::class,
        StudyPreferencesEntity::class,
        ChapterEntity::class,
        TaskEntity::class,
        StudySessionEntity::class,
        NoteEntity::class,
        ResourceEntity::class,
        FlashcardEntity::class,
        TestEntity::class,
        TestAttemptEntity::class,
        ExamEntity::class,
        StudyPlanEntity::class,
        NotificationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class StudyOSDatabase : RoomDatabase() {

    abstract fun studentDao(): StudentDao
    abstract fun subjectDao(): SubjectDao
    abstract fun studyPreferencesDao(): StudyPreferencesDao
    abstract fun chapterDao(): ChapterDao
    abstract fun taskDao(): TaskDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun noteDao(): NoteDao
    abstract fun resourceDao(): ResourceDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun testDao(): TestDao
    abstract fun testAttemptDao(): TestAttemptDao
    abstract fun examDao(): ExamDao
    abstract fun studyPlanDao(): StudyPlanDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: StudyOSDatabase? = null

        fun getDatabase(context: Context): StudyOSDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudyOSDatabase::class.java,
                    "studyos_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
