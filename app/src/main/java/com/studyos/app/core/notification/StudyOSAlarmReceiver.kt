package com.studyos.app.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.studyos.app.StudyOSApplication
import com.studyos.app.core.database.entity.NotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StudyOSAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(AlarmScheduler.EXTRA_NOTIFICATION_ID, 1001)
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: "📚 StudyOS Reminder"
        val message = intent.getStringExtra(AlarmScheduler.EXTRA_MESSAGE) ?: "Time to focus on your studies."
        val route = intent.getStringExtra(AlarmScheduler.EXTRA_ROUTE)
        val subjectId = intent.getStringExtra(AlarmScheduler.EXTRA_SUBJECT_ID)
        val chapterId = intent.getStringExtra(AlarmScheduler.EXTRA_CHAPTER_ID)
        val sessionId = intent.getStringExtra(AlarmScheduler.EXTRA_SESSION_ID)
        val isDaily = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_DAILY, false)
        val dailyHour = intent.getIntExtra(AlarmScheduler.EXTRA_DAILY_HOUR, 19)
        val dailyMinute = intent.getIntExtra(AlarmScheduler.EXTRA_DAILY_MINUTE, 0)

        // If this was a daily reminder, schedule the next day's alarm
        if (isDaily) {
            val scheduler = AlarmScheduler(context)
            scheduler.scheduleDailyReminder(dailyHour, dailyMinute)
        }

        // Show local native notification
        StudyOSNotificationManager.showNotification(
            context = context,
            notificationId = notificationId,
            title = title,
            message = message,
            route = route,
            subjectId = subjectId,
            chapterId = chapterId,
            sessionId = sessionId
        )

        // Persist notification record in Room database and check for smart revisions
        val app = context.applicationContext as? StudyOSApplication
        app?.let { application ->
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = application.container.database
                    db.notificationDao().insert(
                        NotificationEntity(
                            title = title,
                            message = message,
                            scheduledTime = System.currentTimeMillis(),
                            isRead = false
                        )
                    )

                    // Check for due revisions if this is the daily reminder check
                    if (isDaily) {
                        val now = System.currentTimeMillis()
                        val dueCount = db.revisionDao().getDueCount(now)
                        if (dueCount > 0) {
                            StudyOSNotificationManager.showNotification(
                                context = context,
                                notificationId = 888_888,
                                title = "🔄 $dueCount Revisions Ready",
                                message = "Beat the forgetting curve: review your spaced repetition queue today.",
                                route = "revision"
                            )
                        }
                    }

                    // Trigger widget refresh
                    com.studyos.app.core.widget.DailyPlanWidgetProvider.triggerUpdate(context)
                } catch (e: Exception) {
                    // Non-critical logging
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
