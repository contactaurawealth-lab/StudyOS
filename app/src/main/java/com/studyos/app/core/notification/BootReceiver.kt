package com.studyos.app.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.studyos.app.StudyOSApplication
import com.studyos.app.core.database.entity.toDomain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != Intent.ACTION_TIME_CHANGED &&
            action != Intent.ACTION_TIMEZONE_CHANGED
        ) {
            return
        }

        val app = context.applicationContext as? StudyOSApplication ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val container = app.container
                val scheduler = AlarmScheduler(context)
                val now = System.currentTimeMillis()

                // 1. Check daily reminder preferences
                val dailyEnabled = container.preferencesDataSource.dailyReminderEnabled.firstOrNull() ?: true
                if (dailyEnabled) {
                    val dailyTimeStr = container.preferencesDataSource.dailyReminderTime.firstOrNull() ?: "19:00"
                    val parts = dailyTimeStr.split(":")
                    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 19
                    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                    scheduler.scheduleDailyReminder(hour, minute)
                }

                // 2. Check study reminders preferences
                val studyRemindersEnabled = container.preferencesDataSource.studyRemindersEnabled.firstOrNull() ?: true
                if (studyRemindersEnabled) {
                    val upcomingSessions = container.database.studySessionDao().getUpcomingSessionsOnce(now)

                    for (sessionEntity in upcomingSessions) {
                        val session = sessionEntity.toDomain()
                        val subId = session.subjectId
                        val subjectName = if (subId != null) {
                            container.subjectRepository.getSubjectByIdOnce(subId)?.name
                        } else null

                        val chapId = session.chapterId
                        val chapterName = if (chapId != null) {
                            container.chapterRepository.getChapterById(chapId)?.name
                        } else null

                        scheduler.scheduleSessionReminder(session, subjectName, chapterName)
                    }
                }

                // 3. Reschedule upcoming exam reminders
                val upcomingExams = container.database.examDao().getUpcomingExamsOnce(now)
                for (exam in upcomingExams) {
                    scheduler.scheduleExamReminder(exam.id, exam.name, exam.date)
                }

                // 4. Refresh home-screen widgets
                com.studyos.app.core.widget.DailyPlanWidgetProvider.triggerUpdate(context)
                com.studyos.app.core.widget.ExamCountdownWidgetProvider.triggerUpdate(context)
            } catch (e: Exception) {
                // Non-critical
            } finally {
                pendingResult.finish()
            }
        }
    }
}
