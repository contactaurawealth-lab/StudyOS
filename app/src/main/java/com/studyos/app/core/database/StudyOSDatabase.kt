package com.studyos.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
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

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `chapters_new` (
                        `id` TEXT NOT NULL,
                        `subjectId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT,
                        `orderIndex` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `progress` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())

                // Copy over from old chapters table if exists
                db.execSQL("""
                    INSERT INTO `chapters_new` (`id`, `subjectId`, `name`, `description`, `orderIndex`, `status`, `progress`, `createdAt`, `updatedAt`)
                    SELECT `id`, `subjectId`, `title`, NULL, `orderIndex`,
                           CASE WHEN `isCompleted` = 1 THEN 'COMPLETED' ELSE 'NOT_STARTED' END,
                           CASE WHEN `isCompleted` = 1 THEN 100 ELSE 0 END,
                           `createdAt`, `updatedAt`
                    FROM `chapters`
                """.trimIndent())

                db.execSQL("DROP TABLE `chapters`")
                db.execSQL("ALTER TABLE `chapters_new` RENAME TO `chapters`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_chapters_subjectId` ON `chapters` (`subjectId`)")
            }
        }

        fun getDatabase(context: Context): StudyOSDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudyOSDatabase::class.java,
                    "studyos_database.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
