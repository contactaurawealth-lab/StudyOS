package com.studyos.app.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.studyos.app.domain.model.StudySession
import com.studyos.app.navigation.Screen
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun scheduleSessionReminder(
        session: StudySession,
        subjectName: String?,
        chapterName: String?
    ) {
        val startTime = session.scheduledStart ?: return
        if (startTime <= System.currentTimeMillis()) return

        val title = if (!subjectName.isNullOrBlank()) {
            "📚 Time to study $subjectName"
        } else {
            "📚 Time to study: ${session.title}"
        }

        val message = when {
            !chapterName.isNullOrBlank() -> "Chapter: $chapterName • ${session.plannedMinutes} mins planned"
            else -> "${session.plannedMinutes} mins study session"
        }

        val route = when {
            !session.chapterId.isNullOrBlank() -> Screen.ChapterDetail.createRoute(session.chapterId)
            !session.subjectId.isNullOrBlank() -> Screen.SubjectDetail.createRoute(session.subjectId)
            else -> Screen.StudySession.createRoute(session.id)
        }

        val requestCode = getSessionRequestCode(session.id)

        val intent = Intent(context, StudyOSAlarmReceiver::class.java).apply {
            action = ACTION_STUDY_REMINDER
            putExtra(EXTRA_NOTIFICATION_ID, requestCode)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_ROUTE, route)
            putExtra(EXTRA_SUBJECT_ID, session.subjectId)
            putExtra(EXTRA_CHAPTER_ID, session.chapterId)
            putExtra(EXTRA_SESSION_ID, session.id)
            putExtra(EXTRA_IS_DAILY, false)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarm(startTime, pendingIntent)
    }

    fun cancelSessionReminder(sessionId: String) {
        val requestCode = getSessionRequestCode(sessionId)
        val intent = Intent(context, StudyOSAlarmReceiver::class.java).apply {
            action = ACTION_STUDY_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun scheduleDailyReminder(hour: Int, minute: Int) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis() + 60_000) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val title = "📚 Daily Study Check-in"
        val message = "Review your study plan and stay on track today."
        val route = Screen.Today.route

        val intent = Intent(context, StudyOSAlarmReceiver::class.java).apply {
            action = ACTION_STUDY_REMINDER
            putExtra(EXTRA_NOTIFICATION_ID, DAILY_REMINDER_REQUEST_CODE)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_ROUTE, route)
            putExtra(EXTRA_IS_DAILY, true)
            putExtra(EXTRA_DAILY_HOUR, hour)
            putExtra(EXTRA_DAILY_MINUTE, minute)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarm(calendar.timeInMillis, pendingIntent)
    }

    fun cancelDailyReminder() {
        val intent = Intent(context, StudyOSAlarmReceiver::class.java).apply {
            action = ACTION_STUDY_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun scheduleRevisionReminder(
        chapterId: String,
        subjectName: String,
        chapterName: String,
        triggerAtMillis: Long
    ) {
        if (triggerAtMillis <= System.currentTimeMillis()) return

        val title = "🔄 Time to revise $subjectName"
        val message = "📖 Continue $chapterName"
        val route = Screen.ChapterDetail.createRoute(chapterId)
        val requestCode = getRevisionRequestCode(chapterId)

        val intent = Intent(context, StudyOSAlarmReceiver::class.java).apply {
            action = ACTION_STUDY_REMINDER
            putExtra(EXTRA_NOTIFICATION_ID, requestCode)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_ROUTE, route)
            putExtra(EXTRA_CHAPTER_ID, chapterId)
            putExtra(EXTRA_IS_DAILY, false)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarm(triggerAtMillis, pendingIntent)
    }

    fun cancelRevisionReminder(chapterId: String) {
        val requestCode = getRevisionRequestCode(chapterId)
        val intent = Intent(context, StudyOSAlarmReceiver::class.java).apply {
            action = ACTION_STUDY_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun setAlarm(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        val am = alarmManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            // Fallback for security restriction
            am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } catch (e: Exception) {
            // Log or ignore gracefully
        }
    }

    private fun getSessionRequestCode(sessionId: String): Int {
        return (sessionId.hashCode() and 0x7FFFFFFF) % 1_000_000
    }

    private fun getRevisionRequestCode(chapterId: String): Int {
        return ((chapterId.hashCode() and 0x7FFFFFFF) % 1_000_000) + 1_000_000
    }

    companion object {
        const val ACTION_STUDY_REMINDER = "com.studyos.app.ACTION_STUDY_REMINDER"
        const val DAILY_REMINDER_REQUEST_CODE = 999_999

        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_ROUTE = "route"
        const val EXTRA_SUBJECT_ID = "subject_id"
        const val EXTRA_CHAPTER_ID = "chapter_id"
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_IS_DAILY = "is_daily"
        const val EXTRA_DAILY_HOUR = "daily_hour"
        const val EXTRA_DAILY_MINUTE = "daily_minute"
    }
}
