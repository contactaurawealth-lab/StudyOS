package com.studyos.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.studyos.app.core.database.dao.AiConversationDao
import com.studyos.app.core.database.dao.AiMessageDao
import com.studyos.app.core.database.dao.ChapterDao
import com.studyos.app.core.database.dao.ExamDao
import com.studyos.app.core.database.dao.FlashcardDao
import com.studyos.app.core.database.dao.MistakeDao
import com.studyos.app.core.database.dao.NoteDao
import com.studyos.app.core.database.dao.NotificationDao
import com.studyos.app.core.database.dao.QuizDao
import com.studyos.app.core.database.dao.ResourceDao
import com.studyos.app.core.database.dao.StudentDao
import com.studyos.app.core.database.dao.RevisionDao
import com.studyos.app.core.database.dao.StudyPlanDao
import com.studyos.app.core.database.dao.StudyPreferencesDao
import com.studyos.app.core.database.dao.StudySessionDao
import com.studyos.app.core.database.dao.SubjectDao
import com.studyos.app.core.database.dao.TaskDao
import com.studyos.app.core.database.dao.TestAttemptDao
import com.studyos.app.core.database.dao.TestDao
import com.studyos.app.core.database.entity.ActiveQuizStateEntity
import com.studyos.app.core.database.entity.ActiveRecallLogEntity
import com.studyos.app.core.database.entity.AiConversationEntity
import com.studyos.app.core.database.entity.AiMessageEntity
import com.studyos.app.core.database.entity.ChapterEntity
import com.studyos.app.core.database.entity.ExamEntity
import com.studyos.app.core.database.entity.ExamSubjectCrossRefEntity
import com.studyos.app.core.database.entity.FlashcardEntity
import com.studyos.app.core.database.entity.FlashcardReviewEntity
import com.studyos.app.core.database.entity.MistakeEntity
import com.studyos.app.core.database.entity.NoteEntity
import com.studyos.app.core.database.entity.NotificationEntity
import com.studyos.app.core.database.entity.QuestionResultEntity
import com.studyos.app.core.database.entity.QuizAttemptEntity
import com.studyos.app.core.database.entity.QuizEntity
import com.studyos.app.core.database.entity.QuizQuestionEntity
import com.studyos.app.core.database.entity.ResourceEntity
import com.studyos.app.core.database.entity.RevisionScheduleEntity
import com.studyos.app.core.database.entity.StudentEntity
import com.studyos.app.core.database.entity.StudyPlanEntity
import com.studyos.app.core.database.entity.StudyPreferencesEntity
import com.studyos.app.core.database.entity.StudySessionEntity
import com.studyos.app.core.database.dao.RecallDao
import com.studyos.app.core.database.entity.RecallAttemptEntity
import com.studyos.app.core.database.entity.RecallItemEntity
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
        FlashcardReviewEntity::class,
        QuizEntity::class,
        QuizQuestionEntity::class,
        QuizAttemptEntity::class,
        QuestionResultEntity::class,
        ActiveQuizStateEntity::class,
        MistakeEntity::class,
        TestEntity::class,
        TestAttemptEntity::class,
        ExamEntity::class,
        ExamSubjectCrossRefEntity::class,
        StudyPlanEntity::class,
        NotificationEntity::class,
        AiConversationEntity::class,
        AiMessageEntity::class,
        RevisionScheduleEntity::class,
        ActiveRecallLogEntity::class,
        RecallItemEntity::class,
        RecallAttemptEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class StudyOSDatabase : RoomDatabase() {

    abstract fun studentDao(): StudentDao
    abstract fun subjectDao(): SubjectDao
    abstract fun studyPreferencesDao(): StudyPreferencesDao
    abstract fun chapterDao(): ChapterDao
    abstract fun taskDao(): TaskDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun aiConversationDao(): AiConversationDao
    abstract fun aiMessageDao(): AiMessageDao
    abstract fun noteDao(): NoteDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun quizDao(): QuizDao
    abstract fun mistakeDao(): MistakeDao
    abstract fun examDao(): ExamDao
    abstract fun revisionDao(): RevisionDao
    abstract fun resourceDao(): ResourceDao
    abstract fun testDao(): TestDao
    abstract fun testAttemptDao(): TestAttemptDao
    abstract fun studyPlanDao(): StudyPlanDao
    abstract fun notificationDao(): NotificationDao
    abstract fun recallDao(): RecallDao

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

                db.execSQL("""
                    INSERT OR IGNORE INTO `chapters_new` (`id`, `subjectId`, `name`, `orderIndex`, `status`, `progress`, `createdAt`, `updatedAt`)
                    SELECT `id`, `subjectId`, `name`, `orderIndex`, `status`, 0, `createdAt`, `updatedAt` FROM `chapters`
                """.trimIndent())

                db.execSQL("DROP TABLE IF EXISTS `chapters`")
                db.execSQL("ALTER TABLE `chapters_new` RENAME TO `chapters`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_chapters_subjectId` ON `chapters` (`subjectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_chapters_orderIndex` ON `chapters` (`orderIndex`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `chapters` ADD COLUMN `lastOpenedAt` INTEGER DEFAULT NULL")
                } catch (e: Exception) {
                    // Column might already exist
                }
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `tasks` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT,
                        `subjectId` TEXT,
                        `chapterId` TEXT,
                        `dueAt` INTEGER,
                        `priority` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`chapterId`) REFERENCES `chapters`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """.trimIndent())

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_subjectId` ON `tasks` (`subjectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_chapterId` ON `tasks` (`chapterId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_dueAt` ON `tasks` (`dueAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_status` ON `tasks` (`status`)")

                try {
                    db.execSQL("ALTER TABLE `study_sessions` ADD COLUMN `scheduledStart` INTEGER DEFAULT NULL")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `study_sessions` ADD COLUMN `scheduledEnd` INTEGER DEFAULT NULL")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `study_sessions` ADD COLUMN `plannedMinutes` INTEGER NOT NULL DEFAULT 45")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `study_sessions` ADD COLUMN `actualMinutes` INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_sessions_scheduledStart` ON `study_sessions` (`scheduledStart`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `ai_conversations` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `subjectId` TEXT,
                        `chapterId` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`chapterId`) REFERENCES `chapters`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """.trimIndent())

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_conversations_subjectId` ON `ai_conversations` (`subjectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_conversations_chapterId` ON `ai_conversations` (`chapterId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_conversations_updatedAt` ON `ai_conversations` (`updatedAt`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `ai_messages` (
                        `id` TEXT NOT NULL,
                        `conversationId` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `errorMessage` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`conversationId`) REFERENCES `ai_conversations`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_messages_conversationId` ON `ai_messages` (`conversationId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_messages_createdAt` ON `ai_messages` (`createdAt`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Notes schema upgrade
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `notes_new` (
                        `id` TEXT NOT NULL,
                        `subjectId` TEXT,
                        `chapterId` TEXT,
                        `title` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `isPinned` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`chapterId`) REFERENCES `chapters`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT OR IGNORE INTO `notes_new` (`id`, `subjectId`, `title`, `content`, `createdAt`, `updatedAt`)
                    SELECT `id`, `subjectId`, `title`, `content`, `createdAt`, `updatedAt` FROM `notes`
                """.trimIndent())
                db.execSQL("DROP TABLE IF EXISTS `notes`")
                db.execSQL("ALTER TABLE `notes_new` RENAME TO `notes`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_subjectId` ON `notes` (`subjectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_chapterId` ON `notes` (`chapterId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_isPinned` ON `notes` (`isPinned`)")

                // 2. Flashcards schema upgrade
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `flashcards_new` (
                        `id` TEXT NOT NULL,
                        `subjectId` TEXT NOT NULL,
                        `chapterId` TEXT,
                        `question` TEXT NOT NULL,
                        `answer` TEXT NOT NULL,
                        `difficulty` TEXT NOT NULL DEFAULT 'MEDIUM',
                        `createdAt` INTEGER NOT NULL,
                        `lastReviewed` INTEGER,
                        `nextReview` INTEGER,
                        `reviewCount` INTEGER NOT NULL DEFAULT 0,
                        `intervalDays` INTEGER NOT NULL DEFAULT 0,
                        `easeFactor` REAL NOT NULL DEFAULT 2.5,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`chapterId`) REFERENCES `chapters`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT OR IGNORE INTO `flashcards_new` (`id`, `subjectId`, `chapterId`, `question`, `answer`, `createdAt`, `updatedAt`)
                    SELECT `id`, `subjectId`, `chapterId`, `front`, `back`, `createdAt`, `updatedAt` FROM `flashcards`
                """.trimIndent())
                db.execSQL("DROP TABLE IF EXISTS `flashcards`")
                db.execSQL("ALTER TABLE `flashcards_new` RENAME TO `flashcards`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_flashcards_subjectId` ON `flashcards` (`subjectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_flashcards_chapterId` ON `flashcards` (`chapterId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_flashcards_nextReview` ON `flashcards` (`nextReview`)")

                // 3. Flashcard reviews
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `flashcard_reviews` (
                        `id` TEXT NOT NULL,
                        `flashcardId` TEXT NOT NULL,
                        `rating` TEXT NOT NULL,
                        `reviewedAt` INTEGER NOT NULL,
                        `intervalAfterDays` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`flashcardId`) REFERENCES `flashcards`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_flashcard_reviews_flashcardId` ON `flashcard_reviews` (`flashcardId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_flashcard_reviews_reviewedAt` ON `flashcard_reviews` (`reviewedAt`)")

                // 4. Quizzes
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `quizzes` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `subjectId` TEXT NOT NULL,
                        `chapterId` TEXT,
                        `questionCount` INTEGER NOT NULL,
                        `difficulty` TEXT NOT NULL DEFAULT 'MEDIUM',
                        `timeLimitMinutes` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`chapterId`) REFERENCES `chapters`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_quizzes_subjectId` ON `quizzes` (`subjectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_quizzes_chapterId` ON `quizzes` (`chapterId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_quizzes_createdAt` ON `quizzes` (`createdAt`)")

                // 5. Quiz Questions
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `quiz_questions` (
                        `id` TEXT NOT NULL,
                        `quizId` TEXT NOT NULL,
                        `question` TEXT NOT NULL,
                        `optionsJson` TEXT NOT NULL,
                        `correctAnswer` TEXT NOT NULL,
                        `explanation` TEXT,
                        `questionType` TEXT NOT NULL DEFAULT 'MCQ',
                        `topic` TEXT,
                        `orderIndex` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`quizId`) REFERENCES `quizzes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_quiz_questions_quizId` ON `quiz_questions` (`quizId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_quiz_questions_orderIndex` ON `quiz_questions` (`orderIndex`)")

                // 6. Quiz Attempts
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `quiz_attempts` (
                        `id` TEXT NOT NULL,
                        `quizId` TEXT NOT NULL,
                        `subjectId` TEXT NOT NULL,
                        `chapterId` TEXT,
                        `startedAt` INTEGER NOT NULL,
                        `completedAt` INTEGER,
                        `score` INTEGER NOT NULL DEFAULT 0,
                        `totalQuestions` INTEGER NOT NULL DEFAULT 0,
                        `accuracyPercentage` INTEGER NOT NULL DEFAULT 0,
                        `timeSpentSeconds` INTEGER NOT NULL DEFAULT 0,
                        `isCompleted` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`quizId`) REFERENCES `quizzes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`chapterId`) REFERENCES `chapters`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_quiz_attempts_quizId` ON `quiz_attempts` (`quizId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_quiz_attempts_subjectId` ON `quiz_attempts` (`subjectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_quiz_attempts_chapterId` ON `quiz_attempts` (`chapterId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_quiz_attempts_startedAt` ON `quiz_attempts` (`startedAt`)")

                // 7. Question Results
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `question_results` (
                        `id` TEXT NOT NULL,
                        `attemptId` TEXT NOT NULL,
                        `questionId` TEXT NOT NULL,
                        `studentAnswer` TEXT NOT NULL,
                        `isCorrect` INTEGER NOT NULL,
                        `topic` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`attemptId`) REFERENCES `quiz_attempts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_question_results_attemptId` ON `question_results` (`attemptId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_question_results_questionId` ON `question_results` (`questionId`)")

                // 8. Active Quiz States
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `active_quiz_states` (
                        `quizId` TEXT NOT NULL,
                        `currentQuestionIndex` INTEGER NOT NULL DEFAULT 0,
                        `answersJson` TEXT NOT NULL DEFAULT '{}',
                        `elapsedSeconds` INTEGER NOT NULL DEFAULT 0,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`quizId`)
                    )
                """.trimIndent())

                // 9. Mistakes
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `mistakes` (
                        `id` TEXT NOT NULL,
                        `question` TEXT NOT NULL,
                        `studentAnswer` TEXT NOT NULL,
                        `correctAnswer` TEXT NOT NULL,
                        `explanation` TEXT,
                        `subjectId` TEXT NOT NULL,
                        `chapterId` TEXT,
                        `topic` TEXT,
                        `missedCount` INTEGER NOT NULL DEFAULT 1,
                        `lastMissedAt` INTEGER NOT NULL,
                        `isResolved` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`chapterId`) REFERENCES `chapters`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mistakes_subjectId` ON `mistakes` (`subjectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mistakes_chapterId` ON `mistakes` (`chapterId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mistakes_isResolved` ON `mistakes` (`isResolved`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mistakes_lastMissedAt` ON `mistakes` (`lastMissedAt`)")

                // 10. Exams & Exam Subjects
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `exams_new` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `date` INTEGER NOT NULL,
                        `targetScore` INTEGER,
                        `notes` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("DROP TABLE IF EXISTS `exams`")
                db.execSQL("ALTER TABLE `exams_new` RENAME TO `exams`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exams_date` ON `exams` (`date`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `exam_subjects` (
                        `examId` TEXT NOT NULL,
                        `subjectId` TEXT NOT NULL,
                        PRIMARY KEY(`examId`, `subjectId`),
                        FOREIGN KEY(`examId`) REFERENCES `exams`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exam_subjects_examId` ON `exam_subjects` (`examId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exam_subjects_subjectId` ON `exam_subjects` (`subjectId`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `revision_schedules` (
                        `chapterId` TEXT NOT NULL,
                        `subjectId` TEXT NOT NULL,
                        `intervalDays` INTEGER NOT NULL,
                        `revisionCount` INTEGER NOT NULL,
                        `lastRevisedAt` INTEGER,
                        `nextRevisionDue` INTEGER NOT NULL,
                        `easeFactor` REAL NOT NULL,
                        PRIMARY KEY(`chapterId`),
                        FOREIGN KEY(`chapterId`) REFERENCES `chapters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_revision_schedules_chapterId` ON `revision_schedules` (`chapterId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_revision_schedules_subjectId` ON `revision_schedules` (`subjectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_revision_schedules_nextRevisionDue` ON `revision_schedules` (`nextRevisionDue`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `active_recall_logs` (
                        `id` TEXT NOT NULL,
                        `sessionType` TEXT NOT NULL,
                        `durationSeconds` INTEGER NOT NULL,
                        `completedAt` INTEGER NOT NULL,
                        `itemsReviewedCount` INTEGER NOT NULL,
                        `correctCount` INTEGER NOT NULL,
                        `accuracyPercentage` INTEGER NOT NULL,
                        `streakDays` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_active_recall_logs_completedAt` ON `active_recall_logs` (`completedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_active_recall_logs_sessionType` ON `active_recall_logs` (`sessionType`)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `recall_items` (
                        `id` TEXT NOT NULL,
                        `chapterId` TEXT NOT NULL,
                        `subjectId` TEXT NOT NULL,
                        `questionType` TEXT NOT NULL,
                        `prompt` TEXT NOT NULL,
                        `expectedAnswer` TEXT NOT NULL,
                        `explanation` TEXT NOT NULL,
                        `optionsJson` TEXT NOT NULL,
                        `recallState` TEXT NOT NULL,
                        `lastStudiedTimestamp` INTEGER,
                        `lastRecalledTimestamp` INTEGER,
                        `recallAccuracy` INTEGER NOT NULL,
                        `recallAttempts` INTEGER NOT NULL,
                        `consecutiveCorrect` INTEGER NOT NULL,
                        `consecutiveIncorrect` INTEGER NOT NULL,
                        `confidence` REAL NOT NULL,
                        `intervalDays` INTEGER NOT NULL,
                        `nextReviewTimestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`chapterId`) REFERENCES `chapters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recall_items_chapterId` ON `recall_items` (`chapterId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recall_items_subjectId` ON `recall_items` (`subjectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recall_items_recallState` ON `recall_items` (`recallState`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recall_items_nextReviewTimestamp` ON `recall_items` (`nextReviewTimestamp`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `recall_attempts` (
                        `id` TEXT NOT NULL,
                        `recallItemId` TEXT NOT NULL,
                        `chapterId` TEXT NOT NULL,
                        `userAnswer` TEXT NOT NULL,
                        `wasCorrect` INTEGER NOT NULL,
                        `confidenceRating` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`recallItemId`) REFERENCES `recall_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recall_attempts_recallItemId` ON `recall_attempts` (`recallItemId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recall_attempts_chapterId` ON `recall_attempts` (`chapterId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recall_attempts_timestamp` ON `recall_attempts` (`timestamp`)")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `mistakes` ADD COLUMN `photoUri` TEXT DEFAULT NULL")
                } catch (e: Exception) {
                    // Column might already exist
                }
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `exams` ADD COLUMN `actualScore` INTEGER DEFAULT NULL")
                } catch (e: Exception) {
                    // Column might already exist
                }
                try {
                    db.execSQL("ALTER TABLE `exams` ADD COLUMN `isCompleted` INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // Column might already exist
                }
            }
        }

        fun getDatabase(context: Context): StudyOSDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudyOSDatabase::class.java,
                    "studyos_database.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
