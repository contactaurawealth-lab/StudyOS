package com.studyos.app.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.studyos.app.MainActivity
import com.studyos.app.R
import com.studyos.app.navigation.Screen

object StudyOSNotificationManager {

    const val CHANNEL_ID = "study_reminders"
    const val CHANNEL_NAME = "Study reminders"
    const val CHANNEL_DESCRIPTION = "StudyOS reminders and study notifications."

    const val CHANNEL_TIMER_ID = "study_timer_ongoing"
    const val CHANNEL_TIMER_NAME = "Study Focus Timer"
    const val CHANNEL_TIMER_DESCRIPTION = "Ongoing study timer countdown and progress."

    const val TIMER_NOTIFICATION_ID = 2001
    const val TIMER_COMPLETED_NOTIFICATION_ID = 2002

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            
            val reminderChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager?.createNotificationChannel(reminderChannel)

            val timerChannel = NotificationChannel(
                CHANNEL_TIMER_ID,
                CHANNEL_TIMER_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = CHANNEL_TIMER_DESCRIPTION
                enableVibration(false)
                setShowBadge(false)
            }
            notificationManager?.createNotificationChannel(timerChannel)
        }
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        route: String? = null,
        subjectId: String? = null,
        chapterId: String? = null,
        sessionId: String? = null
    ) {
        createNotificationChannel(context)

        if (!areNotificationsEnabled(context)) {
            return
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            route?.let { putExtra(MainActivity.EXTRA_ROUTE, it) }
            subjectId?.let { putExtra(MainActivity.EXTRA_SUBJECT_ID, it) }
            chapterId?.let { putExtra(MainActivity.EXTRA_CHAPTER_ID, it) }
            sessionId?.let { putExtra(MainActivity.EXTRA_SESSION_ID, it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 350, 150, 350))

        if (route?.startsWith("exams") == true) {
            val examIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_ROUTE, route)
            }
            val examPendingIntent = PendingIntent.getActivity(
                context,
                notificationId + 500_000,
                examIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "📝 Open Exam", examPendingIntent)
        } else {
            // Action 1: "Start Timer"
            val timerIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_ROUTE, Screen.StudyTimer.route)
                putExtra(MainActivity.EXTRA_AUTO_START_TIMER, true)
                subjectId?.let { putExtra(MainActivity.EXTRA_SUBJECT_ID, it) }
                chapterId?.let { putExtra(MainActivity.EXTRA_CHAPTER_ID, it) }
            }
            val timerPendingIntent = PendingIntent.getActivity(
                context,
                notificationId + 500_000,
                timerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "▶️ Start Timer", timerPendingIntent)
        }

        // Action 2: "Snooze 10m"
        val snoozeIntent = Intent(context, StudyOSAlarmReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_SNOOZE_REMINDER
            putExtra(AlarmScheduler.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(AlarmScheduler.EXTRA_TITLE, title)
            putExtra(AlarmScheduler.EXTRA_MESSAGE, message)
            putExtra(AlarmScheduler.EXTRA_ROUTE, route)
            putExtra(AlarmScheduler.EXTRA_SUBJECT_ID, subjectId)
            putExtra(AlarmScheduler.EXTRA_CHAPTER_ID, chapterId)
            putExtra(AlarmScheduler.EXTRA_SESSION_ID, sessionId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 600_000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        builder.addAction(0, "⏰ Snooze 10m", snoozePendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted on Android 13+
        }
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(notificationId)
    }

    fun showOngoingTimerNotification(
        context: Context,
        title: String,
        timeFormatted: String,
        isPaused: Boolean
    ) {
        createNotificationChannel(context)
        if (!areNotificationsEnabled(context)) return

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_ROUTE, Screen.StudyTimer.route)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            TIMER_NOTIFICATION_ID,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = if (isPaused) "⏸️ Paused • $timeFormatted" else "⏱️ Active • $timeFormatted"

        val builder = NotificationCompat.Builder(context, CHANNEL_TIMER_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(statusText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(!isPaused)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(TIMER_NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted on Android 13+
        }
    }

    fun showTimerCompletedNotification(
        context: Context,
        title: String,
        message: String
    ) {
        createNotificationChannel(context)
        if (!areNotificationsEnabled(context)) return

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_ROUTE, Screen.StudyTimer.route)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            TIMER_COMPLETED_NOTIFICATION_ID,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(TIMER_COMPLETED_NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted on Android 13+
        }
    }

    fun cancelTimerNotification(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(TIMER_NOTIFICATION_ID)
    }

    fun playCompletionChimeAndVibrate(context: Context) {
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, notificationUri)
            ringtone?.play()
        } catch (e: Exception) {
            // Ignore ringtone failure
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 300, 150, 300), -1)
                }
            }
        } catch (e: Exception) {
            // Ignore vibrator failure
        }
    }
}
