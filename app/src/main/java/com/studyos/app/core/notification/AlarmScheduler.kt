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

    fun scheduleExamReminder(
        examId: String,
        examName: String,
        examTime: Long,
        isMockTest: Boolean = false
    ) {
        val now = System.currentTimeMillis()
        if (examTime <= now) return

        val dateFormat = java.text.SimpleDateFormat("EEEE, MMMM d 'at' hh:mm a", java.util.Locale.getDefault())
        val formattedTime = dateFormat.format(java.util.Date(examTime))

        // 1. 24 hours prior reminder
        val reminder24h = examTime - 24 * 60 * 60 * 1000L
        if (reminder24h > now) {
            val reqCode24 = getExamRequestCode(examId, offsetType = 0)
            val intent24 = Intent(context, StudyOSAlarmReceiver::class.java).apply {
                action = ACTION_STUDY_REMINDER
                putExtra(EXTRA_NOTIFICATION_ID, reqCode24)
                putExtra(EXTRA_TITLE, if (isMockTest) "🎯 Mock Test Tomorrow: $examName" else "📝 Exam Tomorrow: $examName")
                putExtra(EXTRA_MESSAGE, "Scheduled for $formattedTime. Review your formulas & key concepts.")
                putExtra(EXTRA_ROUTE, "exams/$examId")
            }
            val pi24 = PendingIntent.getBroadcast(
                context,
                reqCode24,
                intent24,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setAlarm(reminder24h, pi24)
        } else if (examTime - now > 4 * 60 * 60 * 1000L) {
            // Schedule an interim reminder 4 hours prior if created within 24h
            val reminder4h = examTime - 4 * 60 * 60 * 1000L
            if (reminder4h > now) {
                val reqCode4 = getExamRequestCode(examId, offsetType = 0)
                val intent4 = Intent(context, StudyOSAlarmReceiver::class.java).apply {
                    action = ACTION_STUDY_REMINDER
                    putExtra(EXTRA_NOTIFICATION_ID, reqCode4)
                    putExtra(EXTRA_TITLE, if (isMockTest) "🎯 Mock Test Today: $examName" else "📝 Exam Today: $examName")
                    putExtra(EXTRA_MESSAGE, "Starts at $formattedTime. Final formula check!")
                    putExtra(EXTRA_ROUTE, "exams/$examId")
                }
                val pi4 = PendingIntent.getBroadcast(
                    context,
                    reqCode4,
                    intent4,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setAlarm(reminder4h, pi4)
            }
        }

        // 2. 1 hour prior reminder
        val reminder1h = examTime - 60 * 60 * 1000L
        if (reminder1h > now) {
            val reqCode1 = getExamRequestCode(examId, offsetType = 1)
            val intent1 = Intent(context, StudyOSAlarmReceiver::class.java).apply {
                action = ACTION_STUDY_REMINDER
                putExtra(EXTRA_NOTIFICATION_ID, reqCode1)
                putExtra(EXTRA_TITLE, "⏱️ Starting in 1 Hour: $examName")
                putExtra(EXTRA_MESSAGE, "Get your desk ready, silence distractions. You've got this!")
                putExtra(EXTRA_ROUTE, "exams/$examId")
            }
            val pi1 = PendingIntent.getBroadcast(
                context,
                reqCode1,
                intent1,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setAlarm(reminder1h, pi1)
        }

        // 3. Exact exam start time alarm
        val reqCodeStart = getExamRequestCode(examId, offsetType = 2)
        val intentStart = Intent(context, StudyOSAlarmReceiver::class.java).apply {
            action = ACTION_STUDY_REMINDER
            putExtra(EXTRA_NOTIFICATION_ID, reqCodeStart)
            putExtra(EXTRA_TITLE, if (isMockTest) "🔔 Mock Test Starts Now: $examName" else "🔔 Exam Starts Now: $examName")
            putExtra(EXTRA_MESSAGE, "Test session is open. Stay calm, read questions carefully, and do your best.")
            putExtra(EXTRA_ROUTE, "exams/$examId")
        }
        val piStart = PendingIntent.getBroadcast(
            context,
            reqCodeStart,
            intentStart,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setAlarm(examTime, piStart)
    }

    fun cancelExamReminder(examId: String) {
        listOf(0, 1, 2).forEach { offsetType ->
            val reqCode = getExamRequestCode(examId, offsetType = offsetType)
            val pi = PendingIntent.getBroadcast(
                context,
                reqCode,
                Intent(context, StudyOSAlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pi != null) {
                alarmManager?.cancel(pi)
                pi.cancel()
            }
        }
    }

    fun scheduleSnooze(
        title: String,
        message: String,
        route: String?,
        subjectId: String?,
        chapterId: String?,
        sessionId: String?,
        snoozeMinutes: Int = 10
    ) {
        val triggerTime = System.currentTimeMillis() + snoozeMinutes * 60 * 1000L
        val requestCode = (System.currentTimeMillis() % 1_000_000).toInt() + 4_000_000

        val intent = Intent(context, StudyOSAlarmReceiver::class.java).apply {
            action = ACTION_STUDY_REMINDER
            putExtra(EXTRA_NOTIFICATION_ID, requestCode)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_ROUTE, route)
            putExtra(EXTRA_SUBJECT_ID, subjectId)
            putExtra(EXTRA_CHAPTER_ID, chapterId)
            putExtra(EXTRA_SESSION_ID, sessionId)
            putExtra(EXTRA_IS_DAILY, false)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarm(triggerTime, pendingIntent)
    }

    fun scheduleTaskReminder(
        taskId: String,
        taskTitle: String,
        dueAt: Long
    ) {
        if (dueAt <= System.currentTimeMillis()) return
        val title = "⚡ Task Due: $taskTitle"
        val message = "Tap to view and complete your task."
        val route = Screen.Tasks.route
        val requestCode = getTaskRequestCode(taskId)

        val intent = Intent(context, StudyOSAlarmReceiver::class.java).apply {
            action = ACTION_STUDY_REMINDER
            putExtra(EXTRA_NOTIFICATION_ID, requestCode)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_ROUTE, route)
            putExtra(EXTRA_IS_DAILY, false)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarm(dueAt, pendingIntent)
    }

    fun cancelTaskReminder(taskId: String) {
        val requestCode = getTaskRequestCode(taskId)
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

    private fun getSessionRequestCode(sessionId: String): Int {
        return (sessionId.hashCode() and 0x7FFFFFFF) % 1_000_000
    }

    private fun getRevisionRequestCode(chapterId: String): Int {
        return ((chapterId.hashCode() and 0x7FFFFFFF) % 1_000_000) + 1_000_000
    }

    private fun getExamRequestCode(examId: String, offsetType: Int): Int {
        val base = (examId.hashCode() and 0x7FFFFFFF) % 1_000_000
        return base + 2_000_000 + (offsetType * 300_000)
    }

    private fun getTaskRequestCode(taskId: String): Int {
        return ((taskId.hashCode() and 0x7FFFFFFF) % 1_000_000) + 4_000_000
    }

    companion object {
        const val ACTION_STUDY_REMINDER = "com.studyos.app.ACTION_STUDY_REMINDER"
        const val ACTION_SNOOZE_REMINDER = "com.studyos.app.ACTION_SNOOZE_REMINDER"
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
